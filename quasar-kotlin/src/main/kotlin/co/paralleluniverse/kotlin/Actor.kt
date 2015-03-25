/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.kotlin

import co.paralleluniverse.actors.KotlinActorSupport
import java.util.concurrent.TimeUnit
import co.paralleluniverse.actors.LifecycleMessage
import co.paralleluniverse.actors.Actor as JActor
import java.util.concurrent.TimeoutException
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.fibers.Fiber
import co.paralleluniverse.strands.SuspendableCallable
import co.paralleluniverse.strands.queues.QueueIterator

/**
 * Ported from {@link co.paralleluniverse.actors.SelectiveReceiveHelper}
 *
 * @author circlespainter
 */
public abstract class Actor : KotlinActorSupport<Any?, Any?>() {
    public companion object {
        private object DeferException : Exception()
        public object Timeout
    }

    protected var currentMessage: Any? = null

    /**
     * Higher-order selective receive
     */
    inline protected fun receive(proc: (Any?) -> Any?) {
        receive(-1, null, proc)
    }

    /**
     * Higher-order selective receive
     */
    inline protected fun receive(timeout: Long, unit: TimeUnit?, proc: (Any?) -> Any?) {
        assert(JActor.currentActor<Any?, Any?>() == null || JActor.currentActor<Any?, Any?>() == this)

        val mailbox = mailbox()

        checkThrownIn1()
        mailbox.maybeSetCurrentStrandAsOwner()

        val start = if (timeout > 0) System.nanoTime() else 0
        var now: Long
        var left = if (unit != null) unit.toNanos(timeout) else 0
        val deadline = start + left

        monitorResetSkippedMessages()
        var i: Int = 0
        val it: QueueIterator<Any> = mailboxQueue().iterator()
        while (true) {
            if (flightRecorder != null)
                record(1, "KotlinActor", "receive", "%s waiting for a message. %s", this, if (timeout > 0) "millis left: " + TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS) else "")

            mailbox.lock()

            if (it.hasNext()) {
                val m = it.next()
                mailbox.unlock()
                if (m == currentMessage) {
                    it.remove()
                    continue
                }

                record(1, "KotlinActor", "receive", "Received %s <- %s", this, m)
                monitorAddMessage()

                currentMessage = m
                try {
                    proc(m)
                    if (it.value() == m) // another call to receive from within the processor may have deleted n
                        it.remove()
                } catch (d: DeferException) {
                    // Skip
                } catch (e: Exception) {
                    if (it.value() == m) // another call to receive from within the processor may have deleted n
                        it.remove()
                    throw e
                } finally {
                    currentMessage = null
                }
                record(1, "KotlinActor", "receive", "%s skipped %s", this, m)
                monitorSkippedMessage()
            } else {
                try {
                    if (unit == null)
                        mailbox.await(i)
                    else if (timeout > 0) {
                        mailbox.await(i, left, TimeUnit.NANOSECONDS)

                        now = System.nanoTime()
                        left = deadline - now
                        if (left <= 0) {
                            record(1, "KotlinActor", "receive", "%s timed out.", this)
                            proc(Timeout)
                        }
                    }
                } finally {
                    mailbox.unlock()
                }
            }
        }
    }

    Suspendable protected fun defer() {
        throw DeferException;
    }
}

// A couple of top-level utils

Suspendable public fun spawn(a: JActor<*, *>): ActorRef<*> {
    [suppress("UNCHECKED_CAST")]
    Fiber(a as SuspendableCallable<Any>).start()
    return a.ref()
}

Suspendable public fun register(ref: String, v: JActor<*, *>): JActor<*, *> {
    return v.register(ref)
}

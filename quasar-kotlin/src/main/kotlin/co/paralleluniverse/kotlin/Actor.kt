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

/**
 * Ported from {@link co.paralleluniverse.actors.SelectiveReceiveHelper}
 *
 * @author circlespainter
 */
public abstract class Actor<Message, V> : KotlinActorSupport<Message, V>() {
    public class object {
        private object DeferException : Exception()
        public object Timeout
    }

    protected var currentMessage: Message? = null

    /**
     * Higher-order selective receive
     */
    inline protected fun receive(timeout: Long, unit: TimeUnit?, proc: (Any) -> Unit) {
        assert(JActor.currentActor<Message, V>() == null || JActor.currentActor<Message, V>() == this)

        val mailbox = mailbox()

        checkThrownIn1()

        mailbox.maybeSetCurrentStrandAsOwner()

        val start = if (timeout > 0) System.nanoTime() else 0
        var now: Long
        var left = if (unit != null) unit.toNanos(timeout) else 0
        val deadline = start + left

        monitorResetSkippedMessages()
        var n: Any? = null
        var i: Int = 0
        while (true) {
            if (flightRecorder != null)
                record(1, "KotlinActor", "rcv", "%s waiting for a message. %s", this, if (timeout > 0) "millis left: " + TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS) else "")

            mailbox.lock()
            n = mailbox.succ(n)

            if (n != null) {
                mailbox.unlock()
                val m = mailbox.value(n)
                if (m == currentMessage) {
                    mailbox.del(n)
                    continue
                }

                record(1, "KotlinActor", "rcv", "Received %s <- %s", this, m)
                monitorAddMessage()
                try {
                    if (m is LifecycleMessage) {
                        mailbox.del(n)
                        handleLifecycleMessage(m)
                    } else {
                        val msg: Message = m as Message
                        currentMessage = msg
                        try {
                            proc(msg)
                            if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                mailbox.del(n)
                        } catch (d: DeferException) {
                            // Skip
                        } catch (e: Exception) {
                            if (mailbox.value(n) == msg) // another call to receive from within the processor may have deleted n
                                mailbox.del(n)
                            throw e
                        } finally {
                            currentMessage = null
                        }
                        record(1, "KotlinActor", "rcv", "%s skipped %s", this, m)
                        monitorSkippedMessage()
                    }
                } catch (e: Exception) {
                    if (mailbox.value(n) == m) // another call to receive from within the processor may have deleted n
                        mailbox.del(n)
                    throw e
                }
            } else {
                try {
                    if (unit == null)
                        mailbox.await(i)
                    else if (timeout > 0) {
                        mailbox.await(i, left, TimeUnit.NANOSECONDS)

                        now = System.nanoTime()
                        left = deadline - now
                        if (left <= 0) {
                            record(1, "KotlinActor", "rcv", "%s timed out.", this)
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
    Fiber(a as SuspendableCallable<Object>).start()
    return a.ref()
}

Suspendable public fun register(ref: String, v: JActor<*, *>): JActor<*, *> {
    return v.register(ref)
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2017, Parallel Universe Software Co. All rights reserved.
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
@file:Suppress("PackageDirectoryMismatch")

package actors

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.actors.LocalActor
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.register
import co.paralleluniverse.kotlin.spawn
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * @author circlespainter
 * @author pron
 */

// This example is meant to be a translation of the canonical
// Erlang [ping-pong example](http://www.erlang.org/doc/getting_started/conc_prog.html#id67347).

data class Msg(val txt: String, val from: ActorRef<Any?>)

class Ping(val n: Int) : Actor() {
    @Suspendable override fun doRun() {
        val pong: ActorRef<Any> = ActorRegistry.getActor("pong")
        for(i in 1..n) {
            pong.send(Msg("ping", self()))          // Fiber-blocking
            receive {                               // Fiber-blocking, always consume the message
                when (it) {
                    "pong" -> println("Ping received pong")
                    else -> null                    // Discard
                }
            }
        }
        pong.send("finished")                       // Fiber-blocking
        println("Ping exiting")
    }
}

class Pong : Actor() {
    @Suspendable override fun doRun() {
        while (true) {
            // snippet Kotlin Actors example
            receive(1000, TimeUnit.MILLISECONDS) {  // Fiber-blocking
                when (it) {
                    is Msg -> {
                        if (it.txt == "ping")
                            it.from.send("pong")    // Fiber-blocking
                    }
                    "finished" -> {
                        println("Pong received 'finished', exiting")
                        return                      // Non-local return, exit actor
                    }
                    is Companion.Timeout -> {
                        println("Pong timeout in 'receive', exiting")
                        return                      // Non-local return, exit actor
                    }
                    else -> defer()
                }
            }
            // end of snippet
        }
    }
}

class KotlinPingPongActorTest {
    @Test fun testActors() {
        val pong = spawn(register("pong", Pong()))
        val ping = spawn(Ping(3))
        LocalActor.join(pong)
        LocalActor.join(ping)
    }
}

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
package actors

import co.paralleluniverse.actors.*
import co.paralleluniverse.fibers.Suspendable
import org.junit.Test
import java.util.concurrent.TimeUnit
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.Actor.ReceiveOutcome.*
import co.paralleluniverse.kotlin.Actor.Companion.Timeout
import co.paralleluniverse.kotlin.*

/**
 * @author circlespainter
 * @author pron
 */

// This example is meant to be a translation of the canonical
// Erlang [ping-pong example](http://www.erlang.org/doc/getting_started/conc_prog.html#id67347).

data class Msg(val txt: String, val from: ActorRef<Any?>)

class Ping(val n: Int) : Actor<Any, Unit>() {
    Suspendable override fun doRun() {
        val pong = ActorRegistry.getActor<Any>("pong")
        for(i in 1..n) {
            pong.send(Msg("ping", self()))          // Fiber-blocking
            when (receive()) {                      // Fiber-blocking, always consume the message
                "pong" -> println("Ping received pong")
            // Else discard the message
            }
        }
        pong.send("finished")                       // Fiber-blocking
        println("Ping exiting")
    }
}

class Pong() : Actor<Any, Unit>() {
    Suspendable override fun doRun() {
        while (true) {
            // snippet Kotlin Actors example
            receive(1000, TimeUnit.MILLISECONDS) {  // Fiber-blocking
                when (it) {
                    is Msg -> {
                        if (it.txt == "ping")
                            it.from.send("pong")    // Fiber-blocking
                        DISCARD
                    }
                    "finished" -> {
                        println("Pong received 'finished', exiting")
                        return                      // Non-local return, exit actor
                    }
                    is Timeout -> {
                        println("Pong timeout in 'receive', exiting")
                        return                      // Non-local return, exit actor
                    }
                    else -> DEFER
                }
            }
            // end of snippet
        }
    }
}

public class Tests {
    Test public fun testActors() {
        spawn(register("pong", Pong()))
        spawn(Ping(3))
    }
}

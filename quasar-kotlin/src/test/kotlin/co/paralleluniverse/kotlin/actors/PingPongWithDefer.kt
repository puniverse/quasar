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
package co.paralleluniverse.kotlin.actors

import co.paralleluniverse.actors.ActorRef
import co.paralleluniverse.actors.ActorRegistry
import co.paralleluniverse.actors.LocalActor
import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.register
import co.paralleluniverse.kotlin.spawn
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author circlespainter
 */

// This example is meant to be a translation of the canonical
// Erlang [ping-pong example](http://www.erlang.org/doc/getting_started/conc_prog.html#id67347).

data class Msg(val txt: String, val from: ActorRef<Any?>)

val sdfDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
fun now(): String = "[${sdfDate.format(Date())}]"

class Ping(val n: Int) : Actor() {
    @Suspendable override fun doRun() {
        val pong: ActorRef<Any> = ActorRegistry.getActor("pong")
        for (i in 1..n) {
            val msg = Msg("ping$i", self())
            println("${now()} Ping sending '$msg' to '$pong'")
            pong.send(msg)          // Fiber-blocking
            println("${now()} Ping sent '$msg' to '$pong'")
            println("${now()} Ping sending garbage 'aaa' to '$pong'")
            pong.send("aaa")        // Fiber-blocking
            println("${now()} Ping sent garbage 'aaa' to '$pong'")
            println("${now()} Ping receiving without timeout")
            receive {                           // Fiber-blocking
                println("${now()} Ping received '$it' (${it?.javaClass})")
                when (it) {
                    is String -> {
                        if (!it.startsWith("pong")) {
                            println("${now()} Ping discarding string '$it'")
                            null
                        } else Unit
                    }
                    else -> {
                        println("${now()} Ping discarding non-string '$it'")
                        null                    // Discard
                    }
                }
            }
        }
        println("${now()} Ping sending 'finished' to '$pong'")
        pong.send("finished")                       // Fiber-blocking
        println("${now()} Ping sent 'finished' to '$pong', exiting")
    }
}

class Pong : Actor() {
    @Suspendable override fun doRun() {
        var discardGarbage = false

        while (true) {
            println("${now()} Pong receiving with 1 sec timeout")
            receive(1, SECONDS) {  // Fiber-blocking
                println("${now()} Pong received '$it' ('${it?.javaClass}')")
                when (it) {
                    is Msg -> {
                        if (it.txt.startsWith("ping")) {
                            val msg = "pong for ${it.txt}"
                            println("${now()} Pong sending '$msg' to '${it.from}'")
                            it.from.send(msg)    // Fiber-blocking
                            println("${now()} Pong sent '$msg' to ${it.from}")
                        } else Unit
                    }
                    "finished" -> {
                        println("${now()} Pong received 'finished', exiting")
                        return                      // Non-local return, exit actor
                    }
                    is Companion.Timeout -> {
                        println("${now()} Pong timeout in 'receive', exiting")
                        return                      // Non-local return, exit actor
                    }
                    else -> {
                        if (discardGarbage) {
                            println("${now()} Pong discarding '$it'")
                            null
                        } else {
                            discardGarbage = true // Net times discard
                            println("${now()} Pong deferring '$it'")
                            defer()
                        }
                    }
                }
            }
        }
    }
}

class KotlinPingPongActorTestWithDefer {
    @Test fun testActors() {
        val pong = spawn(register("pong", Pong()))
        val ping = spawn(Ping(3))
        LocalActor.join(pong)
        LocalActor.join(ping)
    }
}

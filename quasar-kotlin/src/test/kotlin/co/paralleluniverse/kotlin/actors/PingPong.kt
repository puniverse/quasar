package actors

import co.paralleluniverse.actors.*
import co.paralleluniverse.fibers.Suspendable
import org.junit.Test
import java.util.concurrent.TimeUnit
import co.paralleluniverse.kotlin.Actor
import co.paralleluniverse.kotlin.Actor.Default.Timeout
import co.paralleluniverse.kotlin.*

/**
 * @author circlespainter
 * @author pron
 */

// This example is meant to be a translation of the canonical
// Erlang [ping-pong example](http://www.erlang.org/doc/getting_started/conc_prog.html#id67347).

class Ping(val n: Int) : Actor<Any, Unit>() {
    Suspendable override fun doRun() {
        val pong = ActorRegistry.getActor<Any>("pong")
        for(i in 1..n) {
            pong.send(Pair("ping", self()))                             // Fiber-blocking
            when (receive()) {                                          // Fiber-blocking, always consume the message
                "pong" -> println("Ping received pong")                 // Else discard the message
            }
        }
        pong.send("finished")                                           // Fiber-blocking
        println("Ping exiting")
    }
}

class Pong() : Actor<Any, Unit>() {
    Suspendable override fun doRun() {
        while (true) {
            receive(1000, TimeUnit.MILLISECONDS) {                      // Fiber-blocking
                when (it) {
                    is Pair<*, *> -> {
                        val (msg, from) = it as Pair<String, ActorRef<Any>>
                        if (msg == "ping")
                            from.send("pong")                           // Fiber-blocking
                    }
                    "finished" -> {
                        println("Pong received 'finished', exiting")
                        return                                          // Non-local return, exit actor
                    }
                    is Timeout -> {
                        println("Pong timeout in 'receive', exiting")
                        return                                          // Non-local return, exit actor
                    }
                    else -> defer()                                     // Don't consume the message
                }
            }
        }
    }
}

public class Tests {
    Test public fun testActors() {
        spawn(register("pong", Pong()))
        spawn(Ping(3))
    }
}

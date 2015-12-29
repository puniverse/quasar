/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.kotlin.fibers

import co.paralleluniverse.fibers.*
import co.paralleluniverse.kotlin.*
import co.paralleluniverse.strands.channels.Channels

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

/**
 * @author circlespainter
 */
public class FiberKotlinTest {
  @Test public fun testFiber() {
    assertTrue (
      fiber @Suspendable {
        println("Hi there")
        Fiber.sleep(10)
        println("Hi there later")
        1
      }.get() == 1
    )
  }

  @Test public fun testSelect() {
    val ch1 = Channels.newChannel<Int>(1)
    val ch2 = Channels.newChannel<Double>(1)

    assertTrue (
      fiber @Suspendable  {
        select(Receive(ch1), Send(ch2, 2.0)) {
          it
        }
      }.get() is Send<*>)

    ch1.send(1)

    assertTrue (
      fiber @Suspendable {
        select(Receive(ch1), Send(ch2, 2.0)) {
          when (it) {
            is Receive<*> -> it.msg
            is Send<*> -> 0
            else -> -1
          }
        }
      }.get() == 1
    )

    assertTrue (
      fiber @Suspendable {
        select(10, TimeUnit.MILLISECONDS, Receive(ch1), Send(ch2, 2.0)) {
          when (it) {
            is Receive<*> -> it.msg
            is Send<*> -> 0
            else -> -1
          }
        }
      }.get() == -1
    )
  }
}

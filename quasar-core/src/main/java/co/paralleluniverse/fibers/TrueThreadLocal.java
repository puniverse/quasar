/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers;

/**
 * A {@link ThreadLocal} that is local to the current thread, rather than Strand.
 * If the current strand is a thread, then this would behave no different than a {@link ThreadLocal},
 * but if it's a fiber, then the value is local to the underlying thread - not the fiber.
 * 
 * <b>IMPORTANT:</b> This class is <i>only</i> useful in circumstances where a data structure is striped to reduce contention,
 * <i>not</i> when the value needs to actually be associated with the thread in any way.
 *
 * @author pron
 */
public class TrueThreadLocal<T> extends ThreadLocal<T> {
    @Override
    public T get() {
        final Thread thread = Thread.currentThread();
        final Fiber<?> fiber = Fiber.currentFiber();
        if (fiber != null)
            fiber.restoreThreadLocals(thread);
        try {
            return super.get();
        } finally {
            if (fiber != null)
                fiber.installFiberLocals(thread);
        }
    }

    @Override
    public void set(T value) {
        final Thread thread = Thread.currentThread();
        final Fiber<?> fiber = Fiber.currentFiber();
        if (fiber != null)
            fiber.restoreThreadLocals(thread);
        try {
            super.set(value);
        } finally {
            if (fiber != null)
                fiber.installFiberLocals(thread);
        }
    }
}

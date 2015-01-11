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

import co.paralleluniverse.strands.SuspendableCallable;

/**
 * Default fiber factory simply creating new fibers.
 *
 * @author circlespainter
 */
public final class DefaultFiberFactory implements FiberFactory {
    private DefaultFiberFactory() {}

    @Override
    public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
       return new Fiber<>(target);
    }

    private static DefaultFiberFactory instance;

    public static DefaultFiberFactory instance() {
        if (instance == null) {
            return (instance = new DefaultFiberFactory());
        } else return instance;
    }
}

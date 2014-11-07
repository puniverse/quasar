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
 * Functional interface representing the concrete operation to be carried out by
 * {@link FiberAsyncWith#requestAsync} of some extension of {@link FiberAsyncWith}
 * 
 * @param <V>   The async API's result type
 * @param <E>   The async API's exception type
 * @param <FA>  An async API-compatible callback type implementing {@link FyberAsync}
 *
 * @author circlespainter
 */
// @FunctionalInterface // circlespainter: only available in JDK8+
public interface FiberAsyncOp <V, E extends Throwable, FA extends FiberAsync<V, E>> {
    void opAsync(FA fa);
}
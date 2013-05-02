/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.SuspendExecution;

public interface SuspendableCallable<V> {
    /**
     * Entry point for LightweightThread execution.
     *
     * This method should never be called directly.
     *
     * @throws SuspendExecution This exception should never be caught
     */
    V run() throws SuspendExecution, InterruptedException;
}

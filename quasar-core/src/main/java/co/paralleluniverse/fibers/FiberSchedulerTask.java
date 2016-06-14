/*
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
 * The {@code Runnable} tasks passed to a {@link FiberExecutorScheduler}'s {@link java.util.concurrent.Executor}
 * implement this interface.
 * 
 * @author pron
 */
public interface FiberSchedulerTask {
    Fiber<?> getFiber();
}

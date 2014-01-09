/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
abstract class OwnedSynchronizer {
    public abstract void register();

    public abstract void unregister();

    public abstract void await() throws InterruptedException;

    public abstract void await(long timeout, TimeUnit unit) throws InterruptedException;

    public abstract long awaitNanos(long nanos) throws InterruptedException;
    
    public abstract boolean shouldSignal();
    
    public abstract void signal();
}

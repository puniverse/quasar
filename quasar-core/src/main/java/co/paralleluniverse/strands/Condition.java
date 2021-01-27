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
package co.paralleluniverse.strands;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public interface Condition extends Synchronization {
    @Override
    public Object register();

    @Override
    public void unregister(Object registrationToken);

    public void await(int iter) throws InterruptedException, SuspendExecution;

    public void await(int iter, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution;

    public void signal();

    public void signalAll();
}

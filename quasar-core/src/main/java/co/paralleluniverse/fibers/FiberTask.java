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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
interface FiberTask<V> extends Future<V> {
    public static final Object EMERGENCY_UNBLOCKER = new Object();
    public static final int RUNNABLE = 0;
    public static final int LEASED = 1;
    public static final int PARKED = -1;
    public static final int PARKING = -2;

    boolean doExec();
    
    @Override
    boolean isDone();

    void submit();

    @Override
    V get() throws InterruptedException, ExecutionException;

    @Override
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    int getState();

    void setState(int state);
    
    boolean park(Object blocker, boolean exclusive) throws SuspendExecution;

    void yield() throws SuspendExecution;

    void doPark(boolean yield);

    void unpark();

    void unpark(Object unblocker);

    boolean tryUnpark(Object unblocker);

    Object getBlocker();

    Object getUnparker();
    
    StackTraceElement[] getUnparkStackTrace();
}

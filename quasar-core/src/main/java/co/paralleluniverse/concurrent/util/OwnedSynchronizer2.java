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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 *
 * @author pron
 */
class OwnedSynchronizer2 extends OwnedSynchronizer {
    private static final int SPINS = 0; // Runtime.getRuntime().availableProcessors() > 1 ? 1 << 6 : 0; 
    private volatile boolean available;
    private volatile Thread waiter;

    @Override
    public void register() {
        final Thread currentThread = Thread.currentThread();
        if (!casWaiter(null, currentThread))
            throw new IllegalMonitorStateException("attempt by " + currentThread + " but owned by " + waiter);
    }

    @Override
    public void unregister() {
        if (waiter != Thread.currentThread())
            throw new IllegalMonitorStateException("attempt by " + Thread.currentThread() + " but owned by " + waiter);
        waiter = null;
    }

    @Override
    public boolean shouldSignal() {
        return waiter != null;
    }

    @Override
    public void signal() {
        this.available = true;
        final Thread t = waiter;
        if (t != null)
            LockSupport.unpark(t);
    }

    @Override
    public void await() throws InterruptedException {
        for (int spins = SPINS; spins > 0;) {
            if (available) {
                available = false;
                return;
            }
            if (ThreadLocalRandom.current().nextInt() >= 0)
                --spins;
        }
        LockSupport.park(this);

        if (Thread.interrupted())
            throw new InterruptedException();
    }

    @Override
    public long awaitNanos(long nanos) throws InterruptedException {
        long left = nanos;
        long deadline = System.nanoTime() + left;
        for (int spins = SPINS; spins > 0;) {
            if (available) {
                available = false;
                return deadline - System.nanoTime();
            }
            if (ThreadLocalRandom.current().nextInt() >= 0) {
                --spins;
            }
            if (spins % 1000 == 0) {
                left = deadline - System.nanoTime();
                if (left <= 0)
                    return left;
            }
        }
        LockSupport.parkNanos(this, left);
        if (Thread.interrupted())
            throw new InterruptedException();
        left = deadline - System.nanoTime();
        return left;
    }

    @Override
    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        awaitNanos(TimeUnit.NANOSECONDS.convert(timeout, unit));
    }
    
    private static final VarHandle WAITER;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            WAITER = l.findVarHandle(OwnedSynchronizer2.class, "waiter", Thread.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private boolean casWaiter(Thread expected, Thread update) {
        return WAITER.compareAndSet(this, expected, update);
    }
}

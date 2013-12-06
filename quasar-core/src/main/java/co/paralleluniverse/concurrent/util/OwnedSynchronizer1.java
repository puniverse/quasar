/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
class OwnedSynchronizer1 extends OwnedSynchronizer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition available = lock.newCondition();
    private volatile boolean ownerBlocking;

    @Override
    public void register() {
        this.ownerBlocking = true;
        lock.lock();
    }

    @Override
    public void unregister() {
        this.ownerBlocking = false;
        lock.unlock();
    }

    @Override
    public boolean shouldSignal() {
        return ownerBlocking;
    }

    @Override
    public void signal() {
        lock.lock();
        try {
            available.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void await() throws InterruptedException {
        available.await();
    }

    @Override
    public void await(long timeout, TimeUnit unit) throws InterruptedException {
        available.await(timeout, unit);
    }

    @Override
    public long awaitNanos(long nanos) throws InterruptedException {
        return available.awaitNanos(nanos);
    }
}

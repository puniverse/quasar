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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pron
 */
public abstract class OwnedSynchronizer {
    public static OwnedSynchronizer create(Object owner) {
        if (owner instanceof Fiber)
            return create((Fiber) owner);
        else if (owner instanceof Thread)
            return create((Thread) owner);
        else if (owner instanceof Strand)
            return create(((Strand) owner).getUnderlying());
        else
            throw new IllegalArgumentException("owner must be a Thread, a Fiber or a Strand, but is " + owner.getClass().getName());
    }

    public static OwnedSynchronizer create(Strand owner) {
        return create(owner.getUnderlying());
    }

    public static OwnedSynchronizer create(Thread owner) {
        return new ThreadOwnedSynchronizer(owner);
    }

    public static OwnedSynchronizer create(Fiber owner) {
        return new FiberOwnedSynchronizer(owner);
    }

    public abstract Object getOwner();

    public abstract void verifyOwner();

    public abstract boolean isOwnerAlive();

    public abstract void lock();

    public abstract void unlock();

    public abstract void await() throws InterruptedException, SuspendExecution;

    public abstract void await(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution;

    public abstract void signal();

    public abstract void signalAndTryToExecNow();

    private static class ThreadOwnedSynchronizer extends OwnedSynchronizer {
        private final Thread owner;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();

        public ThreadOwnedSynchronizer(Thread owner) {
            this.owner = owner;
        }

        @Override
        public Thread getOwner() {
            return owner;
        }

        @Override
        public void verifyOwner() {
            // assert owner == Thread.currentThread() : "This method has been called by a different strand (thread or fiber) than that owning this object";
            if(owner != Thread.currentThread())
                throw new RuntimeException("This method has been called by a different strand (thread or fiber) than that owning this object");
        }

        @Override
        public boolean isOwnerAlive() {
            return owner.isAlive();
        }

        @Override
        public void lock() {
            lock.lock();
        }

        @Override
        public void unlock() {
            lock.unlock();
        }

        @Override
        public void await() throws InterruptedException {
            condition.await();
        }

        @Override
        public void await(long timeout, TimeUnit unit) throws InterruptedException {
            condition.await(timeout, unit);
        }

        @Override
        public void signal() {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void signalAndTryToExecNow() {
            signal();
        }
    }

    private static class FiberOwnedSynchronizer extends OwnedSynchronizer {
        private final Fiber owner;

        public FiberOwnedSynchronizer(Fiber owner) {
            this.owner = owner;
        }

        @Override
        public Fiber getOwner() {
            return owner;
        }

        @Override
        public void verifyOwner() {
//            assert owner == Fiber.currentFiber() : "This method has been called by a different strand (thread or fiber) than that owning this object";
            if(owner != Fiber.currentFiber())
                throw new RuntimeException("This method has been called by a different strand (thread or fiber) than that owning this object");
        }

        @Override
        public boolean isOwnerAlive() {
            return owner.isAlive();
        }

        @Override
        public void lock() {
            owner.setBlocker(this);
        }

        @Override
        public void unlock() {
            owner.setBlocker(null);
        }

        @Override
        public void await() throws SuspendExecution {
            Fiber.park(this);
        }

        @Override
        public void await(long timeout, TimeUnit unit) throws SuspendExecution {
            Fiber.park(this, timeout, unit);
        }

        @Override
        public void signal() {
            if (owner.getBlocker() == this)
                owner.unpark();
        }

        @Override
        public void signalAndTryToExecNow() {
            if (!owner.exec(this))
                signal();
        }
    }
}

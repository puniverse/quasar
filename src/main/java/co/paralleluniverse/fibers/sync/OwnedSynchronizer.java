/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.sync;

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
        else
            return create((Thread) owner);
    }

    public static OwnedSynchronizer create(Thread owner) {
        return new ThreadOwnedSynchronizer(owner);
    }

    public static OwnedSynchronizer create(Fiber owner) {
        return new LightweightThreadOwnedSynchronizer(owner);
    }

    public abstract Object getOwner();

    public abstract void verifyOwner();

    public abstract boolean isOwnerAlive();

    public abstract void lock();

    public abstract void unlock();

    public abstract void await() throws InterruptedException, SuspendExecution;

    public abstract void await(Object blocker, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution;

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
            assert owner == Thread.currentThread();
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
        public void await(Object blocker, long timeout, TimeUnit unit) throws InterruptedException {
            condition.await(timeout, unit);
        }

        @Override
        public void signal() {
            condition.signal();
        }

        @Override
        public void signalAndTryToExecNow() {
            signal();
        }
    }

    private static class LightweightThreadOwnedSynchronizer extends OwnedSynchronizer {
        private final Fiber owner;

        public LightweightThreadOwnedSynchronizer(Fiber owner) {
            this.owner = owner;
        }

        @Override
        public Fiber getOwner() {
            return owner;
        }

        @Override
        public void verifyOwner() {
            assert owner == Fiber.currentFiber();
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
        public void await() throws InterruptedException, SuspendExecution {
            Fiber.park(this);
        }

        @Override
        public void await(Object blocker, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
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

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * A Strand is either a Thread or a Fiber
 *
 * @author pron
 */
public abstract class Strand {
    public static Strand create(Object owner) {
        if (owner instanceof Fiber)
            return (Fiber) owner;
        else
            return create((Thread) owner);
    }

    public static Strand create(Thread owner) {
        return new ThreadStrand(owner);
    }

    public static Strand create(Fiber fiber) {
        return fiber;
    }

    public abstract Object getUnderlying();

    public abstract String getName();

    public abstract boolean isAlive();

    public abstract Strand start();

    public abstract void join() throws ExecutionException, InterruptedException;

    public abstract void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException;

    public abstract void interrupt();

    public abstract boolean isInterrupted();

    public abstract void unpark();

    public abstract Object getBlocker();

    public abstract StackTraceElement[] getStackTrace();

    /**
     * Returns the current fiber, if there is one, or the current thread otherwise.
     *
     * @return
     */
    public static Strand currentStrand() {
        final Fiber fiber = Fiber.currentFiber();
        if (fiber != null)
            return create(fiber);
        else
            return create(Thread.currentThread());
    }

    public static boolean interrupted() {
        if (Fiber.currentFiber() != null)
            return Fiber.interrupted();
        else
            return Thread.interrupted();
    }

    public static void yield() throws SuspendExecution {
        if (Fiber.currentFiber() != null)
            Fiber.yield();
        else
            Thread.yield();
    }

    public static void sleep(long millis) throws SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() != null)
            Fiber.sleep(millis);
        else
            Thread.sleep(millis);
    }

    public static void park() throws SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() != null)
            Fiber.park();
        else
            LockSupport.park();
    }

    public static void park(Object blocker) throws SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() != null)
            Fiber.park(blocker);
        else
            LockSupport.park(blocker);
    }

    public static void parkNanos(long nanos) throws SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() != null)
            Fiber.park(nanos, TimeUnit.NANOSECONDS);
        else
            LockSupport.parkNanos(nanos);
    }

    public static void parkNanos(Object blocker, long nanos) throws SuspendExecution, InterruptedException {
        if (Fiber.currentFiber() != null)
            Fiber.park(blocker, nanos, TimeUnit.NANOSECONDS);
        else
            LockSupport.parkNanos(blocker, nanos);
    }

    public static void dumpStack() {
        if (Fiber.currentFiber() != null)
            Fiber.dumpStack();
        else
            Thread.dumpStack();
    }

    private static class ThreadStrand extends Strand {
        private final Thread thread;

        public ThreadStrand(Thread owner) {
            this.thread = owner;
        }

        @Override
        public Thread getUnderlying() {
            return thread;
        }

        @Override
        public String getName() {
            return thread.getName();
        }

        @Override
        public boolean isAlive() {
            return thread.isAlive();
        }

        @Override
        public Strand start() {
            thread.start();
            return this;
        }

        @Override
        public void join() throws InterruptedException {
            thread.join();
        }

        @Override
        public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            long nanos = unit.toNanos(timeout);
            long millis = TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
            thread.join(millis, (int) (nanos - millis));
            if (thread.isAlive())
                throw new TimeoutException();
        }

        @Override
        public void interrupt() {
            thread.interrupt();
        }

        @Override
        public boolean isInterrupted() {
            return thread.isInterrupted();
        }

        @Override
        public void unpark() {
            LockSupport.unpark(thread);
        }

        @Override
        public Object getBlocker() {
            return LockSupport.getBlocker(thread);
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return thread.getStackTrace();
        }

        @Override
        public String toString() {
            return thread.toString();
        }
    }

    private static class FiberStrand extends Strand {
        private final Fiber fiber;

        public FiberStrand(Fiber owner) {
            this.fiber = owner;
        }

        @Override
        public Fiber getUnderlying() {
            return fiber;
        }

        @Override
        public String getName() {
            return fiber.getName();
        }

        @Override
        public boolean isAlive() {
            return fiber.isAlive();
        }

        @Override
        public Strand start() {
            fiber.start();
            return this;
        }

        @Override
        public void join() throws ExecutionException, InterruptedException {
            fiber.join();
        }

        @Override
        public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            fiber.join(timeout, unit);
        }

        @Override
        public void interrupt() {
            fiber.interrupt();
        }

        @Override
        public boolean isInterrupted() {
            return fiber.isInterrupted();
        }

        @Override
        public void unpark() {
            fiber.unpark();
        }

        @Override
        public Object getBlocker() {
            return fiber.getBlocker();
        }

        @Override
        public StackTraceElement[] getStackTrace() {
            return fiber.getStackTrace();
        }

        @Override
        public String toString() {
            return fiber.toString();
        }
    }
}

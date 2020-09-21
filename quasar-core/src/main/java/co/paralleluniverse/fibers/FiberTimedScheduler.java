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
/*
 * Based on code: 
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.concurrent.util.SingleConsumerNonblockingProducerDelayQueue;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FiberTimedScheduler {
    private static final boolean USE_LOCKFREE_DELAY_QUEUE = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.useLockFreeDelayQueue");
    private static final boolean DETECT_RUNAWAY_FIBERS = SystemProperties.isNotFalse("co.paralleluniverse.fibers.detectRunawayFibers");

    /**
     * The duration of a single fiber run that is considered a problem
     */
    private static final long MAX_RUN_DURATION = NANOSECONDS.convert(200, MILLISECONDS);
    /*
     * TODO:
     * We're currently feeding the fj-pool sequentially (from a single thread).
     * We can use a custom implementation of a skip-list, and use it to feed the pool in a forking manner.
     */
    private static final boolean BACKPRESSURE = true;
    private static final int BACKPRESSURE_MASK = (1 << 10) - 1;
    private static final int BACKPRESSURE_THRESHOLD = 1200;
    private static final int BACKPRESSURE_PAUSE_MS = 1;
    private static final AtomicInteger nameSuffixSequence = new AtomicInteger();
    private final Thread worker;
    private final BlockingQueue<ScheduledFutureTask> workQueue;
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int STOP = 1;
    private static final int TERMINATED = 2;
    private volatile int state = RUNNING;
    private final ReentrantLock mainLock = new ReentrantLock();
    private final FiberScheduler scheduler;
    private final FibersMonitor monitor;
    private final Map<Thread, FiberInfo> fibersInfo = new IdentityHashMap<>();

    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public FiberTimedScheduler(FiberScheduler scheduler, ThreadFactory threadFactory, FibersMonitor monitor) {
        this.scheduler = scheduler;
        this.worker = threadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                work();
            }
        });
        this.workQueue = USE_LOCKFREE_DELAY_QUEUE ? new SingleConsumerNonblockingProducerDelayQueue<>() : new co.paralleluniverse.concurrent.util.DelayQueue<>();

        this.monitor = monitor;

        worker.start();
    }

    public FiberTimedScheduler(FiberScheduler scheduler, FibersMonitor monitor) {
        this(scheduler, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "FiberTimedScheduler-" + nameSuffixSequence.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        }, monitor);
    }

    public FiberTimedScheduler(FiberScheduler scheduler) {
        this(scheduler, null);
    }

    public Future<Void> schedule(Fiber<?> fiber, Object blocker, long delay, TimeUnit unit) {
        if (fiber == null || unit == null)
            throw new NullPointerException();
        assert fiber.getScheduler() == scheduler;
        ScheduledFutureTask t = new ScheduledFutureTask(fiber, blocker, triggerTime(delay, unit));
        delayedExecute(t);
        return t;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void work() {
        try {
            int counter = 0;
            long lastRanFindProblemFibers = 0;
            while (state == RUNNING) {
                try {
                    ScheduledFutureTask task = workQueue.poll(MAX_RUN_DURATION >>> 1, NANOSECONDS); // workQueue.take();

                    if (task != null && !task.isCancelled()) {
                        long delay = task.delay;
                        if (BACKPRESSURE && (counter & BACKPRESSURE_MASK) == 0) {
                            while (scheduler.getQueueLength() > BACKPRESSURE_THRESHOLD)
                                Thread.sleep(BACKPRESSURE_PAUSE_MS);
                            delay = now() - task.time;
                        }
                        if (monitor != null)
                            monitor.timedParkLatency(delay);

                        run(task);
                    }

                    if (DETECT_RUNAWAY_FIBERS) {
                        final long now = System.nanoTime();
                        if (now - lastRanFindProblemFibers >= MAX_RUN_DURATION >>> 1) {
                            reportProblemFibers(findProblemFibers(now, MAX_RUN_DURATION));
                            lastRanFindProblemFibers = now;
                        }
                    }
                } catch (InterruptedException e) {
                    if (state != RUNNING) {
                        state = STOP;
                        break;
                    }
                }
                counter++;
            }

            if (state == SHUTDOWN) {
                while (state < STOP && !workQueue.isEmpty()) {
                    try {
                        ScheduledFutureTask task = workQueue.take();
                        if (!task.isCancelled())
                            run(task);
                    } catch (InterruptedException e) {
                        if (state != RUNNING) {
                            state = STOP;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("FiberTimedScheduler terminated!");
            e.printStackTrace();
        } finally {
            state = TERMINATED;
        }
    }

    public int getQueueLength() {
        return workQueue.size();
    }

    private void run(ScheduledFutureTask task) {
        try {
            final Fiber<?> fiber = task.fiber;
            fiber.unpark(task.blocker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns current nanosecond time.
     */
    final long now() {
        return System.nanoTime();
    }

    private class ScheduledFutureTask implements Delayed, Future<Void> {
        final Fiber<?> fiber;
        final Object blocker;
        /**
         * The time the task is enabled to execute in nanoTime units
         */
        final long time;
        private volatile boolean cancelled = false;
        long delay;

        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        ScheduledFutureTask(Fiber<?> fiber, Object blocker, long ns) {
            this.fiber = fiber;
            this.blocker = blocker;
            this.time = ns;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            final long d = unit.convert(time - now(), NANOSECONDS);
            this.delay = -d;
            return d;
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) // compare zero if same object
                return 0;
            final ScheduledFutureTask x = (ScheduledFutureTask) other;
            final long diff = time - x.time;

            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else
                return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            this.cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "Timeout(" + blocker + ')';
        }
    }

    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = state;
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    /**
     * Main execution method for delayed or periodic tasks. If pool
     * is shut down, rejects the task. Otherwise adds task to queue
     * and starts a thread, if necessary, to run it. (We cannot
     * prestart the thread to run the task because the task (probably)
     * shouldn't be run yet.) If the pool is shut down while the task
     * is being added, cancel and remove it if required by state and
     * run-after-shutdown parameters.
     *
     * @param task the task
     */
    private void delayedExecute(ScheduledFutureTask task) {
        if (isShutdown())
            reject(task);
        else
            workQueue.add(task);
    }

    protected void reject(Object command) {
        throw new RejectedExecutionException("Task " + command + " rejected from " + this);
    }

    /**
     * Returns the trigger time of a delayed action.
     */
    private long triggerTime(long delay, TimeUnit unit) {
        return triggerTime(unit.toNanos((delay < 0) ? 0 : delay));
    }

    /**
     * Returns the trigger time of a delayed action.
     */
    private long triggerTime(long delay) {
        return now() + ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
    }

    /**
     * Constrains the values of all delays in the queue to be within
     * Long.MAX_VALUE of each other, to avoid overflow in compareTo.
     * This may occur if a task is eligible to be dequeued, but has
     * not yet been, while some other task is added with a delay of
     * Long.MAX_VALUE.
     */
    private long overflowFree(long delay) {
        Delayed head = workQueue.peek();
        if (head != null) {
            long headDelay = head.getDelay(NANOSECONDS);
            if (headDelay < 0 && (delay - headDelay < 0))
                delay = Long.MAX_VALUE + headDelay;
        }
        return delay;
    }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     * <p>
     * This method does not wait for previously submitted tasks to
     * complete execution. Use {@link #awaitTermination awaitTermination}
     * to do that.
     * <p>
     * If the {@code ExecuteExistingDelayedTasksAfterShutdownPolicy}
     * has been set {@code false}, existing delayed tasks whose delays
     * have not yet elapsed are cancelled. And unless the {@code
     * ContinueExistingPeriodicTasksAfterShutdownPolicy} has been set
     * {@code true}, future executions of existing periodic tasks will
     * be cancelled.
     */
    public void shutdown() {
//      assert false;
        mainLock.lock();
        try {
            if (state < SHUTDOWN)
                state = SHUTDOWN;
        } finally {
            mainLock.unlock();
        }
    }
    
    public void finalize() throws Throwable {
        shutdown();
        super.finalize();
    }

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     * <p>
     * This method does not wait for actively executing tasks to
     * terminate. Use {@link #awaitTermination awaitTermination} to
     * do that.
     * <p>
     * There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks. This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     */
    public void shutdownNow() {
        assert false;
        mainLock.lock();
        try {
            if (state < STOP)
                state = STOP;
            worker.interrupt();
        } finally {
            mainLock.unlock();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long millis = TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
        worker.join(millis, (int) (nanos - millis));
        return !worker.isAlive();
    }

    public boolean isShutdown() {
        return state >= SHUTDOWN;
    }

    public boolean isTerminated() {
        return !worker.isAlive();
    }

    private Collection<Fiber<?>> findProblemFibers(long now, long nanos) {
        final List<Fiber<?>> pfs = new ArrayList<>();
        final Map<Thread, Fiber<?>> fibs = scheduler.getRunningFibers();

        if (fibs == null)
            return null;

        fibersInfo.keySet().retainAll(fibs.keySet());

        for (Iterator<Map.Entry<Thread, Fiber<?>>> it = fibs.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<Thread, Fiber<?>> entry = it.next();
            final Thread t = entry.getKey();
            final Fiber<?> f = entry.getValue();

            if (f != null)
                f.getState(); // volatile read

            final FiberInfo fi = fibersInfo.get(t);
            final long run = f != null ? f.getRun() : 0;
//            if (f != null)
//                System.err.println("XXX findProblemFibers f: " + f + " run: " + run + " time: " + (fi != null ? (now - fi.time) : "NA"));
            if (fi == null)
                fibersInfo.put(t, new FiberInfo(f, run, f != null ? now : -1));
            else if (fi.fiber != f | fi.run != run)
                fi.set(f, run, f != null ? now : -1);
            else if (f != null & now - fi.time > nanos)
                pfs.add(f);
        }
        return pfs;
    }

    private void reportProblemFibers(Collection<Fiber<?>> fs) {
        scheduler.getMonitor().setRunawayFibers(fs);

        if (fs == null)
            return;

        loop:
        for (Fiber<?> f : fs) {
            Thread t = f.getRunningThread();
            StackTraceElement[] stackTrace = f.getStackTrace();
            if (stackTrace != null) {
                for (StackTraceElement ste : stackTrace) { // don't report on classloading
                    if ("defineClass".equals(ste.getMethodName()) && "java.lang.ClassLoader".equals(ste.getClassName()))
                        continue loop;
                    if ("loadClass".equals(ste.getMethodName()) && "java.lang.ClassLoader".equals(ste.getClassName()))
                        continue loop;
                    if ("forName".equals(ste.getMethodName()) && "java.lang.Class".equals(ste.getClassName()))
                        continue loop;
                }
            }

            if (t == null || t.getState() == Thread.State.RUNNABLE)
                System.err.println("WARNING: fiber " + f + " is hogging the CPU or blocking a thread.");
//            else if (t.getState() == Thread.State.RUNNABLE)
//                System.err.println("WARNING: fiber " + f + " is hogging the CPU (" + t + ").");
            else
                System.err.println("WARNING: fiber " + f + " is blocking a thread (" + t + ").");
            Strand.printStackTrace(stackTrace, System.err);
        }
    }

    private static class FiberInfo {
        Fiber<?> fiber;
        long run;
        long time;

        FiberInfo(Fiber<?> fiber, long run, long time) {
            set(fiber, run, time);
        }

        final void set(Fiber<?> fiber, long run, long time) {
            this.fiber = fiber;
            this.run = run;
            this.time = time;
        }
    }
}

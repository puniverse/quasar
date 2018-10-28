/*
 * Quasar: lightweight threads and actors for the JVM.
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

import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MetricsForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.concurrent.forkjoin.ExtendedForkJoinWorkerFactory;
import co.paralleluniverse.concurrent.forkjoin.ExtendedForkJoinWorkerThread;
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import co.paralleluniverse.fibers.instrument.DontInstrument;
import co.paralleluniverse.strands.Strand;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A {@code ForkJoinPool} based scheduler for fibers.
 *
 * @author pron
 */
public class FiberForkJoinScheduler extends FiberScheduler {
    private final ForkJoinPool fjPool;
    private final FiberTimedScheduler timer;
    private final Set<FiberWorkerThread> activeThreads = Collections.newSetFromMap(new ConcurrentHashMap<FiberWorkerThread, Boolean>());

    /**
     * Creates a new fiber scheduler.
     *
     * @param name             the scheuler's name. This name is used in naming the scheduler's threads.
     * @param parallelism      the number of threads in the pool
     * @param exceptionHandler an {@link UncaughtExceptionHandler UncaughtExceptionHandler} to be used for exceptions thrown in fibers that aren't caught.
     * @param monitorType      the {@link MonitorType} type to use for the {@code ForkJoinPool}.
     * @param detailedInfo     whether detailed information about the fibers is collected by the fibers monitor.
     */
    public FiberForkJoinScheduler(String name, int parallelism, UncaughtExceptionHandler exceptionHandler, MonitorType monitorType, boolean detailedInfo) {
        super(name, monitorType, detailedInfo);
        this.fjPool = createForkJoinPool(name, parallelism, exceptionHandler, monitorType);
        this.timer = createTimer(fjPool, getMonitor());
    }

    /**
     * Creates a new fiber scheduler using a default {@link UncaughtExceptionHandler UncaughtExceptionHandler}.
     *
     * @param name         the scheuler's name. This name is used in naming the scheduler's threads.
     * @param parallelism  the number of threads in the pool
     * @param monitorType  the {@link MonitorType} type to use for the {@code ForkJoinPool}.
     * @param detailedInfo whether detailed information about the fibers is collected by the fibers monitor.
     */
    public FiberForkJoinScheduler(String name, int parallelism, MonitorType monitorType, boolean detailedInfo) {
        this(name, parallelism, null, monitorType, detailedInfo);
    }

    /**
     * Creates a new fiber scheduler using a default {@link UncaughtExceptionHandler UncaughtExceptionHandler} and no monitoring.
     *
     * @param name        the scheuler's name. This name is used in naming the scheduler's threads.
     * @param parallelism the number of threads in the pool
     */
    public FiberForkJoinScheduler(String name, int parallelism) {
        this(name, parallelism, null, null, false);
    }

    private FiberForkJoinScheduler(ForkJoinPool fjPool, FiberTimedScheduler timeService, boolean detailedInfo) {
        super(fjPool instanceof MonitoredForkJoinPool ? ((MonitoredForkJoinPool) fjPool).getName() : null,
                (fjPool instanceof MonitoredForkJoinPool && ((MonitoredForkJoinPool) fjPool).getMonitor() != null) ? MonitorType.JMX : MonitorType.NONE,
                detailedInfo);
        if (!fjPool.getAsyncMode())
            throw new IllegalArgumentException("ForkJoinPool is not async");
        this.fjPool = fjPool;

        this.timer = timeService != null ? timeService : createTimer(fjPool, getMonitor());
    }

    public void shutdown() {
        this.timer.shutdown();
        super.shutdown();
    }

    private ForkJoinPool createForkJoinPool(String name, int parallelism, UncaughtExceptionHandler exceptionHandler, MonitorType monitorType) {
        final MonitoredForkJoinPool pool = new MonitoredForkJoinPool(name, parallelism, new ExtendedForkJoinWorkerFactory(name) {
            @Override
            protected ExtendedForkJoinWorkerThread createThread(ForkJoinPool pool) {
                return new FiberWorkerThread(pool);
            }
        }, exceptionHandler, true);
        pool.setMonitor(createForkJoinPoolMonitor(name, pool, monitorType));
        return pool;
    }

    static ForkJoinPoolMonitor createForkJoinPoolMonitor(String name, ForkJoinPool fjPool, MonitorType monitorType) {
        if (monitorType == null)
            return null;
        switch (monitorType) {
            case JMX:
                return new JMXForkJoinPoolMonitor(name, fjPool);
            case METRICS:
                return new MetricsForkJoinPoolMonitor(name, fjPool);
            case NONE:
                return null;
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }
    }

    private FiberTimedScheduler createTimer(ForkJoinPool fjPool, FibersMonitor monitor) {
        if (fjPool instanceof MonitoredForkJoinPool)
            return new FiberTimedScheduler(this,
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FiberTimedScheduler-" + ((MonitoredForkJoinPool) fjPool).getName()).build(),
                    monitor);
        else
            return new FiberTimedScheduler(this);
    }

    public ForkJoinPool getForkJoinPool() {
        return fjPool;
    }

    @Override
    public Executor getExecutor() {
        return fjPool;
    }

    @Override
    Future<Void> schedule(Fiber<?> fiber, Object blocker, long delay, TimeUnit unit) {
        return timer.schedule(fiber, blocker, delay, unit);
    }

    @Override
    <V> FiberTask<V> newFiberTask(Fiber<V> fiber) {
        return new FiberForkJoinTask<V>(fiber, fjPool);
    }

    @Override
    Map<Thread, Fiber> getRunningFibers() {
        Map<Thread, Fiber> fibers = new HashMap<>(activeThreads.size() + 2);
        for (FiberWorkerThread t : activeThreads)
            fibers.put(t, getTargetFiber(t));
        return fibers;
    }

    @Override
    protected int getQueueLength() {
        return fjPool.getQueuedSubmissionCount();
    }

    @Override
    int getTimedQueueLength() {
        return timer.getQueueLength();
    }

    @Override
    protected final boolean isCurrentThreadInScheduler() {
        return ForkJoinTask.getPool() == fjPool;
    }

    public static boolean isFiberThread(Thread t) {
        return t instanceof FiberWorkerThread;
    }

    static Fiber getTargetFiber(Thread thread) {
        final Object target = ParkableForkJoinTask.getTarget(thread);
        if (target == null || !(target instanceof Fiber.DummyRunnable))
            return null;
        return ((Fiber.DummyRunnable) target).fiber;
    }

    @Override
    void setCurrentFiber(Fiber target, Thread currentThread) {
        if (isFiberThread(currentThread))
            ParkableForkJoinTask.setTarget(currentThread, target.fiberRef);
        else
            Fiber.setCurrentStrand(target);
    }

    @Override
    void setCurrentTarget(Object target, Thread currentThread) {
        if (isFiberThread(currentThread))
            ParkableForkJoinTask.setTarget(currentThread, target);
        else
            Fiber.setCurrentStrand((Strand) target);
    }

    @Override
    Object getCurrentTarget(Thread currentThread) {
        if (isFiberThread(currentThread))
            return ParkableForkJoinTask.getTarget(currentThread);
        else
            return Fiber.getCurrentStrand();
    }
    
    void tryOnIdle() {
        if (FiberForkJoinTask.isIdle())
            onIdle();
    }

    protected void onIdle() {
    }

    private class FiberWorkerThread extends ExtendedForkJoinWorkerThread {
        public FiberWorkerThread(ForkJoinPool pool) {
            super(pool);
        }

        @Override
        protected void onStart() {
            super.onStart();
            activeThreads.add(this);
        }

        @Override
        protected void onTermination(Throwable exception) {
            super.onTermination(exception);
            activeThreads.remove(this);
        }
    }

    static final class FiberForkJoinTask<V> extends ParkableForkJoinTask<V> implements FiberTask<V> {
        private final ForkJoinPool fjPool;
        private final Fiber<V> fiber;

        public FiberForkJoinTask(Fiber<V> fiber) {
            this(fiber, null);
        }

        public FiberForkJoinTask(Fiber<V> fiber, ForkJoinPool fjPool) {
            this.fiber = fiber;
            this.fjPool = fjPool;
        }

        @Override
        public Fiber<V> getFiber() {
            return fiber;
        }

        @Override
        public void submit() {
//            final FibersMonitor monitor = fiber.getMonitor();
//            if (monitor != null & fiber.getState() != Strand.State.STARTED)
//                monitor.fiberResumed();
            if (getPool() == fjPool)
                fork();
            else
                fjPool.submit(this);
        }

        @Override
        protected boolean exec1() {
            return fiber.exec();
        }

        @Override
        public boolean doExec() {
            boolean done;
            if (!(done = isDone())) {
                if ((done = super.exec()))
                    quietlyComplete();
            }
            return done;
        }

        @Override
        public boolean unpark(Object unblocker) {
            return super.unpark(unblocker == FiberTask.EMERGENCY_UNBLOCKER ? ParkableForkJoinTask.EMERGENCY_UNBLOCKER : unblocker);
        }

        @Override
        @DontInstrument
        public boolean park(Object blocker, boolean exclusive) throws SuspendExecution {
            try {
                return super.park(blocker, exclusive);
            } catch (SuspendExecution p) {
                throw p;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        @DontInstrument
        public void yield() throws SuspendExecution {
            try {
                super.yield();
            } catch (SuspendExecution p) {
                throw p;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void parking(boolean yield) {
            // do nothing. doPark will be called explicitely after the stack has been restored
        }

        @Override
        public void doPark(boolean yield) {
            super.doPark(yield);
        }

        @Override
        protected void onParked(boolean yield) {
            super.onParked(yield);
            fiber.onParked();
        }

        @Override
        @DontInstrument
        protected void throwPark(boolean yield) throws SuspendExecution {
            throw yield ? SuspendExecution.YIELD : SuspendExecution.PARK;
        }

        @Override
        protected void onException(Throwable t) {
        }

        @Override
        protected void onCompletion(boolean res) {
        }

        static boolean isIdle() {
            return peekNextLocalTask() == null;
        }

        @Override
        public V getRawResult() {
            return fiber.getResult();
        }

        @Override
        protected void setRawResult(V v) {
            fiber.setResult(v);
        }

        @Override
        public int getState() {
            return super.getState();
        }

        @Override
        public void setState(int state) {
            super.setState(state);
        }

        @Override
        public boolean tryUnpark(Object unblocker) {
            return super.tryUnpark(unblocker);
        }

        @Override
        public Object getUnparker() {
            return super.getUnparker();
        }

        @Override
        public StackTraceElement[] getUnparkStackTrace() {
            return super.getUnparkStackTrace();
        }

        @Override
        public String toString() {
            return super.toString() + "(Fiber@" + fiber.getId() + ')';
        }
    }
}

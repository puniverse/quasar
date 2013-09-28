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
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.Objects;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import co.paralleluniverse.concurrent.util.ThreadUtil;
import co.paralleluniverse.concurrent.util.UtilUnsafe;
import co.paralleluniverse.fibers.instrument.Retransform;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils.VoidSuspendableCallable;
import static co.paralleluniverse.strands.SuspendableUtils.runnableToCallable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinTask;
import jsr166e.ForkJoinWorkerThread;
import sun.misc.Unsafe;

/**
 * A lightweight thread.
 * <p/>
 * There are two ways to create a new fiber: either subclass the {@code Fiber} class and override the {@code run} method,
 * or pass the code to be executed in the fiber as the {@code target} parameter to the constructor. All in all, the Fiber API
 * resembles the {@link Thread} class in many ways.
 * <p/>
 * A fiber runs inside a ForkJoinPool.
 * <p/>
 * A Fiber can be serialized if it's not running and all involved classes and data types are also {@link Serializable}.
 * <p/>
 * A new Fiber occupies under 400 bytes of memory (when using the default stack size, and compressed OOPs are turned on, as they are by default).
 *
 * @author Ron Pressler
 */
public class Fiber<V> extends Strand implements Joinable<V>, Serializable, Future<V> {
    private static final boolean verifyInstrumentation = Boolean.parseBoolean(System.getProperty("co.paralleluniverse.fibers.verifyInstrumentation", "false"));
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final int PREEMPTION_CREDITS = 3000;
    private static final long TIME_SLICE_MICRO = 1000;
    private static final long serialVersionUID = 2783452871536981L;
    protected static final FlightRecorder flightRecorder = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    static {
        if (Debug.isDebug())
            System.err.println("QUASAR WARNING: Debug mode enabled. This may harm performance.");
        if (Debug.isAssertionsEnabled())
            System.err.println("QUASAR WARNING: Assertions enabled. This may harm performance.");
        assert printVerifyInstrumentationWarning();
    }

    private static boolean printVerifyInstrumentationWarning() {
        if (verifyInstrumentation)
            System.err.println("QUASAR WARNING: Fibers are set to verify instrumentation. This may *severely* harm performance.");
        return true;
    }
    // private static final FiberTimedScheduler timeoutService = new FiberTimedScheduler(new ThreadFactoryBuilder().setNameFormat("fiber-timeout-%d").setDaemon(true).build());
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    private static final AtomicLong idGen = new AtomicLong();

    private static long nextFiberId() {
        return idGen.incrementAndGet();
    }
    //
    private final ForkJoinPool fjPool;
    private final FiberTimedScheduler timeoutService;
    private final FiberForkJoinTask<V> fjTask;
    private final Stack stack;
    private final Strand parent;
    private final String name;
    private final int initialStackSize;
    private final long fid;
    private volatile State state;
    private volatile boolean interrupted;
    private long run;
    private boolean noPreempt;
    private int preemptionCredits;
    private long runStart;
    private Thread runningThread;
    private final SuspendableCallable<V> target;
    private ClassLoader contextClassLoader;
    private Object fiberLocals;
    private Object inheritableFiberLocals;
    private long sleepStart;
    private Future<Void> timeoutTask;
    private ParkAction prePark;
    private ParkAction postPark;
    private V result;
    private boolean getStackTrace;
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
    private final DummyRunnable fiberRef = new DummyRunnable(this);

    /**
     * Creates a new Fiber from the given {@link SuspendableCallable}.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param stackSize the initial size of the data stack.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public Fiber(String name, ForkJoinPool fjPool, FiberTimedScheduler timeService, int stackSize, SuspendableCallable<V> target) {
        this.name = name;
        this.fid = nextFiberId();
        this.fjPool = fjPool;
        this.timeoutService = timeService;
        this.parent = Strand.currentStrand();
        this.target = target;
        this.fjTask = new FiberForkJoinTask<V>(this);
        this.initialStackSize = stackSize;
        this.stack = new Stack(this, stackSize > 0 ? stackSize : DEFAULT_STACK_SIZE);
        this.state = State.NEW;

        if (Debug.isDebug())
            record(1, "Fiber", "<init>", "Creating fiber name: %s, fjPool: %s, parent: %s, target: %s, task: %s, stackSize: %s", name, fjPool, parent, target, fjTask, stackSize);

        if (target != null) {
            verifyInstrumentedTarget(target);

            if (target instanceof Stranded)
                ((Stranded) target).setStrand(this);
        } else if (!isInstrumented(this.getClass())) {
            throw new IllegalArgumentException("Fiber class " + this.getClass() + " has not been instrumented.");
        }

        final Thread currentThread = Thread.currentThread();
        Object inheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);
        if (inheritableThreadLocals != null)
            this.inheritableFiberLocals = ThreadAccess.createInheritedMap(inheritableThreadLocals);
        this.contextClassLoader = ThreadAccess.getContextClassLoader(currentThread);

        record(1, "Fiber", "<init>", "Created fiber %s", this);
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableCallable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     *
     * @param name The name of the fiber (may be null)
     * @param stackSize the initial size of the data stack.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool, int stackSize, SuspendableCallable<V> target) {
        this(name, fjPool, defaultTimedScheduler(), stackSize, target);
    }

    public Fiber(String name, int stackSize, SuspendableCallable<V> target) {
        this(name, defaultPool(), defaultTimedScheduler(), stackSize, target);
    }

    private static ForkJoinPool defaultPool() {
        final Fiber parent = currentFiber();
        if (parent == null)
            return DefaultFiberPool.getInstance();
        else
            return parent.getFjPool();
    }

    private static FiberTimedScheduler defaultTimedScheduler() {
        final Fiber parent = currentFiber();
        if (parent == null)
            return DefaultFiberTimeService.getInstance();
        else
            return parent.timeoutService;
    }

    private static Fiber verifyParent() {
        final Fiber parent = currentFiber();
        if (parent == null)
            throw new IllegalStateException("This constructor may only be used from within a Fiber");
        return parent;
    }

    private static void verifyInstrumentedTarget(SuspendableCallable<?> target) {
        Object t = target;
        if (target instanceof VoidSuspendableCallable)
            t = ((VoidSuspendableCallable) target).getRunnable();

        if (verifyInstrumentation && !isInstrumented(t.getClass()))
            throw new IllegalArgumentException("Target class " + t.getClass() + " has not been instrumented.");
    }

    public final SuspendableCallable<V> getTarget() {
        return target;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public long getId() {
        return fid;
    }

    public ForkJoinTask<V> getForkJoinTask() {
        return fjTask;
    }

    ForkJoinPool getFjPool() {
        return fjPool;
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new Fiber from the given {@link SuspendableCallable}.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(name, fjPool, -1, target);
    }

    /**
     * Creates a new Fiber from the given {@link SuspendableCallable}.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(null, fjPool, -1, target);
    }

    /**
     * Creates a new Fiber from the given {@link SuspendableRunnable}.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param stackSize the initial size of the data stack.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool, int stackSize, SuspendableRunnable target) {
        this(name, fjPool, stackSize, (SuspendableCallable<V>) runnableToCallable(target));
    }

    /**
     * Creates a new Fiber from the given {@link SuspendableRunnable}.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool, SuspendableRunnable target) {
        this(name, fjPool, -1, target);
    }

    /**
     * Creates a new Fiber from the given SuspendableRunnable.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(ForkJoinPool fjPool, SuspendableRunnable target) {
        this(null, fjPool, -1, target);
    }

    /**
     * Creates a new Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @param stackSize the initial size of the data stack.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool, int stackSize) {
        this(name, fjPool, stackSize, (SuspendableCallable) null);
    }

    /**
     * Creates a new Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @param fjPool The fork/join pool in which the fiber should run.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, ForkJoinPool fjPool) {
        this(name, fjPool, -1, (SuspendableCallable) null);
    }

    /**
     * Creates a new Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @param fjPool The fork/join pool in which the fiber should run.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(ForkJoinPool fjPool) {
        this(null, fjPool, -1, (SuspendableCallable) null);
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableCallable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableCallable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableRunnable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     *
     * @param name The name of the fiber (may be null)
     * @param stackSize the initial size of the data stack.
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, int stackSize, SuspendableRunnable target) {
        this(name, stackSize, (SuspendableCallable<V>) runnableToCallable(target));
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableRunnable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, SuspendableRunnable target) {
        this(name, -1, target);
    }

    /**
     * Creates a new child Fiber from the given {@link SuspendableRunnable}.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @param target the SuspendableRunnable for the Fiber.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(SuspendableRunnable target) {
        this(null, -1, target);
    }

    /**
     * Creates a new child Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     *
     * @param name The name of the fiber (may be null)
     * @param stackSize the initial size of the data stack.
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name, int stackSize) {
        this(name, stackSize, (SuspendableCallable) null);
    }

    /**
     * Creates a new child Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber uses the default initial stack size.
     *
     * @param name The name of the fiber (may be null)
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber(String name) {
        this(name, -1, (SuspendableCallable) null);
    }

    /**
     * Creates a new child Fiber subclassing the Fiber class and overriding the {@link #run() run} method.
     * This constructor may only be called from within another fiber. This fiber will use the same fork/join pool as the creating fiber.
     * The new fiber has no name, and uses the default initial stack size.
     *
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public Fiber() {
        this(null, -1, (SuspendableCallable) null);
    }

    public Fiber(Fiber fiber, SuspendableCallable<V> target) {
        this(fiber.name, fiber.fjPool, fiber.initialStackSize, target);
    }

    public Fiber(Fiber fiber, SuspendableRunnable target) {
        this(fiber.name, fiber.fjPool, fiber.initialStackSize, target);
    }

    public Fiber(Fiber fiber, ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(fiber.name, fjPool, fiber.initialStackSize, target);
    }

    public Fiber(Fiber fiber, ForkJoinPool fjPool, SuspendableRunnable target) {
        this(fiber.name, fjPool, fiber.initialStackSize, target);
    }
    //</editor-fold>

    /**
     * Returns the active Fiber on this thread or NULL if no Fiber is running.
     *
     * @return the active Fiber on this thread or NULL if no Fiber is running.
     */
    public static Fiber currentFiber() {
        return getCurrentFiber();
    }

    @Override
    public final boolean isFiber() {
        return true;
    }

    @Override
    public final Object getUnderlying() {
        return this;
    }

    /**
     * Suspends (deschedules) the currently running Fiber unless the
     * permit is available.
     * <p/>
     * Returns {@code true} iff we've been suspended and then resumed.
     *
     * @throws SuspendExecution This exception is used for control transfer and must never be caught.
     * @throws IllegalStateException If not called from a Fiber
     */
    public static boolean park(Object blocker, ParkAction postParkActions, long timeout, TimeUnit unit) throws SuspendExecution {
        return verifySuspend().park1(blocker, postParkActions, timeout, unit);
    }

    public static boolean park(Object blocker, ParkAction postParkActions) throws SuspendExecution {
        return park(blocker, postParkActions, 0, null);
    }

    public static boolean park(Object blocker, long timeout, TimeUnit unit) throws SuspendExecution {
        return park(blocker, null, timeout, unit);
    }

    public static void park(Object blocker) throws SuspendExecution {
        park(blocker, null, 0, null);
    }

    public static void park(long timeout, TimeUnit unit) throws SuspendExecution {
        park(null, null, timeout, unit);
    }

    public static void park() throws SuspendExecution {
        park(null, null, 0, null);
    }

    public static void yield() throws SuspendExecution {
        verifySuspend().yield1();
    }

    public static void sleep(long millis) throws InterruptedException, SuspendExecution {
        verifySuspend().sleep1(millis);
    }

    public static boolean interrupted() {
        final Fiber current = verifySuspend();
        final boolean interrupted = current.isInterrupted();
        if (interrupted)
            current.interrupted = false;
        return interrupted;
    }

    /**
     * Returns {@code true} iff we've been suspended and then resumed.
     * (The return value in the Java code is actually ignored. It is generated and injected in InstrumentMethod.accept())
     * <p/>
     *
     * @param blocker
     * @param postParkAction
     * @param timeout
     * @param unit
     * @return
     * @throws SuspendExecution
     */
    private boolean park1(Object blocker, ParkAction postParkAction, long timeout, TimeUnit unit) throws SuspendExecution {
        record(1, "Fiber", "park", "Parking %s blocker: %s", this, blocker);
        if (isRecordingLevel(2))
            record(2, "Fiber", "park", "Parking %s at %s", this, Arrays.toString(getStackTrace()));
        if(prePark != null)
            prePark.run(this);
        this.postPark = postParkAction;
        if (timeout > 0 && unit != null)
            this.timeoutTask = timeoutService.schedule(this, timeout, unit);

        return fjTask.park1(blocker);
    }

    private void yield1() throws SuspendExecution {
        if (isRecordingLevel(2))
            record(2, "Fiber", "yield", "Yielding %s at %s", this, Arrays.toString(getStackTrace()));
        if(prePark != null)
            prePark.run(this);
        fjTask.yield1();
    }

    void preempt() throws SuspendExecution {
        if (isRecordingLevel(2))
            record(2, "Fiber", "preempt", "Preempting %s at %s", this, Arrays.toString(getStackTrace()));
        fjTask.yield1();
    }

    private boolean exec1() {
        if (fjTask.isDone() | state == State.RUNNING)
            throw new IllegalStateException("Not new or suspended");

        cancelTimeoutTask();

        final FibersMonitor monitor = getMonitor();
        record(1, "Fiber", "exec1", "running %s %s", state, this);
        // if (monitor != null && state == State.STARTED)
        //    monitor.fiberStarted(); - done elsewhere

        final Fiber oldFiber = getCurrentFiber(); // a fiber can directly call exec on another fiber, e.g.: Channel.sendSync
        final Thread currentThread = Thread.currentThread();
        installFiberDataInThread(currentThread);

        run++;
        preemptionCredits = PREEMPTION_CREDITS;
        runStart = 0L;
        runningThread = currentThread;
        state = State.RUNNING;

        boolean restored = false;
        try {
            this.result = run1(); // we jump into the continuation

            runningThread = null;
            state = State.TERMINATED;
            record(1, "Fiber", "exec1", "finished %s %s res: %s", state, this, this.result);
            monitorFiberTerminated(monitor);
            return true;
        } catch (SuspendExecution ex) {
            assert ex == SuspendExecution.PARK || ex == SuspendExecution.YIELD;
            //stack.dump();
            stack.resumeStack();
            runningThread = null;
            state = State.WAITING;

            final ParkAction ppa = postPark;
            this.postPark = null;

            restoreThreadData(currentThread, oldFiber);
            restored = true;

            record(1, "Fiber", "exec1", "parked %s %s", state, this);
            fjTask.doPark(ex == SuspendExecution.YIELD); // now we can complete parking

            assert ppa == null || ex == SuspendExecution.PARK; // can't have postParkActions on yield
            if (ppa != null)
                ppa.run(this);

            if (monitor != null)
                monitor.fiberSuspended();
            return false;
        } catch (InterruptedException e) {
            runningThread = null;
            state = State.TERMINATED;
            record(1, "Fiber", "exec1", "InterruptedException: %s, %s", state, this);
            monitorFiberTerminated(monitor);
            throw new RuntimeException(e);
        } catch (Throwable t) {
            runningThread = null;
            state = State.TERMINATED;
            record(1, "Fiber", "exec1", "Exception in %s %s: %s", state, this, t);
            monitorFiberTerminated(monitor);
            throw t;
        } finally {
            if (!restored)
                restoreThreadData(currentThread, oldFiber);
        }
    }

    private StackTraceElement[] execStackTrace1() {
        if (fjTask.isDone() | state == State.RUNNING)
            throw new IllegalStateException("Not new or suspended");

        this.getStackTrace = true;
        Stack.getStackTrace.set(stack);
        try {
            run1(); // we jump into the continuation
            throw new AssertionError();
        } catch (SuspendExecution | IllegalStateException ex) {
            assert ex != SuspendExecution.PARK && ex != SuspendExecution.YIELD;
            //stack.dump();
            stack.resumeStack();
            fjTask.doPark(false); // now we can complete parking

            StackTraceElement[] st = ex.getStackTrace();

            if (ex instanceof IllegalStateException) { // special case for sleep (not recognized as a yield method by instrumentation
                int index = -1;
                for (int i = 0; i < st.length; i++) {
                    if (Fiber.class.getName().equals(st[i].getClassName()) && "sleep".equals(st[i].getMethodName())) {
                        index = i;
                        break;
                    }
                }
                assert index >= 0;
                st = skipStackTraceElements(st, index);
            } else
                st = skipStackTraceElements(st, 2); // skip Fiber.onResume and Stack.postRestore
            return st;
        } catch (Throwable ex) {
            throw new AssertionError(ex);
        } finally {
            this.getStackTrace = false;
            Stack.getStackTrace.remove();
        }
    }

    @Override
    public FibersMonitor getMonitor() {
        if (fjPool instanceof MonitoredForkJoinPool) {
            final FibersMonitor mon = ((MonitoredForkJoinPool) fjPool).getFibersMonitor();
            if (mon != null)
                return mon;
        }
        return NOOP_FIBERS_MONITOR;
    }

    private void monitorFiberTerminated(FibersMonitor monitor) {
        if (monitor != null)
            monitor.fiberTerminated();
    }

    private void cancelTimeoutTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
    }

    private void installFiberDataInThread(Thread currentThread) {
        record(1, "Fiber", "installFiberDataInThread", "%s <-> %s", this, currentThread);
        setCurrentFiber(this);
        installFiberLocals(currentThread);
        installFiberContextClassLoader(currentThread);
    }

    private void restoreThreadData(Thread currentThread, Fiber oldFiber) {
        record(1, "Fiber", "restoreThreadData", "%s <-> %s", this, currentThread);
        restoreThreadLocals(currentThread);
        restoreThreadContextClassLoader(currentThread);
        setCurrentFiber(oldFiber);
    }

    private void installFiberLocals(Thread currentThread) {
        switchFiberAndThreadLocals(currentThread, true);
    }

    private void restoreThreadLocals(Thread currentThread) {
        switchFiberAndThreadLocals(currentThread, false);
    }

    private void switchFiberAndThreadLocals(Thread currentThread, boolean install) {
        if (fjPool == null) // in tests
            return;

        Object tmpThreadLocals = ThreadAccess.getThreadLocals(currentThread);
        Object tmpInheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);

        if (isRecordingLevel(2)) {
            record(2, "Fiber", "switchFiberAndThreadLocals", "fiberLocals: %s", ThreadUtil.getThreadLocalsString(install ? this.fiberLocals : tmpThreadLocals));
            record(2, "Fiber", "switchFiberAndThreadLocals", "inheritableFilberLocals: %s", ThreadUtil.getThreadLocalsString(install ? this.inheritableFiberLocals : tmpInheritableThreadLocals));
        }

        ThreadAccess.setThreadLocals(currentThread, this.fiberLocals);
        ThreadAccess.setInheritablehreadLocals(currentThread, this.inheritableFiberLocals);

        this.fiberLocals = tmpThreadLocals;
        this.inheritableFiberLocals = tmpInheritableThreadLocals;
    }

    private void installFiberContextClassLoader(Thread currentThread) {
        final ClassLoader origContextClassLoader = ThreadAccess.getContextClassLoader(currentThread);
        ThreadAccess.setContextClassLoader(currentThread, contextClassLoader);
        this.contextClassLoader = origContextClassLoader;
    }

    private void restoreThreadContextClassLoader(Thread currentThread) {
        final ClassLoader origContextClassLoader = contextClassLoader;
        this.contextClassLoader = ThreadAccess.getContextClassLoader(currentThread);
        ThreadAccess.setContextClassLoader(currentThread, origContextClassLoader);
    }

    private void setCurrentFiber(Fiber fiber) {
        if (fjPool == null) // in tests
            return;
        final Thread currentThread = Thread.currentThread();
//        if (ThreadAccess.getTarget(currentThread) != null && fiber != null)
//            throw new RuntimeException("Fiber " + fiber + " target: " + ThreadAccess.getTarget(currentThread));
        ThreadAccess.setTarget(currentThread, fiber != null ? fiber.fiberRef : null);
    }

    private static Fiber getCurrentFiber() {
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof ForkJoinWorkerThread) { // false in tests
            Object target = ThreadAccess.getTarget(currentThread);
            if (target == null)
                return null;
            if (!(target instanceof DummyRunnable))
                return null;
            return ((DummyRunnable) ThreadAccess.getTarget(currentThread)).fiber;
        } else if (Debug.isUnitTest()) {
            try {
                final FiberForkJoinTask currentFJTask = FiberForkJoinTask.getCurrent();
                if (currentFJTask == null)
                    return null;
                return currentFJTask.getFiber();
            } catch (ClassCastException e) {
                return null;
            }
        } else
            return null;
    }

    private static final class DummyRunnable implements Runnable {
        final Fiber fiber;

        public DummyRunnable(Fiber fiber) {
            this.fiber = fiber;
        }

        @Override
        public void run() {
            throw new RuntimeException("This method shouldn't be run. This object is a placeholder.");
        }
    }

    private V run1() throws SuspendExecution, InterruptedException {
        return run(); // this method is always on the stack trace. used for verify instrumentation
    }

    protected V run() throws SuspendExecution, InterruptedException {
        if (target != null)
            return target.run();
        return null;
    }

    /**
     * Causes the current strand's {@link ThreadLocal thread-locals} to be inherited by this fiber. By default only {@link InheritableThreadLocal}s
     * are inherited.<p/>
     * This method must be called <i>before</i> the fiber is started (i.e. before the {@link #start() start} method is called.
     * Otherwise, an {@link IllegalStateException} is thrown.
     *
     * @return {@code this}
     */
    public Fiber inheritThreadLocals() {
        if (state != State.NEW)
            throw new IllegalStateException("Method called on a started fiber");
        this.fiberLocals = ThreadAccess.cloneThreadLocalMap(ThreadAccess.getThreadLocals(Thread.currentThread()));
        return this;
    }

    /**
     *
     * @return {@code this}
     */
    @Override
    public final Fiber<V> start() {
        if (!casState(State.NEW, State.STARTED))
            throw new IllegalThreadStateException("Fiber has already been started or has died");
        fjTask.submit();
        return this;
    }

    protected void onParked() {
    }

    protected void onResume() throws SuspendExecution, InterruptedException {
        if (getStackTrace) {
            try {
                park1(null, null, 0, null);
            } catch (SuspendExecution e) {
            }
            SuspendExecution ex = new SuspendExecution();
            ex.setStackTrace(new Throwable().getStackTrace());
            throw ex;
        }

        record(1, "Fiber", "onResume", "Resuming %s", this);
        if (isRecordingLevel(2))
            record(2, "Fiber", "onResume", "Resuming %s at: %s", this, Arrays.toString(getStackTrace()));
    }

    final void preemptionPoint(int type) throws SuspendExecution {
        if (noPreempt)
            return;
        if (shouldPreempt(type))
            preempt();
    }

    protected boolean shouldPreempt(int type) {
        // 0 - backbranch
        // 1 - call
//        assert type == 1;
//        preemptionCredits -= 3;
//        if (preemptionCredits < 0) {
//            final long now = System.nanoTime();
//            if (runStart == 0)
//                runStart = now;
//            else if (TimeUnit.NANOSECONDS.toMicros(now - runStart) > TIME_SLICE_MICRO)
//                return true;
//            preemptionCredits = 1000;
//        }
        return false;
    }

    protected void onCompletion() {
    }

    protected void onException(Throwable t) {
        if (uncaughtExceptionHandler != null)
            uncaughtExceptionHandler.uncaughtException(this, t);
        else if (defaultUncaughtExceptionHandler != null)
            defaultUncaughtExceptionHandler.uncaughtException(this, t);
        else
            throw Exceptions.rethrow(t);
    }

    @Override
    public final void interrupt() {
        interrupted = true;
        unpark();
    }

    @Override
    public final boolean isInterrupted() {
        return interrupted;
    }

    /**
     * A fiber is alive if it has been started and has not yet died.
     *
     * @return {@code true} if the fiber has been started and has not yet died; {@code false} otherwise.
     */
    @Override
    public final boolean isAlive() {
        return state != State.NEW && !fjTask.isDone();
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final boolean isTerminated() {
        return state == State.TERMINATED;
    }

    @Override
    public final Object getBlocker() {
        return fjTask.getBlocker();
    }

    public final void setBlocker(Object blocker) {
        fjTask.setBlocker(blocker);
    }

    public final Strand getParent() {
        return parent;
    }

    public final boolean exec(Object blocker, long timeout, TimeUnit unit) {
        return exec(blocker, null, timeout, unit);
    }
    /**
     * Executes fiber on this thread, after waiting until the given blocker is indeed the fiber's blocker, and that the fiber is not being run concurrently.
     *
     * @param blocker
     * @return {@code true} if the task has been executed by this method; {@code false} otherwise.
     */
    public final boolean exec(Object blocker, ParkAction prePark, long timeout, TimeUnit unit) {
        if (ForkJoinTask.getPool() != fjPool)
            return false;
        record(1, "Fiber", "exec", "Blocker %s attempting to immediately execute %s", blocker, this);

        long start = 0;
        for (int i = 0;; i++) {
            if (getBlocker() == blocker && fjTask.tryUnpark()) {
                final FibersMonitor monitor = getMonitor();
                if (monitor != null)
                    monitor.fiberSubmitted(false);
                this.prePark = prePark;
                this.noPreempt = true;
                try {
                    if (fjTask.exec())
                        fjTask.quietlyComplete();
                } finally {
                    this.prePark = null;
                    this.noPreempt = false;
                }
                return true;
            }
            if (unit != null && timeout == 0)
                break;
            if (unit != null && timeout > 0 && i > (1 << 12)) {
                if (start == 0)
                    start = System.nanoTime();
                else if (i % 100 == 0) {
                    if (System.nanoTime() - start > unit.toNanos(timeout))
                        break;
                }
            }
        }
        record(1, "Fiber", "exec", "Blocker %s attempt to immediately execute %s - FAILED", blocker, this);
        return false;
    }

    private StackTraceElement[] execStackTrace(long timeout, TimeUnit unit) {
        long start = 0;
        for (int i = 0;; i++) {
            if (fjTask.tryUnpark()) {
                this.noPreempt = true;
                try {
                    return execStackTrace1();
                } finally {
                    this.noPreempt = false;
                }
            }

            if (unit != null && timeout == 0)
                break;
            if (unit != null && timeout > 0 && i > (1 << 12)) {
                if (start == 0)
                    start = System.nanoTime();
                else if (i % 100 == 0) {
                    if (System.nanoTime() - start > unit.toNanos(timeout))
                        break;
                }
            }
        }
        return null;
    }

    /**
     * Makes available the permit for this fiber, if it was not already available.
     * If the fiber was blocked on {@code park} then it will unblock.
     * Otherwise, its next call to {@code park} is guaranteed not to block.
     */
    @Override
    public final void unpark() {
        fjTask.unpark();
    }

    @Override
    public final void join() throws ExecutionException, InterruptedException {
        get();
    }

    @Override
    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        get(timeout, unit);
    }

    @Override
    public final V get() throws ExecutionException, InterruptedException {
        return fjTask.get();
    }

    @Override
    public final V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return fjTask.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
        return state == State.TERMINATED;
    }

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public final boolean isCancelled() {
        return false;
    }

    private void sleep1(long millis) throws InterruptedException, SuspendExecution {
        if (getStackTrace) { // special case because this method isn't instrumented
            onResume();
            assert false : "shouldn't get here";
        }
        // this class's methods aren't instrumented, so we can't rely on the stack. This method will be called again when unparked
        try {
            for (;;) {
                if (interrupted)
                    throw new InterruptedException();
                final long now = System.nanoTime();
                if (sleepStart == 0)
                    this.sleepStart = now;
                final long deadline = sleepStart + TimeUnit.MILLISECONDS.toNanos(millis);
                final long left = deadline - now;
                if (left <= 0) {
                    this.sleepStart = 0;
                    return;
                }
                park1(null, null, left, TimeUnit.NANOSECONDS); // must be the last statement because we're not instrumented so we don't return here when awakened
            }
        } catch (SuspendExecution s) {
            throw s;
        } catch (Throwable t) {
            this.sleepStart = 0;
            throw t;
        }
    }

    public final void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public final UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler defaultUncaughtExceptionHandler) {
        Fiber.defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler;
    }

    @Override
    public final StackTraceElement[] getStackTrace() {
        StackTraceElement[] threadStack = null;
        if (currentFiber() == this)
            threadStack = skipStackTraceElements(Thread.currentThread().getStackTrace(), 1); // remove Thread.getStackTrace
        else if (state == State.TERMINATED || state == State.NEW)
            threadStack = null;
        else {
            for (;;) {
                if (state == State.RUNNING) {
                    final long r = run;
                    final Thread t = runningThread;
                    if (t != null)
                        threadStack = t.getStackTrace();
                    if (state == State.RUNNING && run == r && runningThread == t)
                        break;
                } else {
                    threadStack = execStackTrace(1, TimeUnit.MILLISECONDS);
                    if (threadStack != null) {
                        // we need to unpark because if someone else had tried to unpark while we were in execStackTrace(), it would have silently failed.
                        unpark();
                        break;
                    }
                }
            }
        }
        return threadToFiberStack(threadStack);
    }

    private static StackTraceElement[] threadToFiberStack(StackTraceElement[] threadStack) {
        if (threadStack == null)
            return null;
        if (threadStack.length == 0)
            return threadStack;

        int count = 0;
        for (int i = 0; i < threadStack.length; i++) {
            count++;
            StackTraceElement ste = threadStack[i];
            if (Fiber.class.getName().equals(ste.getClassName())) {
                if ("run".equals(ste.getMethodName()))
                    break;
                if ("run1".equals(ste.getMethodName())) {
                    count--;
                    break;
                }
            }
        }

        StackTraceElement[] fiberStack = new StackTraceElement[count];
        System.arraycopy(threadStack, 0, fiberStack, 0, count);
        return fiberStack;
    }

    public static void dumpStack() {
        verifyCurrent();
        printStackTrace(new Exception("Stack trace"), System.err);
    }

    @SuppressWarnings("CallToThrowablePrintStackTrace")
    private static void printStackTrace(Throwable t, java.io.OutputStream out) {
        t.printStackTrace(new java.io.PrintStream(out) {
            boolean seenExec;

            @Override
            public void println(String x) {
                if (x.startsWith("\tat ")) {
                    if (seenExec)
                        return;
                    if (x.startsWith("\tat " + Fiber.class.getName() + ".exec1")) {
                        seenExec = true;
                        return;
                    }
                }
                super.println(x);
            }
        });
    }

    @Override
    public final String toString() {
        return "Fiber@" + Long.toHexString(fid) + (name != null ? (':' + name) : "") + "[task: " + fjTask + ", target: " + Objects.systemToStringSimpleName(target) + ']';
    }

    ////////
    static private final class FiberForkJoinTask<V> extends ParkableForkJoinTask<V> {
        private final Fiber<V> fiber;

        public FiberForkJoinTask(Fiber<V> fiber) {
            this.fiber = fiber;
        }

        protected static FiberForkJoinTask getCurrent() {
            return (FiberForkJoinTask) ParkableForkJoinTask.getCurrent();
        }

        Fiber getFiber() {
            return fiber;
        }

        @Override
        protected void submit() {
            final FibersMonitor monitor = fiber.getMonitor();
            if (monitor != null)
                monitor.fiberSubmitted(fiber.getState() == State.STARTED);
            if (getPool() == fiber.fjPool)
                fork();
            else
                fiber.fjPool.submit(this);
        }

        @Override
        protected boolean exec1() {
            return fiber.exec1();
        }

        @Override
        protected boolean park1(Object blocker) throws SuspendExecution {
            try {
                return super.park1(blocker);
            } catch (SuspendExecution p) {
                throw p;
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void yield1() throws SuspendExecution {
            try {
                super.yield1();
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
        protected void doPark(boolean yield) {
            super.doPark(yield);
        }

        @Override
        protected void onParked(boolean yield) {
            super.onParked(yield);
            fiber.onParked();
        }

        @Override
        protected void throwPark(boolean yield) throws SuspendExecution {
            throw yield ? SuspendExecution.YIELD : SuspendExecution.PARK;
        }

        @Override
        protected void onException(Throwable t) {
            fiber.onException(t);
        }

        @Override
        protected void onCompletion(boolean res) {
            if (res)
                fiber.onCompletion();
        }

        @Override
        public V getRawResult() {
            return fiber.result;
        }

        @Override
        protected void setRawResult(V v) {
            fiber.result = v;
        }

        @Override
        protected int getState() {
            return super.getState();
        }

        @Override
        protected void setBlocker(Object blocker) {
            super.setBlocker(blocker);
        }

        @Override
        protected boolean exec() {
            return super.exec();
        }

        @Override
        protected boolean tryUnpark() {
            return super.tryUnpark();
        }
    }

    public interface UncaughtExceptionHandler {
        void uncaughtException(Fiber lwt, Throwable e);
    }

    //////////////////////////////////////////////////
    final Stack getStack() {
        return stack;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (state == State.RUNNING)
            throw new IllegalStateException("trying to serialize a running Fiber");
        out.defaultWriteObject();
    }

    public static interface ParkAction {
        /**
         * Called by Fiber immediately after park.
         * This method may not use any ThreadLocals as they have been rest by the time the method is called.
         *
         * @param current
         */
        void run(Fiber current);
    }

    private static Fiber verifySuspend() {
        final Fiber current = verifyCurrent();
        if (verifyInstrumentation)
            assert checkInstrumentation();
        return current;
    }

    private static Fiber verifyCurrent() {
        final Fiber current = currentFiber();
        if (current == null)
            throw new IllegalStateException("Not called from withing a Fiber");
        return current;
    }

    private static boolean checkInstrumentation() {
        if (!verifyInstrumentation)
            throw new AssertionError();

        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stes) {
            if (ste.getClassName().equals(Thread.class.getName()) && ste.getMethodName().equals("getStackTrace"))
                continue;
            if (!ste.getClassName().equals(Fiber.class.getName()) && !ste.getClassName().startsWith(Fiber.class.getName() + '$')) {
                if (!Retransform.isWaiver(ste.getClassName(), ste.getMethodName())
                        && (!Retransform.isInstrumented(ste.getClassName()) || !isSuspendableOrUnknown(ste.getClassName(), ste.getMethodName()))) {
                    final String str = "Method " + ste.getClassName() + "." + ste.getMethodName() + " on the call-stack has not been instrumented. (trace: " + Arrays.toString(stes) + ")";
                    System.err.println("WARNING: " + str);
                    throw new IllegalStateException(str);
                }
            } else if (ste.getMethodName().equals("run1"))
                return true;
        }
        throw new IllegalStateException("Not run through Fiber.exec(). (trace: " + Arrays.toString(stes) + ")");
    }

    private static boolean isSuspendableOrUnknown(String className, String methodName) {
        Boolean res = Retransform.isSuspendable(className, methodName);
        if (res == null)
            return true;
        return res;
    }

    @SuppressWarnings("unchecked")
    private static boolean isInstrumented(Class clazz) {
        boolean res = clazz.isAnnotationPresent(Instrumented.class);
        if (!res)
            res = isInstrumented0(clazz); // a second chance
        return res;
    }

    private static boolean isInstrumented0(Class clazz) {
        // Sometimes, a child class does not implement any suspendable methods AND is loaded before its superclass (that does). Test for that:
        Class superclazz = clazz.getSuperclass();
        if (superclazz != null) {
            if (superclazz.isAnnotationPresent(Instrumented.class)) {
                // make sure the child class doesn't have any suspendable methods
                Method[] ms = clazz.getDeclaredMethods();
                for (Method m : ms) {
                    for (Class et : m.getExceptionTypes()) {
                        if (et.equals(SuspendExecution.class))
                            return false;
                    }
                    if (m.isAnnotationPresent(Suspendable.class))
                        return false;
                }
                return true;
            } else
                return isInstrumented0(superclazz);
        } else
            return false;
    }

// for tests only!
    @VisibleForTesting
    final boolean exec() {
        if (!Debug.isUnitTest())
            throw new AssertionError("This method can only be called by unit tests");
        return fjTask.exec();
    }

    @VisibleForTesting
    void resetState() {
        fjTask.tryUnpark();
        assert fjTask.getState() == ParkableForkJoinTask.RUNNABLE;
    }
    private static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long stateOffset;

    static {
        try {
            stateOffset = unsafe.objectFieldOffset(Fiber.class.getDeclaredField("state"));
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private boolean casState(State expected, State update) {
        return unsafe.compareAndSwapObject(this, stateOffset, expected, update);
    }

    //<editor-fold defaultstate="collapsed" desc="Recording">
    /////////// Recording ///////////////////////////////////
    protected final boolean isRecordingLevel(int level) {
        if (!Debug.isDebug())
            return false;
        final FlightRecorder.ThreadRecorder recorder = flightRecorder.get();
        if (recorder == null)
            return false;
        return recorder.recordsLevel(level);
    }

    protected final void record(int level, String clazz, String method, String format) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    protected final void record(int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    protected final void record(int level, String clazz, String method, String format, Object... args) {
        if (flightRecorder != null)
            record(flightRecorder.get(), level, clazz, method, format, args);
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, null));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5, arg6}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, new Object[]{arg1, arg2, arg3, arg4, arg5, arg6, arg7}));
    }

    private static void record(FlightRecorder.ThreadRecorder recorder, int level, String clazz, String method, String format, Object... args) {
        if (recorder != null)
            recorder.record(level, makeFlightRecorderMessage(recorder, clazz, method, format, args));
    }

    private static FlightRecorderMessage makeFlightRecorderMessage(FlightRecorder.ThreadRecorder recorder, String clazz, String method, String format, Object[] args) {
        return new FlightRecorderMessage(clazz, method, format, args);
        //return ((FlightRecorderMessageFactory) recorder.getAux()).makeFlightRecorderMessage(clazz, method, format, args);
    }
    //</editor-fold>
    private static final FibersMonitor NOOP_FIBERS_MONITOR = new NoopFibersMonitor();

    private static StackTraceElement[] skipStackTraceElements(StackTraceElement[] st, int skip) {
        final StackTraceElement[] st1 = new StackTraceElement[st.length - skip];
        System.arraycopy(st, skip, st1, 0, st1.length);
        return st1;
    }
}

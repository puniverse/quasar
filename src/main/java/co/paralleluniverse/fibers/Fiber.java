package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.common.util.NamingThreadFactory;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import co.paralleluniverse.concurrent.util.UtilUnsafe;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinWorkerThread;
import sun.misc.Unsafe;

/**
 * A lightweight thread.
 * <p/>
 * A Fiber can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}.
 * <p/>
 * A new Fiber occupies under 400 bytes of memory (when using the default stack size, and compressed OOPs are turned on, as they are by default).
 *
 * @author Ron Pressler
 */
public class Fiber<V> extends Strand implements Joinable<V>, Serializable {
    private static final boolean verifyInstrumentation = Boolean.parseBoolean(System.getProperty("co.paralleluniverse.lwthreads.verifyInstrumentation", "false"));
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final long serialVersionUID = 2783452871536981L;

    static {
        if (verifyInstrumentation)
            System.err.println("WARNING: Fiber is set to verify instrumentation. This may severely harm performance");
    }

    public static enum State {
        NEW, STARTED, RUNNING, WAITING, TERMINATED
    };
    private static final ScheduledExecutorService timeoutService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("fiber-timeout"));
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    //
    private final ForkJoinPool fjPool;
    private final FiberForkJoinTask<V> fjTask;
    private final Stack stack;
    private final Fiber<?> parent;
    private final String name;
    private volatile State state;
    private volatile boolean interrupted;
    private final SuspendableCallable<V> target;
    private Object fiberLocals;
    private Object inheritableFiberLocals;
    private long sleepStart;
    private PostParkActions postParkActions;
    private V result;
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
    private final DummyRunnable fiberRef = new DummyRunnable(this);

    /**
     * Creates a new Fiber from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the Fiber.
     * @param stackSize the initial stack size for the data stack
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public Fiber(String name, ForkJoinPool fjPool, int stackSize, SuspendableCallable<V> target) {
        this.name = name;
        this.fjPool = fjPool;
        this.parent = currentFiber();
        this.target = target;
        this.fjTask = new FiberForkJoinTask<V>(this);
        this.stack = new Stack(this, stackSize > 0 ? stackSize : DEFAULT_STACK_SIZE);
        this.state = State.NEW;

        if (target != null) {
            if (!(target instanceof VoidSuspendableCallable) && !isInstrumented(target.getClass()))
                throw new IllegalArgumentException("Target class " + target.getClass() + " has not been instrumented.");

            if (target instanceof Stranded)
                ((Stranded) target).setStrand(this);
        } else if (!isInstrumented(this.getClass())) {
            throw new IllegalArgumentException("Fiber class " + this.getClass() + " has not been instrumented.");
        }

        final Thread currentThread = Thread.currentThread();
        Object inheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);
        if (inheritableThreadLocals != null)
            this.inheritableFiberLocals = ThreadAccess.createInheritedMap(inheritableThreadLocals);
    }

    public Fiber(String name, int stackSize, SuspendableCallable<V> target) {
        this(name, verifyParent().fjPool, stackSize, target);
    }

    private static Fiber verifyParent() {
        final Fiber parent = currentFiber();
        if (parent == null)
            throw new IllegalStateException("This constructor may only be used from within a Fiber");
        return parent;
    }

    protected static SuspendableCallable<Void> wrap(SuspendableRunnable runnable) {
        if (!isInstrumented(runnable.getClass()))
            throw new IllegalArgumentException("Target class " + runnable.getClass() + " has not been instrumented.");
        return new VoidSuspendableCallable(runnable);
    }

    private static class VoidSuspendableCallable implements SuspendableCallable<Void> {
        private final SuspendableRunnable runnable;

        public VoidSuspendableCallable(SuspendableRunnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public Void run() throws SuspendExecution, InterruptedException {
            runnable.run();
            return null;
        }
    }

    public SuspendableCallable<V> getTarget() {
        return target;
    }
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////

    public Fiber(ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(null, fjPool, -1, target);
    }

    public Fiber(String name, ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(name, fjPool, -1, target);
    }

    public Fiber(SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    public Fiber(String name, SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    public Fiber(int stackSize, SuspendableCallable<V> target) {
        this(null, stackSize, target);
    }

    public Fiber(ForkJoinPool fjPool) {
        this(null, fjPool, -1, (SuspendableCallable) null);
    }

    public Fiber(String name, ForkJoinPool fjPool) {
        this(name, fjPool, -1, (SuspendableCallable) null);
    }

    public Fiber() {
        this(null, -1, (SuspendableCallable) null);
    }

    public Fiber(String name) {
        this(name, -1, (SuspendableCallable) null);
    }

    public Fiber(int stackSize) {
        this(null, stackSize, (SuspendableCallable) null);
    }

    public Fiber(String name, int stackSize) {
        this(name, stackSize, (SuspendableCallable) null);
    }

    public Fiber(ForkJoinPool fjPool, int stackSize) {
        this(null, fjPool, stackSize, (SuspendableCallable) null);
    }

    public Fiber(String name, ForkJoinPool fjPool, int stackSize) {
        this(name, fjPool, stackSize, (SuspendableCallable) null);
    }

    public Fiber(String name, ForkJoinPool fjPool, int stackSize, SuspendableRunnable target) {
        this(name, fjPool, stackSize, (SuspendableCallable<V>) wrap(target));
    }

    public Fiber(String name, int stackSize, SuspendableRunnable target) {
        this(name, stackSize, (SuspendableCallable<V>) wrap(target));
    }

    public Fiber(ForkJoinPool fjPool, SuspendableRunnable target) {
        this(null, fjPool, -1, target);
    }

    public Fiber(String name, ForkJoinPool fjPool, SuspendableRunnable target) {
        this(name, fjPool, -1, target);
    }

    public Fiber(SuspendableRunnable target) {
        this(null, -1, target);
    }

    public Fiber(String name, SuspendableRunnable target) {
        this(name, -1, target);
    }

    public Fiber(int stackSize, SuspendableRunnable target) {
        this(null, stackSize, target);
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
    public Object getUnderlying() {
        return this;
    }

    /**
     * Suspend the currently running Fiber.
     *
     * @throws SuspendExecution This exception is used for control transfer and must never be caught.
     * @throws IllegalStateException If not called from a Fiber
     */
    public static boolean park(Object blocker, PostParkActions postParkActions, long timeout, TimeUnit unit) throws SuspendExecution {
        return verifySuspend().park1(blocker, postParkActions, timeout, unit);
    }

    public static boolean park(Object blocker, PostParkActions postParkActions) throws SuspendExecution {
        return park(blocker, postParkActions, 0, null);
    }

    public static boolean park(PostParkActions postParkActions) throws SuspendExecution {
        return park(null, postParkActions, 0, null);
    }

    public static void park(Object blocker, long timeout, TimeUnit unit) throws SuspendExecution {
        park(blocker, null, timeout, unit);
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

    public static void sleep(long millis) throws SuspendExecution {
        verifySuspend().sleep1(millis);
    }

    public static boolean interrupted() {
        final Fiber current = verifySuspend();
        final boolean interrupted = current.isInterrupted();
        if (interrupted)
            current.interrupted = false;
        return interrupted;
    }

    private boolean park1(Object blocker, PostParkActions postParkActions, long timeout, TimeUnit unit) throws SuspendExecution {
        this.postParkActions = postParkActions;
        if (timeout > 0 & unit != null) {
            timeoutService.schedule(new Runnable() {
                @Override
                public void run() {
                    fjTask.unpark();
                }
            }, timeout, unit);
        }
        return fjTask.park1(blocker);
    }

    private void yield1() throws SuspendExecution {
        fjTask.yield1();
    }

    protected void postRestore() {
        if (interrupted)
            throw new FiberInterruptedException();
    }

    private boolean exec1() {
        if (fjTask.isDone() | state == State.RUNNING)
            throw new IllegalStateException("Not new or suspended");

        setCurrentFiber(this);
        installFiberLocals();

        state = State.RUNNING;
        try {
            this.result = run1(); // we jump into the continuation

            state = State.TERMINATED;
            return true;
        } catch (SuspendExecution ex) {
            assert ex == SuspendExecution.instance;
            //stack.dump();
            stack.resumeStack();
            state = State.WAITING;
            fjTask.doPark(false); // now we can complete parking

            if (postParkActions != null) {
                postParkActions.run(this);
                postParkActions = null;
            }
            return false;
        } catch (InterruptedException e) {
            state = State.TERMINATED;
            throw new RuntimeException(e);
        } catch (Throwable t) {
            state = State.TERMINATED;
            throw t;
        } finally {
            setCurrentFiber(null);
        }
    }

    private void installFiberLocals() {
        if(fjPool == null) // in tests
            return;
        
        final Thread currentThread = Thread.currentThread();

        Object tmpThreadLocals = ThreadAccess.getThreadLocals(currentThread);
        Object tmpInheritableThreadLocals = ThreadAccess.getInheritableThreadLocals(currentThread);

        ThreadAccess.setThreadLocals(currentThread, this.fiberLocals);
        ThreadAccess.setInheritablehreadLocals(currentThread, this.inheritableFiberLocals);

        this.fiberLocals = tmpThreadLocals;
        this.inheritableFiberLocals = tmpInheritableThreadLocals;
    }

    private void restoreThreadLocals() {
        installFiberLocals();
    }

    private void setCurrentFiber(Fiber fiber) {
        if (fjPool == null) // in tests
            return;
        final Thread currentThread = Thread.currentThread();
        if (ThreadAccess.getTarget(currentThread) != null && fiber != null)
            throw new RuntimeException("Fiber " + fiber + " target: " + ThreadAccess.getTarget(currentThread));
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
        } else {
            try {
                final FiberForkJoinTask currentFJTask = FiberForkJoinTask.getCurrent();
                if (currentFJTask == null)
                    return null;
                return currentFJTask.getFiber();
            } catch (ClassCastException e) {
                return null;
            }
        }
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
     *
     * @return {@code this}
     */
    @Override
    public Fiber start() {
        if (!casState(State.NEW, State.STARTED))
            throw new IllegalStateException("Fiber has already been started or has died");
        fjTask.submit();
        return this;
    }

    protected void onCompletion() {
        restoreThreadLocals();
    }

    protected void onParked() {
        restoreThreadLocals();
    }

    protected void onException(Throwable t) {
        if (uncaughtExceptionHandler != null)
            uncaughtExceptionHandler.uncaughtException(this, t);
        else if (defaultUncaughtExceptionHandler != null)
            defaultUncaughtExceptionHandler.uncaughtException(this, t);
        else
            Exceptions.rethrow(t);
        // swallow exception
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
     * @return
     */
    @Override
    public final boolean isAlive() {
        return state != State.NEW && !fjTask.isDone();
    }

    @Override
    public final Object getBlocker() {
        return fjTask.getBlocker();
    }

    public final void setBlocker(Object blocker) {
        fjTask.setBlocker(blocker);
    }

    public final State getState() {
        return state;
    }

    public Fiber getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Executes LWT on this thread, after waiting until the given blocker is indeed the LWT's blocker, and that the LWT is not being run concurrently.
     *
     * @param blocker
     * @return
     */
    public final boolean exec(Object blocker) {
        for (int i = 0; i < 30; i++) {
            if (getBlocker() == blocker && fjTask.tryUnpark()) {
                fjTask.exec();
                return true;
            }
        }
        return false;
    }

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
    public boolean isDone() {
        return state == State.TERMINATED;
    }
    
    private void sleep1(long millis) throws SuspendExecution {
        // this class's methods aren't instrumented, so we can't rely on the stack. This method will be called again when unparked
        try {
            for (;;) {
                postRestore();
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

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return defaultUncaughtExceptionHandler;
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler defaultUncaughtExceptionHandler) {
        Fiber.defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler;
    }

    public static void dumpStack() {
        verifyCurrent();
        printStackTrace(new Exception("Stack trace"), System.err);
    }

    @SuppressWarnings("CallToThrowablePrintStackTrace")
    public static void printStackTrace(Throwable t, OutputStream out) {
        t.printStackTrace(new PrintStream(out) {
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
    public String toString() {
        return "Fiber@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this))) + "[state: " + state + " pool=" + fjPool + ", task=" + fjTask + ']';
    }

    ////////
    static final class FiberForkJoinTask<V> extends ParkableForkJoinTask<V> {
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
            throw SuspendExecution.instance;
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

    public static interface PostParkActions {
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
        try {
            for (StackTraceElement ste : stes) {
                if (ste.getClassName().equals(Thread.class.getName()) && ste.getMethodName().equals("getStackTrace"))
                    continue;
                if (!ste.getClassName().equals(Fiber.class.getName()) && !ste.getClassName().startsWith(Fiber.class.getName() + '$')) {
                    if (!isInstrumented(Class.forName(ste.getClassName())))
                        throw new IllegalStateException("Method " + ste.getClassName() + "." + ste.getMethodName() + " on the call-stack has not been instrumented. (trace: " + Arrays.toString(stes) + ")");
                } else if (ste.getMethodName().equals("run1"))
                    return true;
            }
            return false;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Not run through Fiber.exec(). (trace: " + Arrays.toString(stes) + ")");
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isInstrumented(Class clazz) {
        return clazz.isAnnotationPresent(Instrumented.class);
    }

// for tests only!
    @VisibleForTesting
    final boolean exec() {
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
}

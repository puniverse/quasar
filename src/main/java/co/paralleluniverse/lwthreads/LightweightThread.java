package co.paralleluniverse.lwthreads;

import co.paralleluniverse.common.util.NamingThreadFactory;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
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

/**
 * A LightweightThread.
 * <p/>
 * A LightweightThread can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}.
 * <p/>
 * A new LightweightThread occupies under 400 bytes of memory (when using the default stack size, and compressed OOPs are turned on, as they are by default).
 *
 * @author Ron Pressler
 */
public class LightweightThread<V> implements Serializable {
    private static final boolean verifyInstrumentation = Boolean.parseBoolean(System.getProperty("co.paralleluniverse.lwthreads.verifyInstrumentation", "false"));
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final long serialVersionUID = 2783452871536981L;

    static {
        if (verifyInstrumentation)
            System.err.println("WARNING: LightweightThread is set to verify instrumentation. This may severely harm performance");
    }

    public static enum State {
        NEW, RUNNING, WAITING, TERMINATED
    };
    private static final ScheduledExecutorService timeoutService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("lightweight-thread-timeout"));
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    //
    private final ForkJoinPool fjPool;
    private final LightweightThreadForkJoinTask<V> fjTask;
    private final Stack stack;
    private final LightweightThread<?> parent;
    private final String name;
    private volatile State state;
    private volatile boolean interrupted;
    private final SuspendableCallable<V> target;
    LightweightThreadLocal.LWThreadLocalMap lwthreadLocals;
    private long sleepStart;
    private PostParkActions postParkActions;
    private V result;
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    /**
     * Creates a new LightweightThread from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the LightweightThread.
     * @param stackSize the initial stack size for the data stack
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public LightweightThread(String name, ForkJoinPool fjPool, int stackSize, SuspendableCallable<V> target) {
        this.name = name;
        this.fjPool = fjPool;
        this.parent = currentLightweightThread();
        this.target = target;
        this.fjTask = new LightweightThreadForkJoinTask<V>(this);
        this.stack = new Stack(this, stackSize > 0 ? stackSize : DEFAULT_STACK_SIZE);
        this.state = State.NEW;

        if (target != null) {
            if (!(target instanceof VoidSuspendableCallable) && !isInstrumented(target.getClass()))
                throw new IllegalArgumentException("Target class " + target.getClass() + " has not been instrumented.");
        } else if (!isInstrumented(this.getClass())) {
            throw new IllegalArgumentException("LightweightThread class " + this.getClass() + " has not been instrumented.");
        }
    }

    public LightweightThread(String name, int stackSize, SuspendableCallable<V> target) {
        this(name, verifyParent().fjPool, stackSize, target);
    }

    private static LightweightThread verifyParent() {
        final LightweightThread parent = currentLightweightThread();
        if (parent == null)
            throw new IllegalStateException("This constructor may only be used from within a LightweightThread");
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

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public LightweightThread(ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(null, fjPool, -1, target);
    }

    public LightweightThread(String name, ForkJoinPool fjPool, SuspendableCallable<V> target) {
        this(name, fjPool, -1, target);
    }

    public LightweightThread(SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    public LightweightThread(String name, SuspendableCallable<V> target) {
        this(null, -1, target);
    }

    public LightweightThread(int stackSize, SuspendableCallable<V> target) {
        this(null, stackSize, target);
    }

    public LightweightThread(ForkJoinPool fjPool) {
        this(null, fjPool, -1, (SuspendableCallable) null);
    }

    public LightweightThread(String name, ForkJoinPool fjPool) {
        this(name, fjPool, -1, (SuspendableCallable) null);
    }

    public LightweightThread() {
        this(null, -1, (SuspendableCallable) null);
    }

    public LightweightThread(String name) {
        this(name, -1, (SuspendableCallable) null);
    }

    public LightweightThread(int stackSize) {
        this(null, stackSize, (SuspendableCallable) null);
    }

    public LightweightThread(String name, int stackSize) {
        this(name, stackSize, (SuspendableCallable) null);
    }

    public LightweightThread(ForkJoinPool fjPool, int stackSize) {
        this(null, fjPool, stackSize, (SuspendableCallable) null);
    }

    public LightweightThread(String name, ForkJoinPool fjPool, int stackSize) {
        this(name, fjPool, stackSize, (SuspendableCallable) null);
    }

    public LightweightThread(String name, ForkJoinPool fjPool, int stackSize, SuspendableRunnable target) {
        this(name, fjPool, stackSize, (SuspendableCallable<V>) wrap(target));
    }

    public LightweightThread(String name, int stackSize, SuspendableRunnable target) {
        this(name, stackSize, (SuspendableCallable<V>) wrap(target));
    }

    public LightweightThread(ForkJoinPool fjPool, SuspendableRunnable target) {
        this(null, fjPool, -1, target);
    }

    public LightweightThread(String name, ForkJoinPool fjPool, SuspendableRunnable target) {
        this(name, fjPool, -1, target);
    }

    public LightweightThread(SuspendableRunnable target) {
        this(null, -1, target);
    }

    public LightweightThread(String name, SuspendableRunnable target) {
        this(name, -1, target);
    }

    public LightweightThread(int stackSize, SuspendableRunnable target) {
        this(null, stackSize, target);
    }
    //</editor-fold>

    /**
     * Returns the active LightweightThread on this thread or NULL if no LightweightThread is running.
     *
     * @return the active LightweightThread on this thread or NULL if no LightweightThread is running.
     */
    public static LightweightThread currentLightweightThread() {
        try {
            final LightweightThreadForkJoinTask currentFJTask = LightweightThreadForkJoinTask.getCurrent();
            if (currentFJTask == null)
                return null;
            return currentFJTask.getLightweightThread();
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Suspend the currently running LightweightThread.
     *
     * @throws SuspendExecution This exception is used for control transfer and must never be caught.
     * @throws IllegalStateException If not called from a LightweightThread
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
        final LightweightThread current = verifySuspend();
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
            throw new LwtInterruptedException();
    }

    private boolean exec1() {
        if (!isAlive() | state == State.RUNNING)
            throw new IllegalStateException("Not new or suspended");

        state = State.RUNNING;
        try {
            this.result = run1();

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
    public LightweightThread start() {
        if (state != State.NEW)
            throw new IllegalStateException("LightweightThread has already been started");
        fjTask.submit();
        return this;
    }

    protected void onCompletion() {
    }

    protected void onException(Throwable t) {
        if (uncaughtExceptionHandler != null)
            uncaughtExceptionHandler.uncaughtException(this, t);
        else if (defaultUncaughtExceptionHandler != null)
            defaultUncaughtExceptionHandler.uncaughtException(this, t);
        else
            printStackTrace(t, System.err);
        // swallow exception
    }

    public final void interrupt() {
        interrupted = true;
        unpark();
    }

    public final boolean isInterrupted() {
        return interrupted;
    }

    public final boolean isAlive() {
        return !fjTask.isDone();
    }

    public final Object getBlocker() {
        return fjTask.getBlocker();
    }

    public final void setBlocker(Object blocker) {
        fjTask.setBlocker(blocker);
    }

    public final State getState() {
        return state;
    }

    public LightweightThread getParent() {
        return parent;
    }

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

    public final void unpark() {
        fjTask.unpark();
    }

    public final void join() throws ExecutionException, InterruptedException {
        get();
    }

    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        get(timeout, unit);
    }

    public final V get() throws ExecutionException, InterruptedException {
        return fjTask.get();
    }

    public final V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return fjTask.get(timeout, unit);
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
        LightweightThread.defaultUncaughtExceptionHandler = defaultUncaughtExceptionHandler;
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
                    if (x.startsWith("\tat " + LightweightThread.class.getName() + ".exec1")) {
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
        return "LightweightThread@" + (name != null ? name : Integer.toHexString(System.identityHashCode(this))) + "[state: " + state + " pool=" + fjPool + ", task=" + fjTask + ']';
    }

    ////////
    static final class LightweightThreadForkJoinTask<V> extends ParkableForkJoinTask<V> {
        private final LightweightThread<V> lwt;

        public LightweightThreadForkJoinTask(LightweightThread<V> lwThread) {
            this.lwt = lwThread;
        }

        protected static LightweightThreadForkJoinTask getCurrent() {
            return (LightweightThreadForkJoinTask) ParkableForkJoinTask.getCurrent();
        }

        LightweightThread getLightweightThread() {
            return lwt;
        }

        @Override
        protected void submit() {
            if (getPool() == lwt.fjPool)
                fork();
            else
                lwt.fjPool.submit(this);
        }

        @Override
        protected boolean exec1() {
            return lwt.exec1();
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
        protected void throwPark(boolean yield) throws SuspendExecution {
            throw SuspendExecution.instance;
        }

        @Override
        protected void onException(Throwable t) {
            lwt.onException(t);
        }

        @Override
        protected void onCompletion(boolean res) {
            if (res)
                lwt.onCompletion();
        }

        @Override
        public V getRawResult() {
            return lwt.result;
        }

        @Override
        protected void setRawResult(V v) {
            lwt.result = v;
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
        void uncaughtException(LightweightThread lwt, Throwable e);
    }

    //////////////////////////////////////////////////
    final Stack getStack() {
        return stack;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (state == State.RUNNING)
            throw new IllegalStateException("trying to serialize a running LightweightThread");
        out.defaultWriteObject();
    }

    public static interface PostParkActions {
        void run(LightweightThread current);
    }

    private static LightweightThread verifySuspend() {
        final LightweightThread current = verifyCurrent();
        if (verifyInstrumentation)
            assert checkInstrumentation();
        return current;
    }

    private static LightweightThread verifyCurrent() {
        final LightweightThread current = currentLightweightThread();
        if (current == null)
            throw new IllegalStateException("Not called from withing a LightweightThread");
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
                if (!ste.getClassName().equals(LightweightThread.class.getName()) && !ste.getClassName().startsWith(LightweightThread.class.getName() + '$')) {
                    if (!isInstrumented(Class.forName(ste.getClassName())))
                        throw new IllegalStateException("Method " + ste.getClassName() + "." + ste.getMethodName() + " on the call-stack has not been instrumented. (trace: " + Arrays.toString(stes) + ")");
                } else if (ste.getMethodName().equals("run1"))
                    return true;
            }
            return false;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Not run through LightweightThread.exec(). (trace: " + Arrays.toString(stes) + ")");
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
}

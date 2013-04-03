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
public class LightweightThread implements Serializable {
    private static final boolean verifyInstrumentation = Boolean.parseBoolean(System.getProperty("co.paralleluniverse.lwthreads.verifyInstrumentation", "false"));
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final long serialVersionUID = 2783452871536981L;

    public static enum State {
        NEW, RUNNING, WAITING, TERMINATED
    };
    private static final ScheduledExecutorService timeoutService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("lightweight-thread-timeout"));
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    //
    private final ForkJoinPool fjPool;
    private final LightweightThreadForkJoinTask fjTask;
    private final Stack stack;
    private volatile State state;
    private volatile boolean interrupted;
    private final SuspendableRunnable target;
    LightweightThreadLocal.LWThreadLocalMap lwthreadLocals;
    private long sleepStart;
    private PostParkActions postParkActions;
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    /**
     * Creates a new LightweightThread from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the LightweightThread.
     */
    public LightweightThread(ForkJoinPool fjPool, SuspendableRunnable target) {
        this(fjPool, target, DEFAULT_STACK_SIZE);
    }

    public LightweightThread(ForkJoinPool fjPool) {
        this(fjPool, null, DEFAULT_STACK_SIZE);
    }

    public LightweightThread(ForkJoinPool fjPool, int stackSize) {
        this(fjPool, null, stackSize);
    }

    /**
     * Creates a new LightweightThread from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the LightweightThread.
     * @param stackSize the initial stack size for the data stack
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public LightweightThread(ForkJoinPool fjPool, SuspendableRunnable target, int stackSize) {
        this.fjPool = fjPool;
        this.target = target;
        this.fjTask = new LightweightThreadForkJoinTask(this);
        this.stack = new Stack(this, stackSize);
        this.state = State.NEW;

        if (target != null) {
            if (!isInstrumented(target.getClass()))
                throw new IllegalArgumentException("Target class " + target.getClass() + " has not been instrumented.");
        } else if (!isInstrumented(this.getClass())) {
            throw new IllegalArgumentException("LightweightThread class " + this.getClass() + " has not been instrumented.");
        }
    }

    /**
     * Returns the active LightweightThread on this thread or NULL if no LightweightThread is running.
     *
     * @return the active LightweightThread on this thread or NULL if no LightweightThread is running.
     */
    public static LightweightThread currentLightweightThread() {
        try {
            return LightweightThreadForkJoinTask.getCurrent().getLightweightThread();
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
            run();
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
        } catch (Throwable t) {
            state = State.TERMINATED;
            throw t;
        }
    }

    protected void run() throws SuspendExecution {
        if (target != null)
            target.run();
    }

    /**
     * 
     * @return {@code this}
     */
    public final LightweightThread start() {
        if (state != State.NEW)
            throw new IllegalStateException("LightweightThread has already been started");
        fjTask.submit();
        return this;
    }

    private void onException(Throwable t) {
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

    public final State getState() {
        return state;
    }

    /**
     * Executes LWT on this thread, after waiting until the given blocker is indeed the LWT's blocker, and that the LWT is not being run concurrently.
     *
     * @param blocker
     * @return
     */
    public final boolean exec(Object blocker) {
        for (;;) {
            if (fjTask.getBlocker() == blocker && fjTask.tryUnpark())
                return fjTask.exec();
        }
    }

    public final void unpark() {
        fjTask.unpark();
    }

    public final void join() throws InterruptedException {
        try {
            fjTask.get();
        } catch (ExecutionException ex) {
            throw new AssertionError(ex);
        }
    }

    public final void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            fjTask.get(timeout, unit);
        } catch (ExecutionException ex) {
            throw new AssertionError(ex);
        }
    }

    private final void sleep1(long millis) throws SuspendExecution {
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
        } catch(Throwable t) {
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

    public static void printStackTrace(Throwable t, OutputStream out) {
        t.printStackTrace(new PrintStream(out) {
            boolean seenExec;

            @Override
            public void println(String x) {
                if (seenExec)
                    return;
                if (x.startsWith("\tat " + LightweightThread.class.getName() + ".exec1")) {
                    seenExec = true;
                    return;
                }
                super.println(x);
            }
        });
    }

    @Override
    public String toString() {
        return "LightweightThread@" + Integer.toHexString(System.identityHashCode(this)) + "[state: " + state + " pool=" + fjPool + ", task=" + fjTask + ']';
    }

    ////////
    static class LightweightThreadForkJoinTask extends ParkableForkJoinTask<Void> {
        private final LightweightThread lwt;

        public LightweightThreadForkJoinTask(LightweightThread lwThread) {
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
        @SuppressWarnings("CallToThrowablePrintStackTrace")
        protected void onException(Throwable t) {
            lwt.onException(t);
        }

        @Override
        public Void getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Void v) {
        }

        @Override
        protected final int getState() {
            return super.getState();
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
            verifyInstrumentation();
        return current;
    }
    
    private static LightweightThread verifyCurrent() {
        final LightweightThread current = currentLightweightThread();
        if (current == null)
            throw new IllegalStateException("Not called from withing a LightweightThread");
        return current;
    }

    private static void verifyInstrumentation() {
        if (!verifyInstrumentation)
            throw new AssertionError();
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        try {
            for (StackTraceElement ste : stes) {
                if (!ste.getClassName().equals(LightweightThread.class.getName())) {
                    if (!isInstrumented(Class.forName(ste.getClassName())))
                        throw new IllegalStateException("Method " + ste.getClassName() + "." + ste.getMethodName() + " on the call-stack has not been instrumented. (trace: " + Arrays.toString(stes) + ")");
                } else if (ste.getMethodName().equals("run"))
                    return;
            }
            throw new Error();
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

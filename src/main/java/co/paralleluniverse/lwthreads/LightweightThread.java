package co.paralleluniverse.lwthreads;

import co.paralleluniverse.common.util.NamingThreadFactory;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

/**
 * A LightweightThread.
 * <p/>
 * A LightweightThread can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}.
 *
 * @author Ron Pressler
 */
public class LightweightThread extends ParkableForkJoinTask<Void> implements Serializable {
    private static final boolean verifyInstrumentation = Boolean.parseBoolean(System.getProperty("co.paralleluniverse.lwthreads.verifyInstrumentation", "false"));
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final long serialVersionUID = 2783452871536981L;
    private static final ScheduledExecutorService timeoutService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("lightweight-thread-timeout"));
    //
    private final ForkJoinPool fjPool;
    private final Stack stack;
    private volatile boolean running;
    private volatile boolean interrupted;
    LightweightThreadLocal.LWThreadLocalMap lwthreadLocals;
    private final SuspendableRunnable target;
    private PostParkActions postParkActions;

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
        this.stack = new Stack(this, stackSize);

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
            return (LightweightThread) ParkableForkJoinTask.getCurrent();
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

    public static boolean interrupted() {
        final LightweightThread current = verifySuspend();
        final boolean interrupted = current.isInterrupted();
        if (interrupted)
            current.interrupted = false;
        return interrupted;
    }

    protected boolean park1(Object blocker, PostParkActions postParkActions, long timeout, TimeUnit unit) throws SuspendExecution {
        this.postParkActions = postParkActions;
        if (timeout > 0 & unit != null) {
            timeoutService.schedule(new Runnable() {
                @Override
                public void run() {
                    unpark();
                }
            }, timeout, unit);
        }
        return park1(blocker);
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

    protected void postRestore() {
        if (interrupted)
            throw new LwtInterruptedException();
    }

    @Override
    protected void submit() {
        if (getPool() == fjPool)
            fork();
        fjPool.submit(this);
    }

    @Override
    protected final boolean exec1() {
        if (isDone() | running)
            throw new IllegalStateException("Not new or suspended");

        try {
            running = true;
            if (isInterrupted())
                throw new LwtInterruptedException();
            boolean finished = true;
            try {
                run();
            } catch (SuspendExecution ex) {
                assert ex == SuspendExecution.instance;
                finished = false;
                //stack.dump();
                stack.resumeStack();
            }
            if (postParkActions != null) {
                postParkActions.run(this);
                postParkActions = null;
            }
            return finished;
        } finally {
            running = false;
        }
    }

    @Override
    protected void throwPark(boolean yield) throws SuspendExecution {
        throw SuspendExecution.instance;
    }

    protected void run() throws SuspendExecution {
        if (target != null)
            target.run();
    }

    public void interrupt() {
        interrupted = true;
        unpark();
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    //////////////////////////////////////////////////
    @VisibleForTesting
    @Override
    protected final int getState() {
        return super.getState();
    }

    @VisibleForTesting
    @Override
    protected final boolean exec() {
        return super.exec();
    }

    final Stack getStack() {
        return stack;
    }

    @Override
    public final Void getRawResult() {
        return null;
    }

    @Override
    protected final void setRawResult(Void v) {
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (running)
            throw new IllegalStateException("trying to serialize a running LightweightThread");
        out.defaultWriteObject();
    }

    public static interface PostParkActions {
        void run(LightweightThread current);
    }

    private static LightweightThread verifySuspend() {
        final LightweightThread current = currentLightweightThread();
        if (current == null)
            throw new IllegalStateException("Not called from withing a LightweightThread");
        if (verifyInstrumentation)
            verifyInstrumentation();
        return current;
    }

    private static void verifyInstrumentation() {
        if (!verifyInstrumentation)
            throw new AssertionError();
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        try {
            for (StackTraceElement ste : stes) {
                if (ste.getClassName().equals(LightweightThread.class.getName()) && ste.getMethodName().equals("run"))
                    return;
                if (!isInstrumented(Class.forName(ste.getClassName())))
                    throw new IllegalStateException("Method " + ste.getClassName() + "." + ste.getMethodName() + " on the call-stack has not been instrumented. (trace: " + Arrays.toString(stes) + ")");
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
    void resetState() {
        tryUnpark();
        assert getState() == RUNNING;
    }
}

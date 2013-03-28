package co.paralleluniverse.concurrent.lwthreads;

import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import java.io.IOException;
import java.io.Serializable;

/**
 * <p>A LightweightThread.</p>
 *
 * <p>A LightweightThread can be serialized if it's not running and all involved
 * classes and data types are also {@link Serializable}.</p>
 *
 * @author Ron Pressler
 * @author Matthias Mann
 */
public class LightweightThread extends ParkableForkJoinTask<Void> implements Serializable {
    public static final int DEFAULT_STACK_SIZE = 16;
    private static final long serialVersionUID = 2783452871536981L;
    private final SuspendableRunnable target;
    private final Stack stack;
    private boolean running;
    LightweightThreadLocal.LWThreadLocalMap lwthreadLocals;

    /**
     * Creates a new LightweightThread from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the LightweightThread.
     */
    public LightweightThread(SuspendableRunnable target) {
        this(target, DEFAULT_STACK_SIZE);
    }

    public LightweightThread() {
        this(null, DEFAULT_STACK_SIZE);
    }

    public LightweightThread(int stackSize) {
        this(null, stackSize);
    }

    /**
     * Creates a new LightweightThread from the given SuspendableRunnable.
     *
     * @param target the SuspendableRunnable for the LightweightThread.
     * @param stackSize the initial stack size for the data stack
     * @throws NullPointerException when proto is null
     * @throws IllegalArgumentException when stackSize is &lt;= 0
     */
    public LightweightThread(SuspendableRunnable target, int stackSize) {
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
     * @throws SuspendExecution This exception is used for control transfer - don't catch it !
     * @throws IllegalStateException If not called from a LightweightThread
     */
    public static void suspend() throws SuspendExecution, IllegalStateException {
        throw new Error("Calling function not instrumented");
    }

    @Override
    protected final boolean exec1() {
        if (isDone() | running)
            throw new IllegalStateException("Not new or suspended");

        try {
            running = true;
            boolean finished = true;
            try {
                run();
            } catch (SuspendExecution ex) {
                assert ex == SuspendExecution.instance;
                finished = false;
                //stack.dump();
                stack.resumeStack();
            }
            return finished;
        } finally {
            running = false;
        }
    }

    protected void run() throws SuspendExecution {
        if (target != null)
            target.run();
    }

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
    private static final Class alreadyInstrumentedAnnotation;

    static {
        Class clz = null;
        try {
            clz = Class.forName("co.paralleluniverse.concurrent.lwthreads.instrument.AlreadyInstrumented");
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }
        alreadyInstrumentedAnnotation = clz;
    }

    @SuppressWarnings("unchecked")
    private static boolean isInstrumented(Class clazz) {
        if (alreadyInstrumentedAnnotation != null)
            return clazz.isAnnotationPresent(alreadyInstrumentedAnnotation);
        return true; // can't check
    }
}

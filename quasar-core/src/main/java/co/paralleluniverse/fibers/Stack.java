package co.paralleluniverse.fibers;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Internal Class - DO NOT USE !
 *
 * Needs to be public so that instrumented code can access it.
 * ANY CHANGE IN THIS CLASS NEEDS TO BE SYNCHRONIZED WITH {@link co.paralleluniverse.fibers.instrument.InstrumentMethod}
 *
 * @author Matthias Mann
 * @author Ron Pressler
 */
public final class Stack implements Serializable {
    private static final long serialVersionUID = 12786283751253L;
    private final Fiber fiber;
    private int methodTOS = -1;
    private int[] method;           // holds each method's entry point as well as stack pointer
    private long[] dataLong;        // holds primitives on stack
    private Object[] dataObject;    // holds refs on stack
    private transient int curMethodSP;
    static final ThreadLocal<Stack> getStackTrace = new ThreadLocal<Stack>();

    Stack(Fiber lwThread, int stackSize) {
        if (stackSize <= 0) {
            throw new IllegalArgumentException("stackSize");
        }
        this.fiber = lwThread;
        this.method = new int[8];
        this.dataLong = new long[stackSize];
        this.dataObject = new Object[stackSize];
    }

    public static Stack getStack() {
        final Fiber currentFiber = Fiber.currentFiber();
        if (currentFiber == null)
            return getStackTrace.get(); // throw new RuntimeException("Not running in a fiber");
        return currentFiber.getStack();
    }

    /**
     * Called before a method is called.
     *
     * @param entry the entry point in the method for resume
     * @param numSlots the number of required stack slots for storing the state
     */
    public final void pushMethod(int entry, int numSlots) {
        final int methodIdx = methodTOS;

        if (method.length - methodIdx < 2)
            growMethodStack();

        curMethodSP = method[methodIdx - 1];
        final int dataTOS = curMethodSP + numSlots;

        method[methodIdx] = entry;
        method[methodIdx + 1] = dataTOS;

        if (dataTOS > dataObject.length)
            growDataStack(dataTOS);

        if (fiber.isRecordingLevel(2))
            fiber.record(2, "Stack", "pushMethod     ", "%s %s", Thread.currentThread().getStackTrace()[2], entry /*Arrays.toString(fiber.getStackTrace())*/);
    }

    /**
     * Called at the end of a method.
     * Undoes the effects of nextMethodEntry() and clears the dataObject[] array
     * to allow the values to be GCed.
     */
    public final void popMethod() {
        final int idx = methodTOS;
        method[idx] = 0;
        final int oldSP = curMethodSP;
        final int newSP = method[idx - 1];
        curMethodSP = newSP;
        methodTOS = idx - 2;
        for (int i = newSP; i < oldSP; i++)
            dataObject[i] = null;

        if (fiber.isRecordingLevel(2))
            fiber.record(2, "Stack", "popMethod      ", "%s", Thread.currentThread().getStackTrace()[2] /*Arrays.toString(fiber.getStackTrace())*/);
    }

    /**
     * called at the beginning of a method
     *
     * @return the entry point of this method
     */
    public final int nextMethodEntry() {
        int idx = methodTOS;
        curMethodSP = method[++idx];
        methodTOS = ++idx;
        final int entry = method[idx];
        if (fiber.isRecordingLevel(2))
            fiber.record(2, "Stack", "nextMethodEntry", "%s %s", Thread.currentThread().getStackTrace()[2], entry /*Arrays.toString(fiber.getStackTrace())*/);
        return entry;
    }

    public static void push(int value, Stack s, int idx) {
//        if (s.fiber.recordsLevel(3))
//            s.fiber.record(3, "Stack", "push", "%d %s", idx, value);
        s.dataLong[s.curMethodSP + idx] = value;
    }

    public static void push(float value, Stack s, int idx) {
//        if (s.fiber.recordsLevel(3))
//            s.fiber.record(3, "Stack", "push", "%d %s", idx, value);
        s.dataLong[s.curMethodSP + idx] = Float.floatToRawIntBits(value);
    }

    public static void push(long value, Stack s, int idx) {
//        if (s.fiber.recordsLevel(3))
//            s.fiber.record(3, "Stack", "push", "%d %s", idx, value);
        s.dataLong[s.curMethodSP + idx] = value;
    }

    public static void push(double value, Stack s, int idx) {
//        if (s.fiber.recordsLevel(3))
//            s.fiber.record(3, "Stack", "push", "%d %s", idx, value);
        s.dataLong[s.curMethodSP + idx] = Double.doubleToRawLongBits(value);
    }

    public static void push(Object value, Stack s, int idx) {
//        if (s.fiber.recordsLevel(3))
//            s.fiber.record(3, "Stack", "push", "%d %s", idx, value);
        s.dataObject[s.curMethodSP + idx] = value;
    }

    public final int getInt(int idx) {
        final int value = (int) dataLong[curMethodSP + idx];
//        if (fiber.recordsLevel(3))
//            fiber.record(3, "Stack", "getInt", "%d %s", idx, value);
        return value;
    }

    public final float getFloat(int idx) {
        final float value = Float.intBitsToFloat((int) dataLong[curMethodSP + idx]);
//        if (fiber.recordsLevel(3))
//            fiber.record(3, "Stack", "getFloat", "%d %s", idx, value);
        return value;
    }

    public final long getLong(int idx) {
        final long value = dataLong[curMethodSP + idx];
//        if (fiber.recordsLevel(3))
//            fiber.record(3, "Stack", "getLong", "%d %s", idx, value);
        return value;
    }

    public final double getDouble(int idx) {
        final double value = Double.longBitsToDouble(dataLong[curMethodSP + idx]);
//        if (fiber.recordsLevel(3))
//            fiber.record(3, "Stack", "getDouble", "%d %s", idx, value);
        return value;
    }

    public final Object getObject(int idx) {
        final Object value = dataObject[curMethodSP + idx];
//        if (fiber.recordsLevel(3))
//            fiber.record(3, "Stack", "getObject", "%d %s", idx, value);
        return value;
    }

    public final void postRestore() throws SuspendExecution, InterruptedException {
        fiber.onResume();
    }

    public final void preemptionPoint(int type) throws SuspendExecution {
        if(fiber.shouldPreempt(type))
            fiber.preempt();
    }
    
    /**
     * called when resuming a stack
     */
    final void resumeStack() {
        methodTOS = -1;
    }

    private void growDataStack(int required) {
        int newSize = dataObject.length;
        do {
            newSize *= 2;
        } while (newSize < required);

        dataLong = Arrays.copyOf(dataLong, newSize);
        dataObject = Arrays.copyOf(dataObject, newSize);
    }

    private void growMethodStack() {
        int newSize = method.length * 2;
        method = Arrays.copyOf(method, newSize);
    }

    void dump() {
        int sp = 0;
        for (int i = 0; i <= methodTOS; i += 2) {
            System.out.println("i=" + i + " entry=" + method[i] + " sp=" + method[i + 1]);
            for (; sp < method[i + 3]; sp++)
                System.out.println("sp=" + sp + " long=" + dataLong[sp] + " obj=" + dataObject[sp]);
        }
    }
}

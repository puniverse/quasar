package co.paralleluniverse.fibers;

public final class LiveInstrumentation {
    public static final boolean ACTIVE = false;

    static boolean fixup(Fiber f) {
        return true;
    }

    private LiveInstrumentation() {}
}

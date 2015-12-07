package co.paralleluniverse.fibers;

final class LiveInstrumentation {
    static boolean fixup(Fiber f) {
        return true;
    }

    private LiveInstrumentation() {}
}

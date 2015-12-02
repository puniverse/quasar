package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.fibers.instrument.Retransform;

import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class Quasar9FiberYieldPseudoAlg {
    static void doIt() throws SuspendExecution {
        boolean checkInstrumentation = true;
        final Fiber f = Fiber.currentFiber();
        if (f != null) {
            final Stack fs = f.getStack();
            if (esw != null) {
                final long stackDepth = esw.walk(s -> s.collect(COUNTING)); // TODO: JMH

                if (stackDepthsDontMatch(stackDepth, getFiberStackDepth(fs))) {
                    // Slow path, we'll take our time to fix up things
                    final java.util.Stack<?> fiberStackRebuildToDoList = new java.util.Stack<>(); // TODO: improve perf
                    final Collection<Class<?>> toRetransform = new ArrayList<>(); // TODO: improve perf
                    esw.walk(s -> s.map(new Function<>() {
                        private boolean yield = true, callingYield = false;
                        private boolean[] ok_last = new boolean[] { true, false };

                        private ExtendedStackTraceElement upper = null;

                        @Override
                        public Void apply(StackWalker.StackFrame sf) { // Top to bottom, skipping internal & reflection
                            if (!ok_last[1]) {
                                final Class<?> c = sf.getDeclaringClass();
                                pushRebuildToDo(sf, fiberStackRebuildToDoList, callingYield);

                                if (yield) { // Skip marking/transforming yield frame TODO check that skipping just the first frame is correct
                                    LOG("Live lazy auto-instrumentation: " + sf.getClassName() + "#" + sf.getMethodName());
                                    yield = false;
                                    callingYield = true;
                                } else {
                                    Fiber.checkInstrumentation(ok_last, new ExtendedStackTraceElement(sf.toStackTraceElement()), upper);
                                    if (!ok_last[0]) {
                                        addNewSuspendableToKB(c, sf.getMethodName());
                                        toRetransform.add(c);
                                    }

                                    if (callingYield)
                                        callingYield = false;
                                }

                                upper = new ExtendedStackTraceElement(sf.toStackTraceElement());
                            }
                            return NOTHING;
                        }
                    }));
                    // Retransform classes
                    try {
                        // TODO Make it
                        Retransform.retransform(toRetransform.toArray(new Class[toRetransform.size()]));
                    } catch (final UnmodifiableClassException e) {
                        throw new RuntimeException(e);
                    }
                    // Method ref seems missing from StackFrame info => diff difficult => rebuild
                    apply(fiberStackRebuildToDoList, fs);
                    // We're done, let's skip checks
                    checkInstrumentation = false;
                }
            }
        }

        regularYield(true /* TODO after debugging pass the following instead */ /* checkInstrumentation */);
    }

    ///////////////////////////// Less interesting

    static long getFiberStackDepth(Stack s) {
        // TODO: let instrumentation increment a counter (w/accessor) in the fiber stack
        return -1;
    }

    static boolean stackDepthsDontMatch(long stackDepth, long fiberStackDepth) {
        // TODO: adjust calculation taking into account any "special" frames; must be _fast_
        return stackDepth > fiberStackDepth;
    }
    static boolean addNewSuspendableToKB(Class<?> c, String methodName) {
        // TODO
        return true;
    }

    static void pushRebuildToDo(StackWalker.StackFrame sf, List<?> fs, boolean callingYield) {
        // TODO: build a representation of the fiber stack building ops that would be performed by instrumented code and add it to the end of a temp list
    }

    static void apply(Collection<?> todo, Stack fs) {
        erase(fs);
        // TODO
    }

    static void erase(Stack fs) {
        // TODO
    }

    static void regularYield(boolean checkInstrumentation) throws SuspendExecution {
        // ...
    }

    static final boolean quasar9;
    static final boolean autoInstr;
    static final StackWalker esw;

    static {
        try {
            quasar9 = true; // TODO
            autoInstr = true; // TODO: read "enableXXX" sysprop in pre-release, change to "disableXXX" when stable

            if (quasar9 && autoInstr) {
                LOG("Live lazy auto-instrumentation ENABLED");

                final Class<?> extendedOptionClass = Class.forName("java.lang.StackWalker$ExtendedOption");
                final Method ewsNI = StackWalker.class.getDeclaredMethod("newInstance", Set.class, extendedOptionClass);
                ewsNI.setAccessible(true);

                final Set<StackWalker.Option> s = new HashSet<>();
                s.add(StackWalker.Option.RETAIN_CLASS_REFERENCE);

                final Field f = extendedOptionClass.getDeclaredField("LOCALS_AND_OPERANDS");
                f.setAccessible(true);
                esw = (StackWalker) ewsNI.invoke(null, s, f.get(null));
            } else {
                esw = null;
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Uninteresting

    private static final Collector<StackWalker.StackFrame, ?, Long> COUNTING = Collectors.counting();
    private static final Void NOTHING = null;
s
    private static void LOG(String s) {
        // TODO
        System.err.println(s);
    }

    private Quasar9FiberYieldPseudoAlg() {}
}

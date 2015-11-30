package co.paralleluniverse.fibers;

import co.paralleluniverse.fibers.instrument.Retransform;

import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class Quasar9FiberYieldPseudoAlg { static void doIt() throws SuspendExecution {

        boolean checkInstrumentation = true;
        final Fiber f = Fiber.currentFiber();
        final Stack fs = f.getStack();
        if (f != null) {
            if (esw != null) {
                final long stackDepth = esw.walk(s -> s.collect(COUNTING)); // TODO: JMH it
                if (stackDepth > getFiberStackDepth(fs)) {
                    final java.util.Stack<?> fiberStackRebuildToDoList = new java.util.Stack<>(); // TODO: improve perf
                    esw.walk(s -> s.map(sf -> { // Top to bottom, skipping internal & reflection
                        try {
                            LOG("Live lazy auto-instrumentation: " + sf.getClassName() + "#" + sf.getMethodName());
                            final Class<?> c = sf.getDeclaringClass();
                            addSuspendableToKB(c, sf.getMethodName());
                            Retransform.retransform(c);
                            pushRebuildToDo(sf, fiberStackRebuildToDoList);
                            return NOTHING;
                        } catch (final UnmodifiableClassException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                    // Method ref seems missing from StackFrame info => diff difficult => rebuild
                    apply(fiberStackRebuildToDoList, fs);
                    // We're done, let's skip checks
                    checkInstrumentation = false;
                }
            }
        }

        regularYield(checkInstrumentation);
    }

    ///////////////////////////// Less interesting

    static long getFiberStackDepth(Stack s) {
        // TODO: let instrumentation increment a counter (w/accessor) in the fiber stack
        return -1;
    }

    static void addSuspendableToKB(Class<?> c, String methodName) {
        // TODO
    }

    static void pushRebuildToDo(StackWalker.StackFrame sf, List<?> fs) {
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

    private static void LOG(String s) {
        // TODO
    }

    private Quasar9FiberYieldPseudoAlg() {}
}

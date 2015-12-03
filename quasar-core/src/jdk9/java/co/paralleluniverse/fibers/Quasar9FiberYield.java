package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.ExtendedStackTrace;
import co.paralleluniverse.common.util.ExtendedStackTraceElement;
import co.paralleluniverse.fibers.instrument.Retransform;
import co.paralleluniverse.fibers.instrument.SuspendableHelper;

import java.lang.instrument.UnmodifiableClassException;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

final class Quasar9FiberYield {
    static void doIt() throws SuspendExecution {
        boolean checkInstrumentation = true;
        final Fiber f = Fiber.currentFiber();
        if (f != null) {
            final Stack fs = f.getStack();
            if (esw != null) {
                final long stackDepth = esw.walk(s -> s.collect(COUNTING)); // TODO: JMH

                if (stackDepthsDontMatch(stackDepth, getFiberStackDepth(fs))) {
                    // Slow path, we'll take our time to fix up things
                    final java.util.Stack<FiberFramePush> fiberStackRebuildToDoList = new java.util.Stack<>(); // TODO: improve perf
                    final Collection<Class<?>> toRetransform = new ArrayList<>(); // TODO: improve perf
                    esw.walk(s -> s.map(new Function<>() {
                        private boolean yield = true, callingYield = false;
                        private boolean[] ok_last = new boolean[] { true, false };

                        public StackWalker.StackFrame upper = null;

                        private FiberFramePush prevFFP = null;

                        @Override
                        public Void apply(StackWalker.StackFrame sf) { // Top to bottom, skipping internal & reflection
                            if (!ok_last[1]) {
                                final Class<?> c = sf.getDeclaringClass();

                                if (prevFFP != null)
                                    prevFFP.setLower(sf);

                                prevFFP = pushRebuildToDo(sf, fiberStackRebuildToDoList, callingYield);

                                if (yield) { // Skip marking/transforming yield frame TODO check that this is ok and enough
                                    LOG("Live lazy auto-instrumentation: " + sf.getClassName() + "#" + sf.getMethodName());
                                    yield = false;
                                    callingYield = true;
                                } else {
                                    final MethodType t;
                                    final int b;
                                    try {
                                        t = (MethodType) getMethodType.invoke(memberName.get(sf));
                                        b = (int) bci.get(sf.getLineNumber());
                                        checkInstr(ok_last, sf.getDeclaringClass(), sf.getMethodName(), t, b, upper);
                                    } catch (final InvocationTargetException | IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                    if (!ok_last[0]) {
                                        recordNewSuspendableCallSiteToBeInstrumented(c, sf.getMethodName(), t, b);
                                        toRetransform.add(c);
                                    }

                                    if (callingYield)
                                        callingYield = false;
                                }

                                upper = sf;
                            }
                            return NOTHING;
                        }

                        private void checkInstr(boolean[] ok_last, Class<?> declaringClass, String methodName,
                                                MethodType t, int bci, StackWalker.StackFrame upperStackFrame) {
                            // TODO: factor with corresponding (Java9-ported) Fiber::checkInstrumentation
                            final String className = declaringClass.getName();

                            if (Thread.class.getName().equals(className) && "getStackTrace".equals(methodName) ||
                                ExtendedStackTrace.class.getName().equals(className) ||
                                className.contains("$$Lambda$"))
                                return; // Skip

                            if (!className.equals(Fiber.class.getName()) && !className.startsWith(Fiber.class.getName() + '$')
                                && !className.equals(Stack.class.getName()) && !SuspendableHelper.isWaiver(className, methodName)) {
                                final boolean classInstrumented = SuspendableHelper.isInstrumented(declaringClass);
                                final Executable m = lookupMethod(declaringClass, methodName, t);
                                if (m != null) {
                                    final boolean methodInstrumented = SuspendableHelper.isInstrumented(m);
                                    final boolean callSiteInstrumented = isCallSiteInstrumented(m, bci, upperStackFrame);

                                    if (!classInstrumented || !methodInstrumented || !callSiteInstrumented)
                                        ok_last[0] = false;
                                } else {
                                    ok_last[0] = false;
                                }
                            } else if (Fiber.class.getName().equals(className) && "run1".equals(methodName)) {
                                ok_last[1] = true;
                            }
                        }

                        private boolean isCallSiteInstrumented(Executable m, int bci, StackWalker.StackFrame upperStackFrame) {
                            // TODO: factor with corresponding (Java9-ported) SuspendableHelper::isCallSiteInstrumented
                            if (m == null)
                                return false;

                            if (SuspendableHelper.isSyntheticAndNotLambda(m))
                                return true;

                            if (upperStackFrame != null
                                // `verifySuspend` and `popMethod` calls are not suspendable call sites, not verifying them.
                                && ((Fiber.class.getName().equals(upperStackFrame.getClassName())
                                        && "verifySuspend".equals(upperStackFrame.getMethodName())) ||
                                    (Stack.class.getName().equals(upperStackFrame.getClassName())
                                        && "popMethod".equals(upperStackFrame.getMethodName())))) {
                                return true;
                            } else {
                                final Instrumented i = SuspendableHelper.getAnnotation(m, Instrumented.class);
                                if (i != null) {
                                    for (int j : i.suspendableCallSitesBCI()) { // TODO: fill BCIs in instrumentor
                                        if (j == bci)
                                            return true;
                                    }
                                }
                            }

                            return false;
                        }
                    }));

                    // Retransform classes, so next time the bytecode will do the fiber stack management
                    // rather than this slow stuff
                    try {
                        // TODO Make it incremental
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

    static class FiberFramePush {
        private final StackWalker.StackFrame sf;
        private final Object[] locals;
        private final Object[] operands;
        private final boolean callingYield;
        private final Executable m;

        private int numSlots = -1;
        private int entry = -1;

        private int[] suspendableCallSiteLineNumbers;

        FiberFramePush(StackWalker.StackFrame sf, Object[] locals, Object[] operands, boolean callingYield) {
            this.sf = sf;
            this.locals = locals;
            this.operands = operands;
            this.callingYield = callingYield;
            this.m = lookupMethod(sf); // Caching it as it's used multiple times
        }

        void setLower(StackWalker.StackFrame lower) {
            final Member lowerM = SuspendableHelper.lookupMethod(new ExtendedStackTraceElement(lower.toStackTraceElement()));
            final Instrumented i = lowerM != null ? SuspendableHelper.getAnnotation(lowerM, Instrumented.class) : null;
            if (i != null) {
                suspendableCallSiteLineNumbers = i.suspendableCallSites();
                if (suspendableCallSiteLineNumbers != null) {
                    Arrays.sort(suspendableCallSiteLineNumbers);
                    // TODO: check
                    final int bsResPlus1 = Arrays.binarySearch(suspendableCallSiteLineNumbers, sf.getLineNumber().orElse(-1)) + 1;
                    entry = Math.abs(bsResPlus1);
                }
            }
        }

        /**
         * Live fiber stack construction
         * <br>
         * !!! Must be kept aligned with `InstrumentMethod.emitStoreState` and `Stack.pusXXX` !!!
         */
        void apply(Stack s) {
            if (entry < 1)
                throw new RuntimeException("Can't determine call site index for " + this.toString());

            final int numArgsToPreserve = callingYield ? m.getParameterCount() : 0;

            // New frame
            s.pushMethod(entry, getNumSlots());

            // Operands and locals (in this order) slot indices
            int idxObj = 0, idxPrim = 0;
            final Map<Object, Integer> opIdxs = new HashMap<>(); // TODO: improve perf

            // Store stack operands
            for (final Object op : operands) {
                final String type = type(op);
                // TODO: check that "omitted" (new value) can't happen at runtime
                if (!isNullableType(type)) {
                    if (primitiveValueClass.isInstance(op)) {
                        storePrim(op, s, idxPrim);
                        opIdxs.put(op, idxPrim);
                        idxPrim++;
                    } else {
                        Stack.push(op, s, idxObj);
                        opIdxs.put(op, idxObj);
                        idxObj++;
                    }
                }
            }

            // Store local vars
            for (int i = Modifier.isStatic(m.getModifiers()) ? 0 : 1 /* Skip `this` TODO: check */ ; i < locals.length ; i++) {
                final Object local = locals[i];
                final String type = type(local);
                if (!isNullableType(type)) {
                    if (primitiveValueClass.isInstance(local)) {
                        storePrim(local, s, idxPrim);
                        idxPrim++;
                    } else {
                        Stack.push(local, s, idxObj);
                        idxObj++;
                    }
                }
            }

            // Restore last numArgsToPreserve operands
            for (int i = operands.length - numArgsToPreserve ; i < operands.length ; i++) {
                final Object op = operands[i];
                final String type = type(op);
                // TODO: check that "omitted" (new value) can't happen at runtime
                if (!isNullableType(type)) {
                    if (primitiveValueClass.isInstance(op)) {
                        restorePrim(op, s, opIdxs.get(op));
                    } else {
                        s.getObject(opIdxs.get(op));
                    }
                }
            }
        }

        private void storePrim(Object op, Stack s, int idx) {
            try {
                final char t = (char) primitiveType.invoke(op);
                switch (t) {
                    case 'I':
                        Stack.push((int) intValue.invoke(op), s, idx);
                        break;
                    case 'S':
                        Stack.push((int) ((short) shortValue.invoke(op)), s, idx);
                        break;
                    case 'Z':
                        Stack.push((boolean) booleanValue.invoke(op) ? 1 : 0, s, idx);
                        break;
                    case 'C':
                        Stack.push((int) ((char) shortValue.invoke(op)), s, idx);
                        break;
                    case 'B':
                        Stack.push((int) ((byte) shortValue.invoke(op)), s, idx);
                        break;
                    case 'J':
                        Stack.push((long) longValue.invoke(op), s, idx);
                        break;
                    case 'F':
                        Stack.push((float) floatValue.invoke(op), s, idx);
                        break;
                    case 'D':
                        Stack.push((double) doubleValue.invoke(op), s, idx);
                        break;
                    default:
                        throw new RuntimeException("Unknown primitive operand type: " + t);
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        private void restorePrim(Object op, Stack s, int idx) {
            final char t;
            try {
                t = (char) primitiveType.invoke(op);
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            switch (t) {
                case 'I':
                case 'S':
                case 'Z':
                case 'C':
                case 'B':
                    s.getInt(idx);
                    break;
                case 'J':
                    s.getLong(idx);
                    break;
                case 'F':
                    s.getFloat(idx);
                    break;
                case 'D':
                    s.getDouble(idx);
                    break;
                default:
                    throw new RuntimeException("Unknown primitive operand type: " + t);
            }
        }

        private int getNumSlots() {
            if (numSlots == -1) {
                int idxPrim = 0, idxObj = 0;
                for (Object operand : operands) {
                    if (primitiveValueClass.isInstance(operand))
                        idxPrim++;
                    else if (!isNullableType(type(operand)))
                        idxObj++;
                }
                numSlots = Math.max(idxObj, idxPrim);
            }
            return numSlots;
        }

        @Override
        public String toString() {
            return "FiberFramePush{" +
                "sf=" + sf +
                ", locals=" + Arrays.toString(locals) +
                ", operands=" + Arrays.toString(operands) +
                ", callingYield=" + callingYield +
                ", m=" + m +
                ", numSlots=" + numSlots +
                ", entry=" + entry +
                ", suspendableCallSiteLineNumbers=" + Arrays.toString(suspendableCallSiteLineNumbers) +
                '}';
        }
    }

    static FiberFramePush pushRebuildToDo(StackWalker.StackFrame sf, Collection<FiberFramePush> todo, boolean callingYield) {
        try {
            final FiberFramePush ffp =
                new FiberFramePush (
                    sf,
                    (Object[]) getLocals.invoke(sf), (Object[]) getOperands.invoke(sf),
                    callingYield
                );
            todo.add(ffp);
            return ffp;
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Less interesting

    static long getFiberStackDepth(Stack s) {
        // TODO: maintain a depth counter (w/accessor) in the fiber stack
        throw new RuntimeException("Not implemented");
    }

    static boolean stackDepthsDontMatch(long stackDepth, long fiberStackDepth) {
        // TODO: adjust calculation taking into account any "special" frames; must be _fast_
        return stackDepth > fiberStackDepth;
    }

    static boolean recordNewSuspendableCallSiteToBeInstrumented(Class<?> c, String methodName, MethodType t, int b) {
        // TODO
        throw new RuntimeException("Not implemented");
    }

    static void apply(Collection<FiberFramePush> todo, Stack fs) {
        erase(fs);
        for (final FiberFramePush ffp : todo)
            ffp.apply(fs);
    }

    static void erase(Stack fs) {
        // TODO: add op to Stack
        throw new RuntimeException("Not implemented");
    }

    static void regularYield(boolean checkInstrumentation) throws SuspendExecution {
        // TODO: call present yield
        throw new RuntimeException("Not implemented");
    }

    static final boolean quasar9;
    static final boolean autoInstr;

    static StackWalker esw = null;

    static Class<?> primitiveValueClass, liveStackFrameClass;

    static Method getLocals, getOperands, getMethodType, primitiveType;
    static Method booleanValue, byteValue, charValue, shortValue, intValue, floatValue, longValue, doubleValue;

    static Field memberName, bci;

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

                /////////////////////////////////////////////////
                // Get internal LiveStackFrame* class/method refs
                //////////////////////////////////////////////////
                liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
                primitiveValueClass = Class.forName("java.lang.LiveStackFrame$PrimitiveValue");

                final Class<?> stackFrameInfoClass = Class.forName("java.lang.StackFrameInfo");
                memberName = stackFrameInfoClass.getDeclaredField("memberName");
                memberName.setAccessible(true);
                bci = stackFrameInfoClass.getDeclaredField("bci");
                bci.setAccessible(true);
                getMethodType = Class.forName("java.lang.invoke.MemberName").getDeclaredMethod("getMethodType");
                getMethodType.setAccessible(true);

                getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
                getLocals.setAccessible(true);

                getOperands = liveStackFrameClass.getDeclaredMethod("getStack");
                getOperands.setAccessible(true);

                primitiveType = primitiveValueClass.getDeclaredMethod("type");
                primitiveType.setAccessible(true);

                booleanValue = primitiveValueClass.getDeclaredMethod("booleanValue");
                // booleanValue.setAccessible(true);

                byteValue = primitiveValueClass.getDeclaredMethod("byteValue");
                // byteValue.setAccessible(true);

                charValue = primitiveValueClass.getDeclaredMethod("charValue");
                // charValue.setAccessible(true);

                shortValue = primitiveValueClass.getDeclaredMethod("shortValue");
                // shortValue.setAccessible(true);

                intValue = primitiveValueClass.getDeclaredMethod("intValue");
                // intValue.setAccessible(true);

                floatValue = primitiveValueClass.getDeclaredMethod("floatValue");
                // floatValue.setAccessible(true);

                longValue = primitiveValueClass.getDeclaredMethod("longValue");
                // longValue.setAccessible(true);

                doubleValue = primitiveValueClass.getDeclaredMethod("doubleValue");
                // doubleValue.setAccessible(true);

                // getMonitors = liveStackFrameClass.getDeclaredMethod("getMonitors");
                // getMonitors.setAccessible(true);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Uninteresting

    private static final Collector<StackWalker.StackFrame, ?, Long> COUNTING = Collectors.counting();
    private static final Void NOTHING = null;

    private static Executable lookupMethod(StackWalker.StackFrame sf) {
        try {
            return lookupMethod(sf.getDeclaringClass(), sf.getMethodName(), (MethodType) getMethodType.invoke(memberName.get(sf)));
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Executable lookupMethod(Class<?> declaringClass, String methodName, MethodType t) {
        if (declaringClass == null || methodName == null || t == null)
            return null;

        for (final Method m : declaringClass.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && Arrays.equals(m.getParameterTypes(), t.parameterArray())) {
                return m;
            }
        }

        return null;
    }

    private static String type(Object operand) {
        if (primitiveValueClass.isInstance(operand)) {
            final char c;
            try {
                c = (char) primitiveType.invoke(operand);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return String.valueOf(c);
        } else {
            return operand.getClass().getName();
        }
    }

    private static boolean isNullableType(String type) {
        // TODO: check if this can really happen at runtime too
        return "null".equals(type.toLowerCase());
    }

    private static void LOG(String s) {
        // TODO
        System.err.println(s);
    }

    private Quasar9FiberYield() {}
}

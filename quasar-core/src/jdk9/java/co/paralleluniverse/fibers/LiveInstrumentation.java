/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.util.ExtendedStackTrace;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask;
import co.paralleluniverse.fibers.instrument.*;
import co.paralleluniverse.strands.SuspendableUtils;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author circlespainter
 */
final class LiveInstrumentation {
    private static final MethodDatabase db;

    static synchronized boolean fixup(Fiber f) {
        boolean checkInstrumentation = true;
        if (autoInstr && f != null) {
            final Stack fs = f.getStack();
            if (esw != null) {
                if (!agree(esw, fs)) {
                    DEBUG("\nFound mismatch between stack depth and fiber stack! Activating live lazy auto-instrumentation");
                    // Slow path, we'll take our time to fix up things
                    checkCaps();
                    // TODO: improve perf
                    final java.util.Stack<FiberFramePush> fiberStackRebuildToDoList = new java.util.Stack<>();
                    // TODO: improve perf
                    final List<StackWalker.StackFrame> frames = esw.walk(s -> s.collect(Collectors.toList()));

                    boolean callingFiberRuntime = false;
                    boolean ok, last = false;
                    StackWalker.StackFrame upper = null;
                    FiberFramePush prevFFP = null;
                    for (final StackWalker.StackFrame sf : frames) {
                        if (isFiberRuntimeMethod(sf.getClassName(), sf.getMethodName())) {
                            // Skip marking/transforming yield frames
                            callingFiberRuntime = true;
                        } else {
                            final Class<?> cCaller = sf.getDeclaringClass();
                            final String mnCaller = sf.getMethodName();

                            if (prevFFP != null)
                                prevFFP.setLower(sf);

                            final MethodType mtCaller;
                            try {
                                mtCaller = (MethodType) getMethodType.invoke(memberName.get(sf));

                                DEBUG("\nLive lazy auto-instrumentation for call frame: " + sf.getClassName() + "#" + sf.getMethodName() + mtCaller.toMethodDescriptorString());

                                final Executable m = lookupMethod(cCaller, mnCaller, mtCaller);
                                final int b = (Integer) bci.get(sf);
                                final CheckCallSiteFrameInstrumentationReport report =
                                    checkCallSiteFrameInstrumentation(cCaller, m, b, upper);
                                ok = report.isOK();
                                last = report.last;

                                DEBUG("Frame instrumentation analysis report:\n" +
                                    "\tclass is " +
                                    (report.classInstrumented ? "instrumented" : "NOT instrumented") + ", \n" +
                                    "\tmethod is " +
                                    (report.methodInstrumented ? "instrumented" : "NOT instrumented") + ", \n" +
                                    "\tcall site in " +
                                    sf.getFileName().orElse("<UNKNOWN SOURCE FILE>") +
                                    " at line " + sf.getLineNumber() + " and bci " + b +
                                    " to " + upper.getClassName() + "#" + upper.getMethodName() +
                                    ((MethodType) getMethodType.invoke(memberName.get(sf))).toMethodDescriptorString() +
                                    " is " + (report.callSiteInstrumented ? "instrumented" : "NOT instrumented"));

                                if (!ok) {
                                    DEBUG("Frame instrumentation analysis found problems");
                                    DEBUG("-> In any case, ensuring suspendable supers are correct");
                                    ensureCorrectSuspendableSupers(cCaller, mnCaller, mtCaller);
                                    if (!report.classInstrumented || !report.methodInstrumented) {
                                        DEBUG("-> Class or method not instrumented at all, marking method suspendable");
                                        suspendable(cCaller, mnCaller, mtCaller, MethodDatabase.SuspendableType.SUSPENDABLE);
                                    }
                                    final String n = cCaller.getName();
                                    DEBUG("-> Reloading class from original classloader");
                                    final InputStream is = cCaller.getResourceAsStream("/" + n.replace(".", "/") + ".class");
                                    final byte[] diskData = ByteStreams.toByteArray(is);
                                    DEBUG("-> Redefining class, Quasar instrumentation with fixed suspendable info will occur");
                                    Retransform.redefine(new ClassDefinition(cCaller, diskData));
                                }

                                // The annotation will be correct now
                                final Instrumented i = SuspendableHelper.getAnnotation(lookupMethod(cCaller, mnCaller, mtCaller), Instrumented.class);
                                if (i != null && !i.methodOptimized()) {
                                    DEBUG("Method is not optimized, creating a fiber stack rebuild record");
                                    prevFFP = pushRebuildToDo(sf, upper, fiberStackRebuildToDoList, callingFiberRuntime, report.methodInstrumented);
                                }

                                callingFiberRuntime = false;
                            } catch (final InvocationTargetException | IllegalAccessException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        upper = sf;

                        if (last)
                            break;
                    }

                    DEBUG("\nRebuilding fiber stack");
                    DEBUG("\t** Fiber stack dump before rebuild:"); // TODO: remove
                    fs.dump(); // TODO: remove
                    apply(fiberStackRebuildToDoList, fs);
                    DEBUG("\t** Fiber stack dump after rebuild:"); // TODO: remove
                    fs.dump(); // TODO: remove

                    // Now it should be ok
                    // assert agree(esw, fs);

                    // We're done, let's skip checks
                    checkInstrumentation = false;
                } else {
                    DEBUG("\nInstrumentation seems OK!\n");
                    DEBUG("\t** Fiber stack dump:"); // TODO: remove
                    fs.dump(); // TODO: remove
                }
            }
        }

        return true /* TODO after debugging pass the following instead */ /* checkInstrumentation */;
    }

    private static boolean isFiberRuntimeMethod(String className, String methodName) {
        return
            Fiber.class.getName().equals(className) ||
            LiveInstrumentation.class.getName().equals(className) ||
            ForkJoinWorkerThread.class.getName().equals(className) ||
            ForkJoinTask.class.getName().equals(className) ||
            ParkableForkJoinTask.class.getName().equals(className) ||
            className.startsWith(SuspendableUtils.VoidSuspendableCallable.class.getName()) ||
            className.startsWith(ForkJoinPool.class.getName()) ||
            className.startsWith(FiberForkJoinScheduler.class.getName());
    }

    private static
    CheckCallSiteFrameInstrumentationReport
    checkCallSiteFrameInstrumentation(Class declaringClass, Executable m, int offset, StackWalker.StackFrame upperStackFrame) {
        // TODO: factor with corresponding (Java9-ported) Fiber::checkInstrumentation
        final String className = declaringClass.getName();
        final String methodName = m.getName();
        final CheckCallSiteFrameInstrumentationReport res = new CheckCallSiteFrameInstrumentationReport();

        if (Thread.class.getName().equals(className) && "getStackTrace".equals(methodName) ||
            ExtendedStackTrace.class.getName().equals(className) ||
            className.contains("$$Lambda$"))
            return CheckCallSiteFrameInstrumentationReport.OK_NOT_FINISHED; // Skip

        if (!className.equals(Fiber.class.getName()) && !className.startsWith(Fiber.class.getName() + '$')
            && !className.equals(Stack.class.getName()) && !SuspendableHelper.isWaiver(className, methodName)) {
            res.classInstrumented = SuspendableHelper.isInstrumented(declaringClass);
            res.methodInstrumented = SuspendableHelper.isInstrumented(m);
            final Instrumented ann = SuspendableHelper.getAnnotation(m, Instrumented.class);
            if (ann != null) {
                DEBUG("\t\tOptimized method: " + ann.methodOptimized());
                DEBUG("\t\tMethod start source line: " + ann.methodStartSourceLine());
                DEBUG("\t\tMethod end source line: " + ann.methodEndSourceLine());
                DEBUG("\t\tSuspendable call source lines: " + Arrays.toString(ann.methodSuspendableCallSourceLines()));
                DEBUG("\t\tSuspendable call signatures: " + Arrays.toString(ann.methodSuspendableCallSignatures()));
                final int[] offsets = ann.methodSuspendableCallOffsets();
                DEBUG("\t\tSuspendable call offsets (after instrumentation): " + Arrays.toString(offsets));

                res.callSiteInstrumented = isCallSiteInstrumented(m, ann, offset, upperStackFrame);
            }

        } else if (Fiber.class.getName().equals(className) && "run1".equals(methodName)) {
            res.last = true;
        }
        return res;
    }

    private static boolean isCallSiteInstrumented(Executable m, Instrumented ann, int offset, StackWalker.StackFrame upperStackFrame) {
        // TODO: factor with corresponding (Java9-ported) SuspendableHelper::isCallSiteInstrumented
        if (m == null)
            return false;

        if (SuspendableHelper.isSyntheticAndNotLambda(m))
            return true;

        if (upperStackFrame != null) {
            final String cnCallSite = upperStackFrame.getClassName();
            final String mnCallSite = upperStackFrame.getMethodName();
            if ((Fiber.class.getName().equals(cnCallSite) && "verifySuspend".equals(mnCallSite)) ||
                (Stack.class.getName().equals(upperStackFrame.getClassName()) && "popMethod".equals(mnCallSite))) {
                return true;
            }

            if (ann != null) {
                final int[] offsets = ann.methodSuspendableCallOffsets();
                for (int i = 0 ; i < offsets.length ; i++) {
                    if (offset == offsets[i])
                        return true;
                }
            }

            return false;
        }

        throw new RuntimeException("Checking yield method instrumentation!");
    }

    private static class CheckCallSiteFrameInstrumentationReport {
        private static final CheckCallSiteFrameInstrumentationReport OK_NOT_FINISHED;

        static {
            OK_NOT_FINISHED = new CheckCallSiteFrameInstrumentationReport();
            OK_NOT_FINISHED.classInstrumented = true;
            OK_NOT_FINISHED.classInstrumented = true;
            OK_NOT_FINISHED.callSiteInstrumented = true;
        }

        private boolean classInstrumented = false;
        private boolean methodInstrumented = false;
        private boolean callSiteInstrumented = false;
        private boolean last = false;

        private boolean isOK() {
            return classInstrumented && methodInstrumented && callSiteInstrumented;
        }
    }

    private static class FiberFramePush {
        private final StackWalker.StackFrame sf;
        private final Executable m;
        private final Object[] locals;
        private final Object[] operands;

        private final StackWalker.StackFrame upper;
        private final Executable upperM;
        private final Object[] upperLocals;
        // private final Object[] upperOperands;

        private final boolean callingYield;
        private final boolean alreadyInstrumented;

        private int numSlots = -1;
        private int entry = 1;

        private int[] suspendableCallOffsets;

        private FiberFramePush(StackWalker.StackFrame sf, StackWalker.StackFrame upper, boolean callingYield, boolean alreadyInstrumented) throws InvocationTargetException, IllegalAccessException {
            this.sf = sf;
            this.m = lookupMethod(sf); // Caching it as it's used multiple times
            this.locals = (Object[]) getLocals.invoke(sf);
            this.operands = (Object[]) getOperands.invoke(sf);

            this.upper = upper;
            this.upperM = lookupMethod(upper);
            this.upperLocals = (Object[]) getLocals.invoke(upper);
            // this.upperOperands = (Object[]) getOperands.invoke(upper);

            this.callingYield = callingYield;
            this.alreadyInstrumented = alreadyInstrumented;
        }

        private void setLower(StackWalker.StackFrame lower) {
            // this.lower = lower;
            // this.lowerM = lookupMethod(lower);
            final MethodType mt;
            try {
                mt = (MethodType) getMethodType.invoke(memberName.get(lower));
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            final Member lowerM = lookupMethod(lower.getDeclaringClass(), lower.getMethodName(), mt);
            final Instrumented i = lowerM != null ? SuspendableHelper.getAnnotation(lowerM, Instrumented.class) : null;
            if (i != null) {
                suspendableCallOffsets = i.methodSuspendableCallOffsets();
                if (suspendableCallOffsets != null) {
                    Arrays.sort(suspendableCallOffsets);
                    final int bsResPlus1;
                    final int b;
                    try {
                        b = (int) bci.get(sf);
                    } catch (final IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    bsResPlus1 = Arrays.binarySearch(suspendableCallOffsets, b);
                    entry = Math.abs(bsResPlus1);
                }
            }
        }

        /**
         * Live fiber stack construction
         * <br>
         * !!! Must be kept aligned with `InstrumentMethod.emitStoreState` and `Stack.pusXXX` !!!
         */
        private void apply(Stack s) {
            if (s.nextMethodEntry() == 0)
                s.isFirstInStackOrPushed();

            // New frame
            s.pushMethod(entry, getNumSlots());

            // Operands and locals (in this order) slot indices
            int idxObj = 0, idxPrim = 0;

            // ************************************************************************************************
            // The in-flight situation of the stack if very different from the static analysis perspective:
            // calls are in progress and in all non-top frames the upper part of operand the stack representing
            // arguments has been eaten already.
            // On the other hand we don't need the values there anymore: the instrumentation saves them only in
            // the case there's a suspension point before the call but this is not the case since we're already
            // in a yield (and a suspension point would be a yield anyway).
            //
            // -> Attempt 1: fake them
            // ************************************************************************************************

            if (!Modifier.isStatic(upperM.getModifiers()))
                Stack.push(upperLocals[0] /* Object ref */, s, idxObj++);
            for (final Class<?> c : upperM.getParameterTypes()) {
                if (c.isPrimitive()) {
                    if (Float.class.equals(c))
                        Stack.push(0.0F, s, idxPrim++);
                    if (Double.class.equals(c))
                        Stack.push(0.0D, s, idxPrim++);
                    if (Long.class.equals(c))
                        Stack.push(0L, s, idxPrim++);
                    else
                        Stack.push(0, s, idxPrim++);
                } else {
                    Stack.push(null, s, idxObj++); // Object ref
                }
            }

            // Store actual non-call stack operands left
            for (final Object op : operands) {
                // TODO: check that "omitted" (new value) can't happen at runtime
                if (op != null) {
                    final String type = type(op);
                    if (!isNullableType(type)) {
                        if (primitiveValueClass.isInstance(op))
                            storePrim(op, s, idxPrim++);
                        else if (!(op instanceof Stack)) // Skip stack operands
                            Stack.push(op, s, idxObj++);
                    }
                }
            }

            // Store local vars
            for (int i = Modifier.isStatic(m.getModifiers()) ? 0 : 1 /* Skip `this` */ ; i < locals.length - (alreadyInstrumented ? 3 : 0) ; i++) {
            // for (int i = 0 ; i < locals.length ; i++) {
                final Object local = locals[i];
                if (local != null) {
                    final String type = type(local);
                    if (!isNullableType(type)) {
                        if (primitiveValueClass.isInstance(local)) {
                            storePrim(local, s, idxPrim);
                            idxPrim++;
                        } else if (!(local instanceof Stack)) { // Skip stack locals
                            Stack.push(local, s, idxObj);
                            idxObj++;
                        }
                    }
                }
            }

            // Since the potential call to a yield method is in progress already (because live instrumentation is
            // called from all yield methods), we don't need to perform any special magic to preserve its args.
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
                        Stack.push((int) ((char) charValue.invoke(op)), s, idx);
                        break;
                    case 'B':
                        Stack.push((int) ((byte) byteValue.invoke(op)), s, idx);
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

        private int getNumSlots() {
            if (numSlots == -1) {
                int idxPrim = 0, idxObj = 0;

                // ************************************************************************************************
                // The in-flight situation of the stack if very different from the static analysis perspective:
                // calls are in progress and in all non-top frames the upper part of operand the stack representing
                // arguments has been eaten already.
                // On the other hand we don't need the values there anymore: the instrumentation saves them only in
                // the case there's a suspension before the call but this is not the case since we're in a yield.
                //
                // -> Attempt 1: fake them
                // ************************************************************************************************

                if (!Modifier.isStatic(upperM.getModifiers()))
                    idxObj++;
                for (final Class<?> c : upperM.getParameterTypes()) {
                    if (c.isPrimitive())
                        idxPrim++;
                    else
                        idxObj++;
                }

                for (final Object operand : operands) {
                    if (operand != null) {
                        if (primitiveValueClass.isInstance(operand))
                            idxPrim++;
                        else if (!isNullableType(type(operand)))
                            idxObj++;
                    }
                }
                for (int i = Modifier.isStatic(m.getModifiers()) ? 0 : 1 /* Skip `this` */ ; i < locals.length - (alreadyInstrumented ? 3 : 0) ; i++) {
                    final Object local = locals[i];
                    if (local != null) {
                        if (primitiveValueClass.isInstance(local))
                            idxPrim++;
                        else if (!isNullableType(type(local)))
                            idxObj++;
                    }
                }
                numSlots = Math.max(idxObj, idxPrim);
            }
            return numSlots;
        }

        @Override
        public String toString() {
            return "FiberFramePush{" +
                "sf=" + sf +
                ", m=" + m +
                ", locals=" + Arrays.toString(locals) +
                ", operands=" + Arrays.toString(operands) +
                ", upper=" + upper +
                ", upperM=" + upperM +
                ", upperLocals=" + Arrays.toString(upperLocals) +
                ", callingYield=" + callingYield +
                ", numSlots=" + numSlots +
                ", entry=" + entry +
                ", suspendableCallOffsets=" + Arrays.toString(suspendableCallOffsets) +
                '}';
        }
    }

    private static LiveInstrumentation.FiberFramePush pushRebuildToDo(StackWalker.StackFrame sf, StackWalker.StackFrame upper, java.util.Stack<FiberFramePush> todo, boolean callingYield, boolean methodInstrumented) {
        try {
            final FiberFramePush ffp =
                new FiberFramePush (
                    sf,
                    upper,
                    callingYield,
                    methodInstrumented
                );
            todo.push(ffp);
            return ffp;
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Less interesting

    private static boolean agree(StackWalker w, Stack fs) {
        // TODO: must be _fast_, JMH it
        final long threadStackDepth = w.walk(s -> s.filter(sf -> !isFiberRuntimeMethod(sf.getClassName(), sf.getMethodName())).collect(COUNTING));
        return threadStackDepth == fs.getInstrumentedCount();
    }

    private static void apply(java.util.Stack<FiberFramePush> todo, Stack fs) {
        fs.clear();
        while (!todo.empty()) {
            final FiberFramePush ffp = todo.pop();
            ffp.apply(fs);
        }
    }

    private static final boolean autoInstr;

    private static StackWalker esw = null;

    private static Class<?> primitiveValueClass;

    private static Method getLocals, getOperands, getMethodType, primitiveType;
    private static Method booleanValue, byteValue, charValue, shortValue, intValue, floatValue, longValue, doubleValue;

    private static Field memberName, bci;

    static {
        try {
            // TODO: change to "disableXXX" when stable
            autoInstr = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.enableLive");
            db = Retransform.getMethodDB();

            if (autoInstr) {
                DEBUG("Live lazy auto-instrumentation ENABLED");

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
                Class<?> liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
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

    private static void ensureCorrectSuspendableSupers(Class<?> cCaller, String mnCaller, MethodType mtCaller) {
        final Class<?> sup = cCaller.getSuperclass();
        if (sup != null)
            suspendableSuper(sup, mnCaller, mtCaller);

        for (final Class<?> i : cCaller.getInterfaces())
            suspendableSuper(i, mnCaller, mtCaller);
    }

    private static void suspendableSuper(Class<?> c, String mn, MethodType mt) {
        Method m;
        try {
            m = c.getDeclaredMethod(mn, mt.parameterArray());
        } catch (final NoSuchMethodException _ignored) {
            m = null;
        }
        if (m != null) {
            suspendable(c, mn, mt, MethodDatabase.SuspendableType.SUSPENDABLE_SUPER);
            ensureCorrectSuspendableSupers(c, mn, mt);
        }
    }

    private static void suspendable(Class<?> c, String n, MethodType t, MethodDatabase.SuspendableType st) {
        final Class<?> s = c.getSuperclass();
        final MethodDatabase.ClassEntry ce =
            Retransform.getMethodDB().getOrCreateClassEntry (
                c.getName().replace(".", "/"),
                s != null ? s.getName().replace(".", "/") : null
            );
        final String td = t.toMethodDescriptorString();
        final MethodDatabase.SuspendableType stOld = ce.check(n, td);
        ce.set(n, td, max(stOld, st));
    }

    private static MethodDatabase.SuspendableType max(MethodDatabase.SuspendableType a, MethodDatabase.SuspendableType b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return b.compareTo(a) > 0 ? b : a;
    }

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

    private static void checkCaps() {
        if (!JavaAgent.isActive())
            throw new RuntimeException("ERROR: live instrumentation needs the Quasar agent but it is not running!");
        if (!Retransform.supportsRedefinition())
            throw new RuntimeException("ERROR: live instrumentation needs redefinition support but it's missing!");
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

    private static void DEBUG(String s) {
        // TODO: plug
        System.err.println(s);
    }

    private LiveInstrumentation() {}
}

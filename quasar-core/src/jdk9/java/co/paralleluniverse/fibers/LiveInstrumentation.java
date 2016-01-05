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

import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.fibers.instrument.*;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author circlespainter
 */
public final class LiveInstrumentation {

    private static final AtomicInteger runCount = new AtomicInteger();

    @VisibleForTesting
    public static int getRunCount() {
        return runCount.get();
    }

    @VisibleForTesting
    public static void resetRunCount() {
        runCount.set(0);
    }

    // TODO: remove `synchronized` or change it to a lock
    static synchronized boolean fixup(Fiber fiber) {
        if (!ACTIVE || fiber == null) // Not using live instrumentation or not in a fiber => don't alter checks
            return true;

        final Stack fiberStack = fiber.getStack();
        final long diff = agree(esw, fiberStack);

        if (diff == 0) { // All OK already
            DEBUG("\nInstrumentation seems OK!\n");
            DEBUG("\t** Fiber stack dump:"); // TODO: remove
            fiberStack.dump(); // TODO: remove
            DEBUG(""); // TODO: remove

            // We're done, let's skip checks
            return false;
        }

        // Else slow path, we'll take our time to fix up things.
        DEBUG("\nFound mismatch between stack depth and fiber stack (" + diff + ") => activating live lazy auto-instrumentation");

        // The goal is to rebuild a fiber stack "good enough" for a correct resume. What this means is not so easy to figure
        // out though, so the initial strategy is to ensure a stronger property by performing calls on the fiber stack
        // object that are equivalent to the ones agent-time instrumentation would inject if it had complete information
        // about suspendables present in the live stack.
        // This means which classes are suspendables, which methods, which call sites and _the resume indexes_ these call sites
        // would have in a fully instrumented method body relative to the suspendables present in the live stack.
        //
        // This latter information is also needed to recover type info from the instrumentation stage as live type info
        // is currently lacking in this respect (no doubles and no longs, they're represented as adjacent int slots).
        //
        // A call site is identified by class, method and bytecode offset. The final offset (and index) of a suspendable
        // call site will be known only at the end of the re-instrumentation stage because it can be moved by
        // instrumentation of other suspendable call sites in the same method that appear in the same live stack.
        //
        // However, for every method the relative ordering of the involved call sites won't be changed by instrumentation.
        // The position in this ordering is not yet the correct index though, because the method may contain previously
        // instrumented call sites that are not present in the current live stack.
        //
        // But when live instrumentation starts, the bytecode is aligned with offsets in `@Instrumented` because the stack
        // will have been unrolled by suspension after a previous run (or lack of) and methods will have re-entered. This
        // means that for any method involved we have both an up-to-date list of already instrumented call sites with
        // correct offsets, and an up-to-date list of involved call sites with correct offsets. Since some involved
        // call sites could have been instrumented already, these two lists could have non-empty intersection.
        //
        // Anyway, since the bytecode offsets are correct and instrumentation won't change relative ordering, the (1-based) position
        // in the ordered set obainined by merging the involved call site offsets (i.e. actual live offsets) with the list of
        // the already instrumented call site offsets corresponds to the correct suspendable call site resume index in the fully
        // instrumented method body relative to the suspendables present in the live stack.

        runCount.getAndIncrement();

        checkCaps();

        // TODO: 1) reduce garbage, 2) make faster

        // 0)
        final StackWalker.StackFrame[] fs = getStackFrames();

        try {
            // 1)
            final List<ReportRecord> reports = getInstrumentationReports(fs);

            // 2)
            final HashMap<String, int[]> methodToSuspOffsetsAfterInstrPreCall =
                getPreCallInstrumentationStatus(reports);

            // 3)
            instrument(reports);

            // 4)
            final HashMap<StackWalker.StackFrame, Integer> frameToSuspendableCallIndex =
                getPerFrameSuspendableCallIndexes(reports, methodToSuspOffsetsAfterInstrPreCall);

            // 5)
            final java.util.Stack<FiberFramePush> fiberStackRebuildToDoList =
                createStackRebuildActions(reports);

            // 6)
            rebuildFiberStack(fiberStack, fiberStackRebuildToDoList, frameToSuspendableCallIndex);
        } catch (final Throwable t) {
            System.err.println("!!!LIVE INSTRUMENTATION INTERNAL ERROR - PLEASE REPORT!!!");
            t.printStackTrace();
            throw new RuntimeException(t);
        }

        // We're done, let's skip checks
        return false;
    }

    private static StackWalker.StackFrame[] getStackFrames() {
        DEBUG("\n0) Retrieving live stack frames");
        final List<StackWalker.StackFrame> fsL = esw.walk(s -> s.collect(Collectors.toList()));
        final StackWalker.StackFrame[] fs = new StackWalker.StackFrame[fsL.size()];
        fsL.toArray(fs);
        return fs;
    }

    private static class ReportRecord {
        final StackWalker.StackFrame f, upper, lower;
        final Verify.CheckFrameInstrumentationReport report;

        private ReportRecord (
            StackWalker.StackFrame f,
            StackWalker.StackFrame upper,
            StackWalker.StackFrame lower,
            Verify.CheckFrameInstrumentationReport report
        ) {
            this.f = f;
            this.upper = upper;
            this.lower = lower;
            this.report = report;
        }
    }

    private static List<ReportRecord> getInstrumentationReports(StackWalker.StackFrame[] fs) {
        // 1) Collect reports
        DEBUG("\n1) Collecting instrumentation reports:");
        final List<ReportRecord> reports = new ArrayList<>();
        boolean upperFiberRuntime = true, lowerFiberRuntime = false;
        boolean last = false;
        for (int i = 0; i < fs.length; i++) {
            final StackWalker.StackFrame f = fs[i];
            final StackWalker.StackFrame upper = i > 0 ? fs[i-1] : null;
            final StackWalker.StackFrame lower = i < fs.length - 1 ? fs[i+1] : null;

            final String cn = f.getClassName();

            if (upperFiberRuntime)
                upperFiberRuntime = SuspendableHelper9.isUpperFiberRuntime(cn);

            if (!upperFiberRuntime && !lowerFiberRuntime)
                lowerFiberRuntime = SuspendableHelper9.isFiber(cn);

            if (!upperFiberRuntime && !lowerFiberRuntime) {
                if (!isReflection(f.getClassName())) { // Skip reflection
                    final Verify.CheckFrameInstrumentationReport report =
                        Verify.checkFrameInstrumentation(fs, i, upper);

                    DEBUG("\tFrame: " + f + ", report: " + report.toString());

                    reports.add(new ReportRecord(f, upper, lower, report));

                    last = report.last;
                }
            }

            if (last)
                break;
        }
        return reports;
    }

    private static HashMap<String, int[]> getPreCallInstrumentationStatus(List<ReportRecord> reports) throws IllegalAccessException, InvocationTargetException {
        DEBUG("\n2) Saving pre-call instrumentation status:");
        final HashMap<String, int[]> methodToSuspOffsetsAfterInstrPreCall = new HashMap<>();
        for (final ReportRecord rr : reports) {
            final String mID = getMethodId(rr.f);
            if (methodToSuspOffsetsAfterInstrPreCall.get(mID) == null && rr.report.ann != null) {
                final int len = rr.report.ann.methodSuspendableCallOffsetsAfterInstrumentation().length;
                // TODO: copy probably not needed
                final int[] offsets = Arrays.copyOf(rr.report.ann.methodSuspendableCallOffsetsAfterInstrumentation(),len);
                methodToSuspOffsetsAfterInstrPreCall.put(mID, offsets);
                DEBUG("\t\"" + mID + "\": " + Arrays.toString(offsets));
            }
        }
        return methodToSuspOffsetsAfterInstrPreCall;
    }

    private static void instrument(List<ReportRecord> reports) throws IllegalAccessException, InvocationTargetException {
        DEBUG("\n3) Re-instrumenting");
        final Set<Class<?>> redefines = new HashSet<>();
        for (final ReportRecord rr : reports) {
            final StackWalker.StackFrame f = rr.f;
            final Verify.CheckFrameInstrumentationReport report = rr.report;

            final boolean ok = report.isOK();

            final Class<?> cCaller = f.getDeclaringClass();
            final MethodType mtCaller;
            mtCaller = (MethodType) getMethodType.invoke(memberName.get(f));

            // a) Print debug info
            DEBUG (
                "\tLive lazy auto-instrumentation for call frame: " + f.getClassName() + "#"+
                    f.getMethodName() + mtCaller.toMethodDescriptorString()
            );
            if (!ok)
                DEBUG("\t\tFrame instrumentation analysis found problems!");

            final String cn = cCaller.getName();
            final String mnCaller = f.getMethodName();

            // b) Fix DB
            DEBUG("\t\tEnsuring suspendable supers are correct");
            ensureCorrectSuspendableSupers(cCaller, mnCaller, mtCaller);

            if (!ok) {
                if (!report.classInstrumented || !report.methodInstrumented) {
                    DEBUG("\t\tClass or method not instrumented at all, marking method suspendable");
                    suspendable(cCaller, mnCaller, mtCaller, MethodDatabase.SuspendableType.SUSPENDABLE);
                }
            }

            LiveInstrumentationKB.askFrameTypesRecording(cn);
            redefines.add(cCaller);
        }
        for (final Class<?> c : redefines) {
            final String cn = c.getName();
            // c) Re-instrument
            DEBUG("\t\tReloading class " + cn + " from original classloader");
            try (final InputStream is = c.getResourceAsStream("/" + cn.replace(".", "/") + ".class")) {
                if (is != null) { // For some JDK dynamic classes it can be
                    DEBUG("\t\t\tRedefining => Quasar instrumentation with fixed suspendable info and frame type info will occur");
                    final byte[] diskData = ByteStreams.toByteArray(is);
                    Retransform.redefine(new ClassDefinition(c, diskData));
                } else {
                    DEBUG("\t\t\tClass source stream not found, not reloading");
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static HashMap<StackWalker.StackFrame, Integer> getPerFrameSuspendableCallIndexes(List<ReportRecord> reports, HashMap<String, int[]> methodToSuspOffsetsAfterInstrPreCall) throws IllegalAccessException, InvocationTargetException {
        DEBUG("\n4) Calculating suspendable call indexes");
        final HashMap<String, List<Integer>> methodToInvolvedCallSiteOffsets = new HashMap<>();
        for (final ReportRecord rr : reports) {
            final List<Integer> c;
            final String mID = getMethodId(rr.f);
            final int[] savedPreCallInstrOffsets = methodToSuspOffsetsAfterInstrPreCall.get(mID);
            final Method updatedM =
                SuspendableHelper9.lookupMethod (
                    rr.f.getDeclaringClass(), rr.f.getMethodName(),
                    (MethodType) getMethodType.invoke(memberName.get(rr.f))
                );
            final Instrumented updatedAnn = SuspendableHelper.getAnnotation(updatedM, Instrumented.class);
            final int[] aPrioriSuspendables =
                updatedAnn != null ?
                    updatedAnn.methodSuspendableCallOffsetsBeforeInstrumentation() : EMPTY_INT_ARRAY;
            c = methodToInvolvedCallSiteOffsets.getOrDefault (
                mID,
                Lists.newArrayList (
                    Ints.asList (
                        savedPreCallInstrOffsets != null ?
                            savedPreCallInstrOffsets : aPrioriSuspendables
                    )
                )
            );
            c.add((Integer) offset.get(rr.f));
            methodToInvolvedCallSiteOffsets.put(getMethodId(rr.f), c);
        }
        for (final ReportRecord rr : reports) {
            List<Integer> l = new ArrayList<>(methodToInvolvedCallSiteOffsets.get(getMethodId(rr.f)));
            l = new ArrayList<>(new HashSet<>(l));
            Collections.sort(l);
            methodToInvolvedCallSiteOffsets.put(getMethodId(rr.f), l);
        }
        DEBUG("\tPer-method suspendable call site offsets (pre-call + current live frame offsets, sorted):");
        for(final String m : methodToInvolvedCallSiteOffsets.keySet())
            DEBUG("\t\t\"" + m + "\": " + methodToInvolvedCallSiteOffsets.get(m));
        final HashMap<StackWalker.StackFrame, Integer> frameToSuspendableCallIndex = new HashMap<>();
        for (final ReportRecord rr : reports) {
            final int off;
            off = (int) offset.get(rr.f);
            frameToSuspendableCallIndex.put(rr.f, methodToInvolvedCallSiteOffsets.get(getMethodId(rr.f)).indexOf(off) + 1);
        }
        DEBUG("\tPer-frame suspendable call index (entry):");
        for(final StackWalker.StackFrame f : frameToSuspendableCallIndex.keySet())
            DEBUG("\t\t\"" + f + "\": " + frameToSuspendableCallIndex.get(f));
        return frameToSuspendableCallIndex;
    }

    private static java.util.Stack<FiberFramePush> createStackRebuildActions(List<ReportRecord> reports)
        throws IllegalAccessException, InvocationTargetException {

        final java.util.Stack<FiberFramePush> ret = new java.util.Stack<>();

        DEBUG("\n5) Creating stack rebuild actions");
        FiberFramePush upperFFP = null;
        for (final ReportRecord rr : reports) {
            final StackWalker.StackFrame f = rr.f;
            final StackWalker.StackFrame upper = rr.upper;
            final StackWalker.StackFrame lower = rr.lower;

            final Class<?> cCaller = f.getDeclaringClass();
            final MethodType mtCaller;
            mtCaller = (MethodType) getMethodType.invoke(memberName.get(f));
            final Method m = SuspendableHelper9.lookupMethod(cCaller, f.getMethodName(), mtCaller);
            // d) Enqueue stack-rebuild
            // The annotation will be correct now
            final Instrumented updatedAnn = SuspendableHelper.getAnnotation(m, Instrumented.class);
            if (updatedAnn != null && !updatedAnn.methodOptimized()) {
                DEBUG("\tMethod \"" + m + "\" is not optimized, creating a fiber stack rebuild record");
                upperFFP =
                    pushRebuildToDoFull (
                        f,
                        upper,
                        lower,
                        upperFFP,
                        ret
                    );
            } else {
                DEBUG("\tMethod \"" + m + "\" is optimized, creating an optimized fiber stack rebuild record");
                final FiberFramePushOptimized ffpo = new FiberFramePushOptimized(f);
                ret.push(ffpo);
                upperFFP = ffpo;
            }
        }

        return ret;
    }

    private static void rebuildFiberStack(Stack fiberStack, java.util.Stack<FiberFramePush> fiberStackRebuildToDoList, HashMap<StackWalker.StackFrame, Integer> frameToSuspendableCallIndex) {
        DEBUG("\n6) Rebuilding fiber stack");
        DEBUG("\t** Fiber stack dump before rebuild:"); // TODO: remove
        fiberStack.dump(); // TODO: remove
        DEBUG(""); // TODO: remove
        apply(fiberStackRebuildToDoList, frameToSuspendableCallIndex, fiberStack);
        DEBUG("\n\t** Fiber stack dump after rebuild:"); // TODO: remove
        fiberStack.dump(); // TODO: remove
        DEBUG(""); // TODO: remove
    }

    private static String getMethodId(StackWalker.StackFrame f) throws IllegalAccessException, InvocationTargetException {
        return f.getClassName() + "::" + f.getMethodName() +
            ((MethodType) getMethodType.invoke(memberName.get(f))).toMethodDescriptorString();
    }

    private static void apply(java.util.Stack<FiberFramePush> todo, Map<StackWalker.StackFrame, Integer> entries, Stack fs) {
        fs.clear();
        int i = 1;
        int s = todo.size();
        while (!todo.empty()) {
            final FiberFramePush ffp = todo.pop();
            System.err.println("\tApplying " + i + " of " + s + " (" + ffp.getClass() + ")");
            ffp.apply(entries, fs);
            i++;
        }
    }

    private abstract static class FiberFramePush {
        protected final StackWalker.StackFrame f;
        protected final MethodType mt;
        protected final Method m;
        protected final Object[] locals;
        protected final Object[] operands;
        protected final int currOffset;

        public FiberFramePush(StackWalker.StackFrame f) throws InvocationTargetException, IllegalAccessException {
            this.f = f;
            this.m = SuspendableHelper9.lookupMethod(f);
            this.mt = (MethodType) getMethodType.invoke(memberName.get(f));
            this.currOffset = (int) offset.get(f);
            this.locals = removeNulls((Object[]) getLocals.invoke(f));
            this.operands = removeNulls((Object[]) getOperands.invoke(f));
        }

        private static Object[] removeNulls(Object[] os) {
            final List<Object> l = new ArrayList<>();
            for (final Object o : os) {
                if (o != null)
                    l.add(o);
            }
            final Object[] ret = new Object[l.size()];
            l.toArray(ret);
            return ret;
        }

        abstract void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s);
    }

    private static class FiberFramePushOptimized extends FiberFramePush {

        public FiberFramePushOptimized(StackWalker.StackFrame sf) throws InvocationTargetException, IllegalAccessException {
            super(sf);
        }

        @Override
        public void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s) {
            DEBUG("\t\tFor " + m);
            DEBUG("\t\tJust increasing optimized count");
            s.incOptimizedCount();
        }
    }

    private static class FiberFramePushFull extends FiberFramePush {
        private final Method upperM;
        private final Object[] upperLocals;
        private final FiberFramePush upperFFP;

        private final Method lowerM;
        private final int lowerOffset;

        private final boolean isYield;

        private int numSlots = -1;

        private FiberFramePushFull(StackWalker.StackFrame f,
                                   StackWalker.StackFrame upper,
                                   StackWalker.StackFrame lower,
                                   FiberFramePush upperFFP)
            throws InvocationTargetException, IllegalAccessException
        {
            super(f);

            this.upperM = SuspendableHelper9.lookupMethod(upper);
            this.upperLocals = (Object[]) getLocals.invoke(upper);
            this.upperFFP = upperFFP;

            this.lowerM = SuspendableHelper9.lookupMethod(lower);
            this.lowerOffset = (Integer) offset.get(lower);

            this.isYield = Classes.isYieldMethod(upper.getClassName().replace('.', '/'), upper.getMethodName());
        }

        /**
         * Live fiber stack construction
         * <br>
         * !!! Must be kept aligned with `InstrumentMethod.emitStoreState` and `Stack.pushXXX` !!!
         */
        public void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s) {
            final Integer idx = entries.get(f);
            final String idxS = idx.toString();
            final String cn = f.getClassName();
            final String mn = f.getMethodName();
            final String md = mt.toMethodDescriptorString();

            final org.objectweb.asm.Type[] tsOperands = LiveInstrumentationKB.getFrameOperandStackTypes(cn, mn, md, idxS);
            final org.objectweb.asm.Type[] tsLocals = LiveInstrumentationKB.getFrameLocalTypes(cn, mn, md, idxS);

            // If the class was AOT-instrumented then the frame type info (which is always computed at runtime) will
            // include operands and locals added by instrumentation and some special adjustments are necessary.
            final boolean aot = LiveInstrumentationKB.isAOTInstrumented(f.getDeclaringClass());
            // TODO: do it after the whole process
            // LiveInstrumentationKB.clearAOTInfo(cn);

            DEBUG("\t\tFrame method \"" + m + "\":");
            DEBUG("\t\t\tCalled at offset " + lowerOffset + " of: " + lowerM);
            DEBUG("\t\t\tCalling at offset " + currOffset + ": " + upperM + " (yield = " + isYield + ")");
            DEBUG("\t\tFrame operands types from instrumentation:");
            int i = 1;
            boolean found = false;
            org.objectweb.asm.Type[] tsTmp;
            do {
                final String iStr = Integer.toString(i);
                tsTmp = LiveInstrumentationKB.getFrameOperandStackTypes(cn, mn, md, iStr);
                if (tsTmp != null) {
                    found = true;
                    DEBUG("\t\t\t" + iStr + ": " + Arrays.toString(tsTmp));
                }
                i++;
            } while (tsTmp != null);
            if (!found)
                DEBUG("\t\t\t<none>");
            DEBUG("\t\tLive operands: " + Arrays.toString(operands));
            DEBUG("\t\tUpper locals: " + Arrays.toString(upperLocals));
            DEBUG("\t\tFrame locals types from instrumentation:");
            i = 1;
            found = false;
            do {
                final String iStr = Integer.toString(i);
                tsTmp = LiveInstrumentationKB.getFrameLocalTypes(cn, mn, md, iStr);
                if (tsTmp != null) {
                    found = true;
                    DEBUG("\t\t\t" + iStr + ": " + Arrays.toString(tsTmp));
                }
                i++;
            } while (tsTmp != null);
            if (!found)
                DEBUG("\t\t\t<none>");
            DEBUG("\t\tLive locals: " + Arrays.toString(locals));
            DEBUG("\t\tSuspendable call index: " + idx);
            DEBUG("\t\tClass was AOT-instrumented: " + aot + "");

            // Operands and locals (in this order) slot indices
            int idxObj = 0, idxPrim = 0;

            // ************************************************************************************************
            // The in-flight situation of the stack is skewed compared to the static analysis one:
            // calls are in progress and in all non-top frames the upper part of operand the stack representing
            // arguments has been moved to param locals of the upper frame.
            //
            // On the other hand we don't need the values there anymore: the instrumentation saves them only in
            // the case there's a suspension point before the call but this is not the case since we're already
            // in a yield here (and a suspension point would be a yield anyway).
            //
            // Attempt 2: recover them from the upper frame. Note: their value might have changed but not their
            // number, and the value doesn't matter a lot as per above.
            // ************************************************************************************************

            // Recover shifted-up stack operands
            final int shiftedUpOperandsCount = countArgsAsJVMSingleSlots(upperM);
            final List<Object> preCallOperandsL = new ArrayList<>();
            preCallOperandsL.addAll(Arrays.asList(operands));
            preCallOperandsL.addAll(Arrays.asList(upperLocals).subList(0, shiftedUpOperandsCount));
            final Object[] preCallOperands = new Object[preCallOperandsL.size()];
            preCallOperandsL.toArray(preCallOperands);

            // Store stack operands
            // TODO: remove this reflection hack once the bug causing the locals of
            // TODO: `VIRTUAL java/lang/reflect/Method/invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;`
            // TODO: to be `[0, 0, 0, 0]` when not running in debug mode.
            final boolean callingReflection = isReflection(upperM.getDeclaringClass().getName());
            DEBUG("\t\tCalling reflection: " + callingReflection);
            final int reflectionArgsCount = callingReflection ? upperM.getParameterCount() + 1 : 0;
            DEBUG("\t\tReflection args count: " + reflectionArgsCount);

            if (s.nextMethodEntry() == 0) {
                DEBUG("\tDone `nextMethodEntry` and got 0, doing `isFirstInStackOrPushed`");
                s.isFirstInStackOrPushed();
            }

            // New frame
            final int slots = getNumSlots(tsOperands, tsLocals);
            DEBUG("\tDoing `pushMethod(suspCallIdx: " + idx + ", slots: " + slots + ")`");
            s.pushMethod(idx, slots);

            int idxTypes = 0, idxValues = 0;
            if (tsOperands != null) {
                DEBUG("\tPushing analyzed pre-call frame operands:");
                while (idxTypes + reflectionArgsCount < tsOperands.length /* && idxValues < preCallOperands.length */) {
                    final org.objectweb.asm.Type tOperand = tsOperands[idxTypes];
                    int inc = 1;
                    final Object op = preCallOperands[idxValues];
                    if (op != null) {
                        final String tID = type(op);
                        if (!isNullableType(tID)) {
                            if (primitiveValueClass.isInstance(op)) {
                                inc = storePrim(preCallOperands, idxValues, tOperand, s, idxPrim++);
                                DEBUG("\t\tPushed primitive in operand slots (" + (inc > 1 ? preCallOperands[idxValues + 1] + ", " :"") + preCallOperands[idxValues] + ") of size " + inc + " and type " + tOperand);
                            } else { // if (!(op instanceof Stack)) // Skip stack operands
                                Stack.push(op, s, idxObj++);
                                DEBUG("\t\tPushed object operand " + op + " of type " + tOperand);
                            }
                        }
                    }
                    idxValues += inc;
                    idxTypes++;
                }

                if (callingReflection) {
                    for (final Object o : reconstructReflectionArgs(upperFFP))
                        Stack.push(o, s, idxObj++);
                }
            }

            // TODO: Do it at the end of the whole process, in recursive cases this info is still needed afterwards
            // Cleanup some tmp mem
            // LiveInstrumentationKB.clearFrameOperandStackTypes(cn, mn, md, idx);

            // Store local vars, including args, except "this" (present in actual values but not types)
            idxTypes = 0;
            idxValues = (Modifier.isStatic(m.getModifiers()) ? 0 : 1);

            if (tsLocals != null) {
                DEBUG("\t\tPushing analyzed frame locals:");
                while (
                    idxTypes <
                        tsLocals.length - (
                            aot ? InstrumentMethod.NUM_LOCALS + 1 : 0
                        )
                        /* && idxValues < locals.length */
                ) {
                    final Object local = locals[idxValues];
                    final org.objectweb.asm.Type tLocal = tsLocals[idxTypes];
                    int inc = 1;
                    if (local != null) {
                        final String tID = type(local);
                        if (!isNullableType(tID)) {
                            if (primitiveValueClass.isInstance(local)) {
                                inc = storePrim(locals, idxValues, tLocal, s, idxPrim++);
                                DEBUG("\t\tPushed primitive in local slots (" + (inc > 1 ? locals[idxValues + 1] + ", " :"") + locals[idxValues] + ") of size " + inc + " and type " + tLocal);
                            } else { // if (!(local instanceof Stack)) { // Skip stack locals
                                Stack.push(local, s, idxObj++);
                                DEBUG("\t\tPushed object local " + local + " of type " + tLocal);
                            }
                        }
                    }
                    idxTypes++;
                    idxValues += inc;
                }
            }

            // Since the potential call to a yield method is in progress already (because live instrumentation is
            // called from all yield methods), we don't need to perform any special magic to preserve its args.

            // Cleanup some tmp mem; this assumes that live instrumentation doesn't need to run again for the
            // same methods (as it shouldn't actually need to run again, if it is correct).

            // TODO: Do it at the end of the whole process, in recursive cases this info is still needed afterwards
            // Cleanup some tmp mem
            // LiveInstrumentationKB.clearFrameLocalTypes(cn, mn, md, idx);
        }

        private Iterable<?> reconstructReflectionArgs(FiberFramePush upperFFP) {
            final boolean isStatic = Modifier.isStatic(upperFFP.m.getModifiers());
            final Object target = isStatic ? null : upperFFP.locals[0];
            final List<Object> methodArgs = new ArrayList<>();
            int idx = isStatic ? 0 : 1;
            for (int i = 0 ; i < upperFFP.m.getParameterCount() ; i++) {
                methodArgs.add(upperFFP.locals[idx]);
                idx++;
            }
            final List<Object> pushArgs = new ArrayList<>();
            upperFFP.m.setAccessible(true);
            // TODO: change the line below with `pushArgs.add(upperLocals[0])` (actual Method instance) and remove line above when the "no locals" issue is fixed
            pushArgs.add(upperFFP.m);
            pushArgs.add(target);
            pushArgs.add(methodArgs.toArray());
            return pushArgs;
        }

        private static int countArgsAsJVMSingleSlots(Executable m) {
            int count = Modifier.isStatic(m.getModifiers()) ? 0 : 1;
            for (final Class<?> c : m.getParameterTypes()) {
                if (Double.TYPE.equals(c) || Long.TYPE.equals(c))
                    count += 2;
                else
                    count++;
            }
            return count;
        }

        private int getNumSlots(Type[] tsOperands, Type[] tsLocals) {
            if (numSlots == -1) {
                int idxPrim = 0, idxObj = 0;

                // ************************************************************************************************
                // The in-flight situation of the stack is skewed compared to the static analysis one:
                // calls are in progress and in all non-top frames the upper part of operand the stack representing
                // arguments has been moved to param locals of the upper frame.
                //
                // On the other hand we don't need the values there anymore: the instrumentation saves them only in
                // the case there's a suspension point before the call but this is not the case since we're already
                // in a yield here (and a suspension point would be a yield anyway).
                //
                // Attempt 2: recover them from the upper frame. Note: their value might have changed but not their
                // number, and the value doesn't matter a lot as per above.
                // ************************************************************************************************

                // TODO: operands that are args of reflective calls don't correspond to real target call args

                // Count stack operands
                if (tsOperands != null) {
                    for (final Type tOperand : tsOperands) {
                        if (tOperand != null) {
                            if (isPrimitive(tOperand))
                                idxPrim++;
                            else
                                idxObj++;
                        }
                    }
                }

                // Count local vars
                if (tsLocals != null) {
                    for (final Type tLocal : tsLocals) {
                        if (tLocal != null) {
                            if (isPrimitive(tLocal))
                                idxPrim++;
                            else
                                idxObj++;
                        }
                    }
                }

                numSlots = Math.max(idxObj, idxPrim);
            }

            return numSlots;
        }

        private int storePrim(Object[] objs, int objsIdx, Type t, Stack s, int stackIdx) {
            int inc = 1;
            try {
                // TODO: ask if the present hack will stay (currently all values except double-word are returned as ints)
                if (isSinglePrimitive(t)) {
                    Stack.push((int) intValue.invoke(objs[objsIdx]), s, stackIdx);
                } else {
                    inc = 2;
                    final int i1 = (int) intValue.invoke(objs[objsIdx]), i2 = (int) intValue.invoke(objs[objsIdx + 1]);
                    if (Type.LONG_TYPE.equals(t))
                        Stack.push(twoIntsToLong(i1, i2), s, stackIdx);
                    else if (Type.DOUBLE_TYPE.equals(t))
                        Stack.push(twoIntsToDouble(i1, i2), s, stackIdx);
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return inc;
        }

        private static long twoIntsToLong(int a, int b) {
            // TODO: understand & fix
/*
            System.err.println("** twoIntsToLong **");
            System.err.println("a = " + Integer.toHexString(a) + "_16 = " + Integer.toBinaryString(a) + "_2");
            System.err.println("b = " + Integer.toHexString(b) + "_16 = " + Integer.toBinaryString(b) + "_2");
            System.err.println("4l = " + Long.toHexString(4L) + "_16 = " + Long.toHexString(4L) + "_2");
            final long res = (long)a << 32 | b & 0xFFFFFFFFL;
            System.err.println("(long)a << 32 | b & 0xFFFFFFFFL: " + Long.toHexString(res) + "_16 = " + Long.toHexString(res) + "_2");
            return res;
*/
            return 10;
        }

        private static double twoIntsToDouble(int a, int b) {
            // TODO: understand & fix
/*
            System.err.println("** twoIntsToDouble **");
            double ret = Double.longBitsToDouble(twoIntsToLong(a, b));
            long retBits = Double.doubleToRawLongBits(ret);
            long oneDotFourBits = Double.doubleToRawLongBits(1.4d);
            System.err.println("1.4d = " + Long.toHexString(oneDotFourBits) + "_16 = " + Long.toHexString(oneDotFourBits) + "_2");
            System.err.println("ret = " + Long.toHexString(retBits) + "_16 = " + Long.toHexString(retBits) + "_2");
            return ret;
*/
            return 1.4D;
        }

        // TODO: Re-generate
        // public String toString()
    }

    private static boolean isPrimitive(Type t) {
        return isSinglePrimitive(t) || isDoublePrimitive(t);
    }

    private static boolean isDoublePrimitive(Type t) {
        return Type.LONG_TYPE.equals(t) || Type.DOUBLE_TYPE.equals(t);
    }

    private static boolean isSinglePrimitive(Type t) {
        return
            Type.INT_TYPE.equals(t)  || Type.SHORT_TYPE.equals(t) || Type.BOOLEAN_TYPE.equals(t) ||
            Type.CHAR_TYPE.equals(t) || Type.BYTE_TYPE.equals(t)  || Type.FLOAT_TYPE.equals(t);
    }

    private static LiveInstrumentation.FiberFramePushFull pushRebuildToDoFull (
        StackWalker.StackFrame f,
        StackWalker.StackFrame upper,
        StackWalker.StackFrame lower,
        FiberFramePush upperFFP,
        java.util.Stack<FiberFramePush> todo
    ) {
        try {
            final FiberFramePushFull ffp =
                new FiberFramePushFull (
                    f,
                    upper,
                    lower,
                    upperFFP
                );
            todo.push(ffp);
            return ffp;
        } catch (final InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Less interesting

    private static long agree(StackWalker w, Stack fs) {
        // TODO: must be _fast_, JMH it
        final List<StackWalker.StackFrame> l = w.walk (s -> s.collect(Collectors.toList())); // TODO: remove
        StackFramePredicate.reset();
        final long threadStackDepth = w.walk (s -> s.filter(StackFramePredicate.INSTANCE).collect(COUNTING));
        return threadStackDepth - (fs.getInstrumentedCount() + fs.getOptimizedCount());
        // return 1;
    }

    private static class StackFramePredicate implements Predicate<StackWalker.StackFrame> {
        final private static StackFramePredicate INSTANCE = new StackFramePredicate();

        private boolean upperFiberRuntime, lowerFiberRuntime;

        @Override
        public final boolean test(StackWalker.StackFrame sf) {
            final String cn = sf.getClassName();

            if (upperFiberRuntime)
                upperFiberRuntime = SuspendableHelper9.isUpperFiberRuntime(cn);

            if (!upperFiberRuntime && !lowerFiberRuntime)
                lowerFiberRuntime = SuspendableHelper9.isFiber(cn);

            return
                !upperFiberRuntime &&
                !lowerFiberRuntime &&
                !isReflection(cn);
        }

        private StackFramePredicate() {
            reset(this);
        }

        private static void reset() {
            reset(INSTANCE);
        }

        private static void reset(StackFramePredicate p) {
            p.upperFiberRuntime = true;
            p.lowerFiberRuntime = false;
        }
    }

    private static boolean isReflection(String className) {
        return
            className.startsWith("sun.reflect.") ||
            className.startsWith("java.lang.reflect.");
    }

    public static final boolean ACTIVE;

    private static StackWalker esw = null;

    private static Class<?> primitiveValueClass;

    private static Method getLocals, getOperands, getMethodType, primitiveType;
    private static Method intValue;

    private static Field memberName, offset;

    static {
        try {
            // TODO: change to "disableXXX" when stable
            ACTIVE = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.enableLive");
            db = Retransform.getMethodDB();

            if (ACTIVE) {
                DEBUG("Live lazy auto-instrumentation ENABLED");

                final Class<?> extendedOptionClass = Class.forName("java.lang.StackWalker$ExtendedOption");
                final Method ewsNI = StackWalker.class.getDeclaredMethod("newInstance", Set.class, extendedOptionClass);
                ewsNI.setAccessible(true);

                final Set<StackWalker.Option> s = new HashSet<>();
                s.add(StackWalker.Option.RETAIN_CLASS_REFERENCE);
                s.add(StackWalker.Option.SHOW_REFLECT_FRAMES);
                s.add(StackWalker.Option.SHOW_HIDDEN_FRAMES);

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
                offset = stackFrameInfoClass.getDeclaredField("bci");
                offset.setAccessible(true);
                getMethodType = Class.forName("java.lang.invoke.MemberName").getDeclaredMethod("getMethodType");
                getMethodType.setAccessible(true);

                getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
                getLocals.setAccessible(true);

                getOperands = liveStackFrameClass.getDeclaredMethod("getStack");
                getOperands.setAccessible(true);

                primitiveType = primitiveValueClass.getDeclaredMethod("type");
                primitiveType.setAccessible(true);

                intValue = primitiveValueClass.getDeclaredMethod("intValue");
                // intValue.setAccessible(true);

                // getMonitors = liveStackFrameClass.getDeclaredMethod("getMonitors");
                // getMonitors.setAccessible(true);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////// Uninteresting

    private static final Collector<StackWalker.StackFrame, ?, Long> COUNTING = Collectors.counting();
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private static final MethodDatabase db;

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
            db.getOrCreateClassEntry (
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

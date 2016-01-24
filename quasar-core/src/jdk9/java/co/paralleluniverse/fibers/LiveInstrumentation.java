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

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author circlespainter
 */
public final class LiveInstrumentation {
    // TODO: remove `synchronized` or change it to a lock
    static synchronized boolean fixup(Fiber fiber) {
        DEBUG("\nCurrent live instrumentation count: " + runCount.get());

        if (DUMP_STACK_FRAMES_FIRST) {
            DEBUG("\nWARNING: live instrumentation's preliminar stack dump ACTIVE, this will SEVERELY harm performances");
            lastFrames = getStackFrames();
            try {
                DEBUG(dump(lastFrames));
            } catch (final Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        if (!ACTIVE || fiber == null) // Not using live instrumentation or not in a fiber => don't alter checks
            return true;

        final Stack fiberStack = fiber.getStack();
        final long diff = agree(esw, fiberStack);

        if (diff == 0) { // All OK already
            DEBUG("\nInstrumentation seems OK!\n");
            DEBUG("\t** Fiber stack dump:");
            DEBUG(fiberStack.toString());
            DEBUG("");

            // We're done, let's skip checks
            return false;
        }

        // Else slow path, we'll take our time to fix up things.
        DEBUG("\nFound mismatch between stack depth and fiber stack (" + diff + ") => activating live lazy auto-instrumentation");

        // *************************************************************************************************************
        // The goal is to rebuild a fiber stack "good enough" for a correct resume.
        //
        // The initial strategy is to ensure a stronger (but easier to describe) property: performing calls on the fiber
        // stack object that are equivalent (state-wise) to the ones agent-time instrumentation would inject if it had
        // complete information about suspendables present in the live stack: this means which classes are suspendables,
        // which methods, which call sites and _the resume indexes_ these call sites would have in a fully instrumented
        // method body relative to the suspendables present in the live stack.
        //
        // This latter information is also needed to recover type info from the instrumentation stage (live type info
        // is currently lacking in this respect: no doubles and no longs, they're represented as adjacent int slots).
        //
        // A call site is identified by class, method and bytecode offset. The final offset (and index) of a suspendable
        // call site will be known only at the end of the re-instrumentation stage because it can be increased by
        // instrumentation of other suspendable call sites in the same method that appear in the same live stack.
        //
        // The _relative ordering_ of the call sites active in that method's frames present in the current live stack
        // won't be changed by instrumentation.
        // The position in this ordering at live instrumentation time is not yet the correct index though, because
        // the method may contain previously instrumented call sites that are not present in the current live stack.
        //
        // But when live instrumentation starts, the bytecode offsets are aligned with the offsets in `@Instrumented`
        // because:
        //
        // a) Previous live instrumentation runs (or non-live instrumentation) will have fixed the code and annotation.
        // b) The stack will have been unrolled (by suspension) after a previous live instrumentation run (or lack of)
        //    and methods will have re-entered.
        //
        // This means that, for any method appearing in the live stack, we have both an up-to-date set of _already
        // instrumented call sites_ with correct offsets, and an up-to-date set of _involved call sites_ with correct
        // offsets.
        // As a side note, Since some involved call sites could have been instrumented already, these two sets could
        // have non-empty intersection.
        //
        // This means that, in order to obtain the 1-based final index that any involved suspendable call site would
        // have in the final, fully instrumented method body (relative to the suspendables present in the live stack,
        // which will be exactly the instrumented suspendables present in the stack upon resume) we can act as follows:
        //
        // a) Merge the set of the involved call site offsets (i.e. actual live offsets) with the set of the already
        //    instrumented (at live instrumentation time) call site offsets.
        // b) Sort that set.
        // c) Find the 0-based position of the involved suspendable call site in exam.
        // d) Add 1 (resume indexes are 1-based, "0" means no resume to be performed i.e. method start).
        // *************************************************************************************************************

        runCount.getAndIncrement();

        checkCaps();

        // TODO: 1) reduce garbage, 2) make faster

        final StackWalker.StackFrame[] fs = getStackFrames();

        try {
            // 1)
            final List<ReportRecord> reports =
                getInstrumentationReports(fs);

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
            DEBUG("!!!LIVE INSTRUMENTATION INTERNAL ERROR - PLEASE REPORT!!!");
            t.printStackTrace();
            throw new RuntimeException(t);
        }

        // We're done, let's skip checks
        return false;
    }

    private static long agree(StackWalker w, Stack fs) {
        if (FORCE) {
            DEBUG("\nWARNING: live instrumentation forcing ACTIVE, this will SEVERELY harm performances");
            return Integer.MIN_VALUE;
        }

        // TODO: must be _fast_, JMH it
        StackFramePredicate.reset();
        final List<StackWalker.StackFrame> frames = w.walk(s -> s.collect(Collectors.toList())); // TODO Remove
        final long threadStackDepth = w.walk(s -> s.filter(StackFramePredicate.INSTANCE).collect(COUNTING));
        return threadStackDepth - (fs.getInstrumentedCount() + fs.getOptimizedCount());
        // return 1;
    }

    private static StackWalker.StackFrame[] getStackFrames() {
        final List<StackWalker.StackFrame> fsL = esw.walk(s -> s.collect(Collectors.toList()));
        final StackWalker.StackFrame[] fs = new StackWalker.StackFrame[fsL.size()];
        fsL.toArray(fs);
        return fs;
    }

    private static class StackFramePredicate implements Predicate<StackWalker.StackFrame> {
        final private static StackFramePredicate INSTANCE = new StackFramePredicate();

        private boolean upperFiberRuntime, lowerFiberRuntime;

        @Override
        public final boolean test(StackWalker.StackFrame sf) {
            final String cn = sf.getClassName();
            final String mn = sf.getMethodName();

            if (upperFiberRuntime)
                upperFiberRuntime = SuspendableHelper9.isUpperRuntimeOfSuspendingStack(cn);

            if (!upperFiberRuntime && !lowerFiberRuntime)
                lowerFiberRuntime = SuspendableHelper9.startsLowerRuntimeOfSuspendingStack(cn, mn);

            return
                !upperFiberRuntime &&
                    !lowerFiberRuntime &&
                    !isReflection(cn) &&
                    !isDynamicInvoke(cn);
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

    private static List<ReportRecord> getInstrumentationReports(StackWalker.StackFrame[] fs) throws InvocationTargetException, IllegalAccessException {
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
            final String mn = f.getMethodName();

            if (upperFiberRuntime)
                upperFiberRuntime = SuspendableHelper9.isUpperRuntimeOfSuspendingStack(cn);

            if (!upperFiberRuntime && !lowerFiberRuntime)
                lowerFiberRuntime = SuspendableHelper9.startsLowerRuntimeOfSuspendingStack(cn, mn);

/*
            // Fix stale offsets in AoT-instrumented classes due to shadow transform
            final Class<?> c = f.getDeclaringClass();
            final Instrumented ann = c.getAnnotation(Instrumented.class);
            if (ann != null && ann.isClassAOTInstrumented()) {
                try (final InputStream is = c.getResourceAsStream("/" + cn.replace(".", "/") + ".class")) {
                    if (is != null) { // For some JDK dynamic classes it can be
                        DEBUG("\t\tReloading AoT class " + cn + " from original classloader and redefining to fix offset mismatches (shadow)");
                        final byte[] diskData = ByteStreams.toByteArray(is);
                        Retransform.redefine(new ClassDefinition(c, diskData));
                    } else {
                        DEBUG("\t\t\tClass source stream not found, not reloading to fix offset mismatches (shadow)");
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
*/

            if (!upperFiberRuntime && !lowerFiberRuntime) {
                if (!isReflection(f.getClassName()) && !isDynamicInvoke(f.getClassName())) { // Skip reflection && dynInvoke
                    final Verify.CheckFrameInstrumentationReport report =
                        Verify.checkFrameInstrumentation(fs, i, upper);

                    DEBUG("\tLive frame call site: " + getCallSiteId(f) + ", report: " + report.toString());

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
                final SuspendableCallSite[] suspCallSites = rr.report.ann.methodSuspendableCallSites();
                final int len = suspCallSites.length;
                final int[] offsets = new int[len];
                for (int i = 0 ; i < len ; i++)
                    offsets[i] = suspCallSites[i].postInstrumentationOffset();
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
            DEBUG("\tLive lazy auto-instrumentation for frame call site: " + getCallSiteId(f));
            if (!ok)
                DEBUG("\t\tFrame instrumentation analysis found problems!");

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
/*
            final Instrumented ci = cCaller.getAnnotation(Instrumented.class);
            if (ci != null && ci.isClassAOTInstrumented())
                DEBUG("\t\tClass " + cCaller.getName() + " is AoT-instrumented, not redefining");
            else
*/
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
                e.printStackTrace();
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
            final SuspendableCallSite[] susCallSites = updatedAnn != null ? updatedAnn.methodSuspendableCallSites() : null;
            final int[] aPrioriSuspendables = susCallSites != null ? new int[susCallSites.length] : EMPTY_INT_ARRAY;
            for (int i = 0 ; i < aPrioriSuspendables.length ; i++) {
                assert susCallSites != null;
                aPrioriSuspendables[i] = susCallSites[i].preInstrumentationOffset();
            }
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
            // The annotation will be correct now
            final Instrumented updatedAnn = SuspendableHelper.getAnnotation(m, Instrumented.class);
            if (updatedAnn != null && !updatedAnn.isMethodInstrumentationOptimized()) {
                DEBUG("\tMethod of call site " + getCallSiteId(f) + " is not optimized, creating a fiber stack rebuild record");
                upperFFP =
                    pushRebuildToDoFull (
                        f,
                        upper,
                        lower,
                        upperFFP,
                        ret
                    );
            } else {
                DEBUG("\tMethod of call site " + getCallSiteId(f) + " is optimized, creating an optimized fiber stack rebuild record");
                final FiberFramePushOptimized ffpo = new FiberFramePushOptimized(f);
                ret.push(ffpo);
                upperFFP = ffpo;
            }
        }

        return ret;
    }

    private static void rebuildFiberStack(Stack fiberStack, java.util.Stack<FiberFramePush> fiberStackRebuildToDoList, HashMap<StackWalker.StackFrame, Integer> frameToSuspendableCallIndex) throws InvocationTargetException, IllegalAccessException {
        DEBUG("\n6) Rebuilding fiber stack");
        DEBUG("\t** Fiber stack dump before rebuild:");
        DEBUG(fiberStack.toString());
        DEBUG("");
        apply(fiberStackRebuildToDoList, frameToSuspendableCallIndex, fiberStack);
        DEBUG("\n\t** Fiber stack dump after rebuild:");
        DEBUG(fiberStack.toString());
        DEBUG("");
    }

    private static String getMethodId(StackWalker.StackFrame f) throws IllegalAccessException, InvocationTargetException {
        return f.getClassName() + "#" + f.getMethodName() + ((MethodType) getMethodType.invoke(memberName.get(f))).toMethodDescriptorString();
    }

    private static String getCallSiteId(StackWalker.StackFrame f) throws IllegalAccessException, InvocationTargetException {
        return
            f.getFileName().orElse("<UNKNOWN SOURCE>") + "@l" + f.getLineNumber().orElse(-1) +
            ":\"" + getMethodId(f) + "\"@b" + offset.get(f);
    }

    private static void apply(java.util.Stack<FiberFramePush> todo, Map<StackWalker.StackFrame, Integer> entries, Stack fs) throws InvocationTargetException, IllegalAccessException {
        fs.clear();
        int i = 1;
        int s = todo.size();
        while (!todo.empty()) {
            final FiberFramePush ffp = todo.pop();
            DEBUG("\tApplying " + i + " of " + s);
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
            this.locals = (Object[]) getLocals.invoke(f);
            this.operands = (Object[]) getOperands.invoke(f);
        }

        abstract void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s) throws InvocationTargetException, IllegalAccessException;
    }

    private static class FiberFramePushOptimized extends FiberFramePush {

        public FiberFramePushOptimized(StackWalker.StackFrame sf) throws InvocationTargetException, IllegalAccessException {
            super(sf);
        }

        @Override
        public void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s) throws InvocationTargetException, IllegalAccessException {
            DEBUG("\t\t[FiberFramePushOptimized] Just increasing opt. count for " + getCallSiteId(f));
            s.incOptimizedCount();
        }
    }

    private static class FiberFramePushFull extends FiberFramePush {
        private final StackWalker.StackFrame upper;
        private final Method upperM;
        private final Object[] upperLocals;
        private final FiberFramePush upperFFP;

        private final StackWalker.StackFrame lower;

        private final boolean isUpperYield;

        private FiberFramePushFull(StackWalker.StackFrame f,
                                   StackWalker.StackFrame upper,
                                   StackWalker.StackFrame lower,
                                   FiberFramePush upperFFP)
            throws InvocationTargetException, IllegalAccessException
        {
            super(f);

            this.upper = upper;
            this.upperM = SuspendableHelper9.lookupMethod(upper);
            this.upperLocals = (Object[]) getLocals.invoke(upper);
            this.upperFFP = upperFFP;

            this.lower = lower;

            this.isUpperYield = Classes.isYieldMethod(upper.getClassName().replace('.', '/'), upper.getMethodName());
        }

        @FunctionalInterface
        private interface FiberStackOp {
            void apply(Stack s);
        }

        private class SkipNull implements FiberStackOp {
            private final String msg;

            private SkipNull(String msg) {
                this.msg = msg;
            }

            @Override
            public void apply(Stack s) {
                DEBUG(msg);
            }
        }

        private class PushObject implements FiberStackOp {
            private final Object value;
            private final int idx;
            private final String msg;
            private final Type t;

            private PushObject(Object value, Type t, int idx, String msg) {
                this.value = value;
                this.t = t;
                this.idx = idx;
                this.msg = msg;
            }

            @Override
            public void apply(Stack s) {
                DEBUG(msg);
                if (!matchObj(t, value))
                    throw new IllegalStateException("The type " + t + " is not assignable from the value " + value);
                Stack.push(value, s, idx);
            }
        }

        private class PushPrimitive implements FiberStackOp {
            private final Object[] ops;
            private final int i;
            private final Type t;
            private final int idx;
            private final String msg;

            private PushPrimitive(Object[] ops, int i, Type t, int idx, String msg) {
                this.ops = ops;
                this.i = i;
                this.t = t;
                this.idx = idx;
                this.msg = msg;
            }

            @Override
            public void apply(Stack s) {
                DEBUG(msg);
                if (!matchPrim(t, ops, i))
                    throw new IllegalStateException("The type " + t + " is not assignable from the value " + ops[i]);
                storePrim(ops, i, t, s, idx);
            }
        }

        /**
         * Live fiber stack construction
         * <br>
         * !!! Must be kept aligned with `InstrumentMethod.emitFiberStackStoreState` and `Stack.pushXXX` !!!
         */
        public void apply(Map<StackWalker.StackFrame, Integer> entries, Stack s) throws InvocationTargetException, IllegalAccessException {
            final Instrumented ann = SuspendableHelper.getAnnotation(m, Instrumented.class);
            final Integer idx = entries.get(f);

            DEBUG("\t\t[FiberFramePushFull] " + getCallSiteId(f));
            DEBUG("\t\t\tCalled at: " + getCallSiteId(lower));
            DEBUG("\t\t\tUpper: " + getCallSiteId(upper) + " (yield = " + isUpperYield + ")");
            DEBUG("\t\t\tFrame operands types from instrumentation (reverse):");
            int i = 1;
            boolean found = false;
            for (final SuspendableCallSite scs : ann.methodSuspendableCallSites()) {
                final String[] tsTmp = scs.stackFrameOperandsTypes();
                if (tsTmp != null) {
                    found = true;
                    DEBUG("\t\t\t\t" + i + ": " + Arrays.toString(tsTmp));
                    i++;
                }
            }
            if (!found)
                DEBUG("\t\t\t\t<none>");

            final List<Integer> idxOperandsL = Ints.asList(ann.methodSuspendableCallSites()[idx-1].stackFrameOperandsIndexes());
            Collections.reverse(idxOperandsL);
            final int[] idxOperands = Ints.toArray(idxOperandsL);

            DEBUG("\t\t\tFrame operands indexes from instrumentation: " + Arrays.toString(idxOperands));
            DEBUG("\t\t\tLive operands: " + Arrays.toString(operands));
            DEBUG("\t\t\tUpper locals: " + Arrays.toString(upperLocals));
            DEBUG("\t\t\tFrame locals types from instrumentation:");
            i = 1;
            found = false;
            for (final SuspendableCallSite scs : ann.methodSuspendableCallSites()) {
                final String[] tsTmp = scs.stackFrameLocalsTypes();
                if (tsTmp != null) {
                    found = true;
                    DEBUG("\t\t\t\t" + i + ": " + Arrays.toString(tsTmp));
                    i++;
                }
            }
            if (!found)
                DEBUG("\t\t\t\t<none>");

            final Type[] tsLocals = toTypes(ann.methodSuspendableCallSites()[idx-1].stackFrameLocalsTypes());
            final int[] idxLocals = ann.methodSuspendableCallSites()[idx-1].stackFrameLocalsIndexes();
            DEBUG("\t\t\tFrame locals fiber stack indexes from instrumentation: " + Arrays.toString(idxLocals));
            DEBUG("\t\t\tLive locals: " + Arrays.toString(locals));
            DEBUG("\t\t\tSuspendable call index: " + idx);

            final Instrumented classAnn = m.getDeclaringClass().getAnnotation(Instrumented.class);
            final boolean aot = classAnn.isClassAOTInstrumented();
            DEBUG("\t\t\tClass was AOT-instrumented: " + aot + "");

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
            DEBUG("\t\t\tCalling reflection: " + callingReflection);
            final int reflectionArgsCount = callingReflection ? upperM.getParameterCount() + 1 : 0;
            DEBUG("\t\t\tReflection args count: " + reflectionArgsCount);

            // Stack types are in reverse order w.r.t. what we need here
            final List<Type> tsOperandsL = Arrays.asList(toTypes(ann.methodSuspendableCallSites()[idx-1].stackFrameOperandsTypes()));
            Collections.reverse(tsOperandsL);
            final Type[] tsOperands = new Type[tsOperandsL.size()];
            tsOperandsL.toArray(tsOperands);

            final List<FiberStackOp> operandsOps = new ArrayList<>();
            int idxTypes = 0, idxValues = 0;
            while (idxTypes + reflectionArgsCount < tsOperands.length) {
                int inc = 1;
                final Type tOperand = tsOperands[idxTypes];
                final Object op = preCallOperands[idxValues];
                if (!isNullType(tOperand)) {
                    if (op != null && primitiveValueClass.isInstance(op)) {
                        inc = getTypeSize(tOperand);
                        operandsOps.add (
                            new PushPrimitive (
                                preCallOperands, idxValues, tOperand, idxPrim,
                                "\t\t\t\tPUSH " + idxPrim + " OP(" + idxValues + ") PRIM (" +
                                    op + (inc > 1 ? "," + preCallOperands[idxValues + 1] : "") +
                                    ") :? " + tOperand + " : " + op.getClass()
                            )
                        );
                        idxPrim++;
                    } else {
                        operandsOps.add (
                            new PushObject (
                                op, tOperand, idxObj,
                                "\t\t\t\tPUSH " + idxObj + " OP(" + idxValues + ") OBJ (" +
                                    op +
                                    ") :? " + tOperand + " : " + (op != null ? op.getClass() : "null")
                            )
                        );
                        idxObj++;
                    }
                } else {
                    operandsOps.add(new SkipNull("\t\t\t\tNULL OP(" + idxValues + ")"));
                }
                idxValues += inc;
                idxTypes++;
            }

            if (callingReflection) {
                for (final Object o : reconstructReflectionArgs(upperFFP))
                    operandsOps.add(new PushObject(o, null, idxObj++, "\t\t\tPushed reflection object operand " + o));
            }

            // Store local vars, including args, except "this" (present in actual values but not types)
            final List<FiberStackOp> localsOps = new ArrayList<>();
            idxTypes = 0;
            idxValues = (Modifier.isStatic(m.getModifiers()) ? 0 : 1);
            if (tsLocals != null) {
                while (idxTypes < tsLocals.length &&
                       // The following test is necessary because tail uninitialized locals appear in types but not always in live
                       idxValues < locals.length) {
                    int inc = 1;
                    final Type tLocal = tsLocals[idxTypes];
                    final int slot = idxValues; // Shadow's relocation would scramble them during AoT, difficult to track them; relocation disable for now, see https://github.com/johnrengelman/shadow/issues/176
                    final Object local = locals[slot];
                    if (!isNullType(tLocal)) {
                        if (local != null && primitiveValueClass.isInstance(local)) {
                            inc = getTypeSize(tLocal);
                            localsOps.add (
                                new PushPrimitive (
                                    locals, idxValues, tLocal, idxPrim,
                                    "\t\t\t\tPUSH " + idxPrim + " LOC(" + slot + ") PRIM (" +
                                        local + (inc > 1 ? "," + locals[slot + 1] : "") +
                                        ") :? " + tLocal + " : " + local.getClass()
                                )
                            );
                            idxPrim++;
                        } else {
                            localsOps.add (
                                new PushObject (
                                    local, tLocal, idxObj,
                                    "\t\t\t\tPUSH " + idxObj + " LOC(" + slot + ") OBJ (" +
                                        local +
                                        ") :? " + tLocal + " : " + (local != null ? local.getClass() : "null")
                                )
                            );
                            idxObj++;
                        }
                    } else {
                        operandsOps.add(new SkipNull("\t\t\t\tNULL OP(" + idxValues + ")"));
                    }
                    idxTypes += inc; // In local types a long is a (J, null) seq and a double is a (D, null) seq
                    idxValues += inc;
                }
            }

            DEBUG("\t\tActions:");

            if (s.nextMethodEntry() == 0) {
                DEBUG("\t\t\tDone `nextMethodEntry` and got 0, doing `isFirstInStackOrPushed`");
                s.isFirstInStackOrPushed();
            }

            // New frame
            final int slots = Math.max(idxObj, idxPrim);
            DEBUG("\t\t\tDoing `pushMethod(suspCallIdx: " + idx + ", slots: " + slots + ")`");
            s.pushMethod(idx, slots);

            // Pushes
            DEBUG("\t\t\tPushing analyzed pre-call frame operands:");
            found = false;
            for(final FiberStackOp op : operandsOps) {
                found = true;
                op.apply(s);
            }
            if (!found)
                DEBUG("\t\t\t\t<none>");
            DEBUG("\t\t\tPushing analyzed frame locals:");
            found = false;
            for(final FiberStackOp op : localsOps) {
                found = true;
                op.apply(s);
            }
            if (!found)
                DEBUG("\t\t\t\t<none>");

            // Since the potential call to a yield method is in progress already (because live instrumentation is
            // called from all yield methods), we don't need to perform any special magic to preserve its args.

            // Cleanup some tmp mem; this assumes that live instrumentation doesn't need to run again for the
            // same methods (as it shouldn't actually need to run again, if it is correct).
        }

        private static boolean matchObj(Type t, Object v) {
            // TODO: distinguish between null and uninitialized
            // TODO: make stronger by checking assignability
            return t == null || isNullType(t) || !isDoublePrimitive(t) && !isSinglePrimitive(t);
        }

        private static boolean matchPrim(Type t, Object[] objs, int i) {
            if (isDoublePrimitive(t))
                return primitiveValueClass.isInstance(objs[i]) && primitiveValueClass.isInstance(objs[i+1]);
            else
                return primitiveValueClass.isInstance(objs[i]);
        }

        private static boolean isNullType(Type t) {
            return "null".equals(t.toString());
        }

        private static int getTypeSize(Type t) {
            return isDoublePrimitive(t) ? 2 : 1;
        }

        private static Type[] toTypes(String[] ts) {
            if (ts == null)
                return null;

            final Type[] ret = new Type[ts.length];
            for (int i = 0 ; i < ts.length ; i++)
                ret[i] = Type.getType(ts[i]);

            return ret;
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

        private static void storePrim(Object[] objs, int objsIdx, Type t, Stack s, int stackIdx) {
            try {
                // TODO: ask if the present hack will stay (currently all values except double-word are returned as ints)
                if (isSinglePrimitive(t)) {
                    Stack.push((int) intValue.invoke(objs[objsIdx]), s, stackIdx);
                } else {
                    final int i1 = (int) intValue.invoke(objs[objsIdx]), i2 = (int) intValue.invoke(objs[objsIdx + 1]);
                    if (Type.LONG_TYPE.equals(t))
                        Stack.push(twoIntsToLong(i1, i2), s, stackIdx);
                    else if (Type.DOUBLE_TYPE.equals(t))
                        Stack.push(twoIntsToDouble(i1, i2), s, stackIdx);
                }
            } catch (final InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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

    private static boolean isSinglePrimitive(Type t) {
        return
            Type.INT_TYPE.equals(t)  || Type.SHORT_TYPE.equals(t) || Type.BOOLEAN_TYPE.equals(t) ||
            Type.CHAR_TYPE.equals(t) || Type.BYTE_TYPE.equals(t)  || Type.FLOAT_TYPE.equals(t);
    }

    private static boolean isDoublePrimitive(Type t) {
        return
            Type.DOUBLE_TYPE.equals(t)  || Type.LONG_TYPE.equals(t);
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
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static boolean isReflection(String className) {
        return
            className.startsWith("sun.reflect.") ||
            className.startsWith("java.lang.reflect.");
    }

    private static boolean isDynamicInvoke(String className) {
        return
            className.startsWith("java.lang.invoke.");
    }

    public static final boolean DUMP_STACK_FRAMES_FIRST;
    public static final boolean ACTIVE;
    public static final boolean FORCE;

    private static final PrintStream err;

    private static StackWalker esw = null;

    private static Class<?> primitiveValueClass;

    private static Method getLocals, getOperands, getMonitors, primitiveType, getMethodType, intValue;

    private static Field memberName, offset;

    static {
        try {
            // TODO: change to "disableXXX" when stable
            DUMP_STACK_FRAMES_FIRST = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.live.dumpStackFramesFirst");
            ACTIVE = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.live.enable");
            FORCE = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.instrument.live.force");

            // Bypass any replacement, especially Gradle's threadlocal-based that interfers with
            // serialization of threadlocals in fibers (ClassNotFound)
            err = new PrintStream(new FileOutputStream(FileDescriptor.err));

            db = Retransform.getMethodDB();

            if (ACTIVE || DUMP_STACK_FRAMES_FIRST) {
                if (ACTIVE) {
                    DEBUG("Live lazy auto-instrumentation ENABLED");
                }

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
                final Class<?> liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
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

                getMonitors = liveStackFrameClass.getDeclaredMethod("getMonitors");
                getMonitors.setAccessible(true);

                primitiveType = primitiveValueClass.getDeclaredMethod("type");
                primitiveType.setAccessible(true);

                intValue = primitiveValueClass.getDeclaredMethod("intValue");
                intValue.setAccessible(true);
            }
        } catch (final Exception e) {
            e.printStackTrace();
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

    private static String dump(StackWalker.StackFrame[] fs) throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("\n*********************STACK DUMP START*********************\n");
        for (final StackWalker.StackFrame f : fs) {
            //noinspection RedundantCast
            sb.append(String.format (
                "[FRAME] %s:%d, %s#%s%s @ %db%n",
                f.getFileName().orElse("<UNKNOWN>"), f.getLineNumber().orElse(-1),
                f.getClassName(), f.getMethodName(),
                ((MethodType) getMethodType.invoke(memberName.get(f))).toMethodDescriptorString(),
                (int) offset.get(f)
            ));

            final Object[] locals = (Object[]) getLocals.invoke(f);
            for (int i = 0; i < locals.length; i++)
                sb.append(String.format("\tlocal %d: %s type %s%n", i, locals[i], type(locals[i])));

            final Object[] operands = (Object[]) getOperands.invoke(f);
            for (int i = 0; i < operands.length; i++)
                sb.append(String.format("\toperand %d: %s type %s%n", i, operands[i], type(operands[i])));

            final Object[] monitors = (Object[]) getMonitors.invoke(f);
            for (int i = 0; i < monitors.length; i++)
                sb.append(String.format("\tmonitor %d: %s%n", i, monitors[i]));
        }
        sb.append("\n*********************STACK DUMP END***********************\n");
        return sb.toString();
    }

    private static String type(Object o) throws Exception {
        if (primitiveValueClass.isInstance(o)) {
            final char c = (char) primitiveType.invoke(o);
            return String.valueOf(c);
        } else if (o != null) {
            return o.getClass().getName();
        } else {
            return "null";
        }
    }

    private static void DEBUG(String s) {
        // TODO
        /*
        if (db.isDebug())
            db.getLog().log(LogLevel.DEBUG, "[LIVE] " + s);
        */
        err.println(s); // Workaround Gradle threadlocal-based out/err serializability issue
//        System.out.println(s);
    }

    private static StackWalker.StackFrame[] lastFrames;

    private static final AtomicLong runCount = new AtomicLong(0L);

    @VisibleForTesting
    public static long fetchRunCount() {
        return runCount.getAndSet(0L);
    }

    @VisibleForTesting
    public static void resetRunCount() {
        runCount.set(0L);
    }

    private LiveInstrumentation() {}
}

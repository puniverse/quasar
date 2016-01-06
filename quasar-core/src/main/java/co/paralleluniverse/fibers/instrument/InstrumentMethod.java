/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

// import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.fibers.Instrumented;
import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.Stack;
import static co.paralleluniverse.fibers.instrument.Classes.INSTRUMENTED_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.THROWABLE_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.RUNTIME_EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.INVOCATION_TARGET_EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.SUSPEND_EXECUTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.RUNTIME_SUSPEND_EXECUTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.STACK_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.UNDECLARED_THROWABLE_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.isAllowedToBlock;
import static co.paralleluniverse.fibers.instrument.Classes.blockingCallIdx;
import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;

import co.paralleluniverse.fibers.SuspendableCallSite;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isInvocationHandlerInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isMethodHandleInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isReflectInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isSyntheticAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

/**
 * Instrument a method to allow suspension
 *
 * @author Matthias Mann
 * @author pron
 */
@VisibleForTesting
public class InstrumentMethod {

    static class FrameInfo {
        static final FrameInfo FIRST = new FrameInfo(null, 0, 0, null, null);
        final int endInstruction;
        final int numSlots;
        final int numObjSlots;
        final int[] localSlotIndices;
        final int[] stackSlotIndices;
        BlockLabelNode lBefore;
        BlockLabelNode lAfter;

        FrameInfo(Frame f, int firstLocal, int endInstruction, InsnList insnList, MethodDatabase db) {
            this.endInstruction = endInstruction;

            int idxObj = 0;
            int idxPrim = 0;

            if (f != null) {
                stackSlotIndices = new int[f.getStackSize()];
                for (int i = 0; i < f.getStackSize(); i++) {
                    final BasicValue v = (BasicValue) f.getStack(i);
                    if (v instanceof NewValue) {
                        // explanation in emitNewAndDup
                        final NewValue newValue = (NewValue) v;
                        if (db.isDebug())
                            db.log(LogLevel.DEBUG, "Omit value from stack idx %d at instruction %d with type %s generated by %s", i, endInstruction, v, newValue.formatInsn());
                        if (!newValue.omitted) {
                            newValue.omitted = true;
                            if (db.isDebug())
                                db.log(LogLevel.DEBUG, "Omitting instruction %d: %s", insnList.indexOf(newValue.insn), newValue.formatInsn()); // // log index before replacing instruction
                            insnList.set(newValue.insn, new OmittedInstruction(newValue.insn));
                        }
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    } else if (!isNullType(v)) {
                        if (v.isReference())
                            stackSlotIndices[i] = idxObj++;
                        else
                            stackSlotIndices[i] = idxPrim++;
                    } else {
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    }
                }

                localSlotIndices = new int[f.getLocals()];
                for (int i = firstLocal; i < f.getLocals(); i++) {
                    final BasicValue v = (BasicValue) f.getLocal(i);
                    if (!isNullType(v)) {
                        if (v.isReference())
                            localSlotIndices[i] = idxObj++;
                        else
                            localSlotIndices[i] = idxPrim++;
                    } else
                        localSlotIndices[i] = -666; // an invalid index ;)
                }
            } else {
                stackSlotIndices = null;
                localSlotIndices = null;
            }

            numSlots = Math.max(idxPrim, idxObj);
            numObjSlots = idxObj;
        }

        public LabelNode createBeforeLabel() {
            if (lBefore == null)
                lBefore = new BlockLabelNode(endInstruction);
            return lBefore;
        }

        public LabelNode createAfterLabel() {
            if (lAfter == null)
                lAfter = new BlockLabelNode(endInstruction);
            return lAfter;
        }
    }

    public static final boolean optimizationDisabled = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.disableInstrumentationOptimization");

    private static final boolean HANDLE_PROXY_INVOCATIONS = true;
    // private final boolean verifyInstrumentation; //
    // private static final int PREEMPTION_BACKBRANCH = 0;
    // private static final int PREEMPTION_CALL = 1;
    // private static final String INTERRUPTED_EXCEPTION_NAME = Type.getInternalName(InterruptedException.class);
    // private static final boolean DUAL = true; // true if suspendable methods can be called from regular threads in addition to fibers
    private final MethodDatabase db;
    /** @noinspection FieldCanBeLocal, unused */
    private final String sourceName;
    private final String className;
    private final MethodNode mn;
    private final Frame[] frames;

    public static final int NUM_LOCALS = 3; // = 3 + (verifyInstrumentation ? 1 : 0); // lvarStack, lvarResumed, lvarInvocationReturnValue
    public static final int ADD_OPERANDS = 6; // 4;

    public static final int ADD_OPERANDS_OPTIMIZED = 2;

    private final int lvarStack; // ref to Stack
    private final int lvarResumed; // boolean indicating if we've been resumed
    private final int lvarInvocationReturnValue;
    // private final int lvarSuspendableCalled; // true iff we've called another suspendable method (used when VERIFY_INSTRUMENTATION)

    private final int firstLocal;
    private int additionalLocals;

    private FrameInfo[] codeBlocks = new FrameInfo[32];
    private int numCodeBlocks;

    private boolean warnedAboutMonitors;
    private int warnedAboutBlocking;

    private boolean callsSuspendableSupers;

    private int startSourceLine = -1;
    private int endSourceLine = -1;


    static class SuspCallSite {
        final int idx; // Not output in annotation
        final String desc;
        final int entry;
        final int sourceLine;
        final List<Type> operandTypes;
        final List<Type> localTypes;

        int preInstrumentationOffset = -1, postInstrumentationOffset = -1; // Set in `SuspOffsetsAfterInstrClassVisitor`

        SuspCallSite(int idx, String desc, int entry, int sourceLine, List<Type> operandTypes, List<Type> localTypes) {
            this.idx = idx;
            this.desc = desc;
            this.entry = entry;
            this.sourceLine = sourceLine;
            this.operandTypes = operandTypes;
            this.localTypes = localTypes;
        }

        public SuspCallSite(String desc, int entry, int sourceLine, List<Type> operandTypes, List<Type> localTypes) {
            this(-1, desc, entry, sourceLine, operandTypes, localTypes);
        }
    }

    private List<SuspCallSite> suspCallSites;

    public InstrumentMethod(MethodDatabase db, String sourceName, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.sourceName = sourceName;
        this.className = className;
        this.mn = mn;

        try {
            Analyzer a = new TypeAnalyzer(db);
            this.frames = a.analyze(className, mn);
            this.lvarStack = mn.maxLocals;
            this.lvarResumed = mn.maxLocals + 1;
            this.lvarInvocationReturnValue = mn.maxLocals + 2;
            // this.lvarSuspendableCalled = (verifyInstrumentation ? mn.maxLocals + 3 : -1);
            this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
        } catch (UnsupportedOperationException ex) {
            throw new AnalyzerException(null, ex.getMessage(), ex);
        }
    }

    public static boolean isSuspendableCall(MethodDatabase db, int type, int opcode, String owner, String name, String desc) {
        boolean susp = true;

        if (type == AbstractInsnNode.METHOD_INSN) {
            if (!isSyntheticAccess(owner, name)
                && !isReflectInvocation(owner, name)
                && !isMethodHandleInvocation(owner, name)
                && !isInvocationHandlerInvocation(owner, name)) {
                SuspendableType st = db.isMethodSuspendable(owner, name, desc, opcode);

                if (st == SuspendableType.NON_SUSPENDABLE)
                    susp = false;
            }
        } else if (type == AbstractInsnNode.INVOKE_DYNAMIC_INSN) { // invoke dynamic
            if (owner.equals("java/lang/invoke/LambdaMetafactory")) // lambda
                susp = false;
        } else
            susp = false;

        return susp;
    }

    public static void dumpParameterAnnotations(MethodVisitor mv, List[] parameterAnnotations, boolean visible) {
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (parameterAnnotations[i] != null) {
                for (final Object o : parameterAnnotations[i]) {
                    AnnotationNode an = (AnnotationNode) o;
                    an.accept(mv.visitParameterAnnotation(i, an.desc, visible));
                }
            }
        }
    }

    public static void emitInstrumentedAnn (
        MethodDatabase db, MethodVisitor mv, MethodNode mn, String className,
        boolean optimized, int methodStart, int methodEnd,
        List<SuspCallSite> suspCallSites
    ) {
        final StringBuilder sb = new StringBuilder();

        final AnnotationVisitor instrumentedAV = mv.visitAnnotation(INSTRUMENTED_DESC, true);
        sb.append("@").append(Instrumented.class.getSimpleName()).append("(");

        instrumentedAV.visit(Instrumented.FIELD_NAME_METHOD_START_SOURCE_LINE, methodStart);
        sb.append(Instrumented.FIELD_NAME_METHOD_START_SOURCE_LINE).append("=").append(methodStart).append(",");
        instrumentedAV.visit(Instrumented.FIELD_NAME_METHOD_END_SOURCE_LINE, methodEnd);
        sb.append(Instrumented.FIELD_NAME_METHOD_END_SOURCE_LINE).append("=").append(methodEnd).append(",");
        instrumentedAV.visit(Instrumented.FIELD_NAME_IS_METHOD_INSTRUMENTATION_OPTIMIZED, optimized);
        sb.append(Instrumented.FIELD_NAME_IS_METHOD_INSTRUMENTATION_OPTIMIZED).append("=").append(optimized);

        if (suspCallSites != null) {
            final AnnotationVisitor suspCallSitesAV =
                instrumentedAV.visitArray(Instrumented.FIELD_NAME_METHOD_SUSPENDABLE_CALL_SITES);
            sb.append(Instrumented.FIELD_NAME_METHOD_SUSPENDABLE_CALL_SITES + "=[");
            boolean first = true;
            for (final SuspCallSite scs : suspCallSites) {
                if (!first) {
                    sb.append(",");
                }
                first = false;

                final AnnotationVisitor suspCallSiteAV =
                    suspCallSitesAV.visitAnnotation(null, Classes.SUSPENDABLE_CALL_SITE_DESC);

                sb.append("@").append(SuspendableCallSite.class.getSimpleName()).append("(");
                suspCallSiteAV.visit(SuspendableCallSite.FIELD_NAME_DESC, scs.desc);
                sb.append(SuspendableCallSite.FIELD_NAME_DESC).append("=").append(scs.desc);
                suspCallSiteAV.visit(SuspendableCallSite.FIELD_NAME_ENTRY, scs.entry);
                sb.append(SuspendableCallSite.FIELD_NAME_ENTRY).append("=").append(scs.entry);
                suspCallSiteAV.visit(SuspendableCallSite.FIELD_NAME_SOURCE_LINE, scs.sourceLine);
                sb.append(SuspendableCallSite.FIELD_NAME_SOURCE_LINE).append("=").append(scs.sourceLine);
                suspCallSiteAV.visit(SuspendableCallSite.FIELD_NAME_PRE_INSTRUMENTATION_OFFSET, scs.preInstrumentationOffset);
                sb.append(SuspendableCallSite.FIELD_NAME_PRE_INSTRUMENTATION_OFFSET).append("=").append(scs.preInstrumentationOffset);
                suspCallSiteAV.visit(SuspendableCallSite.FIELD_NAME_POST_INSTRUMENTATION_OFFSET, scs.postInstrumentationOffset);
                sb.append(SuspendableCallSite.FIELD_NAME_POST_INSTRUMENTATION_OFFSET).append("=").append(scs.postInstrumentationOffset);

                if (scs.operandTypes != null) {
                    final AnnotationVisitor frameOpsAV = suspCallSiteAV.visitArray(SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_TYPES);
                    sb.append(SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_TYPES + "=[");
                    boolean first1 = true;
                    for (final Type t : scs.operandTypes) {
                        if (!first1) {
                            sb.append(",");
                        }
                        first1 = false;
                        frameOpsAV.visit(null, t.getDescriptor());
                        sb.append(t.getDescriptor());
                    }
                    frameOpsAV.visitEnd();
                    sb.append("]");
                }

                if (scs.localTypes != null) {
                    final AnnotationVisitor frameLocsAV = suspCallSiteAV.visitArray(SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_TYPES);
                    sb.append(SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_TYPES + "=[");
                    boolean first1 = true;
                    for (final Type t : scs.localTypes) {
                        if (!first1) {
                            sb.append(",");
                        }
                        first1 = false;
                        frameLocsAV.visit(null, t.getDescriptor());
                        sb.append(t.getDescriptor());
                    }
                    frameLocsAV.visitEnd();
                    sb.append("]");
                }

                suspCallSiteAV.visitEnd();
                sb.append(")");
            }

            suspCallSitesAV.visitEnd();
            sb.append("]");
        }

        instrumentedAV.visitEnd();
        sb.append(")");

        db.log(LogLevel.DEBUG, "Annotating method %s#%s%s with %s", className, mn.name, mn.desc, sb);
    }

    public boolean hasSuspendableCalls() {
        collectCodeBlocks(false);
        return suspCallSites.size() > 0;
    }

    private void collectCodeBlocks(boolean possiblyWarn) {
        if (suspCallSites == null) {
            suspCallSites = new ArrayList<>();

            final int numIns = mn.instructions.size();

            int currSourceLine = -1;
            int entry = 0;
            codeBlocks[0] = FrameInfo.FIRST;
            for (int i = 0; i < numIns; i++) {
                final Frame f = frames[i];
                if (f != null) { // reachable ?
                    final AbstractInsnNode in = mn.instructions.get(i);
                    if (in.getType() == AbstractInsnNode.LINE) {
                        final LineNumberNode lnn = (LineNumberNode) in;
                        currSourceLine = lnn.line;
                        if (startSourceLine == -1 || currSourceLine < startSourceLine)
                            startSourceLine = currSourceLine;
                        if (endSourceLine == -1 || currSourceLine > endSourceLine)
                            endSourceLine = currSourceLine;
                    } else if (in.getType() == AbstractInsnNode.METHOD_INSN || in.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                        boolean susp = true;
                        if (in.getType() == AbstractInsnNode.METHOD_INSN) {
                            final MethodInsnNode min = (MethodInsnNode) in;
                            int opcode = min.getOpcode();
                            if (isSyntheticAccess(min.owner, min.name))
                                db.log(LogLevel.DEBUG, "Synthetic accessor method call at instruction %d is assumed suspendable", i);
                            else if (isReflectInvocation(min.owner, min.name))
                                db.log(LogLevel.DEBUG, "Reflective method call at instruction %d is assumed suspendable", i);
                            else if (isMethodHandleInvocation(min.owner, min.name))
                                db.log(LogLevel.DEBUG, "MethodHandle invocation at instruction %d is assumed suspendable", i);
                            else if (isInvocationHandlerInvocation(min.owner, min.name))
                                db.log(LogLevel.DEBUG, "InvocationHandler invocation at instruction %d is assumed suspendable", i);
                            else {
                                SuspendableType st = db.isMethodSuspendable(min.owner, min.name, min.desc, opcode);
                                if (st == SuspendableType.NON_SUSPENDABLE)
                                    susp = false;
                                else if (st == null) {
                                    db.log(LogLevel.WARNING, "Method not found in class - assuming suspendable: %s#%s%s (at %s#%s)", min.owner, min.name, min.desc, className, mn.name);
                                } else if (st != SuspendableType.SUSPENDABLE_SUPER) {
                                    db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is suspendable", i, min.owner, min.name, min.desc);
                                }
                                if (st == SuspendableType.SUSPENDABLE_SUPER) {
                                    db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s to suspendable-super (instrumentation for proxy support will be enabled)", i, min.owner, min.name, min.desc);
                                    this.callsSuspendableSupers = true;
                                }
                            }
                        } else if (in.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                            // invoke dynamic
                            final InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) in;
                            if (idin.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                                // lambda
                                db.log(LogLevel.DEBUG, "Lambda at instruction %d", i);
                                susp = false;
                            } else
                                db.log(LogLevel.DEBUG, "InvokeDynamic Method call at instruction %d to is assumed suspendable", i);
                        }

                        if (susp) {
                            addCodeBlock(f, i);
                            addSuspendableCallSite(f, i, ++entry /* 1-based */, currSourceLine, getMethodName(in) + getMethodDesc(in));
                        } else if (in.getType() == AbstractInsnNode.METHOD_INSN) { // not invokedynamic
                            //noinspection ConstantConditions
                            final MethodInsnNode min = (MethodInsnNode) in;
                            db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is not suspendable", i, min.owner, min.name, min.desc);
                            if (possiblyWarn)
                                possiblyWarnAboutBlocking(min);
                        }
                    }
                }
            }
            addCodeBlock(null, numIns);
        }
    }

    private void splitTryCatches() {
        for (int i = 1 ; i < numCodeBlocks ; i++)
            splitTryCatch(codeBlocks[i]);
    }

    public void applySuspendableCallsInstrumentation(MethodVisitor mv, boolean hasAnnotation) {
        db.log(LogLevel.INFO, "Instrumenting method %s#%s%s", className, mn.name, mn.desc);

        final boolean optimizeInstrumentation = canInstrumentationBeOptimized();

        collectCodeBlocks(true); // Must be called first, sets flags & state used below

        if (optimizeInstrumentation) {
            db.log(LogLevel.INFO, "[OPTIMIZE] Optimizing instrumentation for method %s#%s%s", className, mn.name, mn.desc);
            applyOptimizedInstrumentation(mv);
            // mn.applySuspendableCallsInstrumentation(mv); // Dump
            return;
        }

        // Else instrument

        splitTryCatches();

        //noinspection PointlessBooleanExpression,ConstantConditions
        final boolean handleProxyInvocations = HANDLE_PROXY_INVOCATIONS && callsSuspendableSupers;

        mv.visitCode();

        int currLine = -1;

        final Label lMethodStart = new Label();
        final Label lMethodStart2 = new Label();
        final Label lMethodEnd = new Label();
        final Label lCatchSEE = new Label();
        final Label lCatchUTE = new Label();
        final Label lCatchAll = new Label();
        final Label[] lMethodCalls = new Label[numCodeBlocks - 1];
        final Label[][] refInvokeTryCatch;

        for (int i = 1; i < numCodeBlocks; i++)
            lMethodCalls[i - 1] = new Label();

        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, lvarInvocationReturnValue);

//        if (verifyInstrumentation) {
//            mv.visitInsn(Opcodes.ICONST_0);
//            mv.visitVarInsn(Opcodes.ISTORE, lvarSuspendableCalled);
//        }
        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchSEE, SUSPEND_EXECUTION_NAME);
        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchSEE, RUNTIME_SUSPEND_EXECUTION_NAME);
        if (handleProxyInvocations)
            mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchUTE, UNDECLARED_THROWABLE_NAME);

        // Prepare visitTryCatchBlocks for InvocationTargetException.
        // With reflective invocations, the SuspendExecution exception will be wrapped in InvocationTargetException. We need to catch it and unwrap it.
        // Note that the InvocationTargetException will be regenerated on every park, adding further overhead on top of the reflective call.
        // This must be done here, before all other visitTryCatchBlock, because the exception's handler
        // will be matched according to the order of in which visitTryCatchBlock has been called. Earlier calls take precedence.
        refInvokeTryCatch = new Label[numCodeBlocks - 1][];
        for (int i = 1; i < numCodeBlocks; i++) {
            final FrameInfo fi = codeBlocks[i];
            final AbstractInsnNode in = mn.instructions.get(fi.endInstruction);
            if (mn.instructions.get(fi.endInstruction) instanceof MethodInsnNode) {
                final MethodInsnNode min = (MethodInsnNode) in;
                if (isReflectInvocation(min.owner, min.name)) {
                    Label[] ls = new Label[3];
                    for (int k = 0; k < 3; k++)
                        ls[k] = new Label();
                    refInvokeTryCatch[i - 1] = ls;
                    mv.visitTryCatchBlock(ls[0], ls[1], ls[2], INVOCATION_TARGET_EXCEPTION_NAME);
                }
            }
        }

        // Output try-catch blocks
        for (final Object o : mn.tryCatchBlocks) {
            final TryCatchBlockNode tcb = (TryCatchBlockNode) o;

            //noinspection PointlessBooleanExpression
            if (SUSPEND_EXECUTION_NAME.equals(tcb.type) && !hasAnnotation && !LiveInstrumentation.ACTIVE) // we allow catch of SuspendExecution in method annotated with @Suspendable or if live instrumentation is active.
                throw new UnableToInstrumentException("catch for SuspendExecution", className, mn.name, mn.desc);
            if (handleProxyInvocations && UNDECLARED_THROWABLE_NAME.equals(tcb.type)) // we allow catch of SuspendExecution in method annotated with @Suspendable.
                throw new UnableToInstrumentException("catch for UndeclaredThrowableException", className, mn.name, mn.desc);
//          if (INTERRUPTED_EXCEPTION_NAME.equals(tcb.type))
//              throw new UnableToInstrumentException("catch for " + InterruptedException.class.getSimpleName(), className, mn.name, mn.desc);

            tcb.accept(mv);
        }

        // Output parameter annotations
        if (mn.visibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);
        if (mn.invisibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);

        // Output method annotations
        if (mn.visibleAnnotations != null) {
            for (final Object o : mn.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }

        //noinspection ConstantConditions
        emitInstrumentedAnn(db, mv, mn, className, optimizeInstrumentation, startSourceLine, endSourceLine, suspCallSites);

        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchAll, null);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        // println(mv, "STACK: ", lvarStack);
        // dumpStack(mv);
        // DUAL
        mv.visitJumpInsn(Opcodes.IFNULL, lMethodStart);
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);

        emitStoreResumed(mv, true); // we'll assume we have been resumed

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "nextMethodEntry", "()I", false);
        mv.visitTableSwitchInsn(1, numCodeBlocks - 1, lMethodStart2, lMethodCalls);

        mv.visitLabel(lMethodStart2);

        // the following code handles the case of an instrumented method called not as part of a suspendable code path
        // isFirstInStack will return false in that case.
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "isFirstInStackOrPushed", "()Z", false);
        mv.visitJumpInsn(Opcodes.IFNE, lMethodStart); // if true

        // This will reset the fiber stack local if isFirstStack returns false.
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        mv.visitLabel(lMethodStart);

        emitStoreResumed(mv, false); // no, we have not been resumed

        currLine = emitCodeBlockAfterIdx(mv, 0, 0, currLine);

        // Blocks leading to suspendable calls
        for (int i = 1; i < numCodeBlocks; i++) {
            final FrameInfo fi = codeBlocks[i];

            // Emit instrumented call
            final AbstractInsnNode min = mn.instructions.get(fi.endInstruction);
            final String owner = getMethodOwner(min), name = getMethodName(min), desc = getMethodDesc(min);
            if (isYieldMethod(owner, name)) { // special case - call to yield
                if (min.getOpcode() != Opcodes.INVOKESTATIC)
                    throw new UnableToInstrumentException("invalid call to suspending method.", className, mn.name, mn.desc);

                final int numYieldArgs = TypeAnalyzer.getNumArguments(desc);
                final boolean yieldReturnsValue = (Type.getReturnType(desc) != Type.VOID_TYPE);

                // NO DUAL: yield callers will only be in suspendable call paths
                emitFiberStackStoreState(mv, i, fi, numYieldArgs); // we preserve the arguments for the call to yield on the operand stack
                emitStoreResumed(mv, false); // we have not been resumed
                // emitSuspendableCalled(mv);

                final Label yieldCall = new Label();         // To read the correct offset later
                mv.visitLabel(yieldCall);
                mv.visitLineNumber(currLine, yieldCall);     // Force label output

                min.accept(mv);                              // we call the yield method
                if (yieldReturnsValue)
                    mv.visitInsn(Opcodes.POP);               // we ignore the returned value...

                mv.visitLabel(lMethodCalls[i - 1]);          // we resume AFTER the call

                final Label afterPostRestore = new Label();
                mv.visitVarInsn(Opcodes.ILOAD, lvarResumed);
                mv.visitJumpInsn(Opcodes.IFEQ, afterPostRestore);
                emitFiberStackPostRestore(mv);
                mv.visitLabel(afterPostRestore);

                emitFiberStackRestoreState(mv, fi, numYieldArgs);
                if (yieldReturnsValue)
                    mv.visitVarInsn(Opcodes.ILOAD, lvarResumed); // ... and replace the returned value with the value of resumed

                currLine = emitCodeBlockAfterIdx(mv, i, 1 /* skip the yieldcall */, currLine);
            } else {
                final Label lbl = new Label();

                // DUAL
                mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
                mv.visitJumpInsn(Opcodes.IFNULL, lbl);

                // normal case - call to a suspendable method - resume before the call
                emitFiberStackStoreState(mv, i, fi, 0); // For preemption point
                emitStoreResumed(mv, false); // we have not been resumed
                // emitPreemptionPoint(mv, PREEMPTION_CALL);
                mv.visitLabel(lMethodCalls[i - 1]);
                emitFiberStackRestoreState(mv, fi, 0); // For preemption point

                // DUAL
                mv.visitLabel(lbl); // Also good to collect offset later in non-reflect case

                if (isReflectInvocation(owner, name)) {
                    // We catch the InvocationTargetException and unwrap it if it wraps a SuspendExecution exception.
                    final Label[] ls = refInvokeTryCatch[i - 1];
                    final Label startTry = ls[0];
                    final Label endTry = ls[1];
                    final Label startCatch = ls[2];
                    final Label endCatch = new Label();
                    final Label notSuspendExecution = new Label();

                    // mv.visitTryCatchBlock(startTry, endTry, startCatch, "java/lang/reflect/InvocationTargetException");
                    mv.visitLabel(startTry);   // try { // Also good to collect offset later

                    min.accept(mv);            //   method.invoke()

                    mv.visitVarInsn(Opcodes.ASTORE, lvarInvocationReturnValue); // save return value
                    mv.visitLabel(endTry);     // }
                    mv.visitJumpInsn(Opcodes.GOTO, endCatch);
                    mv.visitLabel(startCatch); // catch(InvocationTargetException ex) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getCause", "()Ljava/lang/Throwable;", false);
                    mv.visitTypeInsn(Opcodes.INSTANCEOF, SUSPEND_EXECUTION_NAME);
                    mv.visitJumpInsn(Opcodes.IFEQ, notSuspendExecution);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getCause", "()Ljava/lang/Throwable;", false);
                    mv.visitLabel(notSuspendExecution);
                    mv.visitInsn(Opcodes.ATHROW);
                    mv.visitLabel(endCatch);

                    mv.visitVarInsn(Opcodes.ALOAD, lvarInvocationReturnValue); // restore return value
                    currLine = emitCodeBlockAfterIdx(mv, i, 1 /* skip the reflective call */, currLine);
                } else {
                    // emitSuspendableCalled(mv);
                    min.accept(mv);            // susp call

                    // Dec instrumented count
                    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
                    mv.visitInsn(Opcodes.DUP);
                    final Label pop = new Label(), rest = new Label();
                    mv.visitJumpInsn(Opcodes.IFNULL, pop);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "decInstrumentedCount", "()V", false);
                    mv.visitJumpInsn(Opcodes.GOTO, rest);

                    mv.visitLabel(pop);
                    mv.visitInsn(Opcodes.POP);

                    mv.visitLabel(rest);

                    currLine = emitCodeBlockAfterIdx(mv, i, 1 /* skip the susp call */, currLine);
                }
            }
        }

        // Emit catchall's catch section
        mv.visitLabel(lMethodEnd);

        if (handleProxyInvocations) {
            mv.visitLabel(lCatchUTE);
            mv.visitInsn(Opcodes.DUP);

            // println(mv, "CTCH: ");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getCause", "()Ljava/lang/Throwable;", false);
            // println(mv, "CAUSE: ");
            mv.visitTypeInsn(Opcodes.INSTANCEOF, SUSPEND_EXECUTION_NAME);
            mv.visitJumpInsn(Opcodes.IFEQ, lCatchAll);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getCause", "()Ljava/lang/Throwable;", false);
            mv.visitJumpInsn(Opcodes.GOTO, lCatchSEE);
        }

        mv.visitLabel(lCatchAll);
        emitFiberStackPopMethod(mv);
        mv.visitLabel(lCatchSEE);

        // println(mv, "THROW: ");
        mv.visitInsn(Opcodes.ATHROW);   // rethrow shared between catchAll and catchSSE

        if (mn.localVariables != null) {
            for (final Object o : mn.localVariables)
                ((LocalVariableNode) o).accept(mv);
        }

        mv.visitMaxs(mn.maxStack + ADD_OPERANDS, mn.maxLocals + NUM_LOCALS + additionalLocals); // Needed by ASM analysis

        mv.visitEnd();
    }

    private static boolean isSuspendableCall(MethodDatabase db, AbstractInsnNode in) {
        final int type = in.getType();
        String owner;
        String name;
        String desc;
        if (type == AbstractInsnNode.METHOD_INSN) {
            final MethodInsnNode min = (MethodInsnNode) in;
            owner = min.owner;
            name = min.name;
            desc = min.desc;
        } else if (type == AbstractInsnNode.INVOKE_DYNAMIC_INSN) { // invoke dynamic
            final InvokeDynamicInsnNode idd = (InvokeDynamicInsnNode) in;
            owner = idd.bsm.getOwner();
            name = idd.name;
            desc = idd.desc;
        } else {
            throw new RuntimeException("Not a call: " + in);
        }

        return isSuspendableCall(db, type, in.getOpcode(), owner, name, desc);
    }

    private void applyOptimizedInstrumentation(MethodVisitor mv) {
        db.log(LogLevel.INFO, "Minimally instrumenting optimized method %s#%s%s", className, mn.name, mn.desc);

        mv.visitCode();

        // Output try-catch blocks
        for (final Object o : mn.tryCatchBlocks) {
            final TryCatchBlockNode tcb = (TryCatchBlockNode) o;
            tcb.accept(mv);
        }

        // Output parameter annotations
        if (mn.visibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);
        if (mn.invisibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);

        // Output method annotations
        if (mn.visibleAnnotations != null) {
            for (final Object o : mn.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }

        emitInstrumentedAnn(db, mv, mn, className, true, startSourceLine, endSourceLine, suspCallSites);

        dumpUnoptimizedCodeBlockAfterIdx(mv, 0, 0);

        // Blocks leading to suspendable calls
        for (int i = 1; i < numCodeBlocks; i++) {
            final FrameInfo fi = codeBlocks[i];

            // Emit instrumented call

            final AbstractInsnNode min = mn.instructions.get(fi.endInstruction);

            final Label suspCall = new Label(), end = new Label();
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false); // * S
            mv.visitJumpInsn(Opcodes.IFNULL, suspCall); // *
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false); // * S
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getCurrentMethodEntry", "()I", false); // * i
            mv.visitJumpInsn(Opcodes.IFNE, suspCall); // != 0 => resuming => skip incrementing count // *
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false); // * S
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "incOptimizedCount", "()V", false); // *

            mv.visitLabel(suspCall); // Also to collect offset later
            min.accept(mv); // susp call // * -> .

            mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false); // . S
            mv.visitJumpInsn(Opcodes.IFNULL, end); // .
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";", false); // . S
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "decOptimizedCount", "()V", false); // .

            mv.visitLabel(end);

            // Emit the rest

            dumpUnoptimizedCodeBlockAfterIdx(mv, i, 1 /* skip the call */);
        }

        if (mn.localVariables != null) {
            for (final Object o : mn.localVariables)
                ((LocalVariableNode) o).accept(mv);
        }

        mv.visitMaxs(mn.maxStack + ADD_OPERANDS_OPTIMIZED /* fiber stack w/dup */, mn.maxLocals); // Needed by ASM analysis

        mv.visitEnd();
    }

    private void possiblyWarnAboutBlocking(final AbstractInsnNode ain) throws UnableToInstrumentException {
        if (ain instanceof MethodInsnNode) {
            final MethodInsnNode min = (MethodInsnNode) ain;
            int blockingId = blockingCallIdx(min);
            if (blockingId >= 0 && !isAllowedToBlock(className, mn.name)) {
                int mask = 1 << blockingId;
                if (!db.isAllowBlocking()) {
                    throw new UnableToInstrumentException("blocking call to " + min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                } else if ((warnedAboutBlocking & mask) == 0) {
                    warnedAboutBlocking |= mask;
                    db.log(LogLevel.WARNING, "Method %s#%s%s contains potentially blocking call to " + min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                }
            }
        }
    }

    private boolean canInstrumentationBeOptimized() {
        if (optimizationDisabled) {
            db.log(LogLevel.DEBUG, "[OPTIMIZE] Optimization disabled, not examining method %s#%s%s", className, mn.name, mn.desc);
            return false;
        }

        db.log(LogLevel.DEBUG, "[OPTIMIZE] Examining method %s#%s%s", className, mn.name, mn.desc);
        return isForwardingToSuspendable(); // Fully instrumentation-transparent methods
    }

    private boolean isForwardingToSuspendable() {
        if (suspCallSites.size() != 1)
            return false; // we allow exactly one suspendable call

        final int susCallIdx = suspCallSites.get(0).idx;

        final AbstractInsnNode susCall = mn.instructions.get(susCallIdx);
        assert isSuspendableCall(db, susCall);
        if (isYieldMethod(getMethodOwner(susCall), getMethodName(susCall)))
            return false; // yield calls require instrumentation (to skip the call when resuming)
        if (isReflectInvocation(getMethodOwner(susCall), getMethodName(susCall)))
            return false; // Reflective calls require instrumentation to handle SuspendExecution wrapped in InvocationTargetException
        if (hasSuspendableTryCatchBlocksAround(susCallIdx))
            return false; // Catching `SuspendableExecution needs instrumentation in order to propagate it

        // before suspendable call:
        for (int i = 0; i < susCallIdx; i++) {
            final AbstractInsnNode ins = mn.instructions.get(i);

            if (ins.getType() == AbstractInsnNode.METHOD_INSN || ins.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN)
                return false; // methods calls might have side effects
            if (ins.getType() == AbstractInsnNode.FIELD_INSN)
                return false; // side effects
            if (ins instanceof JumpInsnNode && mn.instructions.indexOf(((JumpInsnNode) ins).label) <= i)
                return false; // back branches may be costly, so we'd rather capture state
            if (!db.isAllowMonitors() && (ins.getOpcode() == Opcodes.MONITORENTER || ins.getOpcode() == Opcodes.MONITOREXIT))
                return false;  // we need collectCodeBlocksAndSplitTryCatches to warn about monitors
        }

        // after suspendable call
        for (int i = susCallIdx + 1; i <= mn.instructions.size() - 1; i++) {
            final AbstractInsnNode ins = mn.instructions.get(i);

            if (ins instanceof JumpInsnNode && mn.instructions.indexOf(((JumpInsnNode) ins).label) <= susCallIdx)
                return false; // if we jump before the suspendable call we suspend more than once -- need instrumentation
            if (!db.isAllowMonitors() && (ins.getOpcode() == Opcodes.MONITORENTER || ins.getOpcode() == Opcodes.MONITOREXIT))
                return false;  // we need collectCodeBlocksAndSplitTryCatches to warn about monitors
            if (!db.isAllowBlocking() && (ins instanceof MethodInsnNode && blockingCallIdx((MethodInsnNode) ins) != -1))
                return false;  // we need collectCodeBlocksAndSplitTryCatches to warn about blocking calls
        }

        return true;
    }

    private boolean hasSuspendableTryCatchBlocksAround(int idx) {
        //noinspection unchecked
        for (final TryCatchBlockNode tcb : (List<TryCatchBlockNode>) mn.tryCatchBlocks) {
            if (mn.instructions.indexOf(tcb.start) <= idx && mn.instructions.indexOf(tcb.end) >= idx
                && (THROWABLE_NAME.equals(tcb.type)
                || EXCEPTION_NAME.equals(tcb.type)
                || RUNTIME_EXCEPTION_NAME.equals(tcb.type)
                || RUNTIME_SUSPEND_EXECUTION_NAME.equals(tcb.type)
                || SUSPEND_EXECUTION_NAME.equals(tcb.type)))
                return true;
        }
        return false;
    }

    /*
    private void dumpStack(MethodVisitor mv) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V", false);
    }
    */

    private FrameInfo addCodeBlock(Frame f, int end) {
        if (++numCodeBlocks == codeBlocks.length) {
            FrameInfo[] newArray = new FrameInfo[numCodeBlocks * 2];
            System.arraycopy(codeBlocks, 0, newArray, 0, codeBlocks.length);
            codeBlocks = newArray;
        }
        final FrameInfo fi = new FrameInfo(f, firstLocal, end, mn.instructions, db);
        codeBlocks[numCodeBlocks] = fi;
        return fi;
    }

    private void emitStoreResumed(MethodVisitor mv, boolean value) {
        mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, lvarResumed);
    }

    private int getLabelIdx(LabelNode l) {
        int idx;
        if (l instanceof BlockLabelNode) {
            idx = ((BlockLabelNode) l).idx;
        } else {
            idx = mn.instructions.indexOf(l);
        }

        // search for the "real" instruction
        for (;;) {
            int type = mn.instructions.get(idx).getType();
            if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE) {
                return idx;
            }
            idx++;
        }
    }

    @SuppressWarnings("unchecked")
    private void splitTryCatch(FrameInfo fi) {
        for (int i = 0; i < mn.tryCatchBlocks.size(); i++) {
            final TryCatchBlockNode tcb = (TryCatchBlockNode) mn.tryCatchBlocks.get(i);

            int start = getLabelIdx(tcb.start);
            int end = getLabelIdx(tcb.end);

            if (start <= fi.endInstruction && end >= fi.endInstruction) {
                db.log(LogLevel.DEBUG, "Splitting try-catch in %s, block %d call at instruction %d", mn.name, i, fi.endInstruction);
                //System.out.println("i="+i+" start="+start+" end="+end+" split="+splitIdx+
                //        " start="+mn.instructions.get(start)+" end="+mn.instructions.get(end));

                // need to split try/catch around the suspendable call
                if (start == fi.endInstruction) {
                    tcb.start = fi.createAfterLabel();
                } else {
                    if (end > fi.endInstruction) {
                        final TryCatchBlockNode tcb2 = new TryCatchBlockNode(
                            fi.createAfterLabel(),
                            tcb.end, tcb.handler, tcb.type);
                        mn.tryCatchBlocks.add(i + 1, tcb2);
                    }

                    tcb.end = fi.createBeforeLabel();
                }
            }
        }
    }

    private void dumpUnoptimizedCodeBlockAfterIdx(MethodVisitor mv, int idx, int skip) {
        int start = codeBlocks[idx].endInstruction;
        int end = codeBlocks[idx + 1].endInstruction;

        for (int i = start + skip; i < end; i++)
            mn.instructions.get(i).accept(mv);
    }

    private int emitCodeBlockAfterIdx(MethodVisitor mv, int idx, int skip, int currLine) {
        int start = codeBlocks[idx].endInstruction;
        int end = codeBlocks[idx + 1].endInstruction;

        for (int i = start + skip; i < end; i++) {
            final AbstractInsnNode ins = mn.instructions.get(i);

            switch (ins.getOpcode()) {
                case Opcodes.RETURN:
                case Opcodes.ARETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                    emitFiberStackPopMethod(mv);
                    break;

                case Opcodes.MONITORENTER:
                case Opcodes.MONITOREXIT:
                    if (!db.isAllowMonitors()) {
                        if (!className.equals("clojure/lang/LazySeq"))
                            throw new UnableToInstrumentException("synchronization", className, mn.name, mn.desc);
                    } else if (!warnedAboutMonitors) {
                        warnedAboutMonitors = true;
                        db.log(LogLevel.WARNING, "Method %s#%s%s contains synchronization", className, mn.name, mn.desc);
                    }
                    break;

                case Opcodes.INVOKESPECIAL:
                    final MethodInsnNode min = (MethodInsnNode) ins;
                    if ("<init>".equals(min.name)) {
                        int argSize = TypeAnalyzer.getNumArguments(min.desc);
                        final Frame frame = frames[i];
                        int stackIndex = frame.getStackSize() - argSize - 1;
                        final Value thisValue = frame.getStack(stackIndex);
                        if (stackIndex >= 1
                            && isNewValue(thisValue, true)
                            && isNewValue(frame.getStack(stackIndex - 1), false)) {
                            if (isOmitted((NewValue) thisValue))
                                emitNewAndDup(mv, frame, stackIndex, min); // explanation in emitNewAndDup
                        } else {
                            db.log(LogLevel.WARNING, "Expected to find a NewValue on stack index %d: %s", stackIndex, frame);
                        }
                    }
                    break;

                default:
                    if (ins instanceof LineNumberNode)
                        currLine = ((LineNumberNode) ins).line;
            }

            ins.accept(mv);
        }

        return currLine;
    }

    private static void emitConst(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5)
            mv.visitInsn(Opcodes.ICONST_0 + value);
        else if ((byte) value == value)
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        else if ((short) value == value)
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        else
            mv.visitLdcInsn(value);
    }

    /*
    private static void emitConst(MethodVisitor mv, String value) {
        mv.visitLdcInsn(value);
    }
    */

    private void emitNewAndDup(MethodVisitor mv, Frame frame, int stackIndex, MethodInsnNode min) {
        /*
         * This method, and the entire NewValue business has to do with dealing with the following case:
         *
         *   new Foo(suspendableCall())
         *
         * I.e. when the suspendable call is passed as an argument to the constructor. The emitted code may be:
         *
         *   NEW Foo
         *   DUP
         *   INVOKEVIRTUAL suspendableCall
         *   INVOKESPECIAL Foo.<init>
         *
         * Which means that the suspension points is after NEW, leaving the object in an uninitialized state which the verifier rejects.
         * This method rewrites it to be:
         *
         *   INVOKEVIRTUAL suspendableCall
         *   ASTORE X
         *   NEW Foo
         *   DUP
         *   ALOAD X
         *   INVOKESPECIAL Foo.<init>
         *
         */
        int arguments = frame.getStackSize() - stackIndex - 1;
        int neededLocals = 0;
        for (int i = arguments; i >= 1; i--) {
            BasicValue v = (BasicValue) frame.getStack(stackIndex + i);
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), lvarStack + NUM_LOCALS + neededLocals);
            neededLocals += v.getSize();
        }
        if (additionalLocals < neededLocals)
            additionalLocals = neededLocals;

        db.log(LogLevel.DEBUG, "Inserting NEW & DUP for constructor call %s%s with %d arguments (%d locals)", min.owner, min.desc, arguments, neededLocals);
        ((NewValue) frame.getStack(stackIndex - 1)).insn.accept(mv);
        ((NewValue) frame.getStack(stackIndex)).insn.accept(mv);

        for (int i = 1; i <= arguments; i++) {
            BasicValue v = (BasicValue) frame.getStack(stackIndex + i);
            neededLocals -= v.getSize();
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), lvarStack + NUM_LOCALS + neededLocals);
        }
    }

    private void emitFiberStackPopMethod(MethodVisitor mv) {
//        emitVerifyInstrumentation(mv);

        final Label lbl = new Label();
        // DUAL
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitJumpInsn(Opcodes.IFNULL, lbl);

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "popMethod", "()V", false);

        // DUAL
        mv.visitLabel(lbl);
    }

    private void emitFiberStackStoreState(MethodVisitor mv, int idx, FrameInfo fi, int numArgsToPutBackToOperandStackAfterStore) {
        if (idx > Stack.MAX_ENTRY)
            throw new IllegalArgumentException("Entry index (PC) " + idx + " greater than maximum of " + Stack.MAX_ENTRY + " in " + className + "." + mn.name + mn.desc);
        if (fi.numSlots > Stack.MAX_SLOTS)
            throw new IllegalArgumentException("Number of slots required " + fi.numSlots + " greater than maximum of " + Stack.MAX_SLOTS + " in " + className + "." + mn.name + mn.desc);

        final Frame f = frames[fi.endInstruction];

        if (fi.lBefore != null)
            fi.lBefore.accept(mv);

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);
        emitConst(mv, fi.numSlots);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "pushMethod", "(II)V", false);

        // store operand stack
        for (int i = f.getStackSize(); i-- > 0;) {
            final BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitStoreValue(mv, v, lvarStack, slotIdx);
                } else {
                    db.log(LogLevel.DEBUG, "NULL stack entry: type=%s size=%d", v.getType(), v.getSize());
                    mv.visitInsn(Opcodes.POP);
                }
            }
        }

        // store local vars
        for (int i = firstLocal; i < f.getLocals(); i++) {
            final BasicValue v = (BasicValue) f.getLocal(i);
            if (!isNullType(v)) {
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), i);
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitStoreValue(mv, v, lvarStack, slotIdx);
            }
        }

        // restore last numArgsToPutBackToOperandStackAfterStore operands
        for (int i = f.getStackSize() - numArgsToPutBackToOperandStackAfterStore; i < f.getStackSize(); i++) {
            final BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx);
                } else
                    mv.visitInsn(Opcodes.ACONST_NULL);
            }
        }
    }

    private void addSuspendableCallSite(Frame f, int idx, int entry, int currSourceLine, String desc) {
        final List<Type> operandTypes = new ArrayList<>();
        for (int i = f.getStackSize(); i-- > 0;) {
            final BasicValue v = (BasicValue) f.getStack(i);
            if (v != null && v.getType() != null)
                operandTypes.add(v.getType());
        }
        final List<Type>  localTypes = new ArrayList<>();
        for (int i = firstLocal; i < f.getLocals(); i++) {
            final BasicValue v = (BasicValue) f.getLocal(i);
            if (v != null && v.getType() != null)
                localTypes.add(v.getType());
        }
        suspCallSites.add(new SuspCallSite(idx, desc, entry, currSourceLine, operandTypes, localTypes));
    }

    private void emitFiberStackRestoreState(MethodVisitor mv, FrameInfo fi, int numArgsThatHaveBeenPutBackToOperandStackAfterStore) {
        final Frame f = frames[fi.endInstruction];

        // restore local vars
        for (int i = firstLocal; i < f.getLocals(); i++) {
            final BasicValue v = (BasicValue) f.getLocal(i);
            if (!isNullType(v)) {
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitRestoreValue(mv, v, lvarStack, slotIdx);
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), i);
            } else if (v != BasicValue.UNINITIALIZED_VALUE) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitVarInsn(Opcodes.ASTORE, i);
            }
        }

        // restore operand stack
        for (int i = 0; i < f.getStackSize() - numArgsThatHaveBeenPutBackToOperandStackAfterStore; i++) {
            final BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx);
                } else
                    mv.visitInsn(Opcodes.ACONST_NULL);
            }
        }

        if (fi.lAfter != null)
            fi.lAfter.accept(mv);
    }

    private void emitFiberStackPostRestore(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "postRestore", "()V", false);
    }

    /*
    private void emitPreemptionPoint(MethodVisitor mv, int type) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        switch (type) {
            case 0:
                mv.visitInsn(Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(Opcodes.ICONST_3);
                break;
            default:
                throw new AssertionError("Unsupported type: " + type);
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "preemptionPoint", "(I)V", false);
    }
    */

    private void emitStoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx) throws InternalError, IndexOutOfBoundsException {
        String desc;

        switch (v.getType().getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                desc = "(Ljava/lang/Object;L" + STACK_NAME + ";I)V";
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
                desc = "(IL" + STACK_NAME + ";I)V";
                break;
            case Type.FLOAT:
                desc = "(FL" + STACK_NAME + ";I)V";
                break;
            case Type.LONG:
                desc = "(JL" + STACK_NAME + ";I)V";
                break;
            case Type.DOUBLE:
                desc = "(DL" + STACK_NAME + ";I)V";
                break;
            default:
                throw new InternalError("Unexpected type: " + v.getType());
        }

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
//        if (v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY)
//            println(mv, "STORE " + (lvar >= 0 ? ("VAR " + lvar + ": ") : "OPRND: "));
        emitConst(mv, idx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "push", desc, false);
    }

    private void emitRestoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);

        switch (v.getType().getSort()) {
            case Type.OBJECT:
                final String internalName = v.getType().getInternalName();
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject", "(I)Ljava/lang/Object;", false);
                if (!internalName.equals("java/lang/Object"))  // don't cast to Object ;)
                    mv.visitTypeInsn(Opcodes.CHECKCAST, internalName);
//                println(mv, "RESTORE " + (lvar >= 0 ? ("VAR " + lvar + ": ") : "OPRND: "));
                break;
            case Type.ARRAY:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject", "(I)Ljava/lang/Object;", false);
                mv.visitTypeInsn(Opcodes.CHECKCAST, v.getType().getDescriptor());
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2B);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2S);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2C);
                break;
            case Type.BOOLEAN:
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getFloat", "(I)F", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getLong", "(I)J", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getDouble", "(I)D", false);
                break;
            default:
                throw new InternalError("Unexpected type: " + v.getType());
        }
    }

    static boolean isNullType(BasicValue v) {
        return (v == BasicValue.UNINITIALIZED_VALUE)
            || (v.isReference() && v.getType().getInternalName().equals("null"));
    }

    static boolean isOmitted(BasicValue v) {
        return v instanceof NewValue && ((NewValue) v).omitted;
    }

    static boolean isNewValue(Value v, boolean dupped) {
        return v instanceof NewValue && ((NewValue) v).isDupped == dupped;
    }

    private static String getMethodOwner(AbstractInsnNode min) {
        return min instanceof MethodInsnNode ? ((MethodInsnNode) min).owner : null;
    }

    private static String getMethodName(AbstractInsnNode min) {
        return min instanceof MethodInsnNode ? ((MethodInsnNode) min).name
            : min instanceof InvokeDynamicInsnNode ? ((InvokeDynamicInsnNode) min).name
            : null;
    }

    private static String getMethodDesc(AbstractInsnNode min) {
        return min instanceof MethodInsnNode ? ((MethodInsnNode) min).desc
            : min instanceof InvokeDynamicInsnNode ? ((InvokeDynamicInsnNode) min).desc
            : null;
    }

    private static class OmittedInstruction extends AbstractInsnNode {
        private final AbstractInsnNode orgInsn;

        public OmittedInstruction(AbstractInsnNode orgInsn) {
            super(orgInsn.getOpcode());
            this.orgInsn = orgInsn;
        }

        @Override
        public int getType() {
            return orgInsn.getType();
        }

        @Override
        public void accept(MethodVisitor cv) {
        }

        @Override
        public AbstractInsnNode clone(Map labels) {
            return new OmittedInstruction(orgInsn.clone(labels));
        }
    }

    static class BlockLabelNode extends LabelNode {
        final int idx;

        BlockLabelNode(int idx) {
            this.idx = idx;
        }
    }

    // prints a local var
    /*
    private void println(MethodVisitor mv, String prefix, int refVar) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(" " + prefix);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(" var " + refVar + ":");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);

        mv.visitVarInsn(Opcodes.ALOAD, refVar);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
    */

    // prints the value at the top of the operand stack
    /*
    private void println(MethodVisitor mv, String prefix) {
        mv.visitInsn(Opcodes.DUP); // S1 S1

        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;"); // PrintStream S1 S1

        mv.visitInsn(Opcodes.SWAP); // S1 PrintStream S1

        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder"); // StringBuilder S1 PrintStream S1
        mv.visitInsn(Opcodes.DUP); // StringBuilder StringBuilder S1 PrintStream S1
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false); // StringBuilder S1 PrintStream S1
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn(" " + prefix); // prefix StringBuilder S1 PrintStream S1
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false); // StringBuilder S1 PrintStream S1

        mv.visitInsn(Opcodes.SWAP); // S1 StringBuilder PrintStream S1

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false); // StringBuilder PrintStream S1
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false); // PrintStream S1
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false); // S1
    }
    */
}

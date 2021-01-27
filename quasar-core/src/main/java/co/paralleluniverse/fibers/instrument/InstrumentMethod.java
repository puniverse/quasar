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

import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;

import static co.paralleluniverse.fibers.instrument.Classes.INSTRUMENTED_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.LAMBDA_METHOD_PREFIX;
import static co.paralleluniverse.fibers.instrument.Classes.STACK_OPS_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.THROWABLE_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.RUNTIME_EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.SUSPEND_EXECUTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.RUNTIME_SUSPEND_EXECUTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.UNDECLARED_THROWABLE_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.isAllowedToBlock;
import static co.paralleluniverse.fibers.instrument.Classes.blockingCallIdx;
import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isInvocationHandlerInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isMethodHandleInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isReflectInvocation;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.isSyntheticAccess;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
class InstrumentMethod {
    private static final boolean optimizationDisabled = false; // SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.disableInstrumentationOptimization");
    private static final boolean HANDLE_PROXY_INVOCATIONS = true;

    // private final boolean verifyInstrumentation; //
    // private static final int PREEMPTION_BACKBRANCH = 0;
    // private static final int PREEMPTION_CALL = 1;
    private static final int NUM_LOCALS = 3; // = 3 + (verifyInstrumentation ? 1 : 0); // lvarStack, lvarResumed, lvarInvocationReturnValue
    private static final int ADD_OPERANDS = 6; // 4;

    // private static final String INTERRUPTED_EXCEPTION_NAME = Type.getInternalName(InterruptedException.class);
    // private static final boolean DUAL = true; // true if suspendable methods can be called from regular threads in addition to fibers
    private final MethodDatabase db;

    private final String sourceName;
    private final String className;

    private final MethodNode mn;
    private final Frame<?>[] frames;

    private final int lvarStack; // ref to Stack
    private final int lvarResumed; // boolean indicating if we've been resumed
    private final int lvarInvocationReturnValue;
    // private final int lvarSuspendableCalled; // true iff we've called another suspendable method (used when VERIFY_INSTRUMENTATION)

    private final int firstLocal;

    private FrameInfo[] codeBlocks = new FrameInfo[32];
    private int numCodeBlocks;

    private int additionalLocals;
    private int maxRefSlots;

    private boolean warnedAboutMonitors;
    private int warnedAboutBlocking;

    private boolean callsSuspendableSupers;

    private int startSourceLine = -1;
    private int endSourceLine = -1;
    private int[] suspCallsSourceLines = new int[8];
    private String[] suspCallsNames = new String[0];
    private int[] suspCallsBcis = null;

    InstrumentMethod(MethodDatabase db, String sourceName, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.sourceName = sourceName;
        this.className = className;
        this.mn = mn;

        try {
            Analyzer<?> a = new TypeAnalyzer(db);
            this.frames = a.analyze(className, mn);
            this.lvarStack = mn.maxLocals;
            this.lvarResumed = mn.maxLocals + 1;
            this.lvarInvocationReturnValue = mn.maxLocals + 2;
            // this.lvarSuspendableCalled = (verifyInstrumentation ? mn.maxLocals + 3 : -1);
            this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
            this.maxRefSlots = 0;
        } catch (UnsupportedOperationException ex) {
            throw new AnalyzerException(null, ex.getMessage(), ex);
        }
    }

    private void collectCallsites() {
        if (suspCallsBcis == null) {
            suspCallsBcis = new int[8];
            final int numIns = mn.instructions.size();
            int currSourceLine = -1;
            int count = 0;
            final Set<String> callSiteNames = new HashSet<>();
            for (int i = 0; i < numIns; i++) {
                final Frame<?> f = frames[i];
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
                        if (isSuspendableCall(db, in)) {
                            if (count >= suspCallsBcis.length)
                                suspCallsBcis = Arrays.copyOf(suspCallsBcis, suspCallsBcis.length * 2);
                            if (count >= suspCallsSourceLines.length)
                                suspCallsSourceLines = Arrays.copyOf(suspCallsSourceLines, suspCallsSourceLines.length * 2);
                            suspCallsBcis[count] = i;
                            suspCallsSourceLines[count] = currSourceLine;
                            callSiteNames.add(getSuspendableCallName(in));
                            count++;
                        } else
                            possiblyWarnAboutBlocking(in);
                    }
                }
            }

            if (count < suspCallsSourceLines.length)
                suspCallsSourceLines = Arrays.copyOf(suspCallsSourceLines, count);
            if (count < suspCallsBcis.length)
                suspCallsBcis = Arrays.copyOf(suspCallsBcis, count);
            suspCallsNames = callSiteNames.toArray(new String[0]);
        }
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

    static boolean isSuspendableCall(MethodDatabase db, int type, int opcode, String owner, String name, String desc) {
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

    private static String getSuspendableCallName(AbstractInsnNode in) {
        return (in.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN ? "-" : getMethodOwner1(in))
               + "." + getMethodName(in) + getMethodDesc(in);
    }

    private void collectCodeBlocks() {
        final int numIns = mn.instructions.size();

        codeBlocks[0] = FrameInfo.FIRST;
        for (int i = 0; i < numIns; i++) {
            final Frame<?> f = frames[i];
            if (f != null) { // reachable ?
                final AbstractInsnNode in = mn.instructions.get(i);
                if (in.getType() == AbstractInsnNode.METHOD_INSN || in.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
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
                            if (st == SuspendableType.NON_SUSPENDABLE) {
                                susp = false;
                            } else if (st == null) {
                                db.log(LogLevel.WARNING, "Method not found in class - assuming suspendable: %s#%s%s (at %s:%s#%s)", min.owner, min.name, min.desc, sourceName, className, mn.name);
                                susp = true;
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
                        FrameInfo fi = addCodeBlock(f, i);
                        splitTryCatch(fi);
                    } else if (in.getType() == AbstractInsnNode.METHOD_INSN) {// not invokedynamic
                        //noinspection ConstantConditions
                        final MethodInsnNode min = (MethodInsnNode) in;
                        db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is not suspendable", i, min.owner, min.name, min.desc);
                        possiblyWarnAboutBlocking(min);
                    }
                }
            }
        }
        addCodeBlock(null, numIns);
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
                    db.log(LogLevel.WARNING, "Method %s:%s#%s%s contains potentially blocking call to " + min.owner + "#" + min.name + min.desc, sourceName, className, mn.name, mn.desc);
                }
            }
        }
    }

    void accept(MethodVisitor mv, boolean hasAnnotation) {
        db.log(LogLevel.INFO, "Instrumenting method %s:%s#%s%s", sourceName, className, mn.name, mn.desc);

        if (mn.name.charAt(0) == '<')
            throw new UnableToInstrumentException("special method", className, mn.name, mn.desc);

        collectCallsites();
        final boolean skipInstrumentation = canInstrumentationBeSkipped(suspCallsBcis);
        emitInstrumentedAnn(db, mv, mn, sourceName, className, skipInstrumentation,
                startSourceLine, endSourceLine, suspCallsSourceLines, suspCallsNames, null);

        if (skipInstrumentation) {
            db.log(LogLevel.INFO, "[OPTIMIZE] Skipping instrumentation for method %s:%s#%s%s", sourceName, className, mn.name, mn.desc);
            mn.accept(mv); // Dump
            return;
        }

        // Else instrument
        collectCodeBlocks(); // Must be called first, sets flags & state used below

        final boolean handleProxyInvocations = HANDLE_PROXY_INVOCATIONS && callsSuspendableSupers;

        mv.visitCode();

        Label lMethodStart = new Label();
        Label lMethodStart2 = new Label();
        Label lMethodEnd = new Label();
        Label lCatchSEE = new Label();
        Label lCatchUTE = new Label();
        Label lCatchAll = new Label();
        Label[] lMethodCalls = new Label[numCodeBlocks - 1];
        Label[][] refInvokeTryCatch;

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
        // Note that the InvocationTargetException will be regenrated on every park, adding further overhead on top of the reflective call.
        // This must be done here, before all other visitTryCatchBlock, because the exception's handler
        // will be matched according to the order of in which visitTryCatchBlock has been called. Earlier calls take precedence.
        refInvokeTryCatch = new Label[numCodeBlocks - 1][];
        for (int i = 1; i < numCodeBlocks; i++) {
            final FrameInfo fi = codeBlocks[i];
            final AbstractInsnNode in = mn.instructions.get(fi.endInstruction);
            if (mn.instructions.get(fi.endInstruction) instanceof MethodInsnNode) {
                MethodInsnNode min = (MethodInsnNode) in;
                if (isReflectInvocation(min.owner, min.name)) {
                    Label[] ls = new Label[3];
                    for (int k = 0; k < 3; k++)
                        ls[k] = new Label();
                    refInvokeTryCatch[i - 1] = ls;
                    mv.visitTryCatchBlock(ls[0], ls[1], ls[2], "java/lang/reflect/InvocationTargetException");
                }
            }
        }

        // Output try-catch blocks
        for (final Object o : mn.tryCatchBlocks) {
            final TryCatchBlockNode tcb = (TryCatchBlockNode) o;

            if (SUSPEND_EXECUTION_NAME.equals(tcb.type) && !hasAnnotation && !mn.name.startsWith(LAMBDA_METHOD_PREFIX)) // we allow catch of SuspendExecution only in methods annotated with @Suspendable and in lambda-generated ones.
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
            for (Object o : mn.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }

        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchAll, null);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_OPS_NAME, "getStack", "()L" + STACK_OPS_NAME + ";", false);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        // println(mv, "STACK: ", lvarStack);
        // dumpStack(mv);
        // DUAL
        mv.visitJumpInsn(Opcodes.IFNULL, lMethodStart);
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);

        emitStoreResumed(mv, true); // we'll assume we have been resumed

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "nextMethodEntry", "()I", false);
        mv.visitTableSwitchInsn(1, numCodeBlocks - 1, lMethodStart2, lMethodCalls);

        mv.visitLabel(lMethodStart2);

        // the following code handles the case of an instrumented method called not as part of a suspendable code path
        // isFirstInStack will return false in that case.
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "isFirstInStackOrPushed", "()Z", false);
        mv.visitJumpInsn(Opcodes.IFNE, lMethodStart); // if true

        // This will reset the fiber stack local if isFirstStack returns false.
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        mv.visitLabel(lMethodStart);

        emitStoreResumed(mv, false); // no, we have not been resumed

        dumpCodeBlock(mv, 0, 0);

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

                emitStoreState(mv, i, fi, numYieldArgs); // we preserve the arguments for the call to yield on the operand stack
                emitStoreResumed(mv, false); // we have not been resumed
                // emitSuspendableCalled(mv);

                min.accept(mv);                              // we call the yield method
                if (yieldReturnsValue)
                    mv.visitInsn(Opcodes.POP);               // we ignore the returned value...
                mv.visitLabel(lMethodCalls[i - 1]);          // we resume AFTER the call

                final Label afterPostRestore = new Label();
                mv.visitVarInsn(Opcodes.ILOAD, lvarResumed);
                mv.visitJumpInsn(Opcodes.IFEQ, afterPostRestore);
                emitPostRestore(mv);
                mv.visitLabel(afterPostRestore);

                emitRestoreState(mv, i, fi, numYieldArgs);
                if (yieldReturnsValue)
                    mv.visitVarInsn(Opcodes.ILOAD, lvarResumed); // ... and replace the returned value with the value of resumed

                // See #211: if Fiber.park() is the last call before catch, ASM generates
                // empty handlers (start_pc = end_pc) that won't pass ASM's nor JVM's bytecode checker because of
                // exception_table's spec here: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.7.3
                mv.visitInsn(Opcodes.NOP);

                dumpCodeBlock(mv, i, 1 /* skip the call */);
            } else {
                final Label lbl = new Label();

                // DUAL
                mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
                mv.visitJumpInsn(Opcodes.IFNULL, lbl);

                // normal case - call to a suspendable method - resume before the call
                emitStoreState(mv, i, fi, 0);
                emitStoreResumed(mv, false); // we have not been resumed
                // emitPreemptionPoint(mv, PREEMPTION_CALL);

                mv.visitLabel(lMethodCalls[i - 1]);
                emitRestoreState(mv, i, fi, 0);

                // DUAL
                mv.visitLabel(lbl);

                if (isReflectInvocation(owner, name)) {
                    // We catch the InvocationTargetException and unwrap it if it wraps a SuspendExecution exception.
                    Label[] ls = refInvokeTryCatch[i - 1];
                    final Label startTry = ls[0];
                    final Label endTry = ls[1];
                    final Label startCatch = ls[2];
                    final Label endCatch = new Label();
                    final Label notSuspendExecution = new Label();

                    // mv.visitTryCatchBlock(startTry, endTry, startCatch, "java/lang/reflect/InvocationTargetException");
                    mv.visitLabel(startTry);   // try {
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
                    dumpCodeBlock(mv, i, 1 /* skip the call */);
                } else {
                    // emitSuspendableCalled(mv);
                    dumpCodeBlock(mv, i, 0);
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
        emitPopMethod(mv);
        mv.visitLabel(lCatchSEE);

        // println(mv, "THROW: ");
        mv.visitInsn(Opcodes.ATHROW);   // rethrow shared between catchAll and catchSSE

        if (mn.localVariables != null) {
            for (Object o : mn.localVariables)
                ((LocalVariableNode) o).accept(mv);
        }
        mv.visitMaxs(mn.maxStack + ADD_OPERANDS, mn.maxLocals + NUM_LOCALS + additionalLocals); // Needed by ASM analysis
        mv.visitEnd();
    }

    private boolean canInstrumentationBeSkipped(int[] susCallsIndexes) {
        if (susCallsIndexes.length == 0) {
            db.log(LogLevel.INFO, "No callsites to instrument in method %s#%s%s", className, mn.name, mn.desc);
            return true;
        }

        if (optimizationDisabled) {
            db.log(LogLevel.DEBUG, "[OPTIMIZE] Optimization disabled, not examining method %s:%s#%s%s with susCallsIndexes=%s", sourceName, className, mn.name, mn.desc, Arrays.toString(susCallsIndexes));
            return false;
        }

        db.log(LogLevel.DEBUG, "[OPTIMIZE] Examining method %s:%s#%s%s with susCallsIndexes=%s", sourceName, className, mn.name, mn.desc, Arrays.toString(susCallsIndexes));
        return isForwardingToSuspendable(susCallsIndexes); // Fully instrumentation-transparent methods
    }

    private boolean isForwardingToSuspendable(int[] susCallsBcis) {
        if (susCallsBcis.length != 1)
            return false; // we allow exactly one suspendable call

        final int susCallBci = susCallsBcis[0];

        final AbstractInsnNode susCall = mn.instructions.get(susCallBci);
        assert isSuspendableCall(db, susCall);
        if (isYieldMethod(getMethodOwner(susCall), getMethodName(susCall)))
            return false; // yield calls require instrumentation (to skip the call when resuming)
        if (isReflectInvocation(getMethodOwner(susCall), getMethodName(susCall)))
            return false; // Reflective calls require instrumentation to handle SuspendExecution wrapped in InvocationTargetException
        if (hasSuspendableTryCatchBlocksAround(susCallBci))
            return false; // Catching `SuspendableExecution needs instrumentation in order to propagate it

        // before suspendable call:
        for (int i = 0; i < susCallBci; i++) {
            final AbstractInsnNode ins = mn.instructions.get(i);

            if (ins.getType() == AbstractInsnNode.METHOD_INSN || ins.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN)
                return false; // methods calls might have side effects
            if (ins.getType() == AbstractInsnNode.FIELD_INSN)
                return false; // side effects
            if (ins instanceof JumpInsnNode && mn.instructions.indexOf(((JumpInsnNode) ins).label) <= i)
                return false; // back branches may be costly, so we'd rather capture state
            if (!db.isAllowMonitors() && (ins.getOpcode() == Opcodes.MONITORENTER || ins.getOpcode() == Opcodes.MONITOREXIT))
                return false;  // we need collectCodeBlocks to warn about monitors
        }

        // after suspendable call
        for (int i = susCallBci + 1; i <= mn.instructions.size() - 1; i++) {
            final AbstractInsnNode ins = mn.instructions.get(i);

            if (ins instanceof JumpInsnNode && mn.instructions.indexOf(((JumpInsnNode) ins).label) <= susCallBci)
                return false; // if we jump before the suspendable call we suspend more than once -- need instrumentation
            if (!db.isAllowMonitors() && (ins.getOpcode() == Opcodes.MONITORENTER || ins.getOpcode() == Opcodes.MONITOREXIT))
                return false;  // we need collectCodeBlocks to warn about monitors
            if (!db.isAllowBlocking() && (ins instanceof MethodInsnNode && blockingCallIdx((MethodInsnNode) ins) != -1))
                return false;  // we need collectCodeBlocks to warn about blocking calls
        }
        return true;
    }

    private boolean hasSuspendableTryCatchBlocksAround(int bci) {
        for (final TryCatchBlockNode tcb : mn.tryCatchBlocks) {
            if (mn.instructions.indexOf(tcb.start) <= bci && mn.instructions.indexOf(tcb.end) >= bci
                && (THROWABLE_NAME.equals(tcb.type)
                    || EXCEPTION_NAME.equals(tcb.type)
                    || RUNTIME_EXCEPTION_NAME.equals(tcb.type)
                    || RUNTIME_SUSPEND_EXECUTION_NAME.equals(tcb.type)
                    || SUSPEND_EXECUTION_NAME.equals(tcb.type)))
                return true;
        }
        return false;
    }

    static void emitInstrumentedAnn(
            MethodDatabase db, MethodVisitor mv, MethodNode mn, String sourceName, String className,
            boolean skip, int startSourceLine, int endSourceLine,
            int[] suspCallsSourceLines, String[] instrumentedCalls, int[] postInstrOffsets) {

        final StringBuilder sb = db.isDebug() ? new StringBuilder() : null;
        final AnnotationVisitor instrumentedAV = mv.visitAnnotation(INSTRUMENTED_DESC, true);
        if (sb != null)
            sb.append("@Instrumented(");

        instrumentedAV.visit(Constants.FIELD_NAME_METHOD_OPTIMIZED, skip);
        if (sb != null)
            sb.append(Constants.FIELD_NAME_METHOD_OPTIMIZED + '=').append(skip).append(", ");

        instrumentedAV.visit(Constants.FIELD_NAME_METHOD_START, startSourceLine);
        if (sb != null)
            sb.append(Constants.FIELD_NAME_METHOD_START + '=').append(startSourceLine).append(", ");

        instrumentedAV.visit(Constants.FIELD_NAME_METHOD_END, endSourceLine);
        if (sb != null)
            sb.append(Constants.FIELD_NAME_METHOD_END + '=').append(endSourceLine).append(", ");

        if (suspCallsSourceLines != null) {
            final AnnotationVisitor linesAV = instrumentedAV.visitArray(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES);
            if (sb != null)
                sb.append(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES + "=[");
            for (int i = 0; i < suspCallsSourceLines.length; i++) {
                if (sb != null && i != 0)
                    sb.append(", ");

                final int l = suspCallsSourceLines[i];
                linesAV.visit(null, l);
                if (sb != null)
                    sb.append(l);
            }
            linesAV.visitEnd();
            if (sb != null)
                sb.append("], ");
        }

        if (instrumentedCalls != null) {
            final AnnotationVisitor instrumentedCallsAV = instrumentedAV.visitArray(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITE_NAMES);
            if (sb != null)
                sb.append(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITE_NAMES + "=[");
            for (int i = 0; i < instrumentedCalls.length; i++) {
                if (sb != null && i != 0)
                    sb.append(", ");

                final String s = instrumentedCalls[i];
                instrumentedCallsAV.visit(null, s);
                if (sb != null)
                    sb.append(s);
            }
            instrumentedCallsAV.visitEnd();
            if (sb != null)
                sb.append("], ");
        }

        if (postInstrOffsets != null) {
            final AnnotationVisitor postInstrOffsetsAV = instrumentedAV.visitArray(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES_OFFSETS_AFTER_INSTR);
            if (sb != null)
                sb.append(Constants.FIELD_NAME_SUSPENDABLE_CALL_SITES_OFFSETS_AFTER_INSTR + "=[");
            for (int i = 0; i < postInstrOffsets.length; i++) {
                if (sb != null && i != 0)
                    sb.append(", ");

                final int l = postInstrOffsets[i];
                postInstrOffsetsAV.visit(null, l);
                if (sb != null)
                    sb.append(l);
            }
            postInstrOffsetsAV.visitEnd();
            if (sb != null)
                sb.append(']');
        }

        instrumentedAV.visitEnd();
        if (sb != null)
            sb.append(')');

        db.log(LogLevel.DEBUG, "Annotating method %s:%s#%s%s with %s", sourceName, className, mn.name, mn.desc, sb);
    }

    /*
    private void dumpStack(MethodVisitor mv) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V", false);
    }
     */
    private FrameInfo addCodeBlock(Frame<?> f, int end) {
        if (++numCodeBlocks == codeBlocks.length) {
            FrameInfo[] newArray = new FrameInfo[numCodeBlocks * 2];
            System.arraycopy(codeBlocks, 0, newArray, 0, codeBlocks.length);
            codeBlocks = newArray;
        }
        FrameInfo fi = new FrameInfo(f, firstLocal, end, mn.instructions, db);
        codeBlocks[numCodeBlocks] = fi;
        this.maxRefSlots = Math.max(maxRefSlots, fi.numObjSlots);
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

    private void splitTryCatch(FrameInfo fi) {
        for (int i = 0; i < mn.tryCatchBlocks.size(); i++) {
            TryCatchBlockNode tcb = mn.tryCatchBlocks.get(i);

            int start = getLabelIdx(tcb.start);
            int end = getLabelIdx(tcb.end);

            if (start <= fi.endInstruction && end >= fi.endInstruction) {
                db.log(LogLevel.DEBUG, "Splitting try-catch in %s, block %d call at instruction %d", mn.name, i, fi.endInstruction);
                //System.out.println("i="+i+" start="+start+" end="+end+" split="+splitIdx+
                //        " start="+mn.instructions.get(start)+" end="+mn.instructions.get(end));

                // need to split try/catch around the suspendable call
                if (start == fi.endInstruction) {
                    // Starts exactly at the suspendable call => just make it start after it
                    tcb.start = fi.createAfterLabel();
                } else {
                    if (end > fi.endInstruction) {
                        TryCatchBlockNode tcb2 = new TryCatchBlockNode(
                                fi.createAfterLabel(),
                                tcb.end, tcb.handler, tcb.type);
                        mn.tryCatchBlocks.add(i + 1, tcb2);
                    }

                    // Make it end before the suspendable call
                    tcb.end = fi.createBeforeLabel();
                }
            }
        }
    }

    private void dumpCodeBlock(MethodVisitor mv, int idx, int skip) {
        int start = codeBlocks[idx].endInstruction;
        int end = codeBlocks[idx + 1].endInstruction;

        for (int i = start + skip; i < end; i++) {
            AbstractInsnNode ins = mn.instructions.get(i);

            switch (ins.getOpcode()) {
                case Opcodes.RETURN:
                case Opcodes.ARETURN:
                case Opcodes.IRETURN:
                case Opcodes.LRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                    emitPopMethod(mv);
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
                    MethodInsnNode min = (MethodInsnNode) ins;
                    if ("<init>".equals(min.name)) {
                        int argSize = TypeAnalyzer.getNumArguments(min.desc);
                        Frame<?> frame = frames[i];
                        int stackIndex = frame.getStackSize() - argSize - 1;
                        Value thisValue = frame.getStack(stackIndex);
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
            }

            ins.accept(mv);
        }
    }

    private static void dumpParameterAnnotations(MethodVisitor mv, List<?>[] parameterAnnotations, boolean visible) {
        for (int i = 0; i < parameterAnnotations.length; i++) {
            if (parameterAnnotations[i] != null) {
                for (Object o : parameterAnnotations[i]) {
                    AnnotationNode an = (AnnotationNode) o;
                    an.accept(mv.visitParameterAnnotation(i, an.desc, visible));
                }
            }
        }
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
    private void emitNewAndDup(MethodVisitor mv, Frame<?> frame, int stackIndex, MethodInsnNode min) {
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

    private void emitPopMethod(MethodVisitor mv) {
//        emitVerifyInstrumentation(mv);

        final Label lbl = new Label();
        // DUAL
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitJumpInsn(Opcodes.IFNULL, lbl);

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, maxRefSlots);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "popMethod", "(I)V", false);

        // DUAL
        mv.visitLabel(lbl);
    }

    private void emitStoreState(MethodVisitor mv, int idx, FrameInfo fi, int numArgsToPreserve) {
        if (idx > Constants.STACK_MAX_ENTRY)
            throw new IllegalArgumentException("Entry index (PC) " + idx + " greater than maximum of " + Constants.STACK_MAX_ENTRY + " in " + className + "." + mn.name + mn.desc);
        if (fi.numSlots > Constants.STACK_MAX_SLOTS)
            throw new IllegalArgumentException("Number of slots required " + fi.numSlots + " greater than maximum of " + Constants.STACK_MAX_SLOTS + " in " + className + "." + mn.name + mn.desc);

        Frame<?> f = frames[fi.endInstruction];

        if (fi.lBefore != null)
            fi.lBefore.accept(mv);

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);
        emitConst(mv, fi.numSlots);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "pushMethod", "(II)V", false);

        // store operand stack
        for (int i = f.getStackSize(); i-- > 0;) {
            BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitStoreValue(mv, v, lvarStack, slotIdx, -1);
                } else {
                    db.log(LogLevel.DEBUG, "NULL stack entry: type=%s size=%d", v.getType(), v.getSize());
                    mv.visitInsn(Opcodes.POP);
                }
            }
        }

        // store local vars
        for (int i = firstLocal; i < f.getLocals(); i++) {
            BasicValue v = (BasicValue) f.getLocal(i);
            if (!isNullType(v)) {
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), i);
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitStoreValue(mv, v, lvarStack, slotIdx, i);
            }
        }

        // restore last numArgsToPreserve operands
        for (int i = f.getStackSize() - numArgsToPreserve; i < f.getStackSize(); i++) {
            BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx, -1);
                } else
                    mv.visitInsn(Opcodes.ACONST_NULL);
            }
        }
    }

    private void emitRestoreState(MethodVisitor mv, @SuppressWarnings("UnusedParameters") int idx, FrameInfo fi, int numArgsPreserved) {
        Frame<?> f = frames[fi.endInstruction];

        // restore local vars
        for (int i = firstLocal; i < f.getLocals(); i++) {
            BasicValue v = (BasicValue) f.getLocal(i);
            if (!isNullType(v)) {
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitRestoreValue(mv, v, lvarStack, slotIdx, i);
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), i);
            } else if (v != BasicValue.UNINITIALIZED_VALUE) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitVarInsn(Opcodes.ASTORE, i);
            }
        }

        // restore operand stack
        for (int i = 0; i < f.getStackSize() - numArgsPreserved; i++) {
            BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx, -1);
                } else
                    mv.visitInsn(Opcodes.ACONST_NULL);
            }
        }

        if (fi.lAfter != null)
            fi.lAfter.accept(mv);
    }

    private void emitPostRestore(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "postRestore", "()V", false);
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
    private void emitStoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx, @SuppressWarnings("UnusedParameters") int lvar) throws InternalError, IndexOutOfBoundsException {
        String desc;

        switch (v.getType().getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                desc = "(Ljava/lang/Object;L" + STACK_OPS_NAME + ";I)V";
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.SHORT:
            case Type.CHAR:
            case Type.INT:
                desc = "(IL" + STACK_OPS_NAME + ";I)V";
                break;
            case Type.FLOAT:
                desc = "(FL" + STACK_OPS_NAME + ";I)V";
                break;
            case Type.LONG:
                desc = "(JL" + STACK_OPS_NAME + ";I)V";
                break;
            case Type.DOUBLE:
                desc = "(DL" + STACK_OPS_NAME + ";I)V";
                break;
            default:
                throw new InternalError("Unexpected type: " + v.getType());
        }

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
//        if (v.getType().getSort() == Type.OBJECT || v.getType().getSort() == Type.ARRAY)
//            println(mv, "STORE " + (lvar >= 0 ? ("VAR " + lvar + ": ") : "OPRND: "));
        emitConst(mv, idx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_OPS_NAME, "push", desc, false);
    }

    private void emitRestoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx, @SuppressWarnings("UnusedParameters") int lvar) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);

        switch (v.getType().getSort()) {
            case Type.OBJECT:
                String internalName = v.getType().getInternalName();
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getObject", "(I)Ljava/lang/Object;", false);
                if (!internalName.equals("java/lang/Object"))  // don't cast to Object ;)
                    mv.visitTypeInsn(Opcodes.CHECKCAST, internalName);
//                println(mv, "RESTORE " + (lvar >= 0 ? ("VAR " + lvar + ": ") : "OPRND: "));
                break;
            case Type.ARRAY:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getObject", "(I)Ljava/lang/Object;", false);
                mv.visitTypeInsn(Opcodes.CHECKCAST, v.getType().getDescriptor());
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2B);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2S);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getInt", "(I)I", false);
                mv.visitInsn(Opcodes.I2C);
                break;
            case Type.BOOLEAN:
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getInt", "(I)I", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getFloat", "(I)F", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getLong", "(I)J", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_OPS_NAME, "getDouble", "(I)D", false);
                break;
            default:
                throw new InternalError("Unexpected type: " + v.getType());
        }
    }

    private static boolean isNullType(BasicValue v) {
        return (v == BasicValue.UNINITIALIZED_VALUE)
               || (v.isReference() && v.getType().getInternalName().equals("null"));
    }

    private static boolean isOmitted(BasicValue v) {
        return v instanceof NewValue && ((NewValue) v).omitted;
    }

    private static boolean isNewValue(Value v, boolean dupped) {
        return v instanceof NewValue && ((NewValue) v).isDupped == dupped;
    }

    private static String getMethodOwner(AbstractInsnNode min) {
        return min instanceof MethodInsnNode ? ((MethodInsnNode) min).owner : null;
    }

    private static String getMethodOwner1(AbstractInsnNode min) {
        return min instanceof MethodInsnNode ? ((MethodInsnNode) min).owner
                : min instanceof InvokeDynamicInsnNode ? ((InvokeDynamicInsnNode) min).bsm.getOwner()
                        : null;
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

        OmittedInstruction(AbstractInsnNode orgInsn) {
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

    private static class BlockLabelNode extends LabelNode {
        final int idx;

        BlockLabelNode(int idx) {
            this.idx = idx;
        }
    }

    private static class FrameInfo {
        static final FrameInfo FIRST = new FrameInfo(null, 0, 0, null, null);
        final int endInstruction;
        final int numSlots;
        final int numObjSlots;
        final int[] localSlotIndices;
        final int[] stackSlotIndices;
        BlockLabelNode lBefore;
        BlockLabelNode lAfter;

        FrameInfo(Frame<?> f, int firstLocal, int endInstruction, InsnList insnList, MethodDatabase db) {
            this.endInstruction = endInstruction;

            int idxObj = 0;
            int idxPrim = 0;

            if (f != null) {
                stackSlotIndices = new int[f.getStackSize()];
                for (int i = 0; i < f.getStackSize(); i++) {
                    BasicValue v = (BasicValue) f.getStack(i);
                    if (v instanceof NewValue) {
                        // explanation in emitNewAndDup
                        NewValue newValue = (NewValue) v;
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
                    BasicValue v = (BasicValue) f.getLocal(i);
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

        LabelNode createBeforeLabel() {
            if (lBefore == null)
                lBefore = new BlockLabelNode(endInstruction);
            return lBefore;
        }

        LabelNode createAfterLabel() {
            if (lAfter == null)
                lAfter = new BlockLabelNode(endInstruction);
            return lAfter;
        }
    }

    /*
    // prints a local var
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

    // prints the value at the top of the operand stack
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

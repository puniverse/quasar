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

import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.STACK_NAME;
import static co.paralleluniverse.fibers.instrument.Classes.SUSPEND_EXECUTION_CLASS;
import static co.paralleluniverse.fibers.instrument.Classes.isAllowedToBlock;
import static co.paralleluniverse.fibers.instrument.Classes.isBlockingCall;
import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;
import java.util.List;
import java.util.Map;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
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
//  private static final String INTERRUPTED_EXCEPTION_NAME = Type.getInternalName(InterruptedException.class);
    private static final boolean DUAL = true; // true if suspendable methods can be called from regular threads in addition to fibers
    private final MethodDatabase db;
    private final String className;
    private final MethodNode mn;
    private final Frame[] frames;
    private static final int NUM_LOCALS = 2; // lvarStack, lvarResumed
    private final int lvarStack; // ref to Stack
    private final int lvarResumed; // boolean indicating if we've been resumed
    private final int firstLocal;
    private FrameInfo[] codeBlocks = new FrameInfo[32];
    private int numCodeBlocks;
    private int additionalLocals;
    private boolean warnedAboutMonitors;
    private int warnedAboutBlocking;

    public InstrumentMethod(MethodDatabase db, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.className = className;
        this.mn = mn;

        try {
            Analyzer a = new TypeAnalyzer(db);
            this.frames = a.analyze(className, mn);
            this.lvarStack = mn.maxLocals;
            this.lvarResumed = mn.maxLocals + 1;
            this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
        } catch (UnsupportedOperationException ex) {
            throw new AnalyzerException(null, ex.getMessage(), ex);
        }
    }

    public boolean collectCodeBlocks() {
        int numIns = mn.instructions.size();

        codeBlocks[0] = FrameInfo.FIRST;
        for (int i = 0; i < numIns; i++) {
            Frame f = frames[i];
            if (f != null) { // reachable ?
                AbstractInsnNode in = mn.instructions.get(i);
                if (in.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode) in;
                    int opcode = min.getOpcode();
                    Boolean susp;
                    if (MethodDatabase.isReflectInvocation(min.owner, min.name)) {
                        db.log(LogLevel.DEBUG, "Reflective method call at instruction %d is assumed suspendable", i);
                        susp = true;
                    } else {
                        susp = db.isMethodSuspendable(min.owner, min.name, min.desc, opcode);
                        if (susp == null) {
                            db.log(LogLevel.WARNING, "Method not found in class - assuming suspendable: %s#%s%s", min.owner, min.name, min.desc);
                            susp = true;
                        }
                    }
                    if (susp) {
                        db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is suspendable", i, min.owner, min.name, min.desc);
                        FrameInfo fi = addCodeBlock(f, i);
                        splitTryCatch(fi);
                    } else {
                        db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is not suspendable", i, min.owner, min.name, min.desc);
                        int blockingId = isBlockingCall(min);
                        if (blockingId >= 0 && !isAllowedToBlock(className, mn.name)) {
                            int mask = 1 << blockingId;
                            if (!db.isAllowBlocking()) {
                                throw new UnableToInstrumentException("blocking call to "
                                        + min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                            } else if ((warnedAboutBlocking & mask) == 0) {
                                warnedAboutBlocking |= mask;
                                db.log(LogLevel.WARNING, "Method %s#%s%s contains potentially blocking call to "
                                        + min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                            }
                        }
                    }
                }
            }
        }
        addCodeBlock(null, numIns);

        return numCodeBlocks > 1;
    }

    public void accept(MethodVisitor mv) {
        db.log(LogLevel.INFO, "Instrumenting method %s#%s%s", className, mn.name, mn.desc);

        mv.visitCode();

        Label lMethodStart = new Label();
        Label lMethodEnd = new Label();
        Label lCatchSEE = new Label();
        Label lCatchAll = new Label();
        Label[] lMethodCalls = new Label[numCodeBlocks - 1];

        for (int i = 1; i < numCodeBlocks; i++)
            lMethodCalls[i - 1] = new Label();

        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchSEE, EXCEPTION_NAME);

        for (Object o : mn.tryCatchBlocks) {
            TryCatchBlockNode tcb = (TryCatchBlockNode) o;
            if (EXCEPTION_NAME.equals(tcb.type))
                throw new UnableToInstrumentException("catch for " + SUSPEND_EXECUTION_CLASS.getSimpleName(), className, mn.name, mn.desc);
//          if (INTERRUPTED_EXCEPTION_NAME.equals(tcb.type))
//              throw new UnableToInstrumentException("catch for " + InterruptedException.class.getSimpleName(), className, mn.name, mn.desc);

            tcb.accept(mv);
        }

        if (mn.visibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);

        if (mn.invisibleParameterAnnotations != null)
            dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);

        if (mn.visibleAnnotations != null) {
            for (Object o : mn.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }

        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchAll, null);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L" + STACK_NAME + ";");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);

        if (DUAL) {
            mv.visitJumpInsn(Opcodes.IFNULL, lMethodStart);
            mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        }

        emitStoreResumed(mv, true); // we'll assume we have been resumed

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "nextMethodEntry", "()I");
        mv.visitTableSwitchInsn(1, numCodeBlocks - 1, lMethodStart, lMethodCalls);

        mv.visitLabel(lMethodStart);

        emitStoreResumed(mv, false); // no, we have not been resumed

        dumpCodeBlock(mv, 0, 0);

        for (int i = 1; i < numCodeBlocks; i++) {
            FrameInfo fi = codeBlocks[i];

            MethodInsnNode min = (MethodInsnNode) (mn.instructions.get(fi.endInstruction));
            if (isYieldMethod(min.owner, min.name)) { // special case - call to yield
                if (min.getOpcode() != Opcodes.INVOKESTATIC)
                    throw new UnableToInstrumentException("invalid call to suspending method.", className, mn.name, mn.desc);

                final int numYieldArgs = TypeAnalyzer.getNumArguments(min.desc);
                final boolean yieldReturnsValue = (Type.getReturnType(min.desc) != Type.VOID_TYPE);

                emitStoreState(mv, i, fi, numYieldArgs); // we preserve the arguments for the call to yield on the operand stack
                emitStoreResumed(mv, false); // we have not been resumed

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

                dumpCodeBlock(mv, i, 1);    // skip the call
            } else {
                final Label lbl = new Label();
                if (DUAL) {
                    mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
                    mv.visitJumpInsn(Opcodes.IFNULL, lbl);
                }

                // normal case - call to a suspendable method - resume before the call
                emitStoreState(mv, i, fi, 0);
                emitStoreResumed(mv, false); // we have not been resumed

                mv.visitLabel(lMethodCalls[i - 1]);
                emitRestoreState(mv, i, fi, 0);

                if (DUAL)
                    mv.visitLabel(lbl);

                dumpCodeBlock(mv, i, 0);
            }
        }

        mv.visitLabel(lMethodEnd);

        mv.visitLabel(lCatchAll);
        emitPopMethod(mv);
        mv.visitLabel(lCatchSEE);
        mv.visitInsn(Opcodes.ATHROW);   // rethrow shared between catchAll and catchSSE

        if (mn.localVariables != null) {
            for (Object o : mn.localVariables)
                ((LocalVariableNode) o).accept(mv);
        }

        mv.visitMaxs(mn.maxStack + 4, mn.maxLocals + NUM_LOCALS + additionalLocals);
        mv.visitEnd();
    }

    private FrameInfo addCodeBlock(Frame f, int end) {
        if (++numCodeBlocks == codeBlocks.length) {
            FrameInfo[] newArray = new FrameInfo[numCodeBlocks * 2];
            System.arraycopy(codeBlocks, 0, newArray, 0, codeBlocks.length);
            codeBlocks = newArray;
        }
        FrameInfo fi = new FrameInfo(f, firstLocal, end, mn.instructions, db);
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
            TryCatchBlockNode tcb = (TryCatchBlockNode) mn.tryCatchBlocks.get(i);

            int start = getLabelIdx(tcb.start);
            int end = getLabelIdx(tcb.end);

            if (start <= fi.endInstruction && end >= fi.endInstruction) {
                //System.out.println("i="+i+" start="+start+" end="+end+" split="+splitIdx+
                //        " start="+mn.instructions.get(start)+" end="+mn.instructions.get(end));

                // need to split try/catch around the suspendable call
                if (start == fi.endInstruction) {
                    tcb.start = fi.createAfterLabel();
                } else {
                    if (end > fi.endInstruction) {
                        TryCatchBlockNode tcb2 = new TryCatchBlockNode(
                                fi.createAfterLabel(),
                                tcb.end, tcb.handler, tcb.type);
                        mn.tryCatchBlocks.add(i + 1, tcb2);
                    }

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
                        throw new UnableToInstrumentException("synchronisation", className, mn.name, mn.desc);
                    } else if (!warnedAboutMonitors) {
                        warnedAboutMonitors = true;
                        db.log(LogLevel.WARNING, "Method %s#%s%s contains synchronisation", className, mn.name, mn.desc);
                    }
                    break;

                case Opcodes.INVOKESPECIAL:
                    MethodInsnNode min = (MethodInsnNode) ins;
                    if ("<init>".equals(min.name)) {
                        int argSize = TypeAnalyzer.getNumArguments(min.desc);
                        Frame frame = frames[i];
                        int stackIndex = frame.getStackSize() - argSize - 1;
                        Value thisValue = frame.getStack(stackIndex);
                        if (stackIndex >= 1
                                && isNewValue(thisValue, true)
                                && isNewValue(frame.getStack(stackIndex - 1), false)) {
                            NewValue newValue = (NewValue) thisValue;
                            if (newValue.omitted) {
                                emitNewAndDup(mv, frame, stackIndex, min);
                            }
                        } else {
                            db.log(LogLevel.WARNING, "Expected to find a NewValue on stack index %d: %s", stackIndex, frame);
                        }
                    }
                    break;
            }
            ins.accept(mv);
        }
    }

    private static void dumpParameterAnnotations(MethodVisitor mv, List[] parameterAnnotations, boolean visible) {
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
        if (value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if ((byte) value == value) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if ((short) value == value) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    private void emitNewAndDup(MethodVisitor mv, Frame frame, int stackIndex, MethodInsnNode min) {
        int arguments = frame.getStackSize() - stackIndex - 1;
        int neededLocals = 0;
        for (int i = arguments; i >= 1; i--) {
            BasicValue v = (BasicValue) frame.getStack(stackIndex + i);
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), lvarStack + NUM_LOCALS + neededLocals);
            neededLocals += v.getSize();
        }
        db.log(LogLevel.DEBUG, "Inserting NEW & DUP for constructor call %s%s with %d arguments (%d locals)", min.owner, min.desc, arguments, neededLocals);
        if (additionalLocals < neededLocals) {
            additionalLocals = neededLocals;
        }
        ((NewValue) frame.getStack(stackIndex - 1)).insn.accept(mv);
        ((NewValue) frame.getStack(stackIndex)).insn.accept(mv);
        for (int i = 1; i <= arguments; i++) {
            BasicValue v = (BasicValue) frame.getStack(stackIndex + i);
            neededLocals -= v.getSize();
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), lvarStack + NUM_LOCALS + neededLocals);
        }
    }

    private void emitPopMethod(MethodVisitor mv) {
        final Label lbl = new Label();
        if (DUAL) {
            mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
            mv.visitJumpInsn(Opcodes.IFNULL, lbl);
        }

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "popMethod", "()V");

        if (DUAL)
            mv.visitLabel(lbl);
    }

    private void emitStoreState(MethodVisitor mv, int idx, FrameInfo fi, int numArgsToPreserve) {
        Frame f = frames[fi.endInstruction];

        if (fi.lBefore != null) {
            fi.lBefore.accept(mv);
        }

        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);
        emitConst(mv, fi.numSlots);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "pushMethod", "(II)V");

        // store operand stack
        for (int i = f.getStackSize(); i-- > 0;) {
            BasicValue v = (BasicValue) f.getStack(i);
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
            BasicValue v = (BasicValue) f.getLocal(i);
            if (!isNullType(v)) {
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), i);
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitStoreValue(mv, v, lvarStack, slotIdx);
            }
        }

        // restore last numArgsToPreserve operands
        for (int i = f.getStackSize() - numArgsToPreserve; i < f.getStackSize(); i++) {
            BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx);
                } else {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                }
            }
        }
    }

    private void emitRestoreState(MethodVisitor mv, int idx, FrameInfo fi, int numArgsPreserved) {
        Frame f = frames[fi.endInstruction];

        // restore local vars
        for (int i = firstLocal; i < f.getLocals(); i++) {
            BasicValue v = (BasicValue) f.getLocal(i);
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
        for (int i = 0; i < f.getStackSize() - numArgsPreserved; i++) {
            BasicValue v = (BasicValue) f.getStack(i);
            if (!isOmitted(v)) {
                if (!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx);
                } else {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                }
            }
        }

        if (fi.lAfter != null) {
            fi.lAfter.accept(mv);
        }
    }

    private void emitPostRestore(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "postRestore", "()V");
    }

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
        emitConst(mv, idx);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "push", desc);
    }

    private void emitRestoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        emitConst(mv, idx);

        switch (v.getType().getSort()) {
            case Type.OBJECT:
                String internalName = v.getType().getInternalName();
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject", "(I)Ljava/lang/Object;");
                if (!internalName.equals("java/lang/Object")) {  // don't cast to Object ;)
                    mv.visitTypeInsn(Opcodes.CHECKCAST, internalName);
                }
                break;
            case Type.ARRAY:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject", "(I)Ljava/lang/Object;");
                mv.visitTypeInsn(Opcodes.CHECKCAST, v.getType().getDescriptor());
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I");
                mv.visitInsn(Opcodes.I2B);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I");
                mv.visitInsn(Opcodes.I2S);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I");
                mv.visitInsn(Opcodes.I2C);
                break;
            case Type.BOOLEAN:
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getInt", "(I)I");
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getFloat", "(I)F");
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getLong", "(I)J");
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getDouble", "(I)D");
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
        if (v instanceof NewValue) {
            return ((NewValue) v).omitted;
        }
        return false;
    }

    static boolean isNewValue(Value v, boolean dupped) {
        if (v instanceof NewValue) {
            return ((NewValue) v).isDupped == dupped;
        }
        return false;
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
                    BasicValue v = (BasicValue) f.getStack(i);
                    if (v instanceof NewValue) {
                        NewValue newValue = (NewValue) v;
                        if (db.isDebug()) {
                            db.log(LogLevel.DEBUG, "Omit value from stack idx %d at instruction %d with type %s generated by %s",
                                    i, endInstruction, v, newValue.formatInsn());
                        }
                        if (!newValue.omitted) {
                            newValue.omitted = true;
                            if (db.isDebug()) {
                                // need to log index before replacing instruction
                                db.log(LogLevel.DEBUG, "Omitting instruction %d: %s", insnList.indexOf(newValue.insn), newValue.formatInsn());
                            }
                            insnList.set(newValue.insn, new OmittedInstruction(newValue.insn));
                        }
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    } else if (!isNullType(v)) {
                        if (v.isReference()) {
                            stackSlotIndices[i] = idxObj++;
                        } else {
                            stackSlotIndices[i] = idxPrim++;
                        }
                    } else {
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    }
                }

                localSlotIndices = new int[f.getLocals()];
                for (int i = firstLocal; i < f.getLocals(); i++) {
                    BasicValue v = (BasicValue) f.getLocal(i);
                    if (!isNullType(v)) {
                        if (v.isReference()) {
                            localSlotIndices[i] = idxObj++;
                        } else {
                            localSlotIndices[i] = idxPrim++;
                        }
                    } else {
                        localSlotIndices[i] = -666; // an invalid index ;)
                    }
                }
            } else {
                stackSlotIndices = null;
                localSlotIndices = null;
            }

            numSlots = Math.max(idxPrim, idxObj);
            numObjSlots = idxObj;
        }

        public LabelNode createBeforeLabel() {
            if (lBefore == null) {
                lBefore = new BlockLabelNode(endInstruction);
            }
            return lBefore;
        }

        public LabelNode createAfterLabel() {
            if (lAfter == null) {
                lAfter = new BlockLabelNode(endInstruction);
            }
            return lAfter;
        }
    }
}

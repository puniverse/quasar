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
package de.matthiasmann.continuations.instrument;

import de.matthiasmann.continuations.Stack;
import de.matthiasmann.continuations.SuspendExecution;
import java.util.List;
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
 */
public class InstrumentMethod {

    private static final String STACK_NAME = Type.getInternalName(Stack.class);
    
    private final MethodDatabase db;
    private final String className;
    private final MethodNode mn;
    private final Frame[] frames;
    private final int lvarStack;
    private final int firstLocal;
    
    private FrameInfo[] codeBlocks = new FrameInfo[32];
    private int numCodeBlocks;
    private int additionalLocals;
    
    private boolean warnedAboutMonitors;
    private int warnedAboutBlocking;

    private static final BlockingMethod BLOCKING_METHODS[] = {
        new BlockingMethod("java/lang/Thread", "sleep", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Thread", "join", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Object", "wait", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lock", "()V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lockInterruptibly", "()V"),
    };
    
    public InstrumentMethod(MethodDatabase db, String className, MethodNode mn) throws AnalyzerException {
        this.db = db;
        this.className = className;
        this.mn = mn;
        
        try {
            Analyzer a = new TypeAnalyzer(db);
            this.frames = a.analyze(className, mn);
            this.lvarStack = mn.maxLocals;
            this.firstLocal = ((mn.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC) ? 0 : 1;
        } catch (UnsupportedOperationException ex) {
            throw new AnalyzerException(null, ex.getMessage(), ex);
        }
    }
    
    public boolean collectCodeBlocks() {
        int numIns = mn.instructions.size();
        
        codeBlocks[0] = FrameInfo.FIRST;
        for(int i=0 ; i<numIns ; i++) {
            Frame f = frames[i];
            if(f != null) { // reachable ?
                AbstractInsnNode in = mn.instructions.get(i);
                if(in.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode min = (MethodInsnNode)in;
                    int opcode = min.getOpcode();
                    if(db.isMethodSuspendable(min.owner, min.name, min.desc,
                            opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESTATIC)) {
                        db.log(LogLevel.DEBUG, "Method call at instruction %d to %s#%s%s is suspendable", i, min.owner, min.name, min.desc);
                        FrameInfo fi = addCodeBlock(f, i);
                        splitTryCatch(fi);
                    } else {
                        int blockingId = isBlockingCall(min);
                        if(blockingId >= 0) {
                            int mask = 1 << blockingId;
                            if(!db.isAllowBlocking()) {
                                throw new UnableToInstrumentException("blocking call to " +
                                        min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                            } else if((warnedAboutBlocking & mask) == 0) {
                                warnedAboutBlocking |= mask;
                                db.log(LogLevel.WARNING, "Method %s#%s%s contains potentially blocking call to " +
                                        min.owner + "#" + min.name + min.desc, className, mn.name, mn.desc);
                            }
                        }
                    }
                }
            }
        }
        addCodeBlock(null, numIns);
        
        return numCodeBlocks > 1;
    }
    
    private static int isBlockingCall(MethodInsnNode ins) {
        for(int i=0,n=BLOCKING_METHODS.length ; i<n ; i++) {
            if(BLOCKING_METHODS[i].match(ins)) {
                return i;
            }
        }
        return -1;
    }
    
    public void accept(MethodVisitor mv) {
        db.log(LogLevel.INFO, "Instrumenting method %s%s%s", className, mn.name, mn.desc);
        
        mv.visitCode();
        
        Label lMethodStart = new Label();
        Label lMethodEnd = new Label();
        Label lCatchSEE = new Label();
        Label lCatchAll = new Label();
        Label[] lMethodCalls = new Label[numCodeBlocks-1];
        
        for(int i=1 ; i<numCodeBlocks ; i++) {
            lMethodCalls[i-1] = new Label();
        }
        
        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchSEE, CheckInstrumentationVisitor.EXCEPTION_NAME);
        
        for(Object o : mn.tryCatchBlocks) {
            TryCatchBlockNode tcb = (TryCatchBlockNode)o;
            if(CheckInstrumentationVisitor.EXCEPTION_NAME.equals(tcb.type)) {
                throw new UnableToInstrumentException("catch for " +
                        SuspendExecution.class.getSimpleName(), className, mn.name, mn.desc);
            }
            tcb.accept(mv);
        }
        
        if(mn.visibleParameterAnnotations != null) {
            dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);
        }
        
        if(mn.invisibleParameterAnnotations != null) {
            dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);
        }
        
        if(mn.visibleAnnotations != null) {
            for(Object o : mn.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode)o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }
        
        mv.visitTryCatchBlock(lMethodStart, lMethodEnd, lCatchAll, null);
    
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, STACK_NAME, "getStack", "()L"+STACK_NAME+";");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ASTORE, lvarStack);
        
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "nextMethodEntry", "()I");
        mv.visitTableSwitchInsn(1, numCodeBlocks-1, lMethodStart, lMethodCalls);
        
        mv.visitLabel(lMethodStart);
        dumpCodeBlock(mv, 0, 0);
        
        for(int i=1 ; i<numCodeBlocks ; i++) {
            FrameInfo fi = codeBlocks[i];
            
            MethodInsnNode min = (MethodInsnNode)(mn.instructions.get(fi.endInstruction));
            if(InstrumentClass.COROUTINE_NAME.equals(min.owner) && "yield".equals(min.name)) {
                // special case - call to yield() - resume AFTER the call
                if(min.getOpcode() != Opcodes.INVOKESTATIC) {
                    throw new UnableToInstrumentException("invalid call to yield()", className, mn.name, mn.desc);
                }
                emitStoreState(mv, i, fi);
                mv.visitFieldInsn(Opcodes.GETSTATIC, STACK_NAME,
                        "exception_instance_not_for_user_code",
                        CheckInstrumentationVisitor.EXCEPTION_DESC);
                mv.visitInsn(Opcodes.ATHROW);
                min.accept(mv); // only the call
                mv.visitLabel(lMethodCalls[i-1]);
                emitRestoreState(mv, i, fi);
                dumpCodeBlock(mv, i, 1);    // skip the call
            } else {
                // normal case - call to a suspendable method - resume before the call
                emitStoreState(mv, i, fi);
                mv.visitLabel(lMethodCalls[i-1]);
                emitRestoreState(mv, i, fi);
                dumpCodeBlock(mv, i, 0);
            }
        }
        
        mv.visitLabel(lMethodEnd);
        
        mv.visitLabel(lCatchAll);
        emitPopMethod(mv);
        mv.visitLabel(lCatchSEE);
        mv.visitInsn(Opcodes.ATHROW);   // rethrow shared between catchAll and catchSSE
        
        if(mn.localVariables != null) {
            for(Object o : mn.localVariables) {
                ((LocalVariableNode)o).accept(mv);
            }
        }
        
        mv.visitMaxs(mn.maxStack+3, mn.maxLocals+1+additionalLocals);
        mv.visitEnd();
    }

    private FrameInfo addCodeBlock(Frame f, int end) {
        if(++numCodeBlocks == codeBlocks.length) {
            FrameInfo[] newArray = new FrameInfo[numCodeBlocks*2];
            System.arraycopy(codeBlocks, 0, newArray, 0, codeBlocks.length);
            codeBlocks = newArray;
        }
        FrameInfo fi = new FrameInfo(f, firstLocal, end, mn.instructions, db);
        codeBlocks[numCodeBlocks] = fi;
        return  fi;
    }

    private int getLabelIdx(LabelNode l) {
        int idx;
        if(l instanceof BlockLabelNode) {
            idx = ((BlockLabelNode)l).idx;
        } else {
            idx = mn.instructions.indexOf(l);
        }
        
        // search for the "real" instruction
        for(;;) {
            int type = mn.instructions.get(idx).getType();
            if(type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE) {
                return idx;
            }
            idx++;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void splitTryCatch(FrameInfo fi) {
        for(int i=0 ; i<mn.tryCatchBlocks.size() ; i++) {
            TryCatchBlockNode tcb = (TryCatchBlockNode)mn.tryCatchBlocks.get(i);
            
            int start = getLabelIdx(tcb.start);
            int end = getLabelIdx(tcb.end);
            
            if(start <= fi.endInstruction && end >= fi.endInstruction) {
                //System.out.println("i="+i+" start="+start+" end="+end+" split="+splitIdx+
                //        " start="+mn.instructions.get(start)+" end="+mn.instructions.get(end));
                
                // need to split try/catch around the suspendable call
                if(start == fi.endInstruction) {
                    tcb.start = fi.createAfterLabel();
                } else {
                    if(end > fi.endInstruction) {
                        TryCatchBlockNode tcb2 = new TryCatchBlockNode(
                                fi.createAfterLabel(),
                                tcb.end, tcb.handler, tcb.type);
                        mn.tryCatchBlocks.add(i+1, tcb2);
                    }

                    tcb.end = fi.createBeforeLabel();
                }
            }
        }
    }

    private void dumpCodeBlock(MethodVisitor mv, int idx, int skip) {
        int start = codeBlocks[idx].endInstruction;
        int end = codeBlocks[idx+1].endInstruction;
        
        for(int i=start+skip ; i<end ; i++) {
            AbstractInsnNode ins = mn.instructions.get(i);
            switch(ins.getOpcode()) {
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
                if(!db.isAllowMonitors()) {
                    throw new UnableToInstrumentException("synchronisation", className, mn.name, mn.desc);
                } else if(!warnedAboutMonitors) {
                    warnedAboutMonitors = true;
                    db.log(LogLevel.WARNING, "Method %s#%s%s contains synchronisation", className, mn.name, mn.desc);
                }
                break;
                
            case Opcodes.INVOKESPECIAL:
                MethodInsnNode min = (MethodInsnNode)ins;
                if("<init>".equals(min.name)) {
                    int argSize = TypeAnalyzer.getNumArguments(min.desc);
                    Frame frame = frames[i];
                    int stackIndex = frame.getStackSize() - argSize - 1;
                    Value thisValue = frame.getStack(stackIndex);
                    if(stackIndex >= 1 &&
                            isNewValue(thisValue, true) &&
                            isNewValue(frame.getStack(stackIndex-1), false)) {
                        NewValue newValue = (NewValue)thisValue;
                        if(newValue.omitted) {
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
        for(int i=0 ; i<parameterAnnotations.length ; i++) {
            if(parameterAnnotations[i] != null) {
                for(Object o : parameterAnnotations[i]) {
                    AnnotationNode an = (AnnotationNode)o;
                    an.accept(mv.visitParameterAnnotation(i, an.desc, visible));
                }
            }
        }
    }

    private static void emitConst(MethodVisitor mv, int value) {
        if(value >= -1 && value <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + value);
        } else if((byte)value == value) {
            mv.visitIntInsn(Opcodes.BIPUSH, value);
        } else if((short)value == value) {
            mv.visitIntInsn(Opcodes.SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }
    
    private void emitNewAndDup(MethodVisitor mv, Frame frame, int stackIndex, MethodInsnNode min) {
        int arguments = frame.getStackSize() - stackIndex - 1;
        int neededLocals = 0;
        for(int i=arguments ; i>=1 ; i--) {
            BasicValue v = (BasicValue)frame.getStack(stackIndex+i);
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), lvarStack+1+neededLocals);
            neededLocals += v.getSize();
        }
        db.log(LogLevel.DEBUG, "Inserting NEW & DUP for constructor call %s%s with %d arguments (%d locals)", min.owner, min.desc, arguments, neededLocals);
        if(additionalLocals < neededLocals) {
            additionalLocals = neededLocals;
        }
        ((NewValue)frame.getStack(stackIndex-1)).insn.accept(mv);
        ((NewValue)frame.getStack(stackIndex  )).insn.accept(mv);
        for(int i=1 ; i<=arguments ; i++) {
            BasicValue v = (BasicValue)frame.getStack(stackIndex+i);
            neededLocals -= v.getSize();
            mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), lvarStack+1+neededLocals);
        }
    }
    
    private void emitPopMethod(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, lvarStack);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "popMethod", "()V");
    }

    private void emitStoreState(MethodVisitor mv, int idx, FrameInfo fi) {
        Frame f = frames[fi.endInstruction];
        
        if(fi.lBefore != null) {
            fi.lBefore.accept(mv);
        }
            
        mv.visitVarInsn(Opcodes.ALOAD,lvarStack);
        emitConst(mv, idx);
        emitConst(mv, fi.numSlots);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "pushMethodAndReserveSpace", "(II)V");
                
        for(int i=f.getStackSize() ; i-->0 ;) {
            BasicValue v = (BasicValue) f.getStack(i);
            if(!isOmitted(v)) {
                if(!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitStoreValue(mv, v, lvarStack, slotIdx);
                } else {
                    db.log(LogLevel.DEBUG, "NULL stack entry: type=%s size=%d", v.getType(), v.getSize());
                    mv.visitInsn(Opcodes.POP);
                }
            }
        }
        
        for(int i=firstLocal ; i<f.getLocals() ; i++) {
            BasicValue v = (BasicValue) f.getLocal(i);
            if(!isNullType(v)) {
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ILOAD), i);
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitStoreValue(mv, v, lvarStack, slotIdx);
            }
        }
    }

    private void emitRestoreState(MethodVisitor mv, int idx, FrameInfo fi) {
        Frame f = frames[fi.endInstruction];
        
        for(int i=firstLocal ; i<f.getLocals() ; i++) {
            BasicValue v = (BasicValue) f.getLocal(i);
            if(!isNullType(v)) {
                int slotIdx = fi.localSlotIndices[i];
                assert slotIdx >= 0 && slotIdx < fi.numSlots;
                emitRestoreValue(mv, v, lvarStack, slotIdx);
                mv.visitVarInsn(v.getType().getOpcode(Opcodes.ISTORE), i);
            } else if(v != BasicValue.UNINITIALIZED_VALUE) {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitVarInsn(Opcodes.ASTORE, i);
            }
        }
        
        for(int i=0 ; i<f.getStackSize() ; i++) {
            BasicValue v = (BasicValue) f.getStack(i);
            if(!isOmitted(v)) {
                if(!isNullType(v)) {
                    int slotIdx = fi.stackSlotIndices[i];
                    assert slotIdx >= 0 && slotIdx < fi.numSlots;
                    emitRestoreValue(mv, v, lvarStack, slotIdx);
                } else {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                }
            }
        }
        
        if(fi.lAfter != null) {
            fi.lAfter.accept(mv);
        }
    }

    private void emitStoreValue(MethodVisitor mv, BasicValue v, int lvarStack, int idx) throws InternalError, IndexOutOfBoundsException {
        String desc;

        switch(v.getType().getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            desc = "(Ljava/lang/Object;L"+STACK_NAME+";I)V";
            break;
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.SHORT:
        case Type.CHAR:
        case Type.INT:
            desc = "(IL"+STACK_NAME+";I)V";
            break;
        case Type.FLOAT:
            desc = "(FL"+STACK_NAME+";I)V";
            break;
        case Type.LONG:
            desc = "(JL"+STACK_NAME+";I)V";
            break;
        case Type.DOUBLE:
            desc = "(DL"+STACK_NAME+";I)V";
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
        
        switch(v.getType().getSort()) {
        case Type.OBJECT:
            String internalName = v.getType().getInternalName();
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STACK_NAME, "getObject", "(I)Ljava/lang/Object;");
            if(!internalName.equals("java/lang/Object")) {  // don't cast to Object ;)
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
        return (v == BasicValue.UNINITIALIZED_VALUE) ||
                (v.isReference() && v.getType().getInternalName().equals("null"));
    }
    
    static boolean isOmitted(BasicValue v) {
        if(v instanceof NewValue) {
            return ((NewValue)v).omitted;
        }
        return false;
    }
    
    static boolean isNewValue(Value v, boolean dupped) {
        if(v instanceof NewValue) {
            return ((NewValue)v).isDupped == dupped;
        }
        return false;
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
            
            if(f != null) {
                stackSlotIndices = new int[f.getStackSize()];
                for(int i=0 ; i<f.getStackSize() ; i++) {
                    BasicValue v = (BasicValue)f.getStack(i);
                    if(v instanceof NewValue) {
                        NewValue newValue = (NewValue)v;
                        if(db.isDebug()) {
                            db.log(LogLevel.DEBUG, "Omit value from stack idx %d at instruction %d with type %s generated by %s",
                                    i, endInstruction, v, newValue.formatInsn());
                        }
                        if(!newValue.omitted) {
                            newValue.omitted = true;
                            if(db.isDebug()) {
                                // need to log index before replacing instruction
                                db.log(LogLevel.DEBUG, "Omitting instruction %d: %s", insnList.indexOf(newValue.insn), newValue.formatInsn());
                            }
                            insnList.set(newValue.insn, new OmittedInstruction(newValue.insn));
                        }
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    } else if(!isNullType(v)) {
                        if(v.isReference()) {
                            stackSlotIndices[i] = idxObj++;
                        } else {
                            stackSlotIndices[i] = idxPrim++;
                        }
                    } else {
                        stackSlotIndices[i] = -666; // an invalid index ;)
                    }
                }

                localSlotIndices = new int[f.getLocals()];
                for(int i=firstLocal ; i<f.getLocals() ; i++) {
                    BasicValue v = (BasicValue)f.getLocal(i);
                    if(!isNullType(v)) {
                        if(v.isReference()) {
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
            if(lBefore == null) {
                lBefore = new BlockLabelNode(endInstruction);
            }
            return lBefore;
        }
        
        public LabelNode createAfterLabel() {
            if(lAfter == null) {
                lAfter = new BlockLabelNode(endInstruction);
            }
            return lAfter;
        }
    }
    
    private static class BlockingMethod {
        final String owner;
        final String name;
        final String[] descs;

        BlockingMethod(String owner, String name, String ... descs) {
            this.owner = owner;
            this.name = name;
            this.descs = descs;
        }
        
        public boolean match(MethodInsnNode min) {
            if(owner.equals(min.owner) && name.equals(min.name)) {
                for(String desc : descs) {
                    if(desc.equals(min.desc)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}

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
package co.paralleluniverse.fibers.instrument;

import com.google.common.primitives.Ints;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;
import static co.paralleluniverse.fibers.instrument.QuasarInstrumentor.ASMAPI;

/**
 * @author circlespainter
 */
public class SuspOffsetsBeforeInstrClassVisitor extends ClassVisitor {
    private final MethodDatabase db;

    private String className;

    private boolean record = true;

    public SuspOffsetsBeforeInstrClassVisitor(ClassVisitor cv, MethodDatabase db) {
        super(ASMAPI, cv);
        this.db = db;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, boolean b) {
        if (Classes.DONT_INSTRUMENT_DESC.equals(name))
            record = false;
        return super.visitAnnotation(name, b);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;

        // need atleast 1.5 for annotations to work
        if (version < Opcodes.V1_5)
            version = Opcodes.V1_5;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        if (record && (access & Opcodes.ACC_NATIVE) == 0 && !isYieldMethod(className, name)) {
            // Bytecode-level AST of a method, being a MethodVisitor itself can be filled through delegation from another visitor
            final MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);

            // Analyze, fill and enqueue method ASTs
            final MethodVisitor outMV = super.visitMethod(access, name, desc, signature, exceptions);

            return new MethodVisitor(ASMAPI, outMV) {
                private List<Integer> suspOffsetsBeforeInstrL = new ArrayList<>();
                private int prevOffset = -1;
                private Label currLabel = null;
                private boolean record = true;

                @Override
                public AnnotationVisitor visitAnnotation(String name, boolean b) {
                    if (Classes.DONT_INSTRUMENT_DESC.equals(name))
                        record = false;
                    return super.visitAnnotation(name, b);
                }

                @Override
                public void visitLabel(Label label) {
                    currLabel = label;
                    super.visitLabel(label);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                    final int type = AbstractInsnNode.METHOD_INSN;
                    if (InstrumentMethod.isSuspendableCall(db, type, opcode, owner, name, desc) &&
                            currLabel != null && currLabel.info instanceof Integer)
                        addLine();
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle handle, Object... objects) {
                    final int type = AbstractInsnNode.INVOKE_DYNAMIC_INSN;
                    final int opcode = Opcodes.INVOKEDYNAMIC;
                    if (InstrumentMethod.isSuspendableCall(db, type, opcode, handle.getOwner(), name, desc) &&
                            currLabel != null && currLabel.info instanceof Integer)
                        addLine();
                    super.visitInvokeDynamicInsn(name, desc, handle, objects);
                }

                @Override
                public void visitEnd() {
                    InstrumentationKB.setMethodPreInstrumentationOffsets(className, mn.name, mn.desc, Ints.toArray(suspOffsetsBeforeInstrL));
                    super.visitEnd();
                }

                private void addLine() {
                    final int currOffset = (Integer) currLabel.info;
                    if (record && currOffset > prevOffset) {
                        suspOffsetsBeforeInstrL.add(currOffset);
                        prevOffset = currOffset;
                    }
                }
            };
        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}

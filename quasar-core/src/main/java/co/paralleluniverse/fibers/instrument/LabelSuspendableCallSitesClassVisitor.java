/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015-2016, Parallel Universe Software Co. All rights reserved.
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

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;

import static co.paralleluniverse.common.asm.ASMUtil.ASMAPI;
import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;

/**
 * @author circlespainter
 */
class LabelSuspendableCallSitesClassVisitor extends ClassVisitor {
    private final MethodDatabase db;
    private String className;

    LabelSuspendableCallSitesClassVisitor(ClassVisitor cv, MethodDatabase db) {
        super(ASMAPI, cv);
        this.db = db;
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
        if ((access & Opcodes.ACC_NATIVE) == 0 && !isYieldMethod(className, name)) {
            // Bytecode-level AST of a method, being a MethodVisitor itself can be filled through delegation from another visitor
            // Analyze, fill and enqueue method ASTs
            final MethodVisitor outMV = super.visitMethod(access, name, desc, signature, exceptions);

            return new MethodVisitor(ASMAPI, outMV) {
                private int currLineNumber = -1;

                @Override
                public void visitLineNumber(int i, Label label) {
                    currLineNumber = i;
                    super.visitLineNumber(i, label);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                    final int type = AbstractInsnNode.METHOD_INSN;
                    if (InstrumentMethod.isSuspendableCall(db, type, opcode, owner, name, desc)) {
                        final Label l = new Label();
                        super.visitLabel(l);
                        super.visitLineNumber(currLineNumber, l); // Force label
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle handle, Object... objects) {
                    final int type = AbstractInsnNode.INVOKE_DYNAMIC_INSN;
                    final int opcode = Opcodes.INVOKEDYNAMIC;
                    if (InstrumentMethod.isSuspendableCall(db, type, opcode, handle.getOwner(), name, desc)) {
                        final Label l = new Label();
                        super.visitLabel(l);
                        super.visitLineNumber(currLineNumber, l); // Force label
                    }
                    super.visitInvokeDynamicInsn(name, desc, handle, objects);
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}

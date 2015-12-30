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

import co.paralleluniverse.fibers.Instrumented;
import com.google.common.primitives.Ints;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

import static co.paralleluniverse.fibers.instrument.Classes.*;
import static co.paralleluniverse.fibers.instrument.QuasarInstrumentor.ASMAPI;

/**
 * @author circlespainter
 */
public class SuspOffsetsAfterInstrClassVisitor extends ClassVisitor {
    private final MethodDatabase db;
    private String className;

    public SuspOffsetsAfterInstrClassVisitor(ClassVisitor cv, MethodDatabase db) {
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
            final MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);

            // Analyze, fill and enqueue method ASTs
            final MethodVisitor outMV = super.visitMethod(access, name, desc, signature, exceptions);

            return new MethodVisitor(ASMAPI, outMV) {
                private Label currLabel = null;
                private int prevOffset = -1;
                private boolean instrumented;
                private boolean optimized = false;
                private int methodStart = -1, methodEnd = -1;
                private int[] suspCallSourceLines;
                private List<String> suspCallSignaturesL = new ArrayList<>();

                private List<Integer> suspOffsetsAfterInstrL = new ArrayList<>();

                @Override
                public AnnotationVisitor visitAnnotation(final String adesc, boolean visible) {
                    if (adesc.equals(Type.getDescriptor(Instrumented.class))) {
                        instrumented = true;
                        return new AnnotationVisitor(ASMAPI) { // Only collect info
                            @Override
                            public void visit(String s, Object o) {
                                if ("methodStartSourceLine".equals(s))
                                    methodStart = (Integer) o;
                                else if ("methodEndSourceLine".equals(s))
                                    methodEnd = (Integer) o;
                                else if ("methodOptimized".equals(s))
                                    optimized = (Boolean) o;
                                else if ("methodSuspendableCallSourceLines".equals(s))
                                    suspCallSourceLines = (int[]) o;
                            }

                            @Override
                            public AnnotationVisitor visitArray(String s) {
                                if ("methodSuspendableCallSignatures".equals(s))
                                    return new AnnotationVisitor(ASMAPI) {
                                        @Override
                                        public void visit(String s, Object o) {
                                            suspCallSignaturesL.add((String) o);
                                        }
                                    };

                                return null;
                            }
                        };
                    }
                    return super.visitAnnotation(adesc, visible);
                }

                @Override
                public void visitLabel(Label label) {
                    if (instrumented) {
                        currLabel = label;
                    }
                    super.visitLabel(label);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
                    if (instrumented) {
                        final int type = AbstractInsnNode.METHOD_INSN;
                        if (InstrumentMethod.isSuspendableCall(db, type, opcode, owner, name, desc) &&
                                !Classes.STACK_NAME.equals(owner) && // postRestore
                                currLabel != null && currLabel.info instanceof Integer)
                            addLine();
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, isInterface);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle handle, Object... objects) {
                    if (instrumented) {
                        final int type = AbstractInsnNode.INVOKE_DYNAMIC_INSN;
                        final int opcode = Opcodes.INVOKEDYNAMIC;
                        if (InstrumentMethod.isSuspendableCall(db, type, opcode, handle.getOwner(), name, desc) &&
                                !Classes.STACK_NAME.equals(handle.getOwner()) && // postRestore
                                currLabel != null && currLabel.info instanceof Integer)
                            addLine();
                    }
                    super.visitInvokeDynamicInsn(name, desc, handle, objects);
                }

                @Override
                public void visitEnd() {
                    if (instrumented)
                        InstrumentMethod.emitInstrumentedAnn (
                            db, outMV, mn, className, optimized, methodStart, methodEnd,
                            suspCallSourceLines, toStringArray(suspCallSignaturesL),
                            InstrumentKB.getMethodPreInstrumentationOffsets(className, mn.name, mn.desc),
                            Ints.toArray(suspOffsetsAfterInstrL)
                        );
                    InstrumentKB.removeMethodPreInstrumentationOffsets(className, mn.name, mn.desc);
                    super.visitEnd();
                }

                private void addLine() {
                    final int currOffset = (Integer) currLabel.info;
                    if (currOffset > prevOffset) {
                        suspOffsetsAfterInstrL.add(currOffset);
                        prevOffset = currOffset;
                    }
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    // TODO: Factor with `InstrumentClassVisitor`
    private static String[] toStringArray(List<?> l) {
        if (l.isEmpty())
            return null;

        //noinspection SuspiciousToArrayCall
        return l.toArray(new String[l.size()]);
    }
}

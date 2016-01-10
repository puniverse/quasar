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
import co.paralleluniverse.fibers.SuspendableCallSite;
import co.paralleluniverse.fibers.SuspendableCalls;
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
                private List<InstrumentMethod.SuspCallSite> suspCallSitesL = new ArrayList<>();

                private List<Integer> suspOffsetsAfterInstrL = new ArrayList<>();

                private int[] preInstrOffsets;

                @Override
                public AnnotationVisitor visitAnnotation(final String adesc, boolean visible) {
                    if (Classes.INSTRUMENTED_DESC.equals(adesc)) {
                        instrumented = true;

                        return new AnnotationVisitor(ASMAPI) { // Only collect info
                            @Override
                            public void visit(String name, Object value) {
                                if (Instrumented.FIELD_NAME_METHOD_START_SOURCE_LINE.equals(name))
                                    methodStart = (Integer) value;
                                else if (Instrumented.FIELD_NAME_METHOD_END_SOURCE_LINE.equals(name))
                                    methodEnd = (Integer) value;
                                else if (Instrumented.FIELD_NAME_IS_METHOD_INSTRUMENTATION_OPTIMIZED.equals(name))
                                    optimized = (Boolean) value;
                                else
                                    throw new RuntimeException("Unexpected `@Instrumented` field: " + name);
                            }

                            @Override
                            public AnnotationVisitor visitArray(String name) {
                                if (Instrumented.FIELD_NAME_METHOD_SUSPENDABLE_CALL_SITES.equals(name))
                                    return new AnnotationVisitor(ASMAPI) {
                                        @Override
                                        public AnnotationVisitor visitAnnotation(String name, String adesc) {
                                            // Will always be a `SuspendableCallSite`
                                            return new AnnotationVisitor(ASMAPI) {
                                                private String desc = null;
                                                private int sourceLine = -1;
                                                private int entry = -1;
                                                private List<Type> operandTypes = new ArrayList<>(),
                                                                   localTypes = new ArrayList<>();
                                                private List<Integer> operandIndexes = new ArrayList<>(),
                                                                      localIndexes = new ArrayList<>();

                                                @Override
                                                public void visit(String name, Object value) {
                                                    if (SuspendableCallSite.FIELD_NAME_DESC.equals(name))
                                                        desc = (String) value;
                                                    else if (SuspendableCallSite.FIELD_NAME_SOURCE_LINE.equals(name))
                                                        sourceLine = (int) value;
                                                    else if (SuspendableCallSite.FIELD_NAME_ENTRY.equals(name))
                                                        entry = (int) value;
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_TYPES.equals(name))
                                                        operandTypes.add(Type.getType((String) value));
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_INDEXES.equals(name))
                                                        operandIndexes.addAll(Ints.asList((int[]) value));
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_TYPES.equals(name))
                                                        localTypes.add(Type.getType((String) value));
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_INDEXES.equals(name))
                                                        localIndexes.addAll(Ints.asList((int[]) value));
                                                    else //noinspection StatementWithEmptyBody
                                                        if (SuspendableCallSite.FIELD_NAME_PRE_INSTRUMENTATION_OFFSET.equals(name))
                                                        ; // Set later
                                                    else //noinspection StatementWithEmptyBody
                                                        if (SuspendableCallSite.FIELD_NAME_POST_INSTRUMENTATION_OFFSET.equals(name))
                                                        ; // Set later
                                                    else
                                                        throw new RuntimeException("Unexpected `@SuspendableCallSite` field: " + name);
                                                }

                                                @Override
                                                public AnnotationVisitor visitArray(String name) {
                                                    if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_TYPES.equals(name))
                                                        return new AnnotationVisitor(ASMAPI) {
                                                            @Override
                                                            public void visit(String name, Object value) {
                                                                operandTypes.add(Type.getType((String) value));
                                                            }
                                                        };
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_OPERANDS_INDEXES.equals(name))
                                                        return new AnnotationVisitor(ASMAPI) {
                                                            @Override
                                                            public void visit(String name, Object value) {
                                                                operandIndexes.add((Integer) value);
                                                            }
                                                        };
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_TYPES.equals(name))
                                                        return new AnnotationVisitor(ASMAPI) {
                                                            @Override
                                                            public void visit(String name, Object value) {
                                                                localTypes.add(Type.getType((String) value));
                                                            }
                                                        };
                                                    else if (SuspendableCallSite.FIELD_NAME_STACK_FRAME_LOCALS_INDEXES.equals(name))
                                                        return new AnnotationVisitor(ASMAPI) {
                                                            @Override
                                                            public void visit(String name, Object value) {
                                                                localIndexes.add((Integer) value);
                                                            }
                                                        };
                                                    else
                                                        throw new RuntimeException("Unexpected `@SuspendableCallSite` field: " + name);
                                                }

                                                @Override
                                                public void visitEnd() {
                                                    suspCallSitesL.add (
                                                        new InstrumentMethod.SuspCallSite (
                                                            this.desc, entry, sourceLine,
                                                            operandTypes, operandIndexes,
                                                            localTypes, localIndexes
                                                        )
                                                    );
                                                }
                                            };
                                        }
                                    };
                                else
                                    throw new RuntimeException("Unexpected `@SuspendableCallSite` field: " + name);
                            }
                        };
                    } else if (Classes.SUSPENDABLE_CALLS_DESC.equals(adesc)) {
                        return new AnnotationVisitor(ASMAPI) { // Only collect info
                            @Override
                            public void visit(String name, Object value) {
                                if (SuspendableCalls.FIELD_NAME_METHOD_SUSPENDABLE_CALL_OFFSETS.equals(name))
                                    preInstrOffsets = (int[]) value;
                                else
                                    throw new RuntimeException("Unexpected `@SuspendableCalls` field: " + name);
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
                    if (preInstrOffsets != null) {
                        // In some cases suspendable calls are found in pre- that later aren't instrumented
                        for (int i = 0; i < preInstrOffsets.length && i < suspCallSitesL.size() && i < suspOffsetsAfterInstrL.size(); i++) {
                            suspCallSitesL.get(i).preInstrumentationOffset = preInstrOffsets[i];
                            suspCallSitesL.get(i).postInstrumentationOffset = suspOffsetsAfterInstrL.get(i);
                        }
                    }

                    if (instrumented)
                        InstrumentMethod.emitInstrumentedAnn (
                            db, outMV, mn, className, optimized, methodStart, methodEnd, suspCallSitesL
                        );

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
}

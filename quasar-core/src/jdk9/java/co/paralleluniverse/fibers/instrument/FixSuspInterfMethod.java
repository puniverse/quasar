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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static co.paralleluniverse.fibers.instrument.Classes.*;

/**
 * @author circlespainter
 */
public class FixSuspInterfMethod {
    private final MethodDatabase db;
    private final String className;
    private final MethodNode mn;

    private Boolean canInterferWithDirectSuspendExecution, canInterferWithReflectSuspendExecution;

    public FixSuspInterfMethod(MethodDatabase db, String className, MethodNode mn) {
        this.db = db;
        this.className = className;
        this.mn = mn;
    }

    public boolean isNeeded() {
        return
            canInterfereWithDirectSuspendExecution() ||
            canInterfereWithReflectSuspendExecution();
    }

    /**
     * Applies a temporary minimal instrumentation to let through `SuspendExecution`. To be used when live instrumentation
     * is enabled, on all methods that could interfer with the suspension mechanism (which is currently based on
     * regular exceptions) and before those methods become part of suspendable call paths (i.e. as pre-run instrumentation
     * or pre-call instrumentation/redefinition).
     * <br/>
     * This is needed because during live instrumentation most suspendables won't be known in advance and instrumentation
     * will be invoked via redefinition just before suspending, when calls are still in progress (if uninstrumented
     * frames are detected).
     * On the JVM, redefinition applies code changes upon method re-entry but suspension interference fixes must be
     * effective before that moment, when the exception is thrown and the calls are still in progress, so the last
     * moment they can be applied in order to be effective during late instrumentation is before entering suspendable
     * calls.
     */
    public void applySuspensionInterferenceFixes(MethodVisitor mv) {
        db.log(LogLevel.INFO, "Applying fiber suspension interference fixes (for live instrumentation) to method %s#%s%s", className, mn.name, mn.desc);

        mv.visitCode();

        // Output parameter annotations
        if (mn.visibleParameterAnnotations != null)
            InstrumentMethod.dumpParameterAnnotations(mv, mn.visibleParameterAnnotations, true);
        if (mn.invisibleParameterAnnotations != null)
            InstrumentMethod.dumpParameterAnnotations(mv, mn.invisibleParameterAnnotations, false);

        // Output method annotations
        if (mn.visibleAnnotations != null) {
            for (final Object o : mn.visibleAnnotations) {
                final AnnotationNode an = (AnnotationNode) o;
                an.accept(mv.visitAnnotation(an.desc, true));
            }
        }

        final Map<Label, Label>
            newDualSuspTCBs = new HashMap<>(),
            newDirectSuspTCBs = new HashMap<>(),
            newReflectSuspTCBs = new HashMap<>();

        // Output try-catch blocks and before the entries for fix handlers
        for (final Object o : mn.tryCatchBlocks) {
            final TryCatchBlockNode tcb = (TryCatchBlockNode) o;
            if (tcb.handler != null) {
                final boolean any = tcb.type == null;
                final boolean se = any || isAssignableFromSuspendExecution(tcb.type);
                final boolean ite = any || isAssignableFromInvocationTargetException(tcb.type);
                if (se || ite) {
                    final Label lFixSEE = new Label();
                    mv.visitTryCatchBlock (
                        tcb.start.getLabel(), tcb.end.getLabel(), lFixSEE,
                        tcb.type != null ? tcb.type : Classes.THROWABLE_NAME
                    );

                    if (se && ite) {
                        newDualSuspTCBs.put(lFixSEE, tcb.handler.getLabel());
                    } else if (se) {
                        newDirectSuspTCBs.put(lFixSEE, tcb.handler.getLabel());
                    } else { // ite
                        newReflectSuspTCBs.put(lFixSEE, tcb.handler.getLabel());
                    }
                }
            }
            tcb.accept(mv);
        }

        // Output original method code
        for (int i = 0; i < mn.instructions.size(); i++)
            mn.instructions.get(i).accept(mv);

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Output fix handlers code
        //
        // Fixed classes can be anywhere so no static references to Quasar and avoiding synthetic methods as well
        /////////////////////////////////////////////////////////////////////////////////////////////////////////

        for (final Map.Entry<Label, Label> e : newDirectSuspTCBs.entrySet()) {
            mv.visitLabel(e.getKey());                                                                                  // -> E (catch)
            mv.visitInsn(Opcodes.DUP);                                                                                  // -> EE
            mv.visitInsn(Opcodes.DUP);                                                                                  // -> EEE

            emitCheckSuspendExec(mv);                                                                                   // -> Ez

            mv.visitJumpInsn(Opcodes.IFEQ, e.getValue());                                                               // -> E ^
            mv.visitInsn(Opcodes.ATHROW);                                                                               // -> _
        }

        for (final Map.Entry<Label, Label> e : newReflectSuspTCBs.entrySet()) {
            mv.visitLabel(e.getKey());                                                                                  // -> E (catch)
            emitCheckITE(mv, e.getValue());
        }

        for (final Map.Entry<Label, Label> e : newDualSuspTCBs.entrySet()) {
            mv.visitLabel(e.getKey());                                                                                  // -> E (catch)
            mv.visitInsn(Opcodes.DUP);                                                                                  // -> EE
            mv.visitInsn(Opcodes.DUP);                                                                                  // -> EEE

            emitCheckSuspendExec(mv);                                                                                   // -> Ez

            final Label lCheckITE = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, lCheckITE);                                                                  // -> E
            mv.visitInsn(Opcodes.ATHROW);                                                                               // -> _

            mv.visitLabel(lCheckITE);
            emitCheckITE(mv, e.getValue());
        }

        if (mn.localVariables != null) {
            for (final Object o : mn.localVariables)
                ((LocalVariableNode) o).accept(mv);
        }

        mv.visitMaxs(mn.maxStack + 3, mn.maxLocals); // Needed by ASM analysis

        mv.visitEnd();
    }

    private void emitCheckSuspendExec(MethodVisitor mv) {
                                                                                                                        // -> EE
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OBJECT_NAME, "getClass", "()L" + CLASS_NAME + ";", false);            // -> EC
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CLASS_NAME, "getName", "()L" + STRING_NAME + ";", false);             // -> ES
        mv.visitLdcInsn(SUSPEND_EXECUTION_NAME.replace('/', '.'));                                                      // -> ESS
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OBJECT_NAME, "equals", "(L" + OBJECT_NAME + ";)Z", false);            // -> Ez
        mv.visitInsn(Opcodes.SWAP);                                                                                     // -> zE
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OBJECT_NAME, "getClass", "()L" + CLASS_NAME + ";", false);            // -> zC
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CLASS_NAME, "getName", "()L" + STRING_NAME + ";", false);             // -> zS
        mv.visitLdcInsn(RUNTIME_SUSPEND_EXECUTION_NAME.replace('/', '.'));                                              // -> zSS
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, OBJECT_NAME, "equals", "(L" + OBJECT_NAME + ";)Z", false);            // -> zz
        mv.visitInsn(Opcodes.IOR);                                                                                      // -> z
    }

    private void emitCheckITE(MethodVisitor mv, Label origHandler) {
        mv.visitInsn(Opcodes.DUP);                                                                                      // -> EE
        mv.visitTypeInsn(Opcodes.INSTANCEOF, INVOCATION_TARGET_EXCEPTION_NAME);                                         // -> Ei
        mv.visitJumpInsn(Opcodes.IFEQ, origHandler);                                                                    // -> E ^
        mv.visitInsn(Opcodes.DUP);                                                                                      // -> EE
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THROWABLE_NAME, "getCause", "()L" + THROWABLE_NAME + ";", false);     // -> EE
        mv.visitInsn(Opcodes.DUP);                                                                                      // -> EEE

        emitCheckSuspendExec(mv);                                                                                       // -> Ez

        mv.visitJumpInsn(Opcodes.IFEQ, origHandler);                                                                    // -> E ^
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, THROWABLE_NAME, "getCause", "()L" + THROWABLE_NAME + ";", false);     // -> E
        mv.visitInsn(Opcodes.ATHROW);                                                                                   // -> _
    }

    private boolean canInterfereWithDirectSuspendExecution() {
        if (canInterferWithDirectSuspendExecution == null) {
            //noinspection unchecked,Convert2streamapi
            for (final TryCatchBlockNode tcb : (List<TryCatchBlockNode>) mn.tryCatchBlocks) {
                if (isAssignableFromSuspendExecution(tcb.type))
                    canInterferWithDirectSuspendExecution = true;
            }
            if (canInterferWithDirectSuspendExecution == null)
                canInterferWithDirectSuspendExecution = false;
        }
        return canInterferWithDirectSuspendExecution;
    }

    private boolean canInterfereWithReflectSuspendExecution() {
        if (canInterferWithReflectSuspendExecution == null) {
            //noinspection unchecked,Convert2streamapi
            for (final TryCatchBlockNode tcb : (List<TryCatchBlockNode>) mn.tryCatchBlocks) {
                if (isAssignableFromInvocationTargetException(tcb.type))
                    canInterferWithReflectSuspendExecution = true;
            }
            if (canInterferWithReflectSuspendExecution == null)
                canInterferWithReflectSuspendExecution = false;
        }
        return canInterferWithReflectSuspendExecution;
    }

    private static boolean isAssignableFromSuspendExecution(String type) {
        return
            SUSPEND_EXECUTION_NAME.equals(type) ||
            RUNTIME_SUSPEND_EXECUTION_NAME.equals(type) ||
            EXCEPTION_NAME.equals(type) ||
            RUNTIME_EXCEPTION_NAME.equals(type) ||
            THROWABLE_NAME.equals(type);
    }

    private static boolean isAssignableFromInvocationTargetException(String type) {
        return
            INVOCATION_TARGET_EXCEPTION_NAME.equals(type) ||
            REFLECTIVE_OPERATION_EXCEPTION_NAME.equals(type) ||
            EXCEPTION_NAME.equals(type) ||
            THROWABLE_NAME.equals(type);
    }
}

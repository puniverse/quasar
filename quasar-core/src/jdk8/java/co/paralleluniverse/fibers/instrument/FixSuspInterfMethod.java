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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

/**
 * Since it's needed only by live instrumentation (JDK9+), it's just an empty stub in JDKs pre-9
 *
 * @author circlespainter
 */
public class FixSuspInterfMethod {
    public FixSuspInterfMethod(MethodDatabase db, String className, MethodNode mn) {}

    public final boolean isNeeded() {
        return false;
    }

    public final void applySuspensionInterferenceFixes(MethodVisitor mv) {}
}

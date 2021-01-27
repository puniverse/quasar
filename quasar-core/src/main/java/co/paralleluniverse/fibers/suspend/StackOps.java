/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2021, R3 Ltd. All rights reserved.
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
package co.paralleluniverse.fibers.suspend;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class StackOps {
    private static volatile Supplier<? extends StackOps> GET_STACK;

    public static StackOps getStack() {
        final Supplier<? extends StackOps> getter = GET_STACK;
        return getter != null ? getter.get() : null;
    }

    public abstract int nextMethodEntry();
    public abstract boolean isFirstInStackOrPushed();
    public abstract void pushMethod(int entry, int numSlots);
    public abstract void popMethod(int slots);
    public abstract void postRestore() throws SuspendExecution, InterruptedException;

    public abstract int getInt(int idx);
    public abstract float getFloat(int idx);
    public abstract long getLong(int idx);
    public abstract double getDouble(int idx);
    public abstract Object getObject(int idx);

    public abstract void push(int value, int idx);
    public abstract void push(float value, int idx);
    public abstract void push(long value, int idx);
    public abstract void push(double value, int idx);
    public abstract void push(Object value, int idx);

    public static void push(int value, StackOps s, int idx) {
        s.push(value, idx);
    }

    public static void push(float value, StackOps s, int idx) {
        s.push(value, idx);
    }

    public static void push(long value, StackOps s, int idx) {
        s.push(value, idx);
    }

    public static void push(double value, StackOps s, int idx) {
        s.push(value, idx);
    }

    public static void push(Object value, StackOps s, int idx) {
        s.push(value, idx);
    }
}

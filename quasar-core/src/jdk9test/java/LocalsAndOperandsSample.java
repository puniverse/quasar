/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
/*
 * Adapted by circlespainter on 2015-12-28
 */
import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocalsAndOperandsSample {
    private static Class<?> liveStackFrameClass;
    private static Class<?> primitiveValueClass;
    private static Method getLocals;
    private static Method getOperands;
    private static Method getMonitors;
    private static Method primitiveType;

    public static void main(String... args) throws Throwable {
        liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");

        // no access to local and operands.
        final StackWalker sw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

        MethodHandles.lookup().unreflect(LocalsAndOperandsSample.class.getDeclaredMethod("test", Long.TYPE, Double.TYPE)).invoke(new LocalsAndOperandsSample(sw, false), 4L, 5.6);

        // access to local and operands.
        primitiveValueClass = Class.forName("java.lang.LiveStackFrame$PrimitiveValue");

        getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
        getLocals.setAccessible(true);

        getOperands = liveStackFrameClass.getDeclaredMethod("getStack");
        getOperands.setAccessible(true);

        getMonitors = liveStackFrameClass.getDeclaredMethod("getMonitors");
        getMonitors.setAccessible(true);

        primitiveType = primitiveValueClass.getDeclaredMethod("type");
        primitiveType.setAccessible(true);

        final Class<?> extendedOptionClass = Class.forName("java.lang.StackWalker$ExtendedOption");
        final Method ewsNI = StackWalker.class.getDeclaredMethod("newInstance", Set.class, extendedOptionClass);
        ewsNI.setAccessible(true);
        final Set<StackWalker.Option> s = new HashSet<>();
        s.add(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        s.add(StackWalker.Option.SHOW_REFLECT_FRAMES);
        final Field f = extendedOptionClass.getDeclaredField("LOCALS_AND_OPERANDS");
        f.setAccessible(true);
        final StackWalker esw = (StackWalker) ewsNI.invoke(null, s, f.get(null));

        LocalsAndOperandsSample.class.getDeclaredMethod("test", Long.TYPE, Double.TYPE).invoke(new LocalsAndOperandsSample(esw, true), 4L, 5.6);
    }

    private final StackWalker walker;
    private final boolean extended;

    private LocalsAndOperandsSample(StackWalker walker, boolean extended) {
        this.walker = walker;
        this.extended = extended;
    }

    public synchronized void test(long l, double d) throws Exception {
        final List<StackWalker.StackFrame> frames = walker.walk(s -> s.collect(Collectors.toList()));
        if (extended) {
            for (final StackWalker.StackFrame f : frames) {
                System.out.println("frame: " + f);
                final Object[] locals = (Object[]) getLocals.invoke(f);
                for (int i = 0; i < locals.length; i++) {
                    System.out.format("local %d: %s type %s%n", i, locals[i], type(locals[i]));
                }

                final Object[] operands = (Object[]) getOperands.invoke(f);
                for (int i = 0; i < operands.length; i++) {
                    System.out.format("operand %d: %s type %s%n", i, operands[i], type(operands[i]));
                }

                final Object[] monitors = (Object[]) getMonitors.invoke(f);
                for (int i = 0; i < monitors.length; i++) {
                    System.out.format("monitor %d: %s%n", i, monitors[i]);
                }
            }
        } else {
            for (final StackFrame f : frames) {
                if (liveStackFrameClass.isInstance(f))
                    throw new RuntimeException("should not be LiveStackFrame");
                else
                    System.out.println("StackFrame.toString(): " + f.toString());
            }
        }
    }

    private static String type(Object o) throws Exception {
        if (primitiveValueClass.isInstance(o)) {
            final char c = (char) primitiveType.invoke(o);
            return String.valueOf(c);
        } else {
            return o.getClass().getName();
        }
    }
}

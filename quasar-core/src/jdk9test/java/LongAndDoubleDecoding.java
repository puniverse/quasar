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
 * Adapted by circlespainter on 2016-01-19
 */

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LongAndDoubleDecoding {
    private static final long LONG_VAL = 4L;
    private static final double DOUBLE_VAL = 5.6D;

    private static final long INT_LONG_MASK = 0xFFFFFFFFL;
    private static final String INT_LONG_MASK_S = "0xFFFFFFFFL";
    private static final int INT_BITLEN = 32;

    private static Method getLocals;
    private static Method intValue;
    private static StackWalker esw;

    public static void main(String... args) throws Throwable {
        final Class<?> liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
        getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
        getLocals.setAccessible(true);

        final Class<?> primitiveValueClass = Class.forName("java.lang.LiveStackFrame$PrimitiveValue");
        intValue = primitiveValueClass.getDeclaredMethod("intValue");
        intValue.setAccessible(true);

        final Class<?> extendedOptionClass = Class.forName("java.lang.StackWalker$ExtendedOption");
        final Method ewsNI = StackWalker.class.getDeclaredMethod("newInstance", Set.class, extendedOptionClass);
        ewsNI.setAccessible(true);
        final Field f = extendedOptionClass.getDeclaredField("LOCALS_AND_OPERANDS");
        f.setAccessible(true);
        esw = (StackWalker) ewsNI.invoke(null, new HashSet<>(), f.get(null));

        testLong(LONG_VAL);
        o();
        testDouble(DOUBLE_VAL);
        o();
        testLongAndDouble(LONG_VAL, DOUBLE_VAL);
        o();
        testDoubleAndLong(DOUBLE_VAL, LONG_VAL);
    }

    /** @noinspection UnusedParameters*/
    public static synchronized void testLong(long l) throws Exception {
        final List<StackFrame> frames = esw.walk(s -> s.collect(Collectors.toList()));
        final Object[] locals = (Object[]) getLocals.invoke(frames.get(0) /* top */);
        o("`testLong` locals: " + Arrays.toString(locals));
        final int locals_0 = intVal(locals[0]), locals_1 = intVal(locals[1]);
        logValAndHalves(LONG_VAL, "Expected long", locals_0, locals_1);
        decodeLong(locals_0, locals_1);
    }

    /** @noinspection UnusedParameters*/
    public static synchronized void testDouble(double d) throws Exception {
        final List<StackFrame> frames = esw.walk(s -> s.collect(Collectors.toList()));
        final Object[] locals = (Object[]) getLocals.invoke(frames.get(0) /* top */);
        o("`testDouble` locals: " + Arrays.toString(locals));
        final int locals_0 = intVal(locals[0]), locals_1 = intVal(locals[1]);
        logValAndHalves(DOUBLE_VAL, "Expected double", locals_0, locals_1);
        decodeDouble(locals_0, locals_1);
    }

    /** @noinspection UnusedParameters*/
    public static synchronized void testLongAndDouble(long l, double d) throws Exception {
        final List<StackFrame> frames = esw.walk(s -> s.collect(Collectors.toList()));
        final Object[] locals = (Object[]) getLocals.invoke(frames.get(0) /* top */);
        o("`testLongAndDouble` locals: " + Arrays.toString(locals));
        final int locals_0 = intVal(locals[0]), locals_1 = intVal(locals[1]);
        final int locals_2 = intVal(locals[2]), locals_3 = intVal(locals[3]);
        logValAndHalves(LONG_VAL, "Expected long",  locals_0, locals_1);
        decodeLong(locals_0, locals_1);
        logValAndHalves(DOUBLE_VAL, "Expected double", locals_2, locals_3);
        decodeDouble(locals_2, locals_3);
    }

    /** @noinspection UnusedParameters*/
    public static synchronized void testDoubleAndLong(double d, long l) throws Exception {
        final List<StackFrame> frames = esw.walk(s -> s.collect(Collectors.toList()));
        final Object[] locals = (Object[]) getLocals.invoke(frames.get(0) /* top */);
        o("`testDoubleAndLong` locals: " + Arrays.toString(locals));
        final int locals_0 = intVal(locals[0]), locals_1 = intVal(locals[1]);
        final int locals_2 = intVal(locals[2]), locals_3 = intVal(locals[3]);
        logValAndHalves(DOUBLE_VAL, "Expected double", locals_0, locals_1);
        decodeDouble(locals_0, locals_1);
        logValAndHalves(LONG_VAL, "Expected long", locals_2, locals_3);
        decodeLong(locals_2, locals_3);
    }

    private static void decodeLong(int half1, int half2) {
        final long res1 = ((long) half1 << INT_BITLEN) | ((long) half2 & INT_LONG_MASK);
        log(res1, "(((long) half1) << " + INT_BITLEN + ") | ((long) half2 & " + INT_LONG_MASK_S + ")");
        final long res2 = ((long) half2 << INT_BITLEN) | ((long) half1 & INT_LONG_MASK);
        log(res2, "(((long) half2) <<" + INT_BITLEN + ") | ((long) half1 & " + INT_LONG_MASK_S + ")");
    }

    private static void decodeDouble(int half1, int half2) {
        final double res1 = Double.longBitsToDouble((((long) half1) << INT_BITLEN) | ((long) half2));
        log(res1, "Double.longBitsToDouble((((long) half1) << " + INT_BITLEN + ") | ((long) half2))");
        final double res2 = Double.longBitsToDouble((((long) half2) << INT_BITLEN) | ((long) half1));
        log(res2, "Double.longBitsToDouble((((long) half2) << " + INT_BITLEN + ") | ((long) half1))");
        final double res3 = Double.longBitsToDouble((((long) half1) << INT_BITLEN) | ((long) half2 & INT_LONG_MASK));
        log(res3, "Double.longBitsToDouble((((long) half1) << " + INT_BITLEN + ") | ((long) half2 & " + INT_LONG_MASK_S + "))");
        final double res4 = Double.longBitsToDouble((((long) half2) << INT_BITLEN) | ((long) half1 & INT_LONG_MASK));
        log(res4, "Double.longBitsToDouble((((long) half2) << " + INT_BITLEN + ") | ((long) half1 & " + INT_LONG_MASK_S + "))");
    }

    private static void logValAndHalves(Object val, String l, int half1, int half2) {
        if (val instanceof Long)
            log((long) val, l);
        else if (val instanceof Double)
            log((double) val, l);
        else
            throw new IllegalArgumentException("Only long and double supported");
        log(half1, "half1");
        log(half2, "half2");
    }

    private static void log(double val, String l) {
        log(Double.doubleToRawLongBits(val), l);
    }

    private static void log(long val, String l) {
        System.err.println (
            "\t* " + l + ":" +
            "\n\t\tSTR: " + val +
            "\n\t\tHEX: " + Long.toHexString(val) +
            "\n\t\tBIN: " + Long.toBinaryString(val)
        );
    }

    private static int intVal(Object l) throws IllegalAccessException, InvocationTargetException {
        return (int) intValue.invoke(l);
    }

    private static void o() {
        o("");
    }

    private static void o(String s) {
        System.err.println(s);
    }

    private LongAndDoubleDecoding() {}
}

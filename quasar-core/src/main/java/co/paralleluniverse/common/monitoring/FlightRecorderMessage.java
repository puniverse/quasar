/*
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.monitoring;

import co.paralleluniverse.common.util.Debug;
import java.util.Arrays;
import java.util.Formatter;

/**
 *
 * @author pron
 */
public class FlightRecorderMessage {
    private final String clazz;
    private final String method;
    private final String format;
    private final Object[] args;
    private final StackTraceElement[] stackTrace;

    public FlightRecorderMessage(String clazz, String method, String format, Object[] args) {
        this.clazz = clazz;
        this.method = method;
        this.format = format;
        this.args = args;
        if (args != null) {
            for (int i = 0; i < args.length; i++)
                args[i] = recordingDouble(args[i]);
        }
        this.stackTrace = (Debug.isRecordStackTraces() ? Thread.currentThread().getStackTrace() : null);
    }

    @Override
    public String toString() {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && args[i].getClass().isArray()) {
                    final Class<?> t = args[i].getClass().getComponentType();
                    if (t == Boolean.TYPE)
                        args[i] = Arrays.toString((boolean[]) args[i]);
                    else if (t == Byte.TYPE)
                        args[i] = Arrays.toString((byte[]) args[i]);
                    else if (t == Character.TYPE)
                        args[i] = Arrays.toString((char[]) args[i]);
                    else if (t == Short.TYPE)
                        args[i] = Arrays.toString((short[]) args[i]);
                    else if (t == Integer.TYPE)
                        args[i] = Arrays.toString((int[]) args[i]);
                    else if (t == Long.TYPE)
                        args[i] = Arrays.toString((long[]) args[i]);
                    else if (t == Float.TYPE)
                        args[i] = Arrays.toString((float[]) args[i]);
                    else if (t == Double.TYPE)
                        args[i] = Arrays.toString((double[]) args[i]);
                    else
                        args[i] = Arrays.toString((Object[]) args[i]);
                }
            }
        }
        Object[] ps = (args != null ? new Object[args.length + 2] : new Object[2]);
        ps[0] = clazz;
        ps[1] = method;
        if (args != null && args.length > 0)
            System.arraycopy(args, 0, ps, 2, args.length);
        try {
            final Formatter formatter = new Formatter();
            formatter.format("%s.%s " + format, ps);
            String string = formatter.toString();
            if (stackTrace != null)
                string += " at: " + Arrays.toString(stackTrace);
            return string;
        } catch (Exception e) {
            //System.err.println("ERROR in formatting this message: " + clazz + "." + method + " " + format;
            //e.printStackTrace();
            return "ERROR in formatting this message: " + clazz + "." + method + " " + format;
        }
    }

    public String getClazz() {
        return clazz;
    }

    public String getMethod() {
        return method;
    }

    public Object recordingDouble(Object obj) {
        try {
            // Use of Guava classes has caused problems with conflicting Guava versions
            if (obj instanceof RecordingDouble)
                return ((RecordingDouble) obj).getRecordingDouble();
            if (obj instanceof java.util.Map)
                return new java.util.HashMap((java.util.Map) obj); // com.google.common.collect.ImmutableMap.copyOf((java.util.Map) obj);
            if (obj instanceof java.util.Set)
                return new java.util.HashSet((java.util.Set) obj); // com.google.common.collect.ImmutableSet.copyOf((java.util.Set) obj);
            if (obj instanceof java.util.List)
                return new java.util.ArrayList((java.util.List) obj); // com.google.common.collect.ImmutableList.copyOf((java.util.List) obj);
            return obj;
        } catch (Exception e) {
            return "<ERROR: " + e.getMessage() + '>';
        }
    }
}

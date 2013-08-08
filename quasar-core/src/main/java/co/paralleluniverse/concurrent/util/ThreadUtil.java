/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.concurrent.util;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Map;

/**
 *
 * @author pron
 */
public final class ThreadUtil {
    public static void dumpThreads() {
        final Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        for (Thread thread : threads.keySet())
            System.out.println((thread.isDaemon() ? "DAEMON  " : "        ") + thread.getName());

        // final ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
//        for(ThreadInfo thread : threads) {
//            System.out.println(thread.getThreadName() + ": ");
//        }
    }

    public static void dumpThreadLocals() {
        try {
            final Object threadLocals = threadLocalsField.get(Thread.currentThread());
            System.out.println(getThreadLocalsString(threadLocals));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static void dumpInheritableThreadLocals() {
        try {
            final Object threadLocals = inheritableThreadLocalsField.get(Thread.currentThread());
            System.out.println(getThreadLocalsString(threadLocals));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public static String getThreadLocalsString() {
        try {
            final Object threadLocals = threadLocalsField.get(Thread.currentThread());
            return(getThreadLocalsString(threadLocals));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
    
    public static String getInheritableThreadLocalsString() {
        try {
            final Object threadLocals = inheritableThreadLocalsField.get(Thread.currentThread());
            return(getThreadLocalsString(threadLocals));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
    
    public static String getThreadLocalsString(Object threadLocals) {
        try {
            final StringBuilder sb = new StringBuilder();

            final Object table = threadLocalMapTableField.get(threadLocals);
            final int threadLocalCount = Array.getLength(table);
            for (int i = 0; i < threadLocalCount; i++) {
                Object entry = Array.get(table, i);
                if (entry != null) {
                    Object value = threadLocalMapEntryValueField.get(entry);

                    sb.append(((Reference)entry).get()).append(": ");
                    sb.append(value != null ? value : "null");
                    sb.append(", ");
                }
            }

            return sb.toString();
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
    private static final Field threadLocalsField;
    private static final Field inheritableThreadLocalsField;
    private static final Field threadLocalMapTableField;
    private static final Field threadLocalMapEntryValueField;

    static {
        try {
            threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);

            inheritableThreadLocalsField = Thread.class.getDeclaredField("inheritableThreadLocals");
            inheritableThreadLocalsField.setAccessible(true);

            Class threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            threadLocalMapTableField = threadLocalMapClass.getDeclaredField("table");
            threadLocalMapTableField.setAccessible(true);

            Class threadLocalMapEntryClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap$Entry");
            threadLocalMapEntryValueField = threadLocalMapEntryClass.getDeclaredField("value");
            threadLocalMapEntryValueField.setAccessible(true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private ThreadUtil() {
    }
}

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

import co.paralleluniverse.fibers.LiveInstrumentation;
import com.google.common.collect.Lists;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author circlespainter
 */
public final class FrameTypesKB {
    /////////////////////////////////////////////////////////////////////
    // Live instrumentation support temporary static analysis info caches
    /////////////////////////////////////////////////////////////////////

    private static final Map<String, List<Type>> operandStackTypesCacheL = new HashMap<>(), localTypesCacheL = new HashMap<>();
    private static final Map<String, Type[]> operandStackTypesCache = new HashMap<>(), localTypesCache = new HashMap<>();

    private static String getCacheMethodCallSiteId(String className, String name, String desc, String callSiteIdx) {
        return className.replace('.', '/') + "::" + name + desc + "#" + callSiteIdx;
    }

    public static Type[] getOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        return operandStackTypesCache.get(getCacheMethodCallSiteId(className, name, desc, callSiteIdx));
    }

    public static Type[] getLocalTypes(String className, String name, String desc, String callSiteIdx) {
        return localTypesCache.get(getCacheMethodCallSiteId(className, name, desc, callSiteIdx));
    }

    public static void addOperandStackType(String className, String name, String desc, String callSiteIdx, Type t) {
        if (LiveInstrumentation.ACTIVE && t != null) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = operandStackTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            l.add(t);
            operandStackTypesCacheL.put(id, l);
        }
    }

    public static void addLocalType(String className, String name, String desc, String callSiteIdx, Type t) {
        if (LiveInstrumentation.ACTIVE && t != null) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = localTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            l.add(t);
            localTypesCacheL.put(id, l);
        }
    }

    public static void sealOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = operandStackTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            final Type[] ta = new Type[l.size()];
            Lists.reverse(l).toArray(ta);
            operandStackTypesCache.put(id, ta);
            operandStackTypesCacheL.remove(id);
        }
    }

    public static void sealLocalTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = localTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            final Type[] ta = new Type[l.size()];
            l.toArray(ta);
            localTypesCache.put(id, ta);
            localTypesCacheL.remove(id);
        }
    }

    public static void clearOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            operandStackTypesCacheL.remove(id);
            operandStackTypesCache.remove(id);
        }
    }

    public static void clearLocalTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getCacheMethodCallSiteId(className, name, desc, callSiteIdx);
            localTypesCacheL.remove(id);
            localTypesCache.remove(id);
        }
    }

    private FrameTypesKB(){}
}

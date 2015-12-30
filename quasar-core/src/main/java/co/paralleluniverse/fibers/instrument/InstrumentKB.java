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

import co.paralleluniverse.common.util.ConcurrentSet;
import co.paralleluniverse.fibers.LiveInstrumentation;
import com.google.common.collect.Lists;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author circlespainter
 */
public final class InstrumentKB {
    ///////////////////////////////////////////////////////////////////////////
    // Temporary information used during agent-time and/or live instrumentation
    ///////////////////////////////////////////////////////////////////////////

    /** @noinspection Convert2Diamond*/
    private static final ConcurrentSet<String> frameTypesAskedClasses = new ConcurrentSet<String>(new ConcurrentHashMap<String, Object>());
    private static final Map<String, List<Type>> frameOperandStackTypesCacheL = new ConcurrentHashMap<>(), frameLocalTypesCacheL = new ConcurrentHashMap<>();
    private static final Map<String, Type[]> frameOperandStackTypesCache = new ConcurrentHashMap<>(), frameLocalTypesCache = new ConcurrentHashMap<>();

    private static String getFrameTypesCacheCallSiteId(String className, String name, String desc, String callSiteIdx) {
        return className.replace('.', '/') + "::" + name + desc + "#" + callSiteIdx;
    }

    private static String getPreInstrOffsetsCacheMethodId(String className, String name, String desc) {
        return className.replace('.', '/') + "::" + name + desc;
    }

    public static Type[] getFrameOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        return frameOperandStackTypesCache.get(getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx));
    }

    public static Type[] getFrameLocalTypes(String className, String name, String desc, String callSiteIdx) {
        return frameLocalTypesCache.get(getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx));
    }

    public static void addFrameOperandStackType(String className, String name, String desc, String callSiteIdx, Type t) {
        if (LiveInstrumentation.ACTIVE && askedFrameTypesRecording(className.replace('.', '/'))) {
            if (t != null) {
                final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
                List<Type> l = frameOperandStackTypesCacheL.get(id);
                if (l == null)
                    l = new ArrayList<>();
                l.add(t);
                frameOperandStackTypesCacheL.put(id, l);
            }
        }
    }

    public static void addFrameLocalType(String className, String name, String desc, String callSiteIdx, Type t) {
        if (LiveInstrumentation.ACTIVE && askedFrameTypesRecording(className.replace('.', '/'))) {
            if (t != null) {
                final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
                List<Type> l = frameLocalTypesCacheL.get(id);
                if (l == null)
                    l = new ArrayList<>();
                l.add(t);
                frameLocalTypesCacheL.put(id, l);
            }
        }
    }

    public static void askFrameTypesRecording(String className) {
        frameTypesAskedClasses.add(className.replace('.', '/'));
    }

    private static boolean askedFrameTypesRecording(String className) {
        return frameTypesAskedClasses.contains(className.replace('.', '/'));
    }

    public static void sealFrameOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE && askedFrameTypesRecording(className.replace('.', '/'))) {
            final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = frameOperandStackTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            final Type[] ta = new Type[l.size()];
            Lists.reverse(l).toArray(ta);
            frameOperandStackTypesCache.put(id, ta);
            frameOperandStackTypesCacheL.remove(id);
        }
    }

    public static void sealFrameLocalTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE && askedFrameTypesRecording(className.replace('.', '/'))) {
            final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
            List<Type> l = frameLocalTypesCacheL.get(id);
            if (l == null)
                l = new ArrayList<>();
            final Type[] ta = new Type[l.size()];
            l.toArray(ta);
            frameLocalTypesCache.put(id, ta);
            frameLocalTypesCacheL.remove(id);
        }
    }

    public static void clearFrameOperandStackTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
            frameOperandStackTypesCacheL.remove(id);
            frameOperandStackTypesCache.remove(id);
        }
    }

    public static void clearFrameLocalTypes(String className, String name, String desc, String callSiteIdx) {
        if (LiveInstrumentation.ACTIVE) {
            final String id = getFrameTypesCacheCallSiteId(className, name, desc, callSiteIdx);
            frameLocalTypesCacheL.remove(id);
            frameLocalTypesCache.remove(id);
        }
    }


    private static final Map<String, int[]> methodPreInstrOffsetsCache = new ConcurrentHashMap<>();
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    public static void setMethodPreInstrumentationOffsets(String className, String name, String desc, int[] offsets) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        methodPreInstrOffsetsCache.put(id, offsets);
    }

    public static int[] getMethodPreInstrumentationOffsets(String className, String name, String desc) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        final int[] ret = methodPreInstrOffsetsCache.get(id);
        return ret == null ? EMPTY_INT_ARRAY : ret;
    }

    public static int[] removeMethodPreInstrumentationOffsets(String className, String name, String desc) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        return methodPreInstrOffsetsCache.remove(id);
    }


    private InstrumentKB(){}
}

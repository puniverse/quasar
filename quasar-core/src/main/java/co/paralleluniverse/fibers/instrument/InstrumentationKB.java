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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author circlespainter
 */
public final class InstrumentationKB {
    private static final Map<String, int[]> methodPreInstrOffsetsCache = new ConcurrentHashMap<>();

    private static String getPreInstrOffsetsCacheMethodId(String className, String name, String desc) {
        return className.replace('.', '/') + "::" + name + desc;
    }

    public static void setMethodPreInstrumentationOffsets(String className, String name, String desc, int[] offsets) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        methodPreInstrOffsetsCache.put(id, offsets);
    }

    public static int[] getMethodPreInstrumentationOffsets(String className, String name, String desc) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        return methodPreInstrOffsetsCache.get(id);
    }

    public static int[] removeMethodPreInstrumentationOffsets(String className, String name, String desc) {
        final String id = getPreInstrOffsetsCacheMethodId(className, name, desc);
        return methodPreInstrOffsetsCache.remove(id);
    }

    private InstrumentationKB(){}
}

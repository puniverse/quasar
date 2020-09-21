/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * This class contains hard-coded values with the names of the classes and methods relevant for instrumentation.
 *
 * @author pron
 */
final class Classes {
    private static final BlockingMethod[] BLOCKING_METHODS = {
        new BlockingMethod("java/lang/Thread", "sleep", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Thread", "join", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Object", "wait", "()V", "(J)V", "(JI)V")
    };

    private static final Set<String> yieldMethods = Set.of(
        "park", "yield", "parkAndUnpark", "yieldAndUnpark", "parkAndSerialize", "parkAndCustomSerialize"
    );

    // Don't load these classes
    static final String STACK_NAME       = /*Stack.class.getName().replace('.', '/')*/ "co/paralleluniverse/fibers/Stack";
    static final String FIBER_CLASS_NAME = /*Fiber.class.getName().replace('.', '/')*/ "co/paralleluniverse/fibers/Fiber";
    static final String STRAND_NAME      = /*Strand.class.getName().replace('.', '/')*/"co/paralleluniverse/strands/Strand";
    static final String FIBER_HELPER_NAME = "co/paralleluniverse/fibers/FiberHelper";

    static final String THROWABLE_NAME         = Throwable.class.getName().replace('.', '/');
    static final String EXCEPTION_NAME         = Exception.class.getName().replace('.', '/');
    static final String RUNTIME_EXCEPTION_NAME = RuntimeException.class.getName().replace('.', '/');

    static final String RUNTIME_SUSPEND_EXECUTION_NAME = "co/paralleluniverse/fibers/RuntimeSuspendExecution";
    static final String UNDECLARED_THROWABLE_NAME      = UndeclaredThrowableException.class.getName().replace('.', '/');
    static final String SUSPEND_EXECUTION_NAME         = "co/paralleluniverse/fibers/SuspendExecution";
    
    // computed
    static final String SUSPENDABLE_DESC = "Lco/paralleluniverse/fibers/Suspendable;";
    static final String DONT_INSTRUMENT_DESC = Type.getDescriptor(DontInstrument.class);
    static final String INSTRUMENTED_DESC = "Lco/paralleluniverse/fibers/Instrumented;";
    static final String LAMBDA_METHOD_PREFIX = "lambda$";

    static boolean isYieldMethod(String className, String methodName) {
        return FIBER_CLASS_NAME.equals(className) && yieldMethods.contains(methodName);
    }

    /**
     * @noinspection UnusedParameters
     */
    static boolean isAllowedToBlock(String className, String methodName) {
        return STRAND_NAME.equals(className);
    }

    static int blockingCallIdx(MethodInsnNode ins) {
        for (int i = 0, n = BLOCKING_METHODS.length; i < n; i++) {
            if (BLOCKING_METHODS[i].match(ins))
                return i;
        }
        return -1;
    }

    private static class BlockingMethod {
        private final String owner;
        private final String name;
        private final String[] descs;

        private BlockingMethod(String owner, String name, String... descs) {
            this.owner = owner;
            this.name = name;
            this.descs = descs;
        }

        boolean match(MethodInsnNode min) {
            if (owner.equals(min.owner) && name.equals(min.name)) {
                for (String desc : descs) {
                    if (desc.equals(min.desc)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    static int[] toIntArray(List<Integer> suspOffsetsAfterInstrL) {
        if (suspOffsetsAfterInstrL == null)
            return null;

        final List<Integer> suspOffsetsAfterInstrLFiltered = new ArrayList<>(suspOffsetsAfterInstrL.size());
        for (final Integer i : suspOffsetsAfterInstrL) {
            if (i != null)
                suspOffsetsAfterInstrLFiltered.add(i);
        }

        final int[] ret = new int[suspOffsetsAfterInstrLFiltered.size()];
        int j = 0;
        for (final Integer i : suspOffsetsAfterInstrLFiltered) {
            ret[j] = i;
            j++;
        }

        return ret;
    }

    private Classes() {
    }
}

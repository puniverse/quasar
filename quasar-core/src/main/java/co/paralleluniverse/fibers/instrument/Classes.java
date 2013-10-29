/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * This class contains hard-coded values with the names of the classes and methods relevant for instrumentation.
 *
 * @author pron
 */
final class Classes {
    static final String EXCEPTION_NAME = "co/paralleluniverse/fibers/SuspendExecution";
    static final String ANNOTATION_NAME = "co/paralleluniverse/fibers/Suspendable";
    static final String COROUTINE_NAME = "co/paralleluniverse/fibers/Fiber"; //Type.getInternalName(COROUTINE_CLASS);
    private static final String STRAND_NAME = "co/paralleluniverse/strands/Strand"; //Type.getInternalName(COROUTINE_CLASS);
    static final String STACK_NAME = "co/paralleluniverse/fibers/Stack";
    //static final String EXCEPTION_INSTANCE_NAME = "exception_instance_not_for_user_code";
    private static final BlockingMethod BLOCKING_METHODS[] = {
        new BlockingMethod("java/lang/Thread", "sleep", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Thread", "join", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Object", "wait", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lock", "()V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lockInterruptibly", "()V"),};
    // computed
    static final String EXCEPTION_DESC = "L" + EXCEPTION_NAME + ";";
    static final String ANNOTATION_DESC = "L" + ANNOTATION_NAME + ";";

    static boolean isYieldMethod(String className, String methodName) {
        return COROUTINE_NAME.equals(className) && ("park".equals(methodName) || "yield".equals(methodName));
    }

    public static boolean isAllowedToBlock(String className, String methodName) {
        return STRAND_NAME.equals(className);
    }

    public static int isBlockingCall(MethodInsnNode ins) {
        for (int i = 0, n = BLOCKING_METHODS.length; i < n; i++) {
            if (BLOCKING_METHODS[i].match(ins))
                return i;
        }
        return -1;
    }

    static class BlockingMethod {
        private final String owner;
        private final String name;
        private final String[] descs;

        private BlockingMethod(String owner, String name, String... descs) {
            this.owner = owner;
            this.name = name;
            this.descs = descs;
        }

        public boolean match(MethodInsnNode min) {
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

    private Classes() {
    }
}

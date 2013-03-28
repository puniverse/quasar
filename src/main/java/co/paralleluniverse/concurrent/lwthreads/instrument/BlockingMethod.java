/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.lwthreads.instrument;

import org.objectweb.asm.tree.MethodInsnNode;

/**
 *
 * @author pron
 */
class BlockingMethod {
    private static final BlockingMethod BLOCKING_METHODS[] = {
        new BlockingMethod("java/lang/Thread", "sleep", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Thread", "join", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/lang/Object", "wait", "()V", "(J)V", "(JI)V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lock", "()V"),
        new BlockingMethod("java/util/concurrent/locks/Lock", "lockInterruptibly", "()V"),};

    public static int isBlockingCall(MethodInsnNode ins) {
        for (int i = 0, n = BLOCKING_METHODS.length; i < n; i++) {
            if (BLOCKING_METHODS[i].match(ins)) {
                return i;
            }
        }
        return -1;
    }
    //
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

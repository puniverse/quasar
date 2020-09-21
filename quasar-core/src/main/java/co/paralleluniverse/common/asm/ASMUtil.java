package co.paralleluniverse.common.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static co.paralleluniverse.asm.ASMUtil.getClassNode;
import static co.paralleluniverse.common.resource.ClassLoaderUtil.classToSlashed;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

public final class ASMUtil {
    public static final int ASMAPI = Opcodes.ASM7;

    public static void findMethod(
        Class<?> clazz,
        String targetMethodName,
        int targetLineNumber,
        AtomicReference<String> descriptor
    ) throws IOException {
        co.paralleluniverse.asm.ASMUtil.accept(clazz, SKIP_FRAMES, new ClassVisitor(ASMAPI) {
            @Override
            public MethodVisitor visitMethod(int access, String name, final String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if (descriptor.get() == null && targetMethodName.equals(name)) {
                    mv = new MethodVisitor(api, mv) {
                        int minLine = Integer.MAX_VALUE, maxLine = Integer.MIN_VALUE;

                        @Override
                        public void visitLineNumber(int line, Label start) {
                            if (line < minLine)
                                minLine = line;
                            if (line > maxLine)
                                maxLine = line;
                        }

                        @Override
                        public void visitEnd() {
                            if (minLine <= targetLineNumber && maxLine >= targetLineNumber)
                                descriptor.set(desc);
                            super.visitEnd();
                        }
                    };
                }
                return mv;
            }
        });
    }

    public static boolean isAssignableFrom(Class<?> supertype, String className, ClassLoader cl) {
        return isAssignableFrom0(classToSlashed(supertype), classToSlashed(className), cl);
    }

    public static boolean isAssignableFrom(String supertypeName, String className, ClassLoader cl) {
        return isAssignableFrom0(classToSlashed(supertypeName), classToSlashed(className), cl);
    }

    private static boolean isAssignableFrom0(String supertypeName, String className, ClassLoader cl) {
        try {
            if (className == null)
                return false;
            if (supertypeName.equals(className))
                return true;
            ClassNode cn = getClassNode(className, cl, true);

            if (supertypeName.equals(cn.superName))
                return true;
            if (isAssignableFrom0(supertypeName, cn.superName, cl))
                return true;

            if (cn.interfaces != null) {
                for (String iface : cn.interfaces) {
                    if (supertypeName.equals(iface))
                        return true;
                    if (isAssignableFrom0(supertypeName, iface, cl))
                        return true;
                }
            }
            return false;
        } catch (IOException e) {
            // e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String getDescriptor(Member m) {
        if (m instanceof Method)
            return Type.getMethodDescriptor((Method) m);
        if (m instanceof Constructor)
            return Type.getConstructorDescriptor((Constructor<?>) m);
        throw new IllegalArgumentException("Not an executable: " + m);
    }

    public static String getReadableDescriptor(String descriptor) {
        Type[] types = Type.getArgumentTypes(descriptor);
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < types.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(types[i].getClassName());
        }
        sb.append(')');
        return sb.toString();
    }

    private ASMUtil() {
    }
}

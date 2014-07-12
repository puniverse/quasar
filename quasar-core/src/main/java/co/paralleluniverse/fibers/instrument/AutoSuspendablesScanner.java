package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AutoSuspendablesScanner {
    SetMultimap<String, String> callers;
    SetMultimap<String, String> supers;
    private final ClassLoader cl;
    private Set<String> suspendables;
    private Set<String> superSuspendables;

    public AutoSuspendablesScanner(final ClassLoader classLoader) {
        this.cl = classLoader;
        this.supers = AutoSuspendablesScanner.<String, String>newHashMultimap();
        this.callers = AutoSuspendablesScanner.<String, String>newHashMultimap();
        mapCallersAndSupers();
        mapSuspendables();
    }

    private void mapSuspendables() {
        Queue<String> q = Queues.newArrayDeque();
        suspendables = new HashSet<>();
        superSuspendables = new HashSet<>();
        for (String callee : callers.keySet()) {
            final String className = getClassName(callee);
            final String methodName = getMethodName(callee);
            if (Classes.isYieldMethod(className, methodName)) {
                q.add(callee);
                suspendables.add(callee);
            }
        }
        while (!q.isEmpty()) {
            final String node = q.poll();
            for (String superCls : supers.get(getClassName(node))) {
                final String superMethod = superCls + "." + getMethodDescName(node);
                if (callers.keySet().contains(superMethod) && !suspendables.contains(superMethod) && !superSuspendables.contains(superMethod)) {
                    q.add(superMethod);
                    superSuspendables.add(superMethod);
                }
            }
            for (String caller : callers.get(node)) {
                if (!suspendables.contains(caller)) {
                    q.add(caller);
                    suspendables.add(caller);
                }
            }
        }
    }

    private void mapCallersAndSupers() {
        try {
            ClassLoaderUtil.accept(cl, new ClassLoaderUtil.Visitor() {

                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (url.getFile().endsWith(CLASSFILE_SUFFIX))
                        try {
                            visitClassFile(cl.getResourceAsStream(resource));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void visitClassFile(InputStream classStream) throws IOException {
        ClassReader cr = new ClassReader(classStream);
        final ClassNode cn = new ClassNode();
        cr.accept(new ClassVisitor(Opcodes.ASM4, cn) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                String cn = name.intern();
                if (!superName.equals(JAVALANG_OBJECT))
                    supers.put(cn, superName);
                for (String iface : interfaces)
                    if (!iface.equals(JAVALANG_OBJECT))
                        supers.put(cn, iface.intern());
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                final MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
                final String caller = (cn.name + "." + name + desc).intern();
                return new MethodVisitor(Opcodes.ASM4, mn) {

                    @Override
                    public void visitEnd() {
                        for (AbstractInsnNode in : mn.instructions.toArray()) {
                            if (in.getType() == AbstractInsnNode.METHOD_INSN) {
                                MethodInsnNode min = (MethodInsnNode) in;
                                final String callee = (min.owner + "." + min.name + min.desc).intern();
                                callers.put(callee, caller);
                            }
                            if (in.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                                InvokeDynamicInsnNode idin = (InvokeDynamicInsnNode) in;
                            }
                        }
                        super.visitEnd();
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG);
    }

    static <K, V> SetMultimap<K, V> newHashMultimap() {
        return Multimaps.newSetMultimap(new HashMap<K, Collection<V>>(), new Supplier<Set<V>>() {
            @Override
            public Set<V> get() {
                return new HashSet<>();
            }
        });
    }

    private static String getClassName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(0, fullMethodWithDesc.lastIndexOf('.'));
    }

    private static String getMethodName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1,
                fullMethodWithDesc.indexOf('('));
    }

    private static String getMethodDescName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1);
    }

    public Set<String> getSuspendables() {
        return suspendables;
    }

    public Set<String> getSuperSuspendables() {
        return superSuspendables;
    }

    // visible for testing
    SetMultimap<String, String> getSupers() {
        return supers;
    }

    // visible for testing
    SetMultimap<String, String> getCallers() {
        return callers;
    }
    private static final String CLASSFILE_SUFFIX = ".class";
    private static final String JAVALANG_OBJECT = "java/lang/Object";
}

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
import java.util.List;
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
    private final Set<String> depSuspendables;
    private Set<String> suspendables;
    private Set<String> superSuspendables;

    public AutoSuspendablesScanner(final ClassLoader classLoader) {
        this.cl = classLoader;
        this.supers = AutoSuspendablesScanner.<String, String>newHashMultimap();
        this.callers = AutoSuspendablesScanner.<String, String>newHashMultimap();
        depSuspendables = new HashSet<>();
        mapCallersAndSupers();
        mapSuspendables();
    }

    private void mapSuspendables() {
        Queue<String> q = Queues.newArrayDeque();
        suspendables = new HashSet<>();
        superSuspendables = new HashSet<>();
        for (String callee : callers.keySet())
            if (depSuspendables.contains(callee)) {
                System.out.println("XXX0 " + callee+" "+callers.get(callee));
                q.add(callee);
            }
        while (!q.isEmpty()) {
            final String node = q.poll();
            for (String superCls : supers.get(getClassName(node))) {
                final String superMethod = superCls + "." + getMethodDescName(node);
                if (callers.keySet().contains(superMethod) && !suspendables.contains(superMethod) && !superSuspendables.contains(superMethod)) {
                    q.add(superMethod);
                    System.out.println("XXX1 " + node + " " + superMethod);
                    superSuspendables.add(superMethod);
                }
            }
            for (String caller : callers.get(node)) {
                if (!suspendables.contains(caller)) {
                    q.add(caller);
                    System.out.println("XXX2 " + node + " " + caller);
                    suspendables.add(caller);
                }
            }
        }
    }

    private void mapCallersAndSupers() {
        try {
            ClassLoaderUtil.accept(cl, true, new ClassLoaderUtil.Visitor() {
                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (ClassLoaderUtil.isClassFile(url.getFile()))
                        mapCalls(cl.getResourceAsStream(resource));
                }
            });
            ClassLoaderUtil.accept(cl, false, new ClassLoaderUtil.Visitor() {
                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (ClassLoaderUtil.isClassFile(url.getFile()))
                        mapSupersAndDepSuspendables(cl.getResourceAsStream(resource));
                }
                
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * map the supers of this class
     * map the depSuspendables with the class' methods
     *
     * @param resourceAsStream
     */
    private void mapSupersAndDepSuspendables(InputStream resourceAsStream) {
        try {
            ClassReader cr = new ClassReader(resourceAsStream);
            final MethodDatabase db = new MethodDatabase(cl, new DefaultSuspendableClassifier(cl));
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
                    MethodDatabase.SuspendableType susp = db.getClassifier().isSuspendable(db, cn.name, cn.superName, ((List<String>) cn.interfaces).toArray(new String[0]), name, desc, signature, exceptions);
                    if (susp == MethodDatabase.SuspendableType.SUSPENDABLE || susp == MethodDatabase.SuspendableType.SUSPENDABLE_SUPER)
                        depSuspendables.add(cn.name + "." + name + desc);
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }

            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void mapCalls(InputStream classStream) {
        try {
            ClassReader cr = new ClassReader(classStream);
            final ClassNode cn = new ClassNode();
            cr.accept(new ClassVisitor(Opcodes.ASM4, cn) {

//            @Override
//            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
//                String cn = name.intern();
//                if (!superName.equals(JAVALANG_OBJECT))
//                    supers.put(cn, superName);
//                for (String iface : interfaces)
//                    if (!iface.equals(JAVALANG_OBJECT))
//                        supers.put(cn, iface.intern());
//                super.visit(version, access, name, signature, superName, interfaces);
//            }
//
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
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
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

    Set<String> getDepSuspendables() {
        return depSuspendables;
    }

    
    // visible for testing
    SetMultimap<String, String> getSupers() {
        return supers;
    }

    // visible for testing
    SetMultimap<String, String> getCallers() {
        return callers;
    }
    private static final String JAVALANG_OBJECT = "java/lang/Object";

    static class Ref<T> {
        T obj;

        void set(T val) {
            obj = val;
        }

        T get() {
            return obj;
        }
    }
}

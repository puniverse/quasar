package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.reflection.ASMUtil;
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
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AutoSuspendablesScanner {

    SetMultimap<String, String> callers;
    private final ClassLoader cl;
    private Set<String> suspendables;
    private Set<String> superSuspendables;
    ClassDb db;

    public static void main(String[] args) throws IOException {
        AutoSuspendablesScanner scanner = new AutoSuspendablesScanner(ClassLoader.getSystemClassLoader());
    }

    public AutoSuspendablesScanner(final ClassLoader classLoader) {
        this.cl = classLoader;
        this.db = new ClassDb(cl);
        this.callers = AutoSuspendablesScanner.<String, String>newHashMultimap();
        try {
            scanSuspendables();
            mapMethodCalls();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        mapSuspendablesAndSupers();
    }

    private void mapSuspendablesAndSupers() {
        Queue<String> q = Queues.newArrayDeque();
        suspendables = new HashSet<>();
        superSuspendables = new HashSet<>();

        // start the bfs from the manualSusp (all classpath)
        q.addAll(db.getSuspendables());

        while (!q.isEmpty()) {
            final String node = q.poll();
            // mark as super suspendables methods which are supers the given bfs node and called by project methods
            for (String superCls : db.getClassEntry(getClassName(node)).getAllSupers()) {
                if (superCls == null || superCls.matches(JAVALANG_REGEXP) || superCls.matches(JAVAUTIL_REGEXP))
                    continue;
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
    private static final String JAVALANG_REGEXP = "java/lang/[^/]*\\..*";
    private static final String JAVAUTIL_REGEXP = "java/util/[^/]*\\..*";

    private void mapMethodCalls() throws IOException {
        ClassLoaderUtil.accept(cl, true, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile()))
                    mapCalls(cl.getResourceAsStream(resource));
            }
        });
    }

    private void scanSuspendables() throws IOException {
        ClassLoaderUtil.accept(cl, false, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile())) {
                    db.getClassEntry(removeClassFileExtension(resource));
                }
            }

        });
//        ClassEntry bq = db.getClassEntry(BlockingQueue.class.getName().replace('.', '/'));
//        System.out.println("BBBBBB "+bq+":"+bq.superName);

    }

    private static String removeClassFileExtension(String resource) {
        return resource.substring(0, resource.length() - ".class".length());
    }

    /**
     * map the supers of this class
     * map the depSuspendables with the class' methods
     *
     * @param resourceAsStream
     */
    private void mapCalls(InputStream classStream) {
        try {
            ClassReader cr = new ClassReader(classStream);
            final ClassNode cn = new ClassNode();
            cr.accept(new ClassVisitor(Opcodes.ASM4, cn) {
                @Override
                public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions) {
                    final String caller = (cn.name + "." + methodname + desc).intern();
                    return new MethodVisitor(Opcodes.ASM4) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            String specificOwner = db.findSpecificImpl(owner, name + desc);
                            if (specificOwner == null) {
                                // enum values/clone or native method
//                                System.out.println("WARN: method not found " + owner + ":" + name + ":" + caller);
                                return;

                            }
//                            assert specificOwner != null : owner + "." + name + desc + " : " + caller;
                            final String callee = (specificOwner + "." + name + desc).intern();
                            callers.put(callee, caller);

                            super.visitMethodInsn(opcode, owner, name, desc, itf); //To change body of generated methods, choose Tools | Templates.
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                            System.out.println("WARN: INVOKE_DYNAMIC_INSN is not supported yet " + cn.name + "." + name);
                            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs); //To change body of generated methods, choose Tools | Templates.
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

    Set<String> getManualSuspOrSupers() {
        return null;
    }

    // visible for testing
    SetMultimap<String, String> getCallers() {
        return callers;
    }
    private static final String JAVALANG_OBJECT = "java/lang/Object";

    static class ClassDb {
        Map<String, ClassEntry> classes = new HashMap<>();
        Set<String> suspendables = new HashSet<>();
        ClassLoader cl;
        private final SimpleSuspendableClassifier ssc;

        public ClassDb(ClassLoader cl) {
            this.cl = cl;
            this.ssc = new SimpleSuspendableClassifier(cl);
        }

        public Set<String> getSuspendables() {
            return suspendables;
        }

        private ClassEntry getClassEntry(String classname) {
            ClassEntry entry = classes.get(classname);
            if (entry != null)
                return entry;
            try {
                ClassNode cn = new ClassNode();
                new ClassReader(cl.getResourceAsStream(classname + ".class")).accept(cn, 0);
                String[] methods = new String[cn.methods.size()];
                int i = 0;
                for (MethodNode mn : (List<MethodNode>) cn.methods) {
                    methods[i++] = mn.name + mn.desc;
                    if (isSuspendable(cn, mn, ssc))
                        suspendables.add(cn.name + "." + mn.name + mn.desc);
                }
                final ClassEntry classEntry = new ClassEntry(cn.superName, ((List<String>) cn.interfaces).toArray(new String[cn.interfaces.size()]), methods);
                classes.put(classname, classEntry);
                return classEntry;
            } catch (IOException ex) {
                return null;
            }
        }

        String findSpecificImpl(String className, String methodDesc) {
            ClassEntry entry = getClassEntry(className);
            if (entry != null) {
                String cn = className;
                for (String method : entry.methodsDescs)
                    if (method.equals(methodDesc))
                        return cn;
                for (String superOrIFace : entry.getAllSupers()) {
                    String superImplClass = findSpecificImpl(superOrIFace, methodDesc);
                    if (superImplClass != null)
                        return superImplClass;
                }
            }
            return null;
        }

        private static boolean isSuspendable(final ClassNode cn, MethodNode mn, SimpleSuspendableClassifier ssc) {
            boolean annotation = ASMUtil.hasAnnotation(Classes.ANNOTATION_DESC, mn);
            boolean file = ssc.isSuspendable(cn.name, mn.name, mn.desc);
            boolean exception = mn.exceptions.contains(Classes.EXCEPTION_NAME);
            final boolean susp = annotation || exception || file;
            return susp;
        }

    }

    static class ClassEntry {
        final String superName;
        final String[] interfaces;
        final String[] methodsDescs;

        public ClassEntry(String superName, String[] interfaces, String[] methodsDescs) {
            this.superName = superName;
            this.interfaces = interfaces;
            this.methodsDescs = methodsDescs;
        }

        public String[] getAllSupers() {
            String[] ar = new String[interfaces.length + 1];
            ar[0] = superName;
            System.arraycopy(interfaces, 0, ar, 1, interfaces.length);
            return ar;
        }

        @Override
        public String toString() {
            return "ClassEntry{" + "superName=" + superName + ", interfaces=" + interfaces + ", methodsDescs=" + methodsDescs + '}';
        }
    }
}

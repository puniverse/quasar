package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.reflection.ASMUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.ScanMode;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType.*;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Queues;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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

    private final Map<String, MethodEntry> callGraph;
    private final ClassLoader cl;
    private final Set<String> suspendables;
    private final Set<String> superSuspendables;
    private final ClassDb db;

    public AutoSuspendablesScanner(final ClassLoader classLoader) {
        this.cl = classLoader;
        this.db = new ClassDb(cl);
        this.callGraph = new HashMap<>();
        try {
            scanSuspendables();
            mapMethodCalls();
            mapSuspendablesAndSupers();
            this.suspendables = new HashSet<>();
            this.superSuspendables = new HashSet<>();
            indexSuspenablesAndSupers();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void mapSuspendablesAndSupers() {
        final Queue<String> q = Queues.newArrayDeque();

        // start the bfs from the manualSusp (all classpath)
        q.addAll(db.getManualSuspendablesOrSupers());

        while (!q.isEmpty()) {
            final String currentMethod = q.poll();
            // mark as super suspendables methods which are supers of the given bfs node and called somewhere by project methods
            for (String superCls : db.getClassEntry(getClassName(currentMethod)).allSupers) {
                if (superCls == null || superCls.matches(JAVALANG_REGEXP) || superCls.matches(JAVAUTIL_REGEXP))
                    continue;
                final String superMethod = (superCls + "." + getMethodDescName(currentMethod)).intern();
                if (!callGraph.containsKey(superMethod)) // continue if superMethods has no reference in the callGraph
                    continue;
                MethodEntry superMethodEntry = callGraph.get(superMethod);
                if (superMethodEntry.suspendType == null) { // not yet marked as suspendable or superSuspendable
                    q.add(superMethod);
                    superMethodEntry.suspendType = SUSPENDABLE_SUPER;
                }
            }
            // mark as suspendables methods from the project which are calling of the given bfs node (which is superSuspenable or suspendable)
            if (callGraph.containsKey(currentMethod))
                for (MethodEntry caller : callGraph.get(currentMethod).callers) {
                    if (caller.suspendType != SUSPENDABLE) { // not yet marked
                        q.add(caller.name);
                        caller.suspendType = SUSPENDABLE;
                    }
                }
        }
    }

    private void mapMethodCalls() throws IOException {
        ClassLoaderUtil.accept(cl, ScanMode.WITHOUT_JARS, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile()))
                    mapCalls(cl.getResourceAsStream(resource));
            }
        });
    }

    private void scanSuspendables() throws IOException {
        ClassLoaderUtil.accept(cl, ScanMode.WHOLE_CLASSPATH, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile())) {
                    db.getClassEntry(removeClassFileExtension(resource));
                }
            }

        });
    }

    private void mapCalls(InputStream classStream) {
        try {
            ClassReader cr = new ClassReader(classStream);
            final ClassNode cn = new ClassNode();
            cr.accept(new ClassVisitor(Opcodes.ASM4, cn) {
                @Override
                public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions) {
                    final MethodEntry caller = getOrCreateMethodEntry((cn.name + "." + methodname + desc).intern());
                    return new MethodVisitor(Opcodes.ASM4) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            final String specificOwner = db.findSpecificClassImpl(owner, name + desc);
                            // enum values/clone or native methods
                            if (specificOwner == null)
                                return;
                            getOrCreateMethodEntry((specificOwner + "." + name + desc).intern()).callers.add(caller);
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

    private void indexSuspenablesAndSupers() {
        for (MethodEntry methodEntry : callGraph.values())
            if (methodEntry.suspendType == SUSPENDABLE)
                suspendables.add(methodEntry.name);
            else if (methodEntry.suspendType == SUSPENDABLE_SUPER)
                superSuspendables.add(methodEntry.name);
    }

    static class ClassDb {
        private final Map<String, ClassEntry> classes = new HashMap<>();
        private final Set<String> manualSuspendablesOrSupers = new HashSet<>();
        private final ClassLoader cl;
        private final SimpleSuspendableClassifier ssc;

        public ClassDb(ClassLoader cl) {
            this.cl = cl;
            this.ssc = new SimpleSuspendableClassifier(cl);
        }

        public Set<String> getManualSuspendablesOrSupers() {
            return manualSuspendablesOrSupers;
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
                    methods[i++] = (mn.name + mn.desc).intern();
                    if (isSuspendable(cn, mn) || isSuperSuspendable(cn, mn))
                        manualSuspendablesOrSupers.add((cn.name + "." + mn.name + mn.desc).intern());
                }
                final ClassEntry classEntry = new ClassEntry(cn.superName, ((List<String>) cn.interfaces).toArray(new String[cn.interfaces.size()]), methods);
                classes.put(classname, classEntry);
                return classEntry;
            } catch (IOException ex) {
                return null;
            }
        }

        public String findSpecificClassImpl(String className, String methodDesc) {
            ClassEntry entry = getClassEntry(className);
            if (entry != null) {
                String cn = className;
                for (String method : entry.methods)
                    if (method.equals(methodDesc))
                        return cn;
                for (String superOrIFace : entry.allSupers) {
                    String superImplClass = findSpecificClassImpl(superOrIFace, methodDesc);
                    if (superImplClass != null)
                        return superImplClass;
                }
            }
            return null;
        }

        private  boolean isSuspendable(final ClassNode cn, MethodNode mn) {
            return ASMUtil.hasAnnotation(Classes.ANNOTATION_DESC, mn)
                    || ssc.isSuspendable(cn.name, mn.name, mn.desc)
                    || mn.exceptions.contains(Classes.EXCEPTION_NAME);
        }
        
        private boolean isSuperSuspendable(final ClassNode cn, MethodNode mn) {
            return  ssc.isSuperSuspendable(cn.name, mn.name, mn.desc);
        }        
    }

    static class ClassEntry {
        final String[] allSupers; // super and interfaces classnames
        final String[] methods; // name+desc

        public ClassEntry(String superName, String[] interfaces, String[] methodsWithDescs) {
            this.methods = methodsWithDescs;
            this.allSupers = new String[interfaces.length + 1];
            this.allSupers[0] = superName;
            System.arraycopy(interfaces, 0, this.allSupers, 1, interfaces.length);
            for (int i = 0; i < allSupers.length; i++)
                allSupers[i] = allSupers[i] != null ? allSupers[i].intern() : null;
        }
    }

    public class MethodEntry {
        String name; // classname.methodname+desc
        List<MethodEntry> callers;
        SuspendableType suspendType;

        public MethodEntry(String name) {
            this.name = name;
            this.callers = new ArrayList<>();
            this.suspendType = null;
        }
    }

    private MethodEntry getOrCreateMethodEntry(String methodName) {
        MethodEntry entry = callGraph.get(methodName);
        if (entry == null) {
            entry = new MethodEntry(methodName);
            callGraph.put(methodName, entry);
        }
        return entry;
    }

    private static String removeClassFileExtension(String resource) {
        return resource.substring(0, resource.length() - ".class".length());
    }

    private static final String JAVALANG_REGEXP = "java/lang/[^/]*\\..*";
    private static final String JAVAUTIL_REGEXP = "java/util/[^/]*\\..*";
}

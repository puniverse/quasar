/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.reflection.ASMUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.ScanMode;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.resourceToSlashed;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType.*;
import com.google.common.base.Supplier;
import com.google.common.collect.Multimaps;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.SetMultimap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class AutoSuspendablesScanner {
    private static final Pattern JAVALANG_REGEXP = Pattern.compile("java/lang/[^/]*\\..*");
    private static final Pattern JAVAUTIL_REGEXP = Pattern.compile("java/util/[^/]*\\..*");
    //
    private final Map<String, MethodEntry> callGraph;
    private final ClassLoader cl;
    private final ClassDb db;
    private final URL[] urls;
    private final SimpleSuspendableClassifier ssc;


    // needed for AntClassLoader where urls cannot be retrieved useing getURLS
    public AutoSuspendablesScanner(final ClassLoader classLoader, URL[] urls) {
        this.urls = urls;
        this.cl = classLoader;
        this.callGraph = new HashMap<>();
        this.ssc = new SimpleSuspendableClassifier(cl);
        this.db = new ClassDb();
    }

    public AutoSuspendablesScanner(final URLClassLoader classLoader) {
        this(classLoader, classLoader.getURLs());
    }

    public void run() {
        try {
            scanSuspendables();
            createCallGraph();
            mapSuspendablesAndSupers();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void mapSuspendablesAndSupers() {
        final Queue<MethodEntry> q = new ArrayDeque<>();

        // start the bfs from the manualSusp (all classpath)
        for (String ms : db.getManualSuspendablesOrSupers())
            q.add(getOrCreateMethodEntry(ms));

        while (!q.isEmpty()) {
            final MethodEntry currentMethod = q.poll();
            // mark as super suspendables methods which are supers of the given bfs node and called somewhere by project methods
            for (String superCls : db.getClassEntry(getClassName(currentMethod.name)).allSupers) {
                if (superCls == null || JAVALANG_REGEXP.matcher(superCls).matches() || JAVAUTIL_REGEXP.matcher(superCls).matches())
                    continue;
                final String superMethod = (superCls + "." + getMethodDescName(currentMethod.name)).intern();
                if (!callGraph.containsKey(superMethod)) // continue if superMethods has no reference in the callGraph
                    continue;
                final MethodEntry superMethodEntry = callGraph.get(superMethod);
                if (superMethodEntry.suspendType == null) { // not yet marked as suspendable or superSuspendable
                    q.add(superMethodEntry);
                    superMethodEntry.suspendType = SUSPENDABLE_SUPER;
                }
            }
            // mark as suspendables methods from the project which are calling of the given bfs node (which is superSuspenable or suspendable)
            for (MethodEntry caller : currentMethod.getCallers()) {
                if (caller.suspendType != SUSPENDABLE) { // not yet marked
                    q.add(caller);
                    caller.suspendType = SUSPENDABLE;
                }
            }
        }
    }

    private void createCallGraph() throws IOException {
        ClassLoaderUtil.accept(cl, urls, ScanMode.WITHOUT_JARS, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile()))
                    createCallGraph(cl.getResourceAsStream(resource));
            }
        });
    }

    private void scanSuspendables() throws IOException {
        ClassLoaderUtil.accept(cl, urls, ScanMode.WHOLE_CLASSPATH, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile())) {
                    db.getClassEntry(resourceToSlashed(resource));
                }
            }
        });
    }

    private void createCallGraph(InputStream classStream) {
        try {
            ClassReader cr = new ClassReader(classStream);
            final ClassNode cn = new ClassNode();
            cr.accept(new ClassVisitor(Opcodes.ASM4, cn) {
                @Override
                public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions) {
                    final MethodEntry caller = getOrCreateMethodEntry(cn.name + "." + methodname + desc);
                    return new MethodVisitor(Opcodes.ASM4) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            final String specificOwner = db.findSpecificClassImpl(owner, name + desc);
                            // enum values/clone or native methods
                            if (specificOwner == null)
                                return;
                            getOrCreateMethodEntry(specificOwner + "." + name + desc).addCaller(caller);
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

    public void getSuspenablesAndSupers(Collection<String> suspendables, Collection<String> suspendableSupers) {
        for (MethodEntry methodEntry : callGraph.values())
            if (methodEntry.suspendType == SUSPENDABLE && suspendables != null)
                suspendables.add(methodEntry.name);
            else if (methodEntry.suspendType == SUSPENDABLE_SUPER && suspendableSupers != null)
                suspendableSupers.add(methodEntry.name);
    }

    private boolean isSuspendable(final ClassNode cn, MethodNode mn) {
        return ASMUtil.hasAnnotation(Classes.ANNOTATION_DESC, mn)
                || ssc.isSuspendable(cn.name, mn.name, mn.desc)
                || mn.exceptions.contains(Classes.EXCEPTION_NAME);
    }

    private boolean isSuperSuspendable(final ClassNode cn, MethodNode mn) {
        return ssc.isSuperSuspendable(cn.name, mn.name, mn.desc);
    }

    private class ClassDb {
        private final Map<String, ClassEntry> classes = new HashMap<>();
        private final Set<String> manualSuspendablesOrSupers = new HashSet<>();

        public Set<String> getManualSuspendablesOrSupers() {
            return manualSuspendablesOrSupers;
        }

        private ClassEntry getClassEntry(String classname) {
            ClassEntry entry = classes.get(classname);
            if (entry != null)
                return entry;
            try {
                final ClassNode cn = new ClassNode();
                new ClassReader(cl.getResourceAsStream(classname + ".class")).accept(cn, 0);
                
                final String[] methods = new String[cn.methods.size()];
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
    }

    private MethodEntry getOrCreateMethodEntry(String methodName) {
        methodName = methodName.intern();
        MethodEntry entry = callGraph.get(methodName);
        if (entry == null) {
            entry = new MethodEntry(methodName);
            callGraph.put(methodName, entry);
        }
        return entry;
    }

    private static class ClassEntry {
        final String[] allSupers; // super and interfaces classnames
        final String[] methods; // name+desc

        public ClassEntry(String superName, String[] interfaces, String[] methodsWithDescs) {
            this.methods = methodsWithDescs;
            this.allSupers = ObjectArrays.concat(superName, interfaces);
            for (int i = 0; i < allSupers.length; i++)
                allSupers[i] = allSupers[i] != null ? allSupers[i].intern() : null;
        }
    }

    private static class MethodEntry {
        final String name; // classname.methodname+desc
        private MethodEntry[] callers;
        private int numCallers;
        SuspendableType suspendType;

        public MethodEntry(String name) {
            this.name = name;
        }

        public void addCaller(MethodEntry caller) {
            if (callers == null)
                callers = new MethodEntry[4];
            if (numCallers + 1 >= callers.length)
                this.callers = Arrays.copyOf(callers, callers.length * 2);
            callers[numCallers] = caller;
            numCallers++;
        }

        public Collection<MethodEntry> getCallers() {
            return new AbstractCollection<MethodEntry>() {
                public int size()                 { return numCallers; }
                public Iterator<MethodEntry> iterator() {
                    return new Iterator<MethodEntry>() {
                        private int i;
                        public boolean hasNext()  { return i < numCallers; }
                        public MethodEntry next() { return callers[i++]; }
                        public void remove()      { throw new UnsupportedOperationException("remove"); }
                    };
                }
            };
        }
    }

    private static String getClassName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(0, fullMethodWithDesc.lastIndexOf('.'));
    }

    private static String getMethodName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1, fullMethodWithDesc.indexOf('('));
    }

    private static String getMethodDescName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1);
    }

    private static URL[] classpathToUrls(String[] classPath) throws RuntimeException {
        URL[] ar = null;
        try {
            List<URL> list = new ArrayList<>();
            for (String cp : classPath)
                list.add(new File(cp).toURI().toURL());
            ar = list.toArray(new URL[0]);
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        return ar;
    }

    private static void outputResults(String outputFile, boolean append1, Collection<String> results) throws Exception {
        try (PrintStream out = getOutputStream(outputFile, append1)) {
            for (String s : results)
                out.println(s);
        }
    }

    private static PrintStream getOutputStream(String outputFile, boolean append1) throws Exception {
        if (outputFile != null) {
            outputFile = outputFile.trim();
            if (outputFile.isEmpty())
                outputFile = null;
        }
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.getParent() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();
            return new PrintStream(new FileOutputStream(file, append1));
        } else
            return System.out;
    }

    public static class AntTask extends Task {
        private String supersFile;
        private String suspendablesFile;

        public void setOutputSuspendableFile(String outputFile) {
            this.suspendablesFile = outputFile;
        }

        public void setOutputSupersFile(String outputFile) {
            this.supersFile = outputFile;
        }

        @Override
        public void execute() throws BuildException {
            try {
                final AntClassLoader cl = (AntClassLoader) getClass().getClassLoader();
                final AutoSuspendablesScanner scanner = new AutoSuspendablesScanner(cl, classpathToUrls(cl.getClasspath().split(":")));
                scanner.run();

                // output results
                final ArrayList<String> suspendables = suspendablesFile != null ? new ArrayList<String>() : null;
                final ArrayList<String> suspendableSupers = supersFile != null ? new ArrayList<String>() : null;
                scanner.getSuspenablesAndSupers(suspendables, suspendableSupers);
                if (suspendablesFile != null) {
                    Collections.sort(suspendables);
                    outputResults(suspendablesFile, false, suspendables);
                }
                if (supersFile != null) {
                    Collections.sort(suspendableSupers);
                    outputResults(supersFile, false, suspendableSupers);
                }
            } catch (Exception e) {
                log(e, Project.MSG_ERR);
                throw new BuildException(e);
            }
        }
    }
}

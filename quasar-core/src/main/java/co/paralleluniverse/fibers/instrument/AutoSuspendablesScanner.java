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

import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.isClassFile;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.resourceToSlashed;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.classToResource;
import static co.paralleluniverse.fibers.instrument.Classes.ANNOTATION_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import static co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType.*;
import java.io.File;
import java.io.FileInputStream;
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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class AutoSuspendablesScanner extends Task {
    private static final Pattern JAVALANG_REGEXP = Pattern.compile("java/lang/[^/]*\\..*");
    private static final Pattern JAVAUTIL_REGEXP = Pattern.compile("java/util/[^/]*\\..*");
    private static final int API = Opcodes.ASM4;
    //
    private final Map<String, MethodNode> callGraph = new HashMap<>();
    private final Map<String, ClassNode> classes = new HashMap<>();
    private final Set<MethodNode> knownSuspendablesOrSupers = new HashSet<>();

    private URLClassLoader cl;
    private final ArrayList<FileSet> filesets = new ArrayList<FileSet>();
    private URL[] urls;
    private SimpleSuspendableClassifier ssc;
    private boolean auto = true;
    private boolean append;
    private String supersFile;
    private String suspendablesFile;

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setOutputSuspendableFile(String outputFile) {
        this.suspendablesFile = outputFile;
    }

    public void setOutputSupersFile(String outputFile) {
        this.supersFile = outputFile;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public void setAppend(boolean value) {
        this.append = value;
    }

    void setURLs(List<URL> urls) {
        this.urls = urls.toArray(new URL[0]);
        this.cl = new URLClassLoader(this.urls);
        this.ssc = new SimpleSuspendableClassifier(cl);
    }

    @Override
    public void execute() throws BuildException {
        try {
            final List<URL> us = new ArrayList<>();
//            for (FileSet fs : filesets)
//                urls.add(fs.getDir().toURI().toURL());
            final AntClassLoader acl = (AntClassLoader) getClass().getClassLoader();
            classpathToUrls(acl.getClasspath().split(":"), us);
            setURLs(us);

            log("URLs: " + this.urls, Project.MSG_INFO);

            scanExternalSuspendables();
            // scan classes in filesets
            for (FileSet fs : filesets) {
                try {
                    final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                    final String[] includedFiles = ds.getIncludedFiles();

                    for (String filename : includedFiles) {
                        if (isClassFile(filename)) {
                            File file = new File(fs.getDir(), filename);
                            if (file.isFile()) {
                                if (auto)
                                    createCallGraph(new FileInputStream(file));
                                else
                                    createClassNode(new FileInputStream(file));
                            } else
                                log("File not found: " + filename);
                        }
                    }
                } catch (BuildException ex) {
                    log(ex.getMessage(), ex, Project.MSG_WARN);
                }
            }
            walkGraph();
            

            log("OUTPUT: " + supersFile, Project.MSG_INFO);
            // output results
            final ArrayList<String> suspendables = suspendablesFile != null ? new ArrayList<String>() : null;
            final ArrayList<String> suspendableSupers = supersFile != null ? new ArrayList<String>() : null;
            getSuspenablesAndSupers(suspendables, suspendableSupers);
            if (suspendablesFile != null) {
                Collections.sort(suspendables);
                outputResults(suspendablesFile, append, suspendables);
            }
            if (supersFile != null) {
                Collections.sort(suspendableSupers);
                outputResults(supersFile, append, suspendableSupers);
            }
        } catch (Exception e) {
            log(e, Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

    private void scanExternalSuspendables() throws IOException {
        ClassLoaderUtil.accept(cl, urls, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                if (ClassLoaderUtil.isClassFile(url.getFile())) {
                    scanSuspendables(cl.getResourceAsStream(resource));
                }
            }
        });
    }

    private void scanSuspendables(InputStream classStream) {
        try {
            final ClassReader cr = new ClassReader(classStream);
            cr.accept(new SuspendableClassifier(false, API, null), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private class SuspendableClassifier extends ClassVisitor {
        private final boolean inProject;
        private String className;
        private boolean suspendableClass;

        public SuspendableClassifier(boolean inProject, int api, ClassVisitor cv) {
            super(api, cv);
            this.inProject = inProject;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name.intern();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
            final AnnotationVisitor av = super.visitAnnotation(adesc, visible);
            if (adesc.equals(ANNOTATION_DESC))
                suspendableClass = true;
            return av;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String methodname, final String desc, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, desc, desc, signature, exceptions);

            SuspendableType suspendable = SuspendableType.NON_SUSPENDABLE;
            final boolean noImpl = (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0;
            if (suspendableClass)
                suspendable = noImpl ? SuspendableType.SUSPENDABLE_SUPER : SuspendableType.SUSPENDABLE;
            if (suspendable != SuspendableType.SUSPENDABLE && checkExceptions(exceptions))
                suspendable = noImpl ? SuspendableType.SUSPENDABLE_SUPER : SuspendableType.SUSPENDABLE;
            if (suspendable != SuspendableType.SUSPENDABLE && ssc.isSuperSuspendable(className, methodname, desc))
                suspendable = max(suspendable, SuspendableType.SUSPENDABLE_SUPER);
            if (suspendable != SuspendableType.SUSPENDABLE && ssc.isSuspendable(className, methodname, desc))
                suspendable = max(suspendable, SuspendableType.SUSPENDABLE);
            
            if (suspendable == SuspendableType.SUSPENDABLE) {
                markSuspendable(methodname, desc, suspendable);
                return mv;
            } else {
                return new MethodVisitor(api, mv) {
                    private boolean susp = false;

                    @Override
                    public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                        if (adesc.equals(ANNOTATION_DESC))
                            susp = true;
                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        super.visitEnd();

                        if (susp)
                            markSuspendable(methodname, desc, noImpl ? SuspendableType.SUSPENDABLE_SUPER : SuspendableType.SUSPENDABLE);
                    }
                };
            }
        }

        private void markSuspendable(String methodname, String desc, SuspendableType sus) {
            final MethodNode method = getOrCreateMethodNode(className + "." + methodname + desc);
            method.owner = className;
            method.inProject |= inProject;
            method.suspendType = sus;

            knownSuspendablesOrSupers.add(method);
        }
        
        private boolean checkExceptions(String[] exceptions) {
            if (exceptions != null) {
                for (String ex : exceptions) {
                    if (ex.equals(EXCEPTION_NAME))
                        return true;
                }
            }
            return false;
        }

        private SuspendableType max(SuspendableType a, SuspendableType b) {
            if (a == null)
                return b;
            if (b == null)
                return a;
            return b.compareTo(a) > 0 ? b : a;
        }
    }

    private class ClassNodeVisitor extends ClassVisitor {
        private final boolean inProject;
        private String className;
        private final List<String> methods = new ArrayList<String>();
        private ClassNode cn;

        public ClassNodeVisitor(boolean inProject, int api, ClassVisitor cv) {
            super(api, cv);
            this.inProject = inProject;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            cn = getOrCreateClassNode(className);
            cn.inProject |= inProject;
            cn.setSupers(superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, desc, desc, signature, exceptions);
            methods.add((methodname + desc).intern());
            return mv;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();

            cn.setMethods(methods);
        }
    }

    private class CallGraphVisitor extends ClassVisitor {
        private String className;

        public CallGraphVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String methodname, final String desc, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, desc, desc, signature, exceptions);
            final MethodNode caller = getOrCreateMethodNode(className + "." + methodname + desc);
            return new MethodVisitor(api, mv) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    if (isReflectInvocation(owner, name))
                        log("NOTE: Reflective invocation in " + methodToString(), Project.MSG_WARN);
                    else if (isInvocationHandlerInvocation(owner, name))
                        log("NOTE: Invocation handler invocation in " + methodToString(), Project.MSG_WARN);
                    else if (isMethodHandleInvocation(owner, name))
                        log("NOTE: Method handle invocation in " + methodToString(), Project.MSG_WARN);
                    else
                        getOrCreateMethodNode(owner + "." + name + desc).addCaller(caller);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                    super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                    System.err.println("NOTE: InvokeDynamic in " + methodToString());
                }

                private String methodToString() {
                    return (className + "." + methodname + "(" + Arrays.toString(Type.getArgumentTypes(desc)) + ") - " + className + "." + methodname + desc);
                }
            };
        }
    }
    
    private void createCallGraph(InputStream classStream) {
        try {
            final ClassReader cr = new ClassReader(classStream);
            cr.accept(
                    new CallGraphVisitor(API,
                            new ClassNodeVisitor(true, API,
                                    new SuspendableClassifier(true, API, null))),
                    ClassReader.SKIP_DEBUG);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void createClassNode(InputStream classStream) {
        try {
            final ClassReader cr = new ClassReader(classStream);
            cr.accept(
                    new ClassNodeVisitor(true, API,
                            new SuspendableClassifier(true, API, null)),
                    ClassReader.SKIP_DEBUG);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void walkGraph() {
        final Queue<MethodNode> q = new ArrayDeque<>();
        q.addAll(knownSuspendablesOrSupers); // start the bfs from the manualSusp (all classpath)

        while (!q.isEmpty()) {
            final MethodNode m = q.poll();
            if (m.inProject) {
                ClassNode c = getClassNode(m);
                followSupers(q, c, m);
            }
            followCallers(q, m);
        }
    }

    private boolean followSupers(Queue<MethodNode> q, ClassNode cls, MethodNode method) {
        if (cls == null)
            return false;

        assert method.suspendType != SuspendableType.NON_SUSPENDABLE;

        boolean foundMethod = false;

        if (cls.hasMethod(method.name) && method.classNode != cls) {
            log("Found parent of annotated method: " + method.owner + "." + method.name + " in " + cls.name
                    + (cls.inProject ? "" : " NOT IN PROJECT"), cls.inProject ? Project.MSG_VERBOSE : Project.MSG_WARN);
            foundMethod = true;
        }

        // recursively look in superclass and interfaces
        boolean methodInParent = false;
        for (ClassNode s : cls.supers)
            methodInParent |= followSupers(q, fill(s), method);

        if (!foundMethod && methodInParent)
            log("Found parent of annotated method in a parent of: " + method.owner + "." + method.name + " in " + cls.name
                    + (cls.inProject ? "" : " NOT IN PROJECT"), cls.inProject ? Project.MSG_VERBOSE : Project.MSG_WARN);
        
        final boolean res = foundMethod | methodInParent;
        if (res) {
            MethodNode m = getOrCreateMethodNode(cls.name + "." + method.name);
            if (m.suspendType != SuspendableType.SUSPENDABLE) {
                m.suspendType = SuspendableType.SUSPENDABLE_SUPER;
                q.add(m);
            }
        }
        return res;
    }

    private void followCallers(Queue<MethodNode> q, MethodNode method) {
        // mark as suspendables methods from the project which are calling of the given bfs node (which is superSuspenable or suspendable)
        for (MethodNode caller : method.getCallers()) {
            if (caller.suspendType == null) { // not yet visited
                q.add(caller);
                caller.suspendType = SUSPENDABLE;
            }
        }
    }

    public void getSuspenablesAndSupers(Collection<String> suspendables, Collection<String> suspendableSupers) {
        for (MethodNode method : callGraph.values())
            if (method.suspendType == SUSPENDABLE && suspendables != null)
                suspendables.add(method.name);
            else if (method.suspendType == SUSPENDABLE_SUPER && suspendableSupers != null)
                suspendableSupers.add(method.name);
    }
    
    private static void outputResults(String outputFile, boolean append, Collection<String> results) throws Exception {
        try (PrintStream out = getOutputStream(outputFile, append)) {
            for (String s : results)
                out.println(s);
        }
    }

    private MethodNode getOrCreateMethodNode(String methodName) {
        methodName = methodName.intern();
        MethodNode entry = callGraph.get(methodName);
        if (entry == null) {
            entry = new MethodNode(getClassName(methodName), getMethodWithDesc(methodName));
            callGraph.put(methodName, entry);
        }
        return entry;
    }

    private ClassNode getOrCreateClassNode(String className) {
        className = className.intern();
        ClassNode node = classes.get(className);
        if (node == null) {
            node = new ClassNode(className);
            classes.put(className, node);
        }
        return node;
    }
    
    private ClassNode getOrLoadClassNode(String className) {
        return fill(getOrCreateClassNode(className.intern()));
    }

    private ClassNode fill(ClassNode node) {
        try {
            if (node.supers == null) {
                final ClassReader cr = new ClassReader(cl.getResourceAsStream(classToResource(node.name)));
                cr.accept(new ClassNodeVisitor(false, API, null), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
                assert node.supers != null;
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClassNode getClassNode(MethodNode m) {
        ClassNode c = m.classNode;
        if (c == null) {
            c = getOrLoadClassNode(m.owner);
            m.classNode = c;
        }
        return c;
    }

    private class ClassNode {
        boolean inProject;
        final String name;
        ClassNode[] supers; // super and interfaces classnames
        private String[] methods;

        public ClassNode(String name) {
            this.name = name;
        }

        void setSupers(String superName, String[] interfaces) {
            this.supers = new ClassNode[(superName != null ? 1 : 0) + (interfaces != null ? interfaces.length : 0)];
            int i = 0;
            if (superName != null)
                supers[i++] = getOrCreateClassNode(superName);
            if (interfaces != null) {
                for (String iface : interfaces)
                    supers[i++] = getOrCreateClassNode(iface);
            }
        }

        void setMethods(Collection<String> ms) {
            this.methods = ms.toArray(new String[ms.size()]);
            for (int i = 0; i < methods.length; i++)
                methods[i] = methods[i].intern();
        }

        boolean hasMethod(String method) {
            for (String m : methods) {
                if (method.equals(m))
                    return true;
            }
            return false;
        }
    }

    private static class MethodNode {
        String owner;
        ClassNode classNode;
        final String name; // methodname+desc
        boolean inProject;
        SuspendableType suspendType;
        private MethodNode[] callers;
        private int numCallers;

        public MethodNode(String owner, String nameAndDesc) {
            this.owner = owner.intern();
            this.name = nameAndDesc.intern();
        }

        public void addCaller(MethodNode caller) {
            if (callers == null)
                callers = new MethodNode[4];
            if (numCallers + 1 >= callers.length)
                this.callers = Arrays.copyOf(callers, callers.length * 2);
            callers[numCallers] = caller;
            numCallers++;
        }

        public Collection<MethodNode> getCallers() {
            if (callers == null)
                return Collections.emptyList();
            return new AbstractCollection<MethodNode>() {
                public int size()                 { return numCallers; }
                public Iterator<MethodNode> iterator() {
                    return new Iterator<MethodNode>() {
                        private int i;
                        public boolean hasNext()  { return i < numCallers; }
                        public MethodNode next() { return callers[i++]; }
                        public void remove()      { throw new UnsupportedOperationException("remove"); }
                    };
                }
            };
        }
    }

    private static String getClassName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(0, fullMethodWithDesc.lastIndexOf('.'));
    }

    private static String getMethodWithDesc(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1);
    }

    private static boolean isReflectInvocation(String className, String methodName) {
        return className.equals("java/lang/reflect/Method") && methodName.equals("invoke");
    }

    private static boolean isInvocationHandlerInvocation(String className, String methodName) {
        return className.equals("java/lang/reflect/InvocationHandler") && methodName.equals("invoke");
    }

    private static boolean isMethodHandleInvocation(String className, String methodName) {
        return className.equals("java/lang/invoke/MethodHandle") && methodName.startsWith("invoke");
    }
    
    private static void classpathToUrls(String[] classPath, List<URL> urls) throws RuntimeException {
        try {
            for (String cp : classPath)
                urls.add(new File(cp).toURI().toURL());
        } catch (MalformedURLException ex) {
            throw new AssertionError(ex);
        }
    }

    private static PrintStream getOutputStream(String outputFile, boolean append) throws Exception {
        if (outputFile != null) {
            outputFile = outputFile.trim();
            if (outputFile.isEmpty())
                outputFile = null;
        }
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.getParent() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();
            return new PrintStream(new FileOutputStream(file, append));
        } else
            return System.out;
    }
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.classToResource;
import static co.paralleluniverse.fibers.instrument.Classes.ANNOTATION_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.DONT_INSTRUMENT_ANNOTATION_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.EXCEPTION_NAME;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import com.google.common.base.Function;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.AbstractCollection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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

public class SuspendablesScanner extends Task {
    private static final int ASMAPI = Opcodes.ASM5;
    //
    private final Map<String, MethodNode> methods = new HashMap<>();
    private final Map<String, ClassNode> classes = new HashMap<>();
    private final Set<MethodNode> knownSuspendablesOrSupers = new HashSet<>();
    private final boolean ant;
    private URLClassLoader cl;
    private final ArrayList<FileSet> filesets = new ArrayList<>();
    private final Path projectDir;
    private URL[] urls;
    private SimpleSuspendableClassifier ssc;
    private boolean auto = true;
    private boolean append = false;
    private String supersFile;
    private String suspendablesFile;

    public SuspendablesScanner() {
        this.ant = getClass().getClassLoader() instanceof AntClassLoader;
        this.projectDir = null;
    }

    public SuspendablesScanner(Path projectDir) {
        this.ant = getClass().getClassLoader() instanceof AntClassLoader;
        this.projectDir = projectDir;
        assert !ant;
    }

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setSuspendablesFile(String outputFile) {
        this.suspendablesFile = outputFile;
    }

    public void setSupersFile(String outputFile) {
        this.supersFile = outputFile;
    }

    /**
     * Whether suspendables should be found based on the method call-graph.
     * I false, only suspendabel-supers of known suspendables will be found.
     */
    public void setAuto(boolean value) {
        this.auto = value;
    }

    /**
     * Whether the found methods should be appended to the output files(s) or replace them.
     */
    public void setAppend(boolean value) {
        this.append = value;
    }

    void setURLs(List<URL> urls) {
        this.urls = unique(urls).toArray(new URL[0]);
        this.cl = new URLClassLoader(this.urls);
        this.ssc = new SimpleSuspendableClassifier(cl);
    }

    private static <T> List<T> unique(List<T> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    @Override
    public void execute() throws BuildException {
        try {
            run();

            log("OUTPUT: " + supersFile, Project.MSG_INFO);
            log("OUTPUT: " + suspendablesFile, Project.MSG_INFO);

            // output results
            final ArrayList<String> suspendables = suspendablesFile != null ? new ArrayList<String>() : null;
            final ArrayList<String> suspendableSupers = supersFile != null ? new ArrayList<String>() : null;
            putSuspendablesAndSupers(suspendables, suspendableSupers);

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

    public void run() {
        try {
            // System.err.println("QQQQQQQQ: " + new SimpleSuspendableClassifier(supersFile).getSuspendableClasses());

            final List<URL> us = new ArrayList<>();

            if (ant) {
                final AntClassLoader acl = (AntClassLoader) getClass().getClassLoader();
                classpathToUrls(acl.getClasspath().split(System.getProperty("path.separator")), us);
                for (FileSet fs : filesets)
                    us.add(fs.getDir().toURI().toURL());
            } else {
                final URLClassLoader ucl = (URLClassLoader) getClass().getClassLoader();
                us.addAll(Arrays.asList(ucl.getURLs()));
            }
            setURLs(us);

            log("Classpath URLs: " + Arrays.toString(this.urls), Project.MSG_INFO);

            List<URL> pus = new ArrayList<>();
            if (ant) {
                for (FileSet fs : filesets)
                    pus.add(fs.getDir().toURI().toURL());
            } else
                pus.add(projectDir.toUri().toURL());
            log("Project URLs: " + pus, Project.MSG_INFO);

            final long tStart = System.nanoTime();

            scanExternalSuspendables();

            final long tScanExternal = System.nanoTime();
            if (auto)
                log("Scanned external suspendables in " + (tScanExternal - tStart) / 1000000 + " ms", Project.MSG_INFO);

            // scan classes in filesets
            Function<InputStream, Void> fileVisitor = new Function<InputStream, Void>() {
                @Override
                public Void apply(InputStream is1) {
                    try (InputStream is = is1) {
                        createGraph(is);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            if (ant)
                visitAntProject(fileVisitor);
            else
                visitProjectDir(fileVisitor);
            scanSuspendablesFile(fileVisitor);

            final long tBuildGraph = System.nanoTime();
            log("Built method graph in " + (tBuildGraph - tScanExternal) / 1000000 + " ms", Project.MSG_INFO);

            walkGraph();
            final long tWalkGraph = System.nanoTime();
            log("Walked method graph in " + (tWalkGraph - tBuildGraph) / 1000000 + " ms", Project.MSG_INFO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void scanExternalSuspendables() throws IOException {
        ClassLoaderUtil.accept(cl, new ClassLoaderUtil.Visitor() {
            @Override
            public void visit(String resource, URL url, ClassLoader cl) throws IOException {
                if (resource.startsWith("java/util") || resource.startsWith("java/lang") || resource.startsWith("co/paralleluniverse/asm"))
                    return;
                if (isClassFile(url.getFile())) {
                    try (InputStream is = cl.getResourceAsStream(resource)) { // cl.getResourceAsStream(resource)
                        new ClassReader(is)
                                .accept(new SuspendableClassifier(false, ASMAPI, null), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
                    } catch (Exception e) {
                        System.err.println("Exception thrown during processing of " + resource + " at " + url);
                        throw e;
                    }
                }
            }
        });
    }

    private void visitAntProject(Function<InputStream, Void> classFileVisitor) throws IOException {
        for (FileSet fs : filesets) {
            try {
                final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                final String[] includedFiles = ds.getIncludedFiles();
                for (String filename : includedFiles) {
                    if (isClassFile(filename)) {
                        try {
                            File file = new File(fs.getDir(), filename);
                            if (file.isFile())
                                classFileVisitor.apply(new FileInputStream(file));
                            else
                                log("File not found: " + filename);
                        } catch (Exception e) {
                            throw new RuntimeException("Exception while processing " + filename, e);
                        }
                    }
                }
            } catch (BuildException ex) {
                log(ex.getMessage(), ex, Project.MSG_WARN);
            }
        }
    }

    private void visitProjectDir(final Function<InputStream, Void> classFileVisitor) throws IOException {
        Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    if (isClassFile(file.getFileName().toString()))
                        classFileVisitor.apply(Files.newInputStream(file));
                    return FileVisitResult.CONTINUE;
                } catch (Exception e) {
                    throw new RuntimeException("Exception while processing " + file, e);
                }
            }
        });
    }

    /**
     * Visits classes whose methods are found in the suspendables file, as if they were part of the project
     */
    private void scanSuspendablesFile(Function<InputStream, Void> classFileVisitor) {
        // scan classes in suspendables file
        if (suspendablesFile != null) {
            SimpleSuspendableClassifier tssc = new SimpleSuspendableClassifier(suspendablesFile);
            final Set<String> cs = new HashSet<>();
            cs.addAll(tssc.getSuspendableClasses());
            for (String susMethod : tssc.getSuspendables())
                cs.add(susMethod.substring(0, susMethod.indexOf('.')));

            for (String className : cs) {
                try {
                    log("Scanning suspendable class:" + className, Project.MSG_VERBOSE);
                    classFileVisitor.apply(cl.getResourceAsStream(classToResource(className)));
                } catch (Exception e) {
                    throw new RuntimeException("Exception while processing " + className, e);
                }
            }
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
            log("Searching suspendables in " + className, Project.MSG_DEBUG);
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
            final MethodVisitor mv = super.visitMethod(access, methodname, desc, signature, exceptions);

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

            final SuspendableType suspendable1 = suspendable;
            return new MethodVisitor(api, mv) {
                private SuspendableType susp = suspendable1 != SuspendableType.NON_SUSPENDABLE ? suspendable1 : null;

                @Override
                public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                    final AnnotationVisitor av = super.visitAnnotation(desc, visible);

                    switch (adesc) {
                        case ANNOTATION_DESC:
                            susp = noImpl ? SuspendableType.SUSPENDABLE_SUPER : SuspendableType.SUSPENDABLE;
                            break;
                        case DONT_INSTRUMENT_ANNOTATION_DESC:
                            susp = SuspendableType.NON_SUSPENDABLE;
                            break;
                    }

                    return av;
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();

                    if (susp != null)
                        markKnownSuspendable(methodname, desc, susp);
                }
            };
        }

        private void markKnownSuspendable(String methodname, String desc, SuspendableType sus) {
            final MethodNode method = getOrCreateMethodNode(className + '.' + methodname + desc);
            method.owner = className;
            method.inProject |= inProject;
            method.setSuspendType(max(method.suspendType, sus));
            method.known = true;

            if (auto || inProject)
                knownSuspendablesOrSupers.add(method);

            log("Known suspendable " + className + '.' + methodname + desc, Project.MSG_VERBOSE);
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
        private final List<String> methods = new ArrayList<>();
        private ClassNode cn;

        public ClassNodeVisitor(boolean inProject, int api, ClassVisitor cv) {
            super(api, cv);
            this.inProject = inProject;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);

            this.className = name;
            log("Loading and analyzing class " + className, Project.MSG_DEBUG);

            cn = getOrCreateClassNode(className);
            cn.inProject |= inProject;
            cn.setSupers(superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodname, String desc, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, methodname, desc, signature, exceptions);

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
        private final boolean inProject;
        private String className;

        public CallGraphVisitor(boolean inProject, int api, ClassVisitor cv) {
            super(api, cv);
            this.inProject = inProject;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String methodname, final String desc, String signature, String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, methodname, desc, signature, exceptions);

            final MethodNode caller = getOrCreateMethodNode(className + '.' + methodname + desc);
            caller.inProject |= inProject;
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
                    else {
                        final MethodNode callee = getOrCreateMethodNode(owner + '.' + name + desc);
                        log("Adding caller " + caller + " to " + callee, Project.MSG_DEBUG);
                        callee.addCaller(caller);
                    }
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                    super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

                    log("NOTE: InvokeDynamic invocation in " + methodToString(), Project.MSG_WARN);
                }

                private String methodToString() {
                    return (className + '.' + methodname + "(" + Arrays.toString(Type.getArgumentTypes(desc)) + ") - " + className + '.' + methodname + desc);
                }
            };
        }
    }

    private void createGraph(InputStream classStream) throws IOException {
        final ClassReader cr = new ClassReader(classStream);
        ClassVisitor cv = null;
        cv = new SuspendableClassifier(true, ASMAPI, cv);
        cv = new ClassNodeVisitor(true, ASMAPI, cv);
        if (auto)
            cv = new CallGraphVisitor(true, ASMAPI, cv);

        cr.accept(cv, ClassReader.SKIP_DEBUG | (auto ? 0 : ClassReader.SKIP_CODE));
    }

    private void walkGraph() {
        final Queue<MethodNode> q = new ArrayDeque<>();
        q.addAll(knownSuspendablesOrSupers); // start the bfs from the manualSusp (all classpath)

        while (!q.isEmpty()) {
            final MethodNode m = q.poll();
            if (m.inProject) {
                followBridges(q, getClassNode(m), m);
                followSupers(q, getClassNode(m), m);
            }
            followNonOverriddenSubs(q, getClassNode(m), m);
            followCallers(q, m);
        }
    }

    private void followBridges(Queue<MethodNode> q, ClassNode cls, MethodNode method) {
        log("followBridges " + method + " " + cls, Project.MSG_DEBUG);
        if (cls == null)
            return;

        if (method.suspendType == SuspendableType.NON_SUSPENDABLE)
            return;
        final List<String> bridges = cls.getMethodWithDifferentReturn(method.name);
        for (String m1 : bridges) {
            if (!method.name.equals(m1)) {
                MethodNode m = getOrCreateMethodNode(cls.name + '.' + m1);
                if (m.suspendType != SuspendableType.SUSPENDABLE && m.suspendType != SuspendableType.SUSPENDABLE_SUPER) {
                    m.setSuspendType(SuspendableType.SUSPENDABLE_SUPER);
                    q.add(m);
                }
            }
        }
    }

    private boolean followSupers(Queue<MethodNode> q, ClassNode cls, MethodNode method) {
        log("followSupers " + method + " " + cls, Project.MSG_DEBUG);
        if (cls == null)
            return false;

        if (method.suspendType == SuspendableType.NON_SUSPENDABLE || method.setBySuper)
            return false;

        boolean foundMethod = false;

        if (cls.hasMethod(method.name) && method.classNode != cls) {
            final MethodNode m1 = methods.get((cls.name + '.' + method.name).intern());
            if (m1 != null && m1.suspendType == SuspendableType.NON_SUSPENDABLE)
                return false;
            if (m1 == null || m1.suspendType == null) {
                log("Found parent of suspendable method: " + method.owner + '.' + method.name + " in " + cls.name
                        + (cls.inProject ? "" : " NOT IN PROJECT"), cls.inProject ? Project.MSG_VERBOSE : Project.MSG_WARN);
                foundMethod = true;
            }
        }

        // recursively look in superclass and interfaces
        boolean methodInParent = false;
        for (ClassNode s : cls.supers)
            methodInParent |= followSupers(q, fill(s), method);

        if (!foundMethod && methodInParent)
            log("Found parent of suspendable method in a parent of: " + method.owner + '.' + method.name + " in " + cls.name
                    + (cls.inProject ? "" : " NOT IN PROJECT"), cls.inProject ? Project.MSG_VERBOSE : Project.MSG_WARN);

        final boolean res = foundMethod | methodInParent;
        if (res) {
            MethodNode m = getOrCreateMethodNode(cls.name + '.' + method.name);
            if (m.suspendType != SuspendableType.SUSPENDABLE && m.suspendType != SuspendableType.SUSPENDABLE_SUPER) {
                m.setSuspendType(SuspendableType.SUSPENDABLE_SUPER);
                q.add(m);
            }
        }
        return res;
    }

    private void followNonOverriddenSubs(Queue<MethodNode> q, ClassNode cls, MethodNode method) {
        log("followNonOverriddenSubs " + method + " " + cls, Project.MSG_DEBUG);
        if (cls == null)
            return;

        if (method.suspendType == SuspendableType.NON_SUSPENDABLE)
            return;

        if (cls.subs != null) {
            for (ClassNode s : cls.subs) {
                if (s != null && !s.hasMethod(method.name) && s.inProject) {
                    MethodNode sm = getOrCreateMethodNode(s.name + '.' + method.name);
                    sm.inProject = true;
                    sm.refersToSuper = true;
                    if (sm.inProject && sm.suspendType == null) {
                        sm.setSuspendType(method.suspendType);
                        sm.setBySuper = true;
                        q.add(sm);
                        followNonOverriddenSubs(q, s, sm);
                    }
                }
            }
        }
    }

    private void followCallers(Queue<MethodNode> q, MethodNode method) {
        // mark as suspendables methods from the project which are calling of the given bfs node (which is superSuspenable or suspendable)
        for (MethodNode caller : method.getCallers()) {
            if (caller.suspendType == null) { // not yet visited
                q.add(caller);
                log("Marking " + caller + " suspendable because it calls " + method, Project.MSG_VERBOSE);
                caller.setSuspendType(SuspendableType.SUSPENDABLE);
            }
        }
    }

    public void putSuspendablesAndSupers(Collection<String> suspendables, Collection<String> suspendableSupers) {
        for (MethodNode method : methods.values()) {
            if (!method.known) {
                if (method.suspendType == SuspendableType.SUSPENDABLE && !method.refersToSuper && suspendables != null)
                    suspendables.add(output(method));
                else if (method.suspendType == SuspendableType.SUSPENDABLE_SUPER && !method.refersToSuper && suspendableSupers != null)
                    suspendableSupers.add(output(method));
                // we don't output refersToSuper methods because the instrumentor's MethodDatabase.isMethodSuspendable0 does that traversal again.
            }
        }
    }

    private static String output(MethodNode method) {
        return method.owner.replace('/', '.') + '.' + method.name;
    }

    private static void outputResults(String outputFile, boolean append, Collection<String> results) throws Exception {
        try (PrintStream out = getOutputStream(outputFile, append)) {
            for (String s : results)
                out.println(s);
        }
    }

    private MethodNode getOrCreateMethodNode(String methodName) {
        methodName = methodName.intern();
        MethodNode entry = methods.get(methodName);
        if (entry == null) {
            entry = new MethodNode(getClassName(methodName), getMethodWithDesc(methodName));
            methods.put(methodName, entry);
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
                try (InputStream is = cl.getResourceAsStream(classToResource(node.name))) {
                    final ClassReader cr = new ClassReader(is);
                    cr.accept(new ClassNodeVisitor(false, ASMAPI, null), ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
                    assert node.supers != null;
                }
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
        private ClassNode[] subs;  // sub classnames
        private int numSubs;
        private String[] methods;

        public ClassNode(String name) {
            this.name = name;
        }

        void setSupers(String superName, String[] interfaces) {
            this.supers = new ClassNode[(superName != null ? 1 : 0) + (interfaces != null ? interfaces.length : 0)];
            int i = 0;
            if (superName != null) {
                supers[i] = getOrCreateClassNode(superName);
                supers[i].addSub(this);
                i++;
            }
            if (interfaces != null) {
                for (String iface : interfaces) {
                    supers[i] = getOrCreateClassNode(iface);
                    supers[i].addSub(this);
                    i++;
                }
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

        List<String> getMethodWithDifferentReturn(String method) {
            method = getMethodWithoutReturn(method);
            List<String> ms = new ArrayList<>();
            for (String m : methods) {
                if (m.startsWith(method))
                    ms.add(m);
            }
            return ms;
        }

        @Override
        public String toString() {
            return "ClassNode{" + name + " inProject: " + inProject + '}';
        }

        private void addSub(ClassNode cn) {
            if (subs == null)
                subs = new ClassNode[4];
            if (numSubs + 1 >= subs.length)
                this.subs = Arrays.copyOf(subs, subs.length * 2);
            subs[numSubs] = cn;
            numSubs++;
        }
    }

    private static class MethodNode {
        String owner;
        ClassNode classNode;
        final String name; // methodname+desc
        //int acc;
        boolean inProject;
        boolean known;
        SuspendableType suspendType;
        boolean refersToSuper;
        boolean setBySuper;
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

        @SuppressWarnings("override")
        public Collection<MethodNode> getCallers() {
            if (callers == null)
                return Collections.emptyList();
            return new AbstractCollection<MethodNode>() {
                public int size() {
                    return numCallers;
                }

                public Iterator<MethodNode> iterator() {
                    return new Iterator<MethodNode>() {
                        private int i;

                        public boolean hasNext() {
                            return i < numCallers;
                        }

                        public MethodNode next() {
                            return callers[i++];
                        }

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }
                    };
                }
            };
        }

        @Override
        public String toString() {
            return "MethodNode{" + owner + '.' + name + " inProject: " + inProject + " suspendType: " + suspendType + '}';
        }

        public void setSuspendType(SuspendableType suspendType) {
//            if ("co/paralleluniverse/strands/Strand".equals(owner) && "join()V".equals(name)) {
//                System.err.println("XXXX:" + owner + "." + name + " " + suspendType);
//                Thread.dumpStack();
//            }
            this.suspendType = suspendType;
            this.setBySuper = false;
        }
    }

    private static String getClassName(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(0, fullMethodWithDesc.lastIndexOf('.'));
    }

    private static String getMethodWithDesc(String fullMethodWithDesc) {
        return fullMethodWithDesc.substring(fullMethodWithDesc.lastIndexOf('.') + 1);
    }

    private static String getMethodWithoutReturn(String fullMethodWithDesc) {
        String m = getMethodWithDesc(fullMethodWithDesc);
        return m.substring(0, m.lastIndexOf(')') + 1);
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

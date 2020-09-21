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

import co.paralleluniverse.asm.ASMUtil;
import co.paralleluniverse.common.reflection.GetDeclaredMethod;
import co.paralleluniverse.fibers.Suspendable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static co.paralleluniverse.asm.ASMUtil.*;
import static java.security.AccessController.doPrivileged;

/**
 *
 * @author pron
 */
public class OldSuspendablesScanner extends Task {
    private static final boolean USE_REFLECTION = false;
    private static final String CLASSFILE_SUFFIX = ".class";
    private URLClassLoader cl;
    private final ArrayList<FileSet> filesets = new ArrayList<>();
    private final Set<String> results = new HashSet<>();
    private String supersFile;
    private boolean append;
    private SimpleSuspendableClassifier ssc;
    private String suspendablesFile;

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setOutputFile(String outputFile) {
        this.supersFile = outputFile;
    }

    public void setAppend(boolean value) {
        this.append = value;
    }

    public void setSuspendablesFile(String suspendablesFile) {
        this.suspendablesFile = suspendablesFile;
    }

    public void run(String[] prefixes) throws Exception {
        for (String prefix : prefixes)
            collect(prefix);
        outputResults(supersFile, append, results);
    }

    public void nonAntExecute(String[] paths) throws Exception {
        readSuspandables();
        if (USE_REFLECTION)
            log("Using reflection", Project.MSG_INFO);
        List<URL> urls = new ArrayList<>();
        for (String path : paths)
            urls.add(new File(path).toURI().toURL());
        log("URLs: " + urls, Project.MSG_VERBOSE);

        cl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        for (String path : paths) {
            for (File file : recursiveWalk(path)) {
                if (file.getName().endsWith(CLASSFILE_SUFFIX) && file.isFile())
                    scanClass(file);
            }
        }
        scanSuspendablesFile();
        outputResults(supersFile, append, results);
    }

    @Override
    public void execute() throws BuildException {
        readSuspandables();
        if (USE_REFLECTION)
            log("Using reflection", Project.MSG_INFO);
        try {
            List<URL> urls = new ArrayList<>();
            for (FileSet fs : filesets)
                urls.add(fs.getDir().toURI().toURL());
            cl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
            log("URLs: " + Arrays.toString(cl.getURLs()), Project.MSG_INFO);

            // scan classes in filesets
            for (FileSet fs : filesets) {
                try {
                    final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                    final String[] includedFiles = ds.getIncludedFiles();

                    for (String filename : includedFiles) {
                        if (filename.endsWith(CLASSFILE_SUFFIX)) {
                            File file = new File(fs.getDir(), filename);
                            if (file.isFile())
                                scanClass(file);
                            else
                                log("File not found: " + filename);
                        }
                    }
                } catch (BuildException ex) {
                    log(ex.getMessage(), ex, Project.MSG_WARN);
                }
            }

            scanSuspendablesFile();
            log("OUTPUT: " + supersFile, Project.MSG_INFO);
            outputResults(supersFile, append, results);
        } catch (Exception e) {
            log(e, Project.MSG_ERR);
            throw new BuildException(e);
        }
    }

    private void scanSuspendablesFile() throws Exception {
        // scan classes in suspendables file
        if (ssc != null) {
            Set<String> classes = new HashSet<>();
            for (String susCls : ssc.getSuspendableClasses())
                classes.add(susCls);
            for (String susMethod : ssc.getSuspendables())
                classes.add(susMethod.substring(0, susMethod.indexOf('.')));
            for (String className : classes) {
                log("scanning suspendable class:" + className, Project.MSG_VERBOSE);
                scanClass(getClassNode(className, cl, true));
            }
        }
    }

    public void readSuspandables() {
        if (suspendablesFile != null) {
            if (!new File(suspendablesFile).isFile())
                log("suspendable file " + suspendablesFile + " not found", Project.MSG_INFO);
            ssc = new SimpleSuspendableClassifier(suspendablesFile);
            log("suspendablesFile: " + suspendablesFile, Project.MSG_INFO);
        }
    }

    private Set<String> collect(String prefix) throws Exception {
        prefix = prefix.trim();
        prefix = prefix.replace('.', '/');
        for (Enumeration<URL> urls = ClassLoader.getSystemResources(prefix); urls.hasMoreElements();) {
            URL url = urls.nextElement();
            File file = new File(url.getFile());
            if (file.isDirectory())
                scanClasses(file);
        }
        return results;
    }

    private void scanClasses(File file) throws Exception {
        if (file.isDirectory()) {
            System.out.println("Scanning dir: " + file.getPath());
            for (File f : file.listFiles())
                scanClasses(f);
        } else
            scanClass(file);
    }

    private void scanClass(File file) throws Exception {
        log("Scanning " + file, Project.MSG_VERBOSE);
        if (file != null) {
            if (USE_REFLECTION)
                scanClass(Class.forName(extractClassName(file)));
            else
                scanClass(getClassNode(new FileInputStream(file), true));
        }
    }

    private static void outputResults(String outputFile, boolean append1, Collection<String> results) throws Exception {
        try (PrintStream out = getOutputStream(outputFile, append1)) {
            List<String> sorted = new ArrayList<>(results);
            Collections.sort(sorted);
            for (String s : sorted) {
                // if (out != System.out)
                //    System.out.println(s);
                out.println(s);
            }
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

    boolean isSuspendable(ClassNode cls, MethodNode m) {
        return hasAnnotation(Suspendable.class, m)
                || (ssc != null && ssc.isSuspendable(cls.name, m.name, m.desc));
    }

    /////////// ASM
    void scanClass(ClassNode cls) throws Exception {
        List<MethodNode> methods = cls.methods;
        for (MethodNode m : methods) {
            if (isSuspendable(cls, m)) {
                log("Found annotated method: " + cls.name + "." + m.name + m.signature, Project.MSG_VERBOSE);
                findSuperDeclarations(cls, cls, m);
            }
        }
    }

    private boolean findSuperDeclarations(ClassNode cls, ClassNode declaringClass, MethodNode method) throws IOException {
        if (cls == null)
            return false;

        boolean foundMethod = false;
        MethodNode m;
        if ((m = getMethod(method, cls)) != null) {
            foundMethod = true;
            if (!ASMUtil.equals(cls, declaringClass) && !isSuspendable(cls, m)) {
                log("Found parent of annotated method: " + declaringClass.name + "." + method.name + method.signature + " in " + cls.name, Project.MSG_VERBOSE);
                results.add(cls.name.replace('/', '.') + '.' + method.name);
            }
        }

        // recursively look in superclass and interfaces
        boolean methodInParent = false;
        methodInParent |= findSuperDeclarations(getClassNode(cls.superName, cl, true), declaringClass, method);
        for (String iface : (List<String>) cls.interfaces)
            methodInParent |= findSuperDeclarations(getClassNode(iface, cl, true), declaringClass, method);
        if (!foundMethod && methodInParent) {
            log("Found parent of annotated method in a parent of: " + declaringClass.name + "." + method.name + method.signature + " in " + cls.name, Project.MSG_VERBOSE);
            results.add(cls.name.replace('/', '.') + '.' + method.name);
        }

        return foundMethod | methodInParent;
    }

    ///////// REFLECTION
    private String extractClassName(File file) {
        String fileName = file.getPath();
        URL[] urls = ((URLClassLoader) cl).getURLs();
        for (URL url : urls) {
            if (fileName.startsWith(url.getPath())) {
                String className = fileName.substring(url.getPath().length(),
                        fileName.length() - CLASSFILE_SUFFIX.length()).replace('/', '.');
                return className;
            }
        }
        throw new RuntimeException();
    }

    private void scanClass(Class<?> cls) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Suspendable.class))
                findSuperDeclarations(cls, m);
        }
    }

    private void findSuperDeclarations(Class<?> cls, Method method) {
        if (cls == null)
            return;

        if (!cls.equals(method.getDeclaringClass())) {
            try {
                doPrivileged(new GetDeclaredMethod(cls, method.getName(), method.getParameterTypes()));
                results.add(cls.getName() + '.' + method.getName());
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (!(t instanceof NoSuchMethodException)) {
                    throw new RuntimeException(t);
                }
            }
        }

        // recursively look in superclass and interfaces
        findSuperDeclarations(cls.getSuperclass(), method);
        for (Class<?> iface : cls.getInterfaces())
            findSuperDeclarations(iface, method);
    }

    private List<File> recursiveWalk(String path) {
        File[] list = new File(path).listFiles();
        List<File> result = new ArrayList<>();
        if (list == null)
            return result;
        for (File f : list)
            if (f.isDirectory())
                result.addAll(recursiveWalk(f.getAbsolutePath()));
            else
                result.add(f);
        return result;
    }
}

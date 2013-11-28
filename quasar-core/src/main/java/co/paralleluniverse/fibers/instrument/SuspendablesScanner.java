/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.fibers.Suspendable;
import static co.paralleluniverse.common.reflection.ASMUtil.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author pron
 */
public class SuspendablesScanner extends Task {
    private static final boolean USE_REFLECTION = false;
    private static final String CLASSFILE_SUFFIX = ".class";
    private ClassLoader cl;
    private final ArrayList<FileSet> filesets = new ArrayList<FileSet>();
    private final Set<String> results = new HashSet<String>();
    private String outputFile;

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void run(String[] prefixes) throws Exception {
        for (String prefix : prefixes)
            collect(prefix);
        outputResults(outputFile);
    }

    @Override
    public void execute() throws BuildException {
        if (USE_REFLECTION)
            log("Using reflection", Project.MSG_INFO);
        try {
            List<URL> urls = new ArrayList<>();
            for (FileSet fs : filesets)
                urls.add(fs.getDir().toURI().toURL());
            System.out.println("URLs: " + urls);
            cl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());

            for (FileSet fs : filesets) {
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
            }
            outputResults(outputFile);
        } catch (Exception e) {
            log(e, Project.MSG_ERR);
            throw new BuildException(e);
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
                scanClass(getClassNode(file, true));
        }
    }

    private void outputResults(String outputFile) throws Exception {
        try (PrintStream out = getOutputStream(outputFile)) {
            List<String> sorted = new ArrayList<String>(results);
            Collections.sort(sorted);
            for (String s : sorted) {
                //            if(out != System.out)
                //                System.out.println(s);
                out.println(s);
            }
        }
    }

    private PrintStream getOutputStream(String outputFile) throws Exception {
        log("OUTPUT: " + outputFile, Project.MSG_INFO);
        if (outputFile != null) {
            outputFile = outputFile.trim();
            if (outputFile.isEmpty())
                outputFile = null;
        }
        if (outputFile != null) {
            File file = new File(outputFile);
            if (file.getParent() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();
            return new PrintStream(file);
        } else
            return System.out;
    }

    /////////// ASM
    private void scanClass(ClassNode cls) throws Exception {
        List<MethodNode> methods = cls.methods;
        for (MethodNode m : methods) {
            if (hasAnnotation(Suspendable.class, m)) {
                log("Found annotated method: " + cls.name + "." + m.name + m.desc, Project.MSG_VERBOSE);
                findSuperDeclarations(cls, cls, m);
            }
        }
    }

    private void findSuperDeclarations(ClassNode cls, ClassNode declaringClass, MethodNode method) throws IOException {
        if (cls == null)
            return;

        if (!ASMUtil.equals(cls, declaringClass) && hasMethod(method, cls)) {
            log("Found parent of annotated method: " + declaringClass.name + "." + method.name + method.desc + " in " + cls.name, Project.MSG_VERBOSE);
            results.add(cls.name.replace('/', '.') + '.' + method.name);
        }

        // recursively look in superclass and interfaces
        findSuperDeclarations(getClassNode(cls.superName, true, cl), declaringClass, method);
        for (String iface : (List<String>) cls.interfaces)
            findSuperDeclarations(getClassNode(iface, true, cl), declaringClass, method);
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

    private void scanClass(Class cls) throws Exception {
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Suspendable.class))
                findSuperDeclarations(cls, m);
        }
    }

    private void findSuperDeclarations(Class cls, Method method) {
        if (cls == null)
            return;

        if (!cls.equals(method.getDeclaringClass())) {
            try {
                cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                results.add(cls.getName() + '.' + method.getName());
            } catch (NoSuchMethodException e) {
            }
        }

        // recursively look in superclass and interfaces
        findSuperDeclarations(cls.getSuperclass(), method);
        for (Class iface : cls.getInterfaces())
            findSuperDeclarations(iface, method);
    }
}

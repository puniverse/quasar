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
package co.paralleluniverse.asm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static co.paralleluniverse.common.resource.ClassLoaderUtil.classToResource;

/**
 *
 * @author pron
 */
public final class ASMUtil {
    public static InputStream getClassInputStream(String className, ClassLoader cl) {
        final String resource = classToResource(className);
        return cl != null ? cl.getResourceAsStream(resource) : ClassLoader.getSystemResourceAsStream(resource);
    }

    public static InputStream getClassInputStream(Class<?> clazz) {
        final InputStream is = getClassInputStream(clazz.getName(), clazz.getClassLoader());
        if (is == null)
            throw new UnsupportedOperationException("Class file " + clazz.getName() + " could not be loaded by the class's classloader " + clazz.getClassLoader());
        return is;
    }

    public static <T extends ClassVisitor> T accept(InputStream is, int flags, T visitor) throws IOException {
        if (is == null)
            return null;
        try (InputStream is1 = is) {
            new ClassReader(is1).accept(visitor, flags);
            return visitor;
        }
    }

    public static <T extends ClassVisitor> T accept(byte[] buffer, int flags, T visitor) {
        if (buffer == null)
            throw new NullPointerException("Buffer is null");
        new ClassReader(buffer).accept(visitor, flags);
        return visitor;
    }

    public static <T extends ClassVisitor> T accept(String className, ClassLoader cl, int flags, T visitor) throws IOException {
        return accept(getClassInputStream(className, cl), flags, visitor);
    }

    public static <T extends ClassVisitor> T accept(Class<?> clazz, int flags, T visitor) throws IOException {
        return accept(getClassInputStream(clazz), flags, visitor);
    }

    public static ClassNode getClassNode(InputStream is, boolean skipCode) throws IOException {
        return accept(is,
                ClassReader.SKIP_DEBUG | (skipCode ? 0 : ClassReader.SKIP_CODE),
                new ClassNode());
    }

    public static ClassNode getClassNode(String className, ClassLoader cl, boolean skipCode) throws IOException {
        final ClassNode cn = getClassNode(getClassInputStream(className, cl), skipCode);
        if (cn == null)
            throw new IOException("Resource " + classToResource(className) + " not found.");
        return cn;
    }

    public static boolean hasAnnotation(String annDesc, List<AnnotationNode> anns) {
        if (anns == null)
            return false;
        for (AnnotationNode ann : anns) {
            if (ann.desc.equals(annDesc))
                return true;
        }
        return false;
    }

    public static boolean hasAnnotation(Class<?> ann, List<AnnotationNode> anns) {
        return hasAnnotation(Type.getDescriptor(ann), anns);
    }

    public static boolean hasAnnotation(Class<?> ann, MethodNode m) {
        return hasAnnotation(ann, m.visibleAnnotations);
    }

    public static MethodNode getMethod(MethodNode method, List<MethodNode> ms) {
        if (ms == null)
            return null;
        for (MethodNode m : ms) {
            if (equals(method, m))
                return m;
        }
        return null;
    }

    public static MethodNode getMethod(MethodNode method, ClassNode c) {
        return getMethod(method, c.methods);
    }

    public static boolean hasMethod(MethodNode method, List<MethodNode> ms) {
        return getMethod(method, ms) != null;
    }

    public static boolean hasMethod(MethodNode method, ClassNode c) {
        return hasMethod(method, c.methods);
    }

    public static boolean equals(MethodNode m1, MethodNode m2) {
//        if (Objects.equals(m1.name, m2.name) && m1.signature != null && Objects.equals(m1.signature, m2.signature) != Objects.equals(m1.desc, m2.desc))
//            System.err.println("XXXXX WARN desc and signtures not equal " + m1.name + ":" + m1.desc + ":" + m1.signature + " vs " + m2.desc + ":" + m2.signature);
        return Objects.equals(m1.name, m2.name) && Objects.equals(m1.desc, m2.desc);
    }

    public static boolean equals(ClassNode c1, ClassNode c2) {
        return Objects.equals(c1.name, c2.name);
    }

    private ASMUtil() {
    }
}

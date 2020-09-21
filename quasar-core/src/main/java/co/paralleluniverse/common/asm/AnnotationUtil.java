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
package co.paralleluniverse.common.asm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import static co.paralleluniverse.common.asm.ASMUtil.ASMAPI;

/**
 *
 * @author pron
 */
public final class AnnotationUtil {
    public static boolean hasClassAnnotation(Class<? extends Annotation> annotationType, byte[] classData) {
        return hasClassAnnotation(annotationType, new ClassReader(classData));
    }

    public static boolean hasClassAnnotation(Class<? extends Annotation> annotationType, InputStream classData) throws IOException {
        return hasClassAnnotation(annotationType, new ClassReader(classData));
    }

    private static boolean hasClassAnnotation(Class<? extends Annotation> annClass, ClassReader r) {
        // annotationName = annotationName.replace('.', '/');
        final String annDesc = Type.getDescriptor(annClass);
        final AtomicBoolean res = new AtomicBoolean(false);
        r.accept(new ClassVisitor(ASMAPI) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals(annDesc))
                    res.set(true);
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
        return res.get();
    }

    private AnnotationUtil() {
    }
}

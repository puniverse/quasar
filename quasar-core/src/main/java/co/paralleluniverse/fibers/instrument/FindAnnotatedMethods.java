/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.instrument;

import java.lang.annotation.Annotation;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

/**
 *
 * @author pron
 */
class FindAnnotatedMethods extends ClassVisitor {
    private final String annotation;
    
    FindAnnotatedMethods(int api, Class<Annotation> annotation) {
        super(api);
        this.annotation = Type.getDescriptor(annotation);
    }

    private void foo(int access, String name, String desc, String signature, String[] exceptions) {
        
    }
    
    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return new MethodVisitor(api) {

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if(annotation.equals(desc))
                    foo(access, name, desc, signature, exceptions);
                return null;
            }
            
        };
    }
}

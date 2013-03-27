/*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.continuations.instrument;

import de.matthiasmann.continuations.Coroutine;
import de.matthiasmann.continuations.instrument.MethodDatabase.ClassEntry;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/**
 * Instrument a class by instrumenting all suspendable methods and copying the others.
 * 
 * @author Matthias Mann
 */
public class InstrumentClass extends ClassVisitor {

    static final String COROUTINE_NAME = Type.getInternalName(Coroutine.class);
    static final String ALREADY_INSTRUMENTED_NAME = Type.getDescriptor(AlreadyInstrumented.class);
    
    private final MethodDatabase db;
    private final boolean forceInstrumentation;
    private String className;
    private ClassEntry classEntry;
    private boolean alreadyInstrumented;
    private ArrayList<MethodNode> methods;
    
    public InstrumentClass(ClassVisitor cv, MethodDatabase db, boolean forceInstrumentation) {
        super(Opcodes.ASM4, cv);
        
        this.db = db;
        this.forceInstrumentation = forceInstrumentation;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.classEntry = new ClassEntry(superName);
        
        // need atleast 1.5 for annotations to work
        if(version < Opcodes.V1_5) {
            version = Opcodes.V1_5;
        }
        
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(desc.equals(InstrumentClass.ALREADY_INSTRUMENTED_NAME)) {
            alreadyInstrumented = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        boolean suspendable = CheckInstrumentationVisitor.checkExceptions(exceptions);
        classEntry.set(name, desc, suspendable);
        
        if(suspendable && checkAccess(access) && !(className.equals(COROUTINE_NAME) && name.equals("yield"))) {
            if(db.isDebug()) {
                db.log(LogLevel.INFO, "Instrumenting method %s#%s", className, name);
            }
            
            if(methods == null) {
                methods = new ArrayList<MethodNode>();
            }
            
            MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);
            methods.add(mn);
            return mn;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    @SuppressWarnings("CallToThreadDumpStack")
    public void visitEnd() {
        db.recordSuspendableMethods(className, classEntry);
        
        if(methods != null) {
            if(alreadyInstrumented && !forceInstrumentation) {
                for(MethodNode mn : methods) {
                    mn.accept(makeOutMV(mn));
                }
            } else {
                if(!alreadyInstrumented) {
                    super.visitAnnotation(ALREADY_INSTRUMENTED_NAME, true);
                }
                
                for(MethodNode mn : methods) {
                    MethodVisitor outMV = makeOutMV(mn);
                    try {
                        InstrumentMethod im = new InstrumentMethod(db, className, mn);
                        if(im.collectCodeBlocks()) {
                            if(mn.name.charAt(0) == '<') {
                                throw new UnableToInstrumentException("special method", className, mn.name, mn.desc);
                            }
                            im.accept(outMV);
                        } else {
                            mn.accept(outMV);
                        }
                    } catch(AnalyzerException ex) {
                        ex.printStackTrace();
                        throw new InternalError(ex.getMessage());
                    }
                }
            }
        }
        super.visitEnd();
    }

    private MethodVisitor makeOutMV(MethodNode mn) {
        return super.visitMethod(mn.access, mn.name, mn.desc, mn.signature, toStringArray(mn.exceptions));
    }

    private static boolean checkAccess(int access) {
        return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
    }
    
    private static String[] toStringArray(List<?> l) {
        if(l.isEmpty()) {
            return null;
        }
        return l.toArray(new String[l.size()]);
    }
}

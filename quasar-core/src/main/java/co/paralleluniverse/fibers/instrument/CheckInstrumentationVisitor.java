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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.SuspendExecution;
import static co.paralleluniverse.fibers.instrument.Classes.ALREADY_INSTRUMENTED_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.ANNOTATION_DESC;
import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Check if a class contains suspendable methods.
 * Basicly this class checks if a method is declared to throw {@link SuspendExecution}.
 *
 * @author Matthias Mann
 */
public class CheckInstrumentationVisitor extends ClassVisitor {
    private final MethodDatabase db;
    private final SuspendableClassifier classifier;
    private String className;
    private boolean isInterface;
    private boolean suspendableInterface;
    private ClassEntry classEntry;
    private boolean hasSuspendable;
    private boolean alreadyInstrumented;

    public CheckInstrumentationVisitor(MethodDatabase db) {
        super(Opcodes.ASM4);
        this.db = db;
        this.classifier = db.getClassifier();
    }

    public boolean needsInstrumentation() {
        return hasSuspendable;
    }

    ClassEntry getClassEntry() {
        return classEntry;
    }

    public String getName() {
        return className;
    }

    public boolean isAlreadyInstrumented() {
        return alreadyInstrumented;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        this.classEntry = new ClassEntry(superName);
        classEntry.setInterfaces(interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(ALREADY_INSTRUMENTED_DESC))
            this.alreadyInstrumented = true;
        else if (isInterface && desc.equals(ANNOTATION_DESC))
            this.suspendableInterface = true;
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
        SuspendableType suspendable = null;
        if (suspendableInterface)
            suspendable = SuspendableType.SUSPENDABLE_SUPER;
        if (suspendable == null)
            suspendable = classEntry.check(name, desc);
        if (suspendable == null)
            suspendable = classifier.isSuspendable(db, className, classEntry.getSuperName(), classEntry.getInterfaces(), name, desc, signature, exceptions);
        if (suspendable == SuspendableType.SUSPENDABLE) {
            hasSuspendable = true;
            // synchronized methods can't be made suspendable
            if ((access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED)
                throw new UnableToInstrumentException("synchronized method", className, name, desc);
        }
        classEntry.set(name, desc, suspendable);

        if (suspendable == null) // look for @Suspendable annotation
            return new MethodVisitor(Opcodes.ASM4) {
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
                    classEntry.set(name, desc, susp ? SuspendableType.SUSPENDABLE : SuspendableType.NON_SUSPENDABLE);
                    hasSuspendable = hasSuspendable | susp;
                }
            };
        else
            return null;
    }
}

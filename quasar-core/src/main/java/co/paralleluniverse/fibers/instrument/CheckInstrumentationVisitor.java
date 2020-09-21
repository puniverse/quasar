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
import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import java.util.ArrayList;
import java.util.List;

import static co.paralleluniverse.common.asm.ASMUtil.ASMAPI;
import static co.paralleluniverse.fibers.instrument.Classes.INSTRUMENTED_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.SUSPENDABLE_DESC;

/**
 * Check if a class contains suspendable methods.
 * Basicly this class checks if a method is declared to throw {@link SuspendExecution}.
 *
 * @author Matthias Mann
 */
class CheckInstrumentationVisitor extends ClassVisitor {
    private final MethodDatabase db;
    private final SuspendableClassifier classifier;
    private String sourceName;
    private String sourceDebugInfo;
    private boolean isInterface;
    private String className;
    private boolean suspendableInterface;
    private ClassEntry classEntry;
    private boolean hasSuspendable;
    private boolean alreadyInstrumented;
    private final List<Pair<String,String>> methodsSyntheticStatic;

    CheckInstrumentationVisitor(MethodDatabase db) {
        super(ASMAPI);
        this.methodsSyntheticStatic = new ArrayList<>();
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
        classEntry.setIsInterface(isInterface);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.sourceName = source;
        this.sourceDebugInfo = debug;
        super.visitSource(source, debug);
        classEntry.setSourceName(sourceName);
        classEntry.setSourceDebugInfo(sourceDebugInfo);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(INSTRUMENTED_DESC))
            this.alreadyInstrumented = true;
        else if (isInterface && desc.equals(SUSPENDABLE_DESC))
            this.suspendableInterface = true;
        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature, String[] exceptions) {
        SuspendableType suspendable = null;
        if (suspendableInterface)
            suspendable = SuspendableType.SUSPENDABLE_SUPER;
        if (suspendable == null)
            suspendable = classEntry.check(name, desc);
        if (suspendable == null)
            suspendable = classifier.isSuspendable(db, sourceName, sourceDebugInfo, isInterface, className, classEntry.getSuperName(), classEntry.getInterfaces(), name, desc, signature, exceptions);
        if (suspendable == SuspendableType.SUSPENDABLE) {
            hasSuspendable = true;
            // synchronized methods can't be made suspendable
            if ((access & Opcodes.ACC_SYNCHRONIZED) == Opcodes.ACC_SYNCHRONIZED) {
                if (!className.equals("clojure/lang/LazySeq") && !db.isAllowMonitors())
                    throw new UnableToInstrumentException("synchronized method", className, name, desc);
            }
        }
        suspendable = InstrumentClass.suspendableToSuperIfAbstract(access, suspendable);
        classEntry.set(name, desc, suspendable);

        if (suspendable == null) // look for @Suspendable annotation
            return new MethodVisitor(ASMAPI) {
                private boolean susp = false;

                @Override
                public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                    if (adesc.equals(SUSPENDABLE_DESC))
                        susp = true;
                    return null;
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();

                    // If we have a method not suspendable that is synthesized static method then save for later processing.
                    // It may be missing annotations that we can fix later in visitEnd.
                    final int ACC_STATIC_SYNTHETIC = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
                    if (!susp && ((access & ACC_STATIC_SYNTHETIC) == ACC_STATIC_SYNTHETIC) && name.endsWith("$default")) {
                        methodsSyntheticStatic.add(new Pair<>(name, desc));
                    }

                    classEntry.set(name, desc, InstrumentClass.suspendableToSuperIfAbstract(access, susp ? SuspendableType.SUSPENDABLE : SuspendableType.NON_SUSPENDABLE));
                    hasSuspendable = hasSuspendable | susp;
                }
            };
        else
            return null;
    }

    // Given a type descriptor for a synthetic static method create the corresponding method type descriptor.
    private String buildDescriptorFromSyntheticStatic(int startAt, String descStatic) {

        // Kotlin compiler generates static counterpart as below. First argument is this, then
        // list of arguments that must match then a bitmask for setting defaults.
        //
        // @Suspendable
        // private final int defFun1(int a)
        //
        // $FF: synthetic method
        // index:                     0                   1         2         3
        // static int defFun1$default(SyntheticTest var0, int var1, int var2, Object var3)

        // Static synthetic methods have two additional arguments at the end.
        final Type[] staticMethodTypes = Type.getArgumentTypes(descStatic);
        StringBuilder desc = new StringBuilder("(");
        for (int i=startAt; i<staticMethodTypes.length-2; ++i) {
            desc.append(staticMethodTypes[i].toString());
        }
        desc.append(')');
        desc.append(Type.getReturnType(descStatic));
        return desc.toString();
    }

    @Override
    public void visitEnd() {
        // Now look at our synthetic static collection and try and match up these methods with their class counterpart.
        // If we find a match in name and type signature then mark it as suspendable. We know these methods are synthetic.
        // Be careful though as we need to look for class members _or_ static class members with no this pointer.
        for (Pair<String,String> p : methodsSyntheticStatic) {
            // We know name ends in $default. So remove last $default characters.
            final int defaultLen = "$default".length();
            final String name = p.getFirst().substring(0, p.getFirst().length() - defaultLen);

            // Try at argument positions 0 an 1. Static method has no this pointer in first argument position.
            for (int i=0; i<2; ++i) {

                // Try and look up a matching method in classEntry.
                final SuspendableType type = classEntry.check(name, buildDescriptorFromSyntheticStatic(i, p.getSecond()));

                if (type != null) {
                    // Finally if we've found a non null match that is suspendable set it on the synthetic static.
                    if (type != SuspendableType.NON_SUSPENDABLE) {
                        classEntry.set(p.getFirst(), p.getSecond(), SuspendableType.SUSPENDABLE);
                    }
                    // Only find one match and we're done.
                    break;
                }
            }
        }
        super.visitEnd();
    }
}

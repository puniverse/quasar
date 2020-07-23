/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import static co.paralleluniverse.fibers.instrument.Classes.*;
import static co.paralleluniverse.fibers.instrument.QuasarInstrumentor.ASMAPI;

import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Type;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/**
 * Instrument a class by instrumenting all suspendable methods and copying the others.
 *
 * @author Matthias Mann
 * @author pron
 */
class InstrumentClass extends ClassVisitor {
    private final SuspendableClassifier classifier;
    private final MethodDatabase db;
    private boolean forceInstrumentation;
    private String className;
    private String sourceDebugInfo;
    private String sourceName;
    private boolean isInterface;
    private boolean suspendableInterface;
    private ClassEntry classEntry;
    private boolean alreadyInstrumented;
    private ArrayList<MethodNode> methodsSuspendable;
    private ArrayList<MethodNode> methodsSyntheticStatic;

    private RuntimeException exception;

    InstrumentClass(ClassVisitor cv, MethodDatabase db, boolean forceInstrumentation) {
        super(ASMAPI, cv);
        this.db = db;
        this.classifier = db.getClassifier();
        this.forceInstrumentation = forceInstrumentation;
        this.suspendableInterface = false;
    }

    static SuspendableType suspendableToSuperIfAbstract(int access, SuspendableType suspendable) {
        if (suspendable == SuspendableType.SUSPENDABLE
                && ((access & Opcodes.ACC_ABSTRACT) == Opcodes.ACC_ABSTRACT || (access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE))
            return SuspendableType.SUSPENDABLE_SUPER;

        return suspendable;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;

        this.classEntry = db.getOrCreateClassEntry(className, superName);
        classEntry.setInterfaces(interfaces);

        this.forceInstrumentation |= classEntry.requiresInstrumentation();

        // need atleast 1.5 for annotations to work
        if (version < Opcodes.V1_5)
            version = Opcodes.V1_5;

// When Java allows adding interfaces in retransformation, we can mark the class with an interface, which makes checking whether it's instrumented faster (with instanceof)       
//        if(classEntry.requiresInstrumentation() && !contains(interfaces, SUSPENDABLE_NAME)) {
//            System.out.println("XX: Marking " + className + " as " + SUSPENDABLE_NAME);
//            interfaces = add(interfaces, SUSPENDABLE_NAME);
//        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.sourceName = source;
        this.sourceDebugInfo = debug;
        super.visitSource(source, debug);
        classEntry.setSourceName(sourceName);
        classEntry.setSourceDebugInfo(sourceDebugInfo);
    }

    boolean hasSuspendableMethods() {
        return methodsSuspendable != null && !methodsSuspendable.isEmpty();
    }
    boolean hasSyntheticStaticMethods() {
        return methodsSyntheticStatic != null && !methodsSyntheticStatic.isEmpty();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(INSTRUMENTED_DESC))
            this.alreadyInstrumented = true;
        else if (isInterface && desc.equals(SUSPENDABLE_DESC))
            this.suspendableInterface = true;

        return super.visitAnnotation(desc, visible);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        SuspendableType markedSuspendable = null;
        if (suspendableInterface)
            markedSuspendable = SuspendableType.SUSPENDABLE_SUPER;
        if (markedSuspendable == null)
            markedSuspendable = classifier.isSuspendable(db, sourceName, sourceDebugInfo, isInterface, className, classEntry.getSuperName(), classEntry.getInterfaces(), name, desc, signature, exceptions);
        final SuspendableType setSuspendable = classEntry.check(name, desc);

        if (setSuspendable == null)
            classEntry.set(name, desc, markedSuspendable != null ? markedSuspendable : SuspendableType.NON_SUSPENDABLE);

        final SuspendableType suspendable = max(markedSuspendable, setSuspendable, SuspendableType.NON_SUSPENDABLE);

        if (checkAccessForMethodVisitor(access) && !isYieldMethod(className, name)) {
            if (methodsSuspendable == null) {
                methodsSuspendable = new ArrayList<>();
            }
            if (methodsSyntheticStatic == null) {
                methodsSyntheticStatic = new ArrayList<>();
            }

            final MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);

            return new MethodVisitor(ASMAPI, mn) {
                private SuspendableType susp = suspendable;
                private boolean commited = false;

                @Override
                public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                    // look for @Suspendable or @DontInstrument annotation
                    if (adesc.equals(SUSPENDABLE_DESC))
                        susp = SuspendableType.SUSPENDABLE;
                    else if (adesc.equals(DONT_INSTRUMENT_DESC))
                        susp = SuspendableType.NON_SUSPENDABLE;

                    susp = suspendableToSuperIfAbstract(access, susp);

                    return super.visitAnnotation(adesc, visible);
                }

                @Override
                public void visitCode() {
                    commit();
                    super.visitCode();
                }

                @Override
                public void visitEnd() {
                    if (exception != null) {
                        return;
                    }
                    commit();
                    try {
                        super.visitEnd();
                    } catch (RuntimeException e) {
                        exception = e;
                    }
                }

                private void commit() {
                    if (commited) {
                        return;
                    }
                    commited = true;

                    if (db.isDebug())
                        db.log(LogLevel.INFO, "Method %s#%s%s suspendable: %s (markedSuspendable: %s setSuspendable: %s)", className, name, desc, susp, susp, setSuspendable);
                    classEntry.set(name, desc, susp);

                    if (susp == SuspendableType.SUSPENDABLE && checkAccessForMethodInstrumentation(access)) {
                        if (isSynchronized(access)) {
                            if (!db.isAllowMonitors())
                                throw new UnableToInstrumentException("synchronization", className, name, desc);
                            else
                                db.log(LogLevel.WARNING, "Method %s#%s%s is synchronized", className, name, desc);
                        }
                        methodsSuspendable.add(mn);
                    } else {

                        final int ACC_STATIC_SYNTHETIC = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

                        // If we may have a synthesized default method then save it for later processing.
                        if (((access & ACC_STATIC_SYNTHETIC) == ACC_STATIC_SYNTHETIC) && name.endsWith("$default")) {
                            methodsSyntheticStatic.add(mn);
                        } else {
                            MethodVisitor _mv = makeOutMV(mn);
                            _mv = new JSRInlinerAdapter(_mv, access, name, desc, signature, exceptions);
                            mn.accept(new MethodVisitor(ASMAPI, _mv) {
                                @Override
                                public void visitEnd() {
                                    // don't call visitEnd on MV
                                }
                            }); // write method as-is
                            this.mv = _mv;
                        }
                    }
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void visitEnd() {
        if (exception != null) {
            throw exception;
        }

        if (hasSyntheticStaticMethods()) {

            // Go through methods that are not suspendable and check for $default methods that have associated suspendable counterparts.
            for (MethodNode mn : methodsSyntheticStatic) {

                // Search though suspendable methods to see if there is a counterpart for this one.
                if (searchSuspendables(mn)) {
                    // We have a matching suspendable method, make this suspendable to.
                    classEntry.set(mn.name, mn.desc, SuspendableType.SUSPENDABLE);
                    methodsSuspendable.add(mn);
                } else {
                    // Output code for this method.
                    mn.accept(makeOutMV(mn));
                }

            }
        }

        classEntry.setRequiresInstrumentation(false);
        db.recordSuspendableMethods(className, classEntry);

        if (hasSuspendableMethods()) {
            if (alreadyInstrumented && !forceInstrumentation) {
                for (MethodNode mn : methodsSuspendable) {
                    db.log(LogLevel.INFO, "Already instrumented and not forcing, so not touching method %s#%s%s", className, mn.name, mn.desc);
                    mn.accept(makeOutMV(mn));
                }
            } else {
                if (!alreadyInstrumented) {
                    emitInstrumentedAnn();
                    classEntry.setInstrumented(true);
                }

                for (MethodNode mn : methodsSuspendable) {
                    final MethodVisitor outMV = makeOutMV(mn);
                    try {
                        InstrumentMethod im = new InstrumentMethod(db, sourceName, className, mn);
                        db.log(LogLevel.DEBUG, "About to instrument method %s#%s%s", className, mn.name, mn.desc);
                        im.accept(outMV, hasAnnotation(mn));
                    } catch (UnableToInstrumentException e) {
                        db.log(LogLevel.WARNING, "UnableToInstrumentException encountered when instrumenting %s#%s%s: %s", 
                                className, mn.name, mn.desc, e.getMessage());
                        mn.accept(outMV);
                    } catch (AnalyzerException ex) {
                        ex.printStackTrace();
                        throw new InternalError(ex.getMessage());
                    }
                }
            }
        } else {
            // if we don't have any suspendable methods, but our superclass is instrumented, we mark this class as instrumented, too.
            if (!alreadyInstrumented && classEntry.getSuperName() != null) {
                ClassEntry superClass = db.getClassEntry(classEntry.getSuperName());
                if (superClass != null && superClass.isInstrumented()) {
                    emitInstrumentedAnn();
                    classEntry.setInstrumented(true);
                }
            }
        }
        super.visitEnd();
    }

    private boolean compareSuspendableArgs(MethodNode mns, Type[] staticMethodTypes) {
        final Type[] suspendableMethodTypes = Type.getArgumentTypes(mns.desc);

        // Double check the length of method arguments array. Should be larger than suspendableMethodTypes.
        if (staticMethodTypes.length <= suspendableMethodTypes.length) {
            return false;
        }

        // Now check the arguments match, just in case method is overloaded.
        // Kotlin compiler generates static counterpart as below. First argument is this, then
        // list of arguments that must match then a bitmask for setting defaults.
        //
        // @Suspendable
        // private final int defFun1(int a)
        //
        // $FF: synthetic method
        // static int defFun1$default(SyntheticTest var0, int var1, int var2, Object var3)

        for (int i = 0; i < suspendableMethodTypes.length; i++) {
            if (!Objects.equals(suspendableMethodTypes[i], staticMethodTypes[i + 1])) {
                return false;
            }
        }

        return true;
    }

    private boolean searchSuspendables(MethodNode mn) {
        // Go through suspendable methods and try to find a match for mn.
        final Type[] staticMethodTypes = Type.getArgumentTypes(mn.desc);
        for (MethodNode mns : methodsSuspendable) {
            if (Objects.equals(mns.name + "$default", mn.name) &&
                compareSuspendableArgs(mns, staticMethodTypes)) {
                return true;
            }
        }
        return false;
    }

    private void emitInstrumentedAnn() {
        final AnnotationVisitor instrumentedAV = visitAnnotation(INSTRUMENTED_DESC, true);
        instrumentedAV.visitEnd();
    }

    private boolean hasAnnotation(MethodNode mn) {
        //noinspection unchecked
        final List<AnnotationNode> ans = mn.visibleAnnotations;
        if (ans == null)
            return false;
        for (AnnotationNode an : ans) {
            if (an.desc.equals(SUSPENDABLE_DESC))
                return true;
        }
        return false;
    }

    private MethodVisitor makeOutMV(MethodNode mn) {
        return super.visitMethod(mn.access, mn.name, mn.desc, mn.signature, toStringArray(mn.exceptions));
    }

    private static boolean isSynchronized(int access) {
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    private static boolean checkAccessForMethodVisitor(int access) {
        return (access & Opcodes.ACC_NATIVE) == 0;
    }

    private static boolean checkAccessForMethodInstrumentation(int access) {
        return (access & Opcodes.ACC_ABSTRACT) == 0;
    }

    private static SuspendableType max(SuspendableType a, SuspendableType b, SuspendableType def) {
        final SuspendableType res = max(a, b);
        return res != null ? res : def;
    }

    private static SuspendableType max(SuspendableType a, SuspendableType b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return b.compareTo(a) > 0 ? b : a;
    }

    private static String[] toStringArray(List<?> l) {
        if (l.isEmpty())
            return null;

        //noinspection RedundantCast,unchecked
        return ((List<String>)l).toArray(new String[l.size()]);
    }
}

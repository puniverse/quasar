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

import static co.paralleluniverse.fibers.instrument.Classes.ALREADY_INSTRUMENTED_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.ANNOTATION_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.DONT_INSTRUMENT_ANNOTATION_DESC;
import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;
import static co.paralleluniverse.fibers.instrument.QuasarInstrumentor.ASMAPI;

import co.paralleluniverse.fibers.LiveInstrumentation;
import co.paralleluniverse.fibers.instrument.MethodDatabase.ClassEntry;
import co.paralleluniverse.fibers.instrument.MethodDatabase.SuspendableType;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;
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
public class InstrumentClassVisitor extends ClassVisitor {
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

    private ArrayList<MethodNode> suspMethods;
    private ArrayList<MethodNode> otherMethods;

    private RuntimeException exception;

    public InstrumentClassVisitor(ClassVisitor cv, MethodDatabase db, boolean forceInstrumentation) {
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

    public boolean hasSuspendableMethods() {
        return suspMethods != null && !suspMethods.isEmpty();
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(ALREADY_INSTRUMENTED_DESC))
            this.alreadyInstrumented = true;
        else if (isInterface && desc.equals(ANNOTATION_DESC))
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

        if (notNative(access) && !isYieldMethod(className, name)) {
            if (suspMethods == null)
                suspMethods = new ArrayList<>();
            if (otherMethods == null)
                otherMethods = new ArrayList<>();
            // Bytecode-level AST of a method, being a MethodVisitor itself can be filled through delegation from another visitor
            final MethodNode mn = new MethodNode(access, name, desc, signature, exceptions);

            // Analyze, fill and enqueue method ASTs
            return new MethodVisitor(ASMAPI, mn) {
                private SuspendableType susp = suspendable;

                private boolean commited = false;

                @Override
                public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                    // look for @Suspendable or @DontInstrument annotation
                    if (adesc.equals(ANNOTATION_DESC))
                        susp = SuspendableType.SUSPENDABLE;
                    else if (adesc.equals(DONT_INSTRUMENT_ANNOTATION_DESC))
                        susp = SuspendableType.NON_SUSPENDABLE;

                    susp = suspendableToSuperIfAbstract(access, susp);

                    return super.visitAnnotation(adesc, visible);
                }

                @Override
                public void visitCode() {
                    commit(); // This is seemingly needed during AoT as `visitEnd` seems not to be called

                    super.visitCode();
                }

                @Override
                public void visitEnd() {
                    if (exception != null)
                        return;

                    commit();
                    try {
                        super.visitEnd();
                    } catch (RuntimeException e) {
                        exception = e;
                    }
                }

                private void commit() {
                    if (commited)
                        return;
                    commited = true;

                    if (db.isDebug())
                        db.log(LogLevel.INFO, "Method %s#%s suspendable: %s (markedSuspendable: %s setSuspendable: %s)", className, name, susp, susp, setSuspendable);
                    classEntry.set(name, desc, susp);

                    // Initial filtering: write out directly methods that we're sure won't need instrumentation
                    if (susp == SuspendableType.SUSPENDABLE && notAbstract(access)) {
                        if (isSynchronized(access)) {
                            if (!db.isAllowMonitors())
                                throw new UnableToInstrumentException("synchronization", className, name, desc);
                            else
                                db.log(LogLevel.WARNING, "Method %s#%s%s is synchronized", className, name, desc);
                        }
                        suspMethods.add(mn);
                    } else if (notAbstract(access)) {
                        otherMethods.add(mn);
                    } else { // Necessary for abstract methods
                        final MethodVisitor _mv = new JSRInlinerAdapter(makeOutMV(mn), access, name, desc, signature, exceptions);
                        mn.accept(new MethodVisitor(ASMAPI, _mv) {
                            @Override
                            public void visitEnd() {
                                // don't call visitEnd on MV
                            }
                        }); // write method as-is
                        this.mv = _mv;
                    }
                }
            };
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void visitEnd() {
        if (exception != null)
            throw exception;

        classEntry.setRequiresInstrumentation(false);
        db.recordSuspendableMethods(className, classEntry);

        // Fix otherMethods
        for (final MethodNode mn : otherMethods) {
            final MethodVisitor outMV = makeOutMV(mn);
            final String[] a = new String[mn.exceptions.size()];
            mn.exceptions.toArray(a);
            final FixSuspInterfMethod fm = new FixSuspInterfMethod(db, className, mn);
            if (db.isDebug())
                db.log(LogLevel.INFO, "About to examine suspension interferences in method %s#%s%s", className, mn.name, mn.desc);

            if (fm.isNeeded()) {
                fm.applySuspensionInterferenceFixes(outMV);
            } else {
                if (db.isDebug())
                    db.log(LogLevel.INFO, "Nothing to fix in method %s#%s%s", className, mn.name, mn.desc);
                mn.accept(outMV);
            }
        }

        if (suspMethods != null && !suspMethods.isEmpty()) {
            if (alreadyInstrumented)
                LiveInstrumentationKB.alreadyInstrumented(className);

            if (alreadyInstrumented && !forceInstrumentation) {
                for (final MethodNode mn : suspMethods) {
                    db.log(LogLevel.INFO, "Already instrumented and not forcing, so only collecting requested live instrumentation information and not touching method %s#%s%s", className, mn.name, mn.desc);
                    try {
                        final InstrumentMethod im = new InstrumentMethod(db, sourceName, className, mn);
                        if (im.analyzeSuspendableCalls())
                            im.collectCodeBlocks(); // Will collect requested live instrumentation information
                    } catch (final AnalyzerException ex) {
                        ex.printStackTrace();
                        throw new InternalError(ex.getMessage());
                    }
                    mn.accept(makeOutMV(mn));
                }
            } else {
                if (!alreadyInstrumented) {
                    LiveInstrumentationKB.instrumenting(className);

                    emitInstrumentedAnn();
                    classEntry.setInstrumented(true);
                }

                // Instrument suspMethods
                for (final MethodNode mn : suspMethods) {
                    final MethodVisitor outMV = makeOutMV(mn);
                    final String[] a = new String[mn.exceptions.size()];
                    mn.exceptions.toArray(a);
                    try {
                        final InstrumentMethod im = new InstrumentMethod(db, sourceName, className, mn);
                        if (db.isDebug())
                            db.log(LogLevel.INFO, "About to instrument method %s#%s%s", className, mn.name, mn.desc);

                        if (im.analyzeSuspendableCalls()) {
                            if (mn.name.charAt(0) == '<')
                                throw new UnableToInstrumentException("special method", className, mn.name, mn.desc);

                            im.applySuspendableCallsInstrumentation(outMV, hasAnnotation(mn));
                        } else {
                            if (db.isDebug())
                                db.log(LogLevel.INFO, "Nothing to instrument in method %s#%s%s", className, mn.name, mn.desc);
                            mn.accept(outMV);
                        }
                    } catch (final AnalyzerException ex) {
                        ex.printStackTrace();
                        throw new InternalError(ex.getMessage());
                    }
                }
            }
        } else {
            // if we don't have any suspendable methods, but our superclass is instrumented, we mark this class as instrumented, too.
            if (!alreadyInstrumented && classEntry.getSuperName() != null) {
                final ClassEntry superClass = db.getClassEntry(classEntry.getSuperName());
                if (superClass != null && superClass.isInstrumented()) {
                    emitInstrumentedAnn();
                    classEntry.setInstrumented(true);
                }
            }
        }
        super.visitEnd();
    }

    private MethodVisitor makeOutMV(MethodNode mn) {
        return super.visitMethod(mn.access, mn.name, mn.desc, mn.signature, toStringArray(mn.exceptions));
    }

    private void emitInstrumentedAnn() {
        final AnnotationVisitor instrumentedAV = visitAnnotation(ALREADY_INSTRUMENTED_DESC, true);
        instrumentedAV.visitEnd();
    }

    private boolean hasAnnotation(MethodNode mn) {
        @SuppressWarnings("unchecked") final List<AnnotationNode> ans = mn.visibleAnnotations;
        if (ans == null)
            return false;
        for (final AnnotationNode an : ans) {
            if (an.desc.equals(ANNOTATION_DESC))
                return true;
        }
        return false;
    }

    static boolean isSynchronized(int access) {
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }

    static boolean notNative(int access) {
        return (access & Opcodes.ACC_NATIVE) == 0;
    }

    static boolean notAbstract(int access) {
        return (access & Opcodes.ACC_ABSTRACT) == 0;
    }

    static SuspendableType max(SuspendableType a, SuspendableType b, SuspendableType def) {
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

    static String[] toStringArray(List<?> l) {
        if (l.isEmpty())
            return null;

        //noinspection SuspiciousToArrayCall
        return l.toArray(new String[l.size()]);
    }
//    
//    private static boolean contains(String[] ifaces, String iface) {
//        for(String i : ifaces) {
//            if(i.equals(iface))
//                return true;
//        }
//        return false;
//    }
//    
//    private static String[] add(String[] ifaces, String iface) {
//        String[] newIfaces = Arrays.copyOf(ifaces, ifaces.length + 1);
//        newIfaces[newIfaces.length - 1] = iface;
//        return newIfaces;
//    }
}

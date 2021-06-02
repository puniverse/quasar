/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2018, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.common.resource.ClassLoaderUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import static co.paralleluniverse.fibers.instrument.Classes.isYieldMethod;
import static java.security.AccessController.doPrivileged;

/**
 * <p>
 * Collects information about classes and their suspendable methods.</p>
 * <p>
 * Provides access to configuration parameters and to logging</p>
 *
 * @author Matthias Mann
 * @author pron
 */
public final class MethodDatabase {
    private static final String JAVA_OBJECT = "java/lang/Object";

    private final WeakReference<ClassLoader> clRef;
    private final SuspendableClassifier classifier;
    private final NavigableMap<String, ClassEntry> classes;
    private final Map<String, String> superClasses;
    private final QuasarInstrumentor instrumentor;

    MethodDatabase(QuasarInstrumentor instrumentor, ClassLoader classloader, SuspendableClassifier classifier) {
        this.instrumentor = instrumentor;
        this.clRef = classloader != null ? new WeakReference<>(classloader) : null;
        this.classifier = classifier;

        classes = new TreeMap<>();
        superClasses = new HashMap<>();
    }

    boolean isAllowMonitors() {
        return instrumentor.isAllowMonitors();
    }

    boolean isAllowBlocking() {
        return instrumentor.isAllowBlocking();
    }

    public SuspendableClassifier getClassifier() {
        return classifier;
    }

    public void log(LogLevel level, String msg, Object... args) {
        instrumentor.log(level, msg, args);
    }

    public void error(String msg, Throwable ex) {
        instrumentor.error(msg, ex);
    }

    public boolean isDebug() {
        return instrumentor.isDebug();
    }

    public boolean isVerbose() {
        return instrumentor.isVerbose();
    }

    public Log getLog() {
        return instrumentor.getLog();
    }

    public String checkClass(File f) {
        try {
            FileInputStream fis = new FileInputStream(f);
            CheckInstrumentationVisitor civ = checkFileAndClose(fis, f.getPath());

            if (civ != null) {
                recordSuspendableMethods(civ.getName(), civ.getClassEntry());

                if (civ.needsInstrumentation()) {
                    if (civ.isAlreadyInstrumented()) {
                        log(LogLevel.INFO, "Found instrumented class: %s", f.getPath());
                        if (JavaAgent.isActive())
                            throw new AssertionError();
                    } else {
                        log(LogLevel.INFO, "Found class: %s", f.getPath());
                        return civ.getName();
                    }
                }
            }
            return null;
        } catch (UnableToInstrumentException ex) {
            throw ex;
        } catch (Exception ex) {
            error(f.getPath(), ex);
            return null;
        }
    }

    private static final int UNKNOWN = 0;
    private static final int JDK = 1;
    private static final int NONSUSPENDABLE = 2;
    private static final int SUSPENDABLE_ABSTRACT = 3;
    private static final int SUSPENDABLE = 4;

    public SuspendableType isMethodSuspendable(String className, String methodName, String methodDesc, int opcode) {
        if (className.startsWith("org/netbeans/lib/")) {
            log(LogLevel.INFO, "Method: %s#%s marked non-suspendable because it is Netbeans library", className, methodName);
            return SuspendableType.NON_SUSPENDABLE;
        }

        int res = isMethodSuspendable0(className, methodName, methodDesc, opcode);
        switch (res) {
            case UNKNOWN:
                return null;
            case JDK:
                if (!className.startsWith("java/"))
                    log(LogLevel.INFO, "Method: %s#%s not in 'java' package but marked non-suspendable anyway because it is a probably part of the JDK", className, methodName);
            // fallthrough
            case NONSUSPENDABLE:
                return SuspendableType.NON_SUSPENDABLE;
            case SUSPENDABLE_ABSTRACT:
                return SuspendableType.SUSPENDABLE_SUPER;
            case SUSPENDABLE:
                return SuspendableType.SUSPENDABLE;
            default:
                throw new AssertionError();
        }
    }

    public ClassEntry getOrLoadClassEntry(String className) {
        ClassEntry entry = getClassEntry(className);
        if (entry == null)
            entry = checkClass(className);
        return entry;
    }

    private int isMethodSuspendable0(String className, String methodName, String methodDesc, int opcode) {
        if (methodName.charAt(0) == '<')
            return NONSUSPENDABLE;   // special methods are never suspendable

        if (isYieldMethod(className, methodName))
            return SUSPENDABLE;

        final ClassEntry entry = getOrLoadClassEntry(className);
        if (entry == null) {
            if (isJDK(className))
                return JDK;

//            if (JavaAgent.isActive())
//                throw new AssertionError();
            return UNKNOWN;
        }

        SuspendableType susp1 = entry.check(methodName, methodDesc);

        int suspendable = UNKNOWN;
        if (susp1 == null)
            suspendable = UNKNOWN;
        else if (susp1 == SuspendableType.SUSPENDABLE)
            suspendable = SUSPENDABLE;
        else if (susp1 == SuspendableType.SUSPENDABLE_SUPER)
            suspendable = SUSPENDABLE_ABSTRACT;
        else if (susp1 == SuspendableType.NON_SUSPENDABLE)
            suspendable = NONSUSPENDABLE;

        if (suspendable == UNKNOWN) {
            if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKESPECIAL) {
                if (entry.getSuperName() != null)
                    suspendable = isMethodSuspendable0(entry.getSuperName(), methodName, methodDesc, opcode);
            }
            if (opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL) { // can be INVOKEVIRTUAL on an abstract class implementing the interface
                for (String iface : entry.getInterfaces()) {
                    int s = isMethodSuspendable0(iface, methodName, methodDesc, opcode);
                    if (s > suspendable)
                        suspendable = s;
                    if (suspendable > JDK)
                        break;
                }
            }
        }

        return suspendable;
    }

    public synchronized ClassEntry getClassEntry(String className) {
        return classes.get(className);
    }

    public synchronized ClassEntry getOrCreateClassEntry(String className, String superType) {
        ClassEntry ce = classes.get(className);
        if (ce == null) {
            ce = new ClassEntry(superType);
            classes.put(className, ce);
        }
        return ce;
    }

    // this method is used by Pulsar
    public synchronized Map<String, ClassEntry> getInnerClassesEntries(String className) {
        Map<String, ClassEntry> tailMap = classes.tailMap(className, true);
        HashMap<String, ClassEntry> map = new HashMap<>();
        for (Map.Entry<String, ClassEntry> entry : tailMap.entrySet()) {
            if (entry.getKey().equals(className) || entry.getKey().startsWith(className + '$'))
                map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    void recordSuspendableMethods(String className, ClassEntry entry) {
        ClassEntry oldEntry;
        synchronized (this) {
            oldEntry = classes.put(className, entry);
        }
        if (oldEntry != null && oldEntry != entry) {
            if (!oldEntry.equals(entry)) {
                log(LogLevel.WARNING, "Duplicate class entries with different data for class: %s", className);
            }
        }
    }

    String getCommonSuperClass(String classA, String classB) {
        if (JAVA_OBJECT.equals(classA) || JAVA_OBJECT.equals(classB)) {
            // If one of these two classes is java.lang.Object
            // then we know that their common super class must
            // also be java.lang.Object. No need to examine any
            // byte-code!
            return JAVA_OBJECT;
        }
        List<String> listA = getSuperClasses(classA);
        List<String> listB = getSuperClasses(classB);
        if (listA == null || listB == null) {
            return null;
        }
        int idx = 0;
        int num = Math.min(listA.size(), listB.size());
        for (; idx < num; idx++) {
            String superClassA = listA.get(idx);
            String superClassB = listB.get(idx);
            if (!superClassA.equals(superClassB)) {
                break;
            }
        }
        if (idx > 0) {
            return listA.get(idx - 1);
        }
        return null;
    }

    public boolean isException(final String className) {
        String currentClassName = className;
        for (;;) {
            if ("java/lang/Throwable".equals(currentClassName))
                return true;

            if (JAVA_OBJECT.equals(currentClassName))
                return false;

            String superClass = getDirectSuperClass(currentClassName);
            if (superClass == null) {
                // Try to extract the names of all super classes from
                // the OSGi bundle wirings instead.
                checkOSGiSuperClasses(className);
                superClass = getSuperClass(currentClassName);
                if (superClass == null) {
                    log(isProblematicClass(currentClassName) ? LogLevel.INFO : LogLevel.WARNING,
                        "Can't determine super class of %s (this is usually related to classloading)", currentClassName);
                    return false;
                }
            }
            currentClassName = superClass;
        }
    }

    protected ClassEntry checkClass(String className) {
        ClassLoader cl = null;
        if (clRef != null) {
            cl = clRef.get();
            if (cl == null) {
                log(LogLevel.INFO, "Can't check class: %s", className);
                return null;
            }
        }

        if (className.startsWith("[")) {
            // Don't try looking for an "array" class.
            return null;
        }

        log(LogLevel.INFO, "Reading class: %s", className);
        try (final InputStream is = doPrivileged(new GetResourceAsStream(cl, className + ".class"))) {
            if (is == null) {
                log(LogLevel.INFO, "Class not found: %s", className);
                return null;
            }
            ClassEntry entry = getClassEntry(className); // getResourceAsStream may have triggered instrumentation
            if (entry == null) {
                final CheckInstrumentationVisitor civ = checkFileAndClose(is, className);
                if (civ != null) {
                    entry = civ.getClassEntry();
                    recordSuspendableMethods(className, entry);
                } else
                    log(LogLevel.INFO, "Class not found: %s", className);
            }
            return entry;
        } catch(IOException e) {
            throw new UncheckedIOException("While opening " + className, e);
        }
    }

    private CheckInstrumentationVisitor checkFileAndClose(InputStream is, String name) throws IOException {
        try (is) {
            ClassReader r = new ClassReader(is);

            CheckInstrumentationVisitor civ = new CheckInstrumentationVisitor(this);
            r.accept(civ, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);

            return civ;
        }
    }

    private String extractSuperClass(String className) {
        ClassLoader cl = null;
        if (clRef != null) {
            cl = clRef.get();
            if (cl == null) {
                return null;
            }
        }

        try (final InputStream is = ClassLoaderUtil.getResourceAsStream(cl, className + ".class")) {
            if (is != null) {
                return ExtractSuperClass.extractFrom(is);
            }
        } catch (IOException ex) {
            error(className, ex);
        }
        return null;
    }

    private List<String> getSuperClasses(final String className) {
        String currentClassName = className;
        List<String> result = new ArrayList<>();
        for (;;) {
            result.add(0, currentClassName);
            if (JAVA_OBJECT.equals(currentClassName)) {
                return result;
            }

            String superClass = getDirectSuperClass(currentClassName);
            if (superClass == null) {
                // Try to extract the names of all super classes from
                // the OSGi bundle wirings instead.
                checkOSGiSuperClasses(className);
                superClass = getSuperClass(currentClassName);
                if (superClass == null) {
                    log(isProblematicClass(currentClassName) ? LogLevel.INFO : LogLevel.WARNING,
                        "Can't determine super class of %s", currentClassName);
                    return null;
                }
            }
            currentClassName = superClass;
        }
    }

    protected String getDirectSuperClass(String className) {
        ClassEntry entry = getClassEntry(className);
        if (entry != null && entry != CLASS_NOT_FOUND)
            return entry.getSuperName();

        String superClass = getSuperClass(className);
        if (superClass == null) {
            superClass = extractSuperClass(className);
            if (superClass != null) {
                String oldSuperClass;
                synchronized (this) {
                    oldSuperClass = superClasses.put(className, superClass);
                }
                if (oldSuperClass != null) {
                    if (!oldSuperClass.equals(superClass))
                        log(LogLevel.WARNING, "Duplicate super class entry with different value: %s vs %s", oldSuperClass, superClass);
                }
            }
        }
        return superClass;
    }

    private void checkOSGiSuperClasses(String className) {
        ClassLoader cl = null;
        if (clRef != null) {
            cl = clRef.get();
        }
        if (cl != null) {
            try {
                Map<String, String> osgiSuperClasses = doPrivileged(new ExtractOSGiSuperClasses(className, cl));
                if (osgiSuperClasses != null) {
                    synchronized(this) {
                        // Do not replace any existing super classes.
                        osgiSuperClasses.keySet().removeAll(superClasses.keySet());
                        superClasses.putAll(osgiSuperClasses);
                    }
                }
            } catch (PrivilegedActionException e) {
                Exception ex = e.getException();
                error(ex.getMessage(), ex);
            }
        }
    }

    private String getSuperClass(String className) {
        synchronized(this) {
            return superClasses.get(className);
        }
    }

    public static boolean isReflectInvocation(String className, String methodName) {
        return "java/lang/reflect/Method".equals(className) && "invoke".equals(methodName);
    }

    public static boolean isSyntheticAccess(String className, String methodName) {
        return methodName.startsWith("access$");
    }

    public static boolean isInvocationHandlerInvocation(String className, String methodName) {
        return className.equals("java/lang/reflect/InvocationHandler") && methodName.equals("invoke");
    }

    public static boolean isMethodHandleInvocation(String className, String methodName) {
        return className.equals("java/lang/invoke/MethodHandle") && methodName.startsWith("invoke");
    }

    public static boolean isJDK(String className) {
        return className.startsWith("java/")
               || className.startsWith("javax/")
               || className.startsWith("sun/")
               || className.startsWith("jdk/")
               || (className.startsWith("com/sun/") && !className.startsWith("com/sun/jersey"));
    }

    public static boolean isProblematicClass(String className) {
        return className.startsWith("org/gradle/")
               || className.startsWith("javax/jms/")
               || className.startsWith("ch/qos/logback/")
               || className.startsWith("org/apache/logging/log4j/")
               || className.startsWith("org/apache/log4j/");
    }

    private static final ClassEntry CLASS_NOT_FOUND = new ClassEntry("<class not found>");

    public enum SuspendableType {
        NON_SUSPENDABLE, SUSPENDABLE_SUPER, SUSPENDABLE
    }

    private static final class GetResourceAsStream implements PrivilegedAction<InputStream> {
        private final ClassLoader cl;
        private final String resourceName;

        GetResourceAsStream(ClassLoader cl, String resourceName) {
            this.cl = cl;
            this.resourceName = resourceName;
        }

        @Override
        public InputStream run() {
            return cl.getResourceAsStream(resourceName);
        }
    }

    public static final class ClassEntry {
        private final Map<String, SuspendableType> methods;
        private String sourceName;
        private String sourceDebugInfo;
        private boolean isInterface;
        private String[] interfaces;
        private final String superName;
        private boolean instrumented;
        private volatile boolean requiresInstrumentation;

        public ClassEntry(String superName) {
            this.superName = superName;
            this.methods = new HashMap<>();
        }

        public void set(String name, String desc, SuspendableType suspendable) {
            String nameAndDesc = key(name, desc);
            methods.put(nameAndDesc, suspendable);
        }

        public String getSourceName() {
            return sourceName;
        }

        public void setSourceName(String sourceName) {
            this.sourceName = sourceName;
        }

        public String getSourceDebugInfo() {
            return sourceDebugInfo;
        }

        public void setSourceDebugInfo(String sourceDebugInfo) {
            this.sourceDebugInfo = sourceDebugInfo;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public void setIsInterface(boolean isInterface) {
            this.isInterface = isInterface;
        }

        public String getSuperName() {
            return superName;
        }

        public void setAll(SuspendableType suspendable) {
            for (Map.Entry<String, SuspendableType> entry : methods.entrySet())
                entry.setValue(suspendable);
        }

        public String[] getInterfaces() {
            return interfaces;
        }

        public void setInterfaces(String[] interfaces) {
            this.interfaces = interfaces;
        }

        public SuspendableType check(String name, String desc) {
            return methods.get(key(name, desc));
        }

        // only for instrumentation verification
        public boolean isSuspendable(String name) {
            for (Map.Entry<String, SuspendableType> entry : methods.entrySet()) {
                String key = entry.getKey();
                if (key.substring(0, key.indexOf('(')).equals(name) && entry.getValue() != SuspendableType.NON_SUSPENDABLE)
                    return true;
            }
            return false;
        }

        public boolean requiresInstrumentation() {
            return requiresInstrumentation;
        }

        public void setRequiresInstrumentation(boolean requiresInstrumentation) {
            this.requiresInstrumentation = requiresInstrumentation;
        }

        @Override
        public int hashCode() {
            return superName.hashCode() * 67 + methods.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassEntry)) {
                return false;
            }
            final ClassEntry other = (ClassEntry) obj;
            // CORDA-3756 names can be null.
            return Objects.equals(superName, other.superName) && methods.equals(other.methods);
        }

        private static String key(String methodName, String methodDesc) {
            return methodName.concat(methodDesc);
        }

        public boolean isInstrumented() {
            return instrumented;
        }

        public void setInstrumented(boolean instrumented) {
            this.instrumented = instrumented;
        }
    }
}

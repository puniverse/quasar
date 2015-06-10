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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.fibers.instrument.MethodDatabase.WorkListEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author pron
 */
public final class QuasarInstrumentor {
    public static final int ASMAPI = Opcodes.ASM5;
    private final static String EXAMINED_CLASS = System.getProperty("co.paralleluniverse.fibers.writeInstrumentedClassesStartingWith");
    private static final boolean allowJdkInstrumentation = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.allowJdkInstrumentation");
    private final MethodDatabase db;
    private boolean check;
    private final boolean aot;

    public QuasarInstrumentor(boolean aot, ClassLoader classLoader, SuspendableClassifier classifier) {
        this.db = new MethodDatabase(classLoader, classifier);
        this.aot = aot;
    }

    public QuasarInstrumentor(ClassLoader classLoader, SuspendableClassifier classifier) {
        this(false, classLoader, classifier);
    }

    public QuasarInstrumentor(boolean aot, ClassLoader classLoader) {
        this(aot, classLoader, new DefaultSuspendableClassifier(classLoader));
    }

    public QuasarInstrumentor(ClassLoader classLoader) {
        this(false, classLoader, new DefaultSuspendableClassifier(classLoader));
    }

    public boolean isAOT() {
        return aot;
    }

    public boolean shouldInstrument(String className) {
        className = className.replace('.', '/');
        if (className.startsWith("co/paralleluniverse/fibers/instrument/") && !Debug.isUnitTest())
            return false;
        if (className.equals(Classes.FIBER_CLASS_NAME) || className.startsWith(Classes.FIBER_CLASS_NAME + '$'))
            return false;
        if (className.equals(Classes.STACK_NAME))
            return false;
        if (className.startsWith("org/objectweb/asm/"))
            return false;
        if (className.startsWith("org/netbeans/lib/"))
            return false;
        if (className.startsWith("java/lang/") || (!allowJdkInstrumentation && MethodDatabase.isJavaCore(className)))
            return false;
        return true;
    }

    public byte[] instrumentClass(String className, byte[] data) {
        className = className.replace('.', '/');
        return shouldInstrument(className) ? instrumentClass(className, new ClassReader(data), false) : data;
    }

    public byte[] instrumentClass(String className, InputStream is) throws IOException {
        className = className.replace('.', '/');
        return instrumentClass(className, new ClassReader(is), false);
    }

    byte[] instrumentClass(String className, InputStream is, boolean forceInstrumentation) throws IOException {
        className = className.replace('.', '/');
        return instrumentClass(className, new ClassReader(is), forceInstrumentation);
    }

    private byte[] instrumentClass(String className, ClassReader r, boolean forceInstrumentation) {
        log(LogLevel.INFO, "TRANSFORM: %s %s", className, (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");
        final ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = (check && EXAMINED_CLASS == null) ? new CheckClassAdapter(cw) : cw;

        if (EXAMINED_CLASS != null && className.startsWith(EXAMINED_CLASS))
            cv = new TraceClassVisitor(cv, new PrintWriter(System.err));

        final InstrumentClass ic = new InstrumentClass(cv, db, forceInstrumentation);
        byte[] transformed = null;
        try {
            r.accept(ic, ClassReader.SKIP_FRAMES);
            transformed = cw.toByteArray();
        } catch (Exception e) {
            if (ic.hasSuspendableMethods()) {
                error("Unable to instrument class " + className, e);
                throw e;
            } else {
                if (!MethodDatabase.isProblematicClass(className))
                    log(LogLevel.DEBUG, "Unable to instrument class " + className);
                return null;
            }
        }

        if (EXAMINED_CLASS != null) {
            if (className.startsWith(EXAMINED_CLASS)) {
                try (OutputStream os = new FileOutputStream(className.replace('/', '.') + ".class")) {
                    os.write(transformed);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (check) {
                ClassReader r2 = new ClassReader(transformed);
                ClassVisitor cv2 = new CheckClassAdapter(new TraceClassVisitor(null), true);
                r2.accept(cv2, 0);
            }
        }

        return transformed;
    }

    public MethodDatabase getMethodDatabase() {
        return db;
    }

    public QuasarInstrumentor setCheck(boolean check) {
        this.check = check;
        return this;
    }

    public QuasarInstrumentor setAllowMonitors(boolean allowMonitors) {
        db.setAllowMonitors(allowMonitors);
        return this;
    }

    public QuasarInstrumentor setAllowBlocking(boolean allowBlocking) {
        db.setAllowBlocking(allowBlocking);
        return this;
    }

    public QuasarInstrumentor setLog(Log log) {
        db.setLog(log);
        return this;
    }

    public QuasarInstrumentor setVerbose(boolean verbose) {
        db.setVerbose(verbose);
        return this;
    }

    public QuasarInstrumentor setDebug(boolean debug) {
        db.setDebug(debug);
        return this;
    }

    public void log(LogLevel level, String msg, Object... args) {
        db.log(level, msg, args);
    }

    public void error(String msg, Throwable ex) {
        db.error(msg, ex);
    }

    public ArrayList<WorkListEntry> getWorkList() {
        return db.getWorkList();
    }

    public void checkClass(File f) {
        db.checkClass(f);
    }
}

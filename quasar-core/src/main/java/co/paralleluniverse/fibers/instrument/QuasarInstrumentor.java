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

import co.paralleluniverse.common.reflection.ReflectionUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.SystemProperties;
import co.paralleluniverse.common.util.VisibleForTesting;
import co.paralleluniverse.fibers.instrument.MethodDatabase.WorkListEntry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;

import org.objectweb.asm.*;
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

    /** @noinspection unused*/
    public QuasarInstrumentor(ClassLoader classLoader, SuspendableClassifier classifier) {
        this(false, classLoader, classifier);
    }

    public QuasarInstrumentor(boolean aot, ClassLoader classLoader) {
        this(aot, classLoader, new DefaultSuspendableClassifier(classLoader));
    }

    public QuasarInstrumentor(ClassLoader classLoader) {
        this(false, classLoader, new DefaultSuspendableClassifier(classLoader));
    }

    /** @noinspection unused*/
    public boolean isAOT() {
        return aot;
    }

    public boolean shouldInstrument(String className) {
        className = className.replace('.', '/');
        return
            !(className.startsWith("co/paralleluniverse/fibers/instrument/") &&
            !Debug.isUnitTest()) &&
                !(className.equals(Classes.FIBER_CLASS_NAME) ||
                  className.startsWith(Classes.FIBER_CLASS_NAME + '$')) &&
            !className.equals(Classes.STACK_NAME) &&
            !className.startsWith("org/objectweb/asm/") &&
            !className.startsWith("org/netbeans/lib/") &&
            !(className.startsWith("java/lang/") ||
                (!allowJdkInstrumentation && MethodDatabase.isJavaCore(className)));
    }

    public byte[] instrumentClass(String className, byte[] data) throws IOException {
        className = className.replace('.', '/');
        try (final InputStream is = new ByteArrayInputStream(data)) {
            return shouldInstrument(className) ? instrumentClass(className, is, false) : data;
        }
    }

    public byte[] instrumentClass(String className, InputStream is) throws IOException {
        className = className.replace('.', '/');
        return instrumentClass(className, is, false);
    }

    @VisibleForTesting
    public byte[] instrumentClass(String className, InputStream is, boolean forceInstrumentation) throws IOException {
        className = className.replace('.', '/');

        log(LogLevel.INFO, "TRANSFORM: %s %s", className, (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");

        // Phase 1, add a label before any suspendable calls, event API is enough
        final ClassReader r1 = new ClassReader(is);
        final ClassWriter cw1 = new ClassWriter(r1, 0);
        final LabelSuspendableCallSitesClassVisitor ic1 = new LabelSuspendableCallSitesClassVisitor(cw1, db);
        r1.accept(ic1, 0);
        byte[] transformed = cw1.toByteArray();

        // DEBUG
        if (EXAMINED_CLASS != null && className.startsWith(EXAMINED_CLASS)) {
            writeToFile(className.replace('/', '.') + "-" + new Date().getTime() + "-before-quasar.class", transformed);
            // cv1 = new TraceClassVisitor(cv, new PrintWriter(System.err));
        }

        // Phase 2, record suspendable call offsets pre-instrumentation, event API is enough (read-only)
        final OffsetClassReader r2 = new OffsetClassReader(transformed);
        final ClassWriter cw2 = new ClassWriter(r2, 0);
        final SuspOffsetsBeforeInstrClassVisitor ic2 = new SuspOffsetsBeforeInstrClassVisitor(cw2, db);
        r2.accept(ic2, 0);
        transformed = cw2.toByteArray(); // Class not really touched, just convenience

        // Phase 3, instrument, tree API
        final ClassReader r3 = new ClassReader(transformed);
        final ClassWriter cw3 = new DBClassWriter(db, r3);
        final ClassVisitor cv3 = (check && EXAMINED_CLASS == null) ? new CheckClassAdapter(cw3) : cw3;
        final InstrumentClassVisitor ic3 = new InstrumentClassVisitor(cv3, db, forceInstrumentation);
        try {
            r3.accept(ic3, ClassReader.SKIP_FRAMES);
            transformed = cw3.toByteArray();
        } catch (final Exception e) {
            if (ic3.hasSuspendableMethods()) {
                error("Unable to instrument class " + className, e);
                throw e;
            } else {
                if (!MethodDatabase.isProblematicClass(className))
                    log(LogLevel.DEBUG, "Unable to instrument class " + className);
                return null;
            }
        }

        // Phase 4, fill suspendable call offsets, event API is enough
        final OffsetClassReader r4 = new OffsetClassReader(transformed);
        final ClassWriter cw4 = new ClassWriter(r4, 0);
        final SuspOffsetsAfterInstrClassVisitor ic4 = new SuspOffsetsAfterInstrClassVisitor(cw4, db);
        r4.accept(ic4, 0);
        transformed = cw4.toByteArray();

        // DEBUG
        if (EXAMINED_CLASS != null) {
            if (className.startsWith(EXAMINED_CLASS))
                writeToFile(className.replace('/', '.') + "-" + new Date().getTime() + "-after-quasar.class", transformed);

            if (check) {
                ClassReader r5 = new ClassReader(transformed);
                ClassVisitor cv5 = new CheckClassAdapter(new TraceClassVisitor(null), true);
                r5.accept(cv5, 0);
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

    private static void writeToFile(String name, byte[] data) {
        try (OutputStream os = Files.newOutputStream(Paths.get(name), StandardOpenOption.CREATE_NEW)) {
            os.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getClassBuffer(ClassReader r) {
        try {
            return (byte[]) ReflectionUtil.accessible(ClassReader.class.getDeclaredField("b")).get(r);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}

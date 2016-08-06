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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.reflection.ReflectionUtil;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.SystemProperties;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.WeakHashMap;

/**
 * @author pron
 */
public final class QuasarInstrumentor {
    public static final int ASMAPI = Opcodes.ASM5;
    private final static String EXAMINED_CLASS = System.getProperty("co.paralleluniverse.fibers.writeInstrumentedClassesStartingWith");
    private static final boolean allowJdkInstrumentation = SystemProperties.isEmptyOrTrue("co.paralleluniverse.fibers.allowJdkInstrumentation");
    private WeakHashMap<ClassLoader, MethodDatabase> dbForClassloader = new WeakHashMap<>();
    private boolean check;
    private final boolean aot;
    private boolean allowMonitors;
    private boolean allowBlocking;
    private Log log;
    private boolean verbose;
    private boolean debug;
    private int logLevelMask;

    public QuasarInstrumentor() {
        this(false);
    }

    public QuasarInstrumentor(boolean aot) {
        this.aot = aot;
        setLogLevelMask();
    }

    public boolean isAOT() {
        return aot;
    }

    public boolean shouldInstrument(String className) {
        if (className != null) {
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
        }
        return true;
    }

    public byte[] instrumentClass(ClassLoader loader, String className, byte[] data) {
        return shouldInstrument(className) ? instrumentClass(loader, className, new ClassReader(data), false) : data;
    }

    public byte[] instrumentClass(ClassLoader loader, String className, InputStream is) throws IOException {
        return instrumentClass(loader, className, new ClassReader(is), false);
    }

    byte[] instrumentClass(ClassLoader loader, String className, InputStream is, boolean forceInstrumentation) throws IOException {
        return instrumentClass(loader, className, new ClassReader(is), forceInstrumentation);
    }

    private byte[] instrumentClass(ClassLoader loader, String className, ClassReader r, boolean forceInstrumentation) {
        className = className != null ? className.replace('.', '/') : null;

        MethodDatabase db = getMethodDatabase(loader);

        if (className != null)
            log(LogLevel.INFO, "TRANSFORM: %s %s", className,
                    (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");
        else
            log(LogLevel.INFO, "TRANSFORM: null className");

        final ClassWriter cw = new DBClassWriter(db, r);
        final ClassVisitor cv = (check && EXAMINED_CLASS == null) ? new CheckClassAdapter(cw) : cw;

        if (EXAMINED_CLASS != null && className != null && className.startsWith(EXAMINED_CLASS)) {
            writeToFile(className.replace('/', '.') + "-before.class", getClassBuffer(r));
            // cv = new TraceClassVisitor(cv, new PrintWriter(System.err));
        }

        final InstrumentClass ic = new InstrumentClass(cv, db, forceInstrumentation);
        byte[] transformed;
        try {
            r.accept(ic, ClassReader.SKIP_FRAMES);
            transformed = cw.toByteArray();
        } catch (final Exception e) {
            if (ic.hasSuspendableMethods()) {
                error("Unable to instrument class " + className, e);
                throw e;
            } else {
                if (className != null && !MethodDatabase.isProblematicClass(className))
                    log(LogLevel.DEBUG, "Unable to instrument class " + className);
                return null;
            }
        }

        if (EXAMINED_CLASS != null) {
            if (className != null && className.startsWith(EXAMINED_CLASS))
                writeToFile(className.replace('/', '.') + "-after.class", transformed);

            if (check) {
                final ClassReader r2 = new ClassReader(transformed);
                final ClassVisitor cv2 = new CheckClassAdapter(new TraceClassVisitor(null), true);
                r2.accept(cv2, 0);
            }
        }

        return transformed;
    }

    public synchronized MethodDatabase getMethodDatabase(ClassLoader loader) {
        if (loader == null)
            throw new IllegalArgumentException();
        if (!dbForClassloader.containsKey(loader)) {
            MethodDatabase newDb = new MethodDatabase(this, loader, new DefaultSuspendableClassifier(loader));
            dbForClassloader.put(loader, newDb);
            return newDb;
        } else
            return dbForClassloader.get(loader);
    }

    public QuasarInstrumentor setCheck(boolean check) {
        this.check = check;
        return this;
    }

    public synchronized boolean isAllowMonitors() {
        return allowMonitors;
    }

    public synchronized QuasarInstrumentor setAllowMonitors(boolean allowMonitors) {
        this.allowMonitors = allowMonitors;
        return this;
    }

    public synchronized boolean isAllowBlocking() {
        return allowBlocking;
    }

    public synchronized QuasarInstrumentor setAllowBlocking(boolean allowBlocking) {
        this.allowBlocking = allowBlocking;
        return this;
    }

    public synchronized QuasarInstrumentor setLog(Log log) {
        this.log = log;
//        for (MethodDatabase db : dbForClassloader.values()) {
//            db.setLog(log);
//        }
        return this;
    }

    public Log getLog() {
        return log;
    }

    public synchronized boolean isVerbose() {
        return verbose;
    }

    public synchronized void setVerbose(boolean verbose) {
        this.verbose = verbose;
        setLogLevelMask();
    }

    public synchronized boolean isDebug() {
        return debug;
    }

    public synchronized void setDebug(boolean debug) {
        this.debug = debug;
        setLogLevelMask();
    }

    private synchronized void setLogLevelMask() {
        logLevelMask = (1 << LogLevel.WARNING.ordinal());
        if (verbose || debug)
            logLevelMask |= (1 << LogLevel.INFO.ordinal());
        if (debug)
            logLevelMask |= (1 << LogLevel.DEBUG.ordinal());
    }

    public void log(LogLevel level, String msg, Object... args) {
        if (log != null && (logLevelMask & (1 << level.ordinal())) != 0)
            log.log(level, msg, args);
    }

    public void error(String msg, Throwable ex) {
        if (log != null)
            log.error(msg, ex);
    }

    public String checkClass(ClassLoader cl, File f) {
        return getMethodDatabase(cl).checkClass(f);
    }

    private static void writeToFile(String name, byte[] data) {
        try (OutputStream os = Files.newOutputStream(Paths.get(name), StandardOpenOption.CREATE_NEW)) {
            os.write(data);
        } catch (final IOException e) {
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

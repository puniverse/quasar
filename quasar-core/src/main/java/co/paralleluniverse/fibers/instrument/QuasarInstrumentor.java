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
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.common.asm.ASMUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * @author pron
 */
public final class QuasarInstrumentor {
    @SuppressWarnings("WeakerAccess")
    public static final int ASMAPI = ASMUtil.ASMAPI;

    private static final List<String> BUILT_IN_PACKAGES = List.of(
        "co/paralleluniverse/asm/",
        "co/paralleluniverse/common/asm/",
        "co/paralleluniverse/common/resource/",
        "org/objectweb/asm/", // For testing
        "org/netbeans/lib/"
    );

    private static boolean isBuiltInPackage(String className) {
        for (String packageName: BUILT_IN_PACKAGES) {
            if (className.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    private static final String EXAMINED_CLASS = System.getProperty("co.paralleluniverse.fibers.writeInstrumentedClasses");
    private static final boolean allowJdkInstrumentation = isEmptyOrTrue("co.paralleluniverse.fibers.allowJdkInstrumentation");
    private final WeakHashMap<ClassLoader, MethodDatabase> dbForClassloader = new WeakHashMap<>();
    private MethodDatabase bootstrapDB;
    private boolean check;
    private final boolean aot;
    private boolean allowMonitors;
    private boolean allowBlocking;
    private final Collection<Pattern> exclusions = new ArrayList<>();
    private final Collection<Pattern> excludedClassLoaders = new ArrayList<>();
    private Log log;
    private boolean verbose;
    private boolean debug;
    private int logLevelMask;

    private static boolean isEmptyOrTrue(String value) {
        if (value == null)
            return false;
        return value.isEmpty() || Boolean.parseBoolean(value);
    }

    public QuasarInstrumentor() {
        this(false);
    }

    public QuasarInstrumentor(boolean aot) {
        this.aot = aot;
        setLogLevelMask();
    }

    @SuppressWarnings("unused")
    public boolean isAOT() {
        return aot;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean shouldInstrument(ClassLoader loader) {
        return loader != null && !isExcludedClassLoader(loader.getClass().getName());
    }

    @SuppressWarnings("WeakerAccess")
    public boolean shouldInstrument(String className) {
        if (className != null) {
            className = className.replace('.', '/');
            if (className.startsWith("co/paralleluniverse/fibers/instrument/") && !Debug.isUnitTest()) {
                return false;
            } else if (className.equals(Classes.FIBER_CLASS_NAME) || className.startsWith(Classes.FIBER_CLASS_NAME + '$')) {
                return false;
            } else if (className.equals(Classes.STACK_NAME)) {
                return false;
            } else if (className.equals(Classes.FIBER_HELPER_NAME) || className.startsWith(Classes.FIBER_HELPER_NAME + '$')) {
                return false;
            } else if (isBuiltInPackage(className)) {
                return false;
            } else if (className.startsWith("java/lang/") || (!allowJdkInstrumentation && MethodDatabase.isJDK(className))) {
                return false;
            } else if (isExcluded(className)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("WeakerAccess")
    public byte[] instrumentClass(ClassLoader loader, String className, byte[] data) throws IOException {
        return shouldInstrument(className) ? instrumentClass(loader, className, new ByteArrayInputStream(data), false) : data;
    }

    @SuppressWarnings("WeakerAccess")
    public byte[] instrumentClass(ClassLoader loader, String className, InputStream is) throws IOException {
        return instrumentClass(loader, className, is, false);
    }

    byte[] instrumentClass(ClassLoader loader, String className, InputStream is, boolean forceInstrumentation) throws IOException {
        className = className != null ? className.replace('.', '/') : null;

        byte[] cb = toByteArray(is);

        MethodDatabase db = getMethodDatabase(loader);

        if (className != null) {
            log(LogLevel.INFO, "TRANSFORM: %s %s", className,
                (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");

            examine(className, "quasar-1-preinstr", cb);
        } else {
            log(LogLevel.INFO, "TRANSFORM: null className");
        }

        // Phase 1, add a label before any suspendable calls, event API is enough
        final ClassReader r1 = new ClassReader(cb);
        final ClassWriter cw1 = new ClassWriter(r1, 0);
        final LabelSuspendableCallSitesClassVisitor ic1 = new LabelSuspendableCallSitesClassVisitor(cw1, db);
        r1.accept(ic1, 0);
        cb = cw1.toByteArray();

        examine(className, "quasar-2", cb);

        // Phase 2, instrument, tree API
        final ClassReader r2 = new ClassReader(cb);
        final ClassWriter cw2 = new DBClassWriter(db, r2);
        final ClassVisitor cv2 = (check && EXAMINED_CLASS == null) ? new CheckClassAdapter(cw2) : cw2;
        final InstrumentClass ic2 = new InstrumentClass(cv2, db, forceInstrumentation);
        try {
            r2.accept(ic2, ClassReader.SKIP_FRAMES);
            cb = cw2.toByteArray();
        } catch (final Exception e) {
            if (ic2.hasSuspendableMethods()) {
                error("Unable to instrument class " + className, e);
                throw e;
            } else {
                if (!MethodDatabase.isProblematicClass(className))
                    log(LogLevel.DEBUG, "Unable to instrument class " + className);
                return null;
            }
        }

        examine(className, "quasar-4", cb);

        // Phase 4, fill suspendable call offsets, event API is enough
        final OffsetClassReader r3 = new OffsetClassReader(cb);
        final ClassWriter cw3 = new ClassWriter(r3, 0);
        final SuspOffsetsAfterInstrClassVisitor ic3 = new SuspOffsetsAfterInstrClassVisitor(cw3, db);
        r3.accept(ic3, 0);
        cb = cw3.toByteArray();

        // DEBUG
        if (EXAMINED_CLASS != null) {
            examine(className, "quasar-5-final", cb);

            if (check) {
                ClassReader r4 = new ClassReader(cb);
                ClassVisitor cv4 = new CheckClassAdapter(new TraceClassVisitor(null), true);
                r4.accept(cv4, 0);
            }
        }

        return cb;
    }

    private void examine(String className, String suffix, byte[] data) {
        if (EXAMINED_CLASS != null && className != null && className.contains(EXAMINED_CLASS)) {
            final String filename = className.replace('/', '.') + "-" + new Date().getTime() + "-" + suffix + ".class";
            writeToFile(filename, data);
//            return new TraceClassVisitor(cv, new PrintWriter(new File(filename)));
        }
    }
    
    @SuppressWarnings("WeakerAccess")
    public synchronized MethodDatabase getMethodDatabase(ClassLoader loader) {
        if (loader == null) {
            if (bootstrapDB == null) {
                bootstrapDB = new MethodDatabase(this, null, new DefaultSuspendableClassifier(null));
            }
            return bootstrapDB;
        }
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

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean isAllowMonitors() {
        return allowMonitors;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized QuasarInstrumentor setAllowMonitors(boolean allowMonitors) {
        this.allowMonitors = allowMonitors;
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean isAllowBlocking() {
        return allowBlocking;
    }

    @SuppressWarnings("WeakerAccess")
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

    @SuppressWarnings("WeakerAccess")
    public synchronized boolean isVerbose() {
        return verbose;
    }

    @SuppressWarnings("WeakerAccess")
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
    
    public synchronized void addExcludedPackage(String packageGlob) {
        exclusions.add(packagePattern(packageGlob));
    }
    
    public synchronized boolean isExcluded(String className) {
        if (className != null) {
            className = className.replace('.', '/');
            
            final int i = className.lastIndexOf('/');
            if (i < 0)
                return false;
            final String packageName = className.substring(0, i);
            
            for (Pattern p : exclusions) {
                if (p.matcher(packageName).matches())
                    return true;
            }
        }
        return false;
    }

    synchronized boolean isExcludedClassLoader(String classLoaderName) {
        for (Pattern pattern : excludedClassLoaders) {
            if (pattern.matcher(classLoaderName).matches())
                return true;
        }
        return false;
    }

    public synchronized void addExcludedClassLoader(String glob) {
        excludedClassLoaders.add(classLoaderPattern(glob));
    }

    private static Pattern classLoaderPattern(String glob) {
        StringBuilder out = new StringBuilder(glob.length() + 5).append('^');
        int i = 0;
        while (i < glob.length()) {
            final char c = glob.charAt(i);
            switch (c) {
                case '.':
                    out.append("\\.");
                    break;
                case '?':
                    out.append('.');
                    break;
                case '*':
                    int j = i + 1;
                    if (j < glob.length()) {
                        char next = glob.charAt(j);
                        if (next == '*') {
                            out.append(".*");
                            ++i;
                            break;
                        }
                    }
                    out.append("[^.]+");
                    break;
                case '$':
                    out.append("\\$");
                    break;
                default:
                    out.append(c);
            }
            ++i;
        }
        out.append('$');
        return Pattern.compile(out.toString());
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

    String checkClass(ClassLoader cl, File f) {
        return getMethodDatabase(cl).checkClass(f);
    }

    private static void writeToFile(String name, byte[] data) {
        try (OutputStream os = Files.newOutputStream(Paths.get(name), StandardOpenOption.CREATE_NEW)) {
            os.write(data);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private static final int BUF_SIZE = 8192;

    private static long copy(InputStream from, OutputStream to)
        throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }
    
    private static Pattern packagePattern(String packageGlob) {
        final String DOT = "[^/]"; // exclude /
        
        final String glob = packageGlob.replace('.', '/');
        
        StringBuilder out = new StringBuilder(glob.length() + 5);
        out.append('^');
        for (int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch (c) {
                case '*':
                    out.append(DOT + "*");
                    break;
                case '?':
                    out.append(DOT);
                    break;
                case '.':
                    out.append("\\.");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                default:
                    out.append(c);
            }
        }
        if (glob.endsWith("**"))
            out.append(".*");
        
        out.append('$');
        
        return Pattern.compile(out.toString());
    }
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
 *
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
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.instrument.MethodDatabase.WorkListEntry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

/**
 *
 * @author pron
 */
public final class QuasarInstrumentor {
    final static String EXAMINED_CLASS = null; // "co/paralleluniverse/fibers/instrument/ReflectionInvokeTest";
    private final MethodDatabase db;
    private boolean check;

    public QuasarInstrumentor(ClassLoader classLoader, SuspendableClassifier classifier) {
        this.db = new MethodDatabase(classLoader, classifier);
    }

    public QuasarInstrumentor(ClassLoader classLoader) {
        this(classLoader, DefaultSuspendableClassifier.instance());
    }

    boolean shouldInstrument(String className) {
        System.out.println("ZZZZZZZZZ: " + className);
        className = className.replace('.', '/');
        if (className.startsWith("org/objectweb/asm/"))
            return false;
        if (className.startsWith("org/netbeans/lib/"))
            return false;
        if (className.equals(Classes.COROUTINE_NAME) || className.startsWith(Classes.COROUTINE_NAME + '$'))
            return false;
        if (MethodDatabase.isJavaCore(className))
            return false;
        System.out.println("VVVVVVVV");
        return true;
    }

    public byte[] instrumentClass(String className, byte[] data) {
        className = className.replace('.', '/');
        return shouldInstrument(className) ? instrumentClass(className, new ClassReader(data)) : data;
    }

    byte[] instrumentClass(String className, FileInputStream fis) throws IOException {
        className = className.replace('.', '/');
        return instrumentClass(className, new ClassReader(fis));
    }

    private byte[] instrumentClass(String className, ClassReader r) {
        log(LogLevel.INFO, "TRANSFORM: %s %s", className, (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");
        final ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = (check && EXAMINED_CLASS == null) ? new CheckClassAdapter(cw) : cw;

        if (EXAMINED_CLASS != null && className.startsWith(EXAMINED_CLASS))
            cv = new TraceClassVisitor(cv, new PrintWriter(System.out));

        final InstrumentClass ic = new InstrumentClass(cv, db, false);
        r.accept(ic, ClassReader.SKIP_FRAMES);
        final byte[] transformed = cw.toByteArray();

        if (EXAMINED_CLASS != null) {
            if (className.startsWith(EXAMINED_CLASS)) {
                try (OutputStream os = new FileOutputStream(className.replace('/', '.') + ".class")) {
                    os.write(transformed);
                } catch (Exception e) {
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

    public void error(String msg, Exception ex) {
        db.error(msg, ex);
    }

    public ArrayList<WorkListEntry> getWorkList() {
        return db.getWorkList();
    }

    public void checkClass(File f) {
        db.checkClass(f);
    }
}

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
/*
 * Copyright (c) 2012, Enhanced Four
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'Enhanced Four' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import jsr166e.ConcurrentHashMapV8;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

/*
 * Created on Nov 21, 2010
 *
 * @author Riven
 * @author Matthias Mann
 */
public class JavaAgent {
    private static volatile boolean active;
    private static final Set<WeakReference<ClassLoader>> classLoaders = Collections.newSetFromMap(new ConcurrentHashMapV8<WeakReference<ClassLoader>, Boolean>());

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        if (!instrumentation.isRetransformClassesSupported())
            System.err.println("Retransforming classes is not supported!");

        MethodDatabase db = new MethodDatabase(Thread.currentThread().getContextClassLoader());
        boolean checkArg = false;
        active = true;

        if (agentArguments != null) {
            for (char c : agentArguments.toCharArray()) {
                switch (c) {
                    case 'v':
                        db.setVerbose(true);
                        break;

                    case 'd':
                        db.setDebug(true);
                        break;

                    case 'm':
                        db.setAllowMonitors(true);
                        break;

                    case 'c':
                        checkArg = true;
                        break;

                    case 'b':
                        db.setAllowBlocking(true);
                        break;

                    default:
                        throw new IllegalStateException("Usage: vdmc (verbose, debug, allow monitors, check class)");
                }
            }
        }

        db.setLog(new Log() {
            @Override
            public void log(LogLevel level, String msg, Object... args) {
                System.out.println("[quasar] " + level + ": " + String.format(msg, args));
            }

            @Override
            public void error(String msg, Exception exc) {
                System.out.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.out);
            }
        });

        Retransform.instrumentation = instrumentation;
        Retransform.db = db;
        Retransform.classLoaders = classLoaders;

        instrumentation.addTransformer(new Transformer(db, checkArg), true);
    }

    public static boolean isActive() {
        return active;
    }

    static byte[] instrumentClass(String className, MethodDatabase db, byte[] data, boolean check) {
//        final String EXAMINED_CLASS = "co/paralleluniverse/fibers/instrument/ReflectionInvokeTest";
        ClassReader r = new ClassReader(data);
        ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = check ? new CheckClassAdapter(cw) : cw;

//        if (className.startsWith(EXAMINED_CLASS))
//            cv = new TraceClassVisitor(cv, new PrintWriter(System.out));

        InstrumentClass ic = new InstrumentClass(cv, db, false);
        r.accept(ic, ClassReader.SKIP_FRAMES);
        byte[] transformed = cw.toByteArray();

//        if (className.startsWith(EXAMINED_CLASS)) {
//            try {
//                OutputStream os = new FileOutputStream(className.replace('/', '.') + ".class");
//                os.write(transformed);
//                os.close();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }

//        if (check) {
//            ClassReader r2 = new ClassReader(transformed);
//            ClassVisitor cv2 = new CheckClassAdapter(new TraceClassVisitor(null), true);
//            r2.accept(cv2, 0);
//        }

        return transformed;
    }

    private static class Transformer implements ClassFileTransformer {
        private final MethodDatabase db;
        private final boolean check;

        public Transformer(MethodDatabase db, boolean check) {
            this.db = db;
            this.check = check;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (className.startsWith("org/objectweb/asm/"))
                return null;
            if (className.equals(Classes.COROUTINE_NAME) || className.startsWith(Classes.COROUTINE_NAME + '$'))
                return null;
            if (MethodDatabase.isJavaCore(className))
                return null;

            db.log(LogLevel.INFO, "TRANSFORM: %s %s", className, (db.getClassEntry(className) != null && db.getClassEntry(className).requiresInstrumentation()) ? "request" : "");

            Retransform.beforeTransform(className, classBeingRedefined, classfileBuffer);

            classLoaders.add(new WeakReference<ClassLoader>(loader));

            try {
                final byte[] tranformed = instrumentClass(className, db, classfileBuffer, check);

                Retransform.afterTransform(className, classBeingRedefined, tranformed);

                return tranformed;
            } catch (Exception ex) {
                if (MethodDatabase.isProblematicClass(className))
                    db.log(LogLevel.INFO, "Unable to instrument %s - %s %s", className, ex, Arrays.toString(ex.getStackTrace()));
                else
                    db.error("Unable to instrument " + className, ex);
                return null;
            } catch (Throwable t) {
                System.out.println("[quasar] ERROR: " + t.getMessage());
                t.printStackTrace(System.out);
                return null;
            }
        }
    }
}

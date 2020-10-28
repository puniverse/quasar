/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2017, Parallel Universe Software Co. All rights reserved.
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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static co.paralleluniverse.common.asm.ASMUtil.ASMAPI;

/*
 * @author pron
 * @author Riven
 * @author Matthias Mann
 */
public class JavaAgent {
    private static final String USAGE = "Usage: vdmcbx(exclusion;...)l(exclusion;...) (verbose, debug, allow monitors, check class, allow blocking)";
    private static volatile boolean ACTIVE;
    private static final Set<WeakReference<ClassLoader>> classLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        if (!instrumentation.isRetransformClassesSupported())
            System.err.println("Retransforming classes is not supported!");

        final QuasarInstrumentor instrumentor = new QuasarInstrumentor(false);
        ACTIVE = true;
        SuspendableHelper.javaAgent = true;

        instrumentor.setLog(new Log() {
            @Override
            public void log(LogLevel level, String msg, Object... args) {
                System.err.println("[quasar] " + level + ": " + String.format(msg, args));
            }

            @Override
            public void error(String msg, Throwable exc) {
                System.err.println("[quasar] ERROR: " + msg);
                exc.printStackTrace(System.err);
            }
        });

        if (agentArguments != null) {
            for (int i = 0; i < agentArguments.length(); i++) {
                char c = agentArguments.charAt(i);
                switch (c) {
                    case 'a': {
                            final String s = parseArgBrackets(agentArguments, ++i);
                            i += s.length() + 1;
                            final String[] attr = s.split("\\=");
                            if (attr.length > 1) {
                                final String[] types = attr[1].split(",");
                                instrumentor.addTypeDesc(attr[0], types);
                            }
                        }
                        break;

                    case 'v':
                        instrumentor.setVerbose(true);
                        break;

                    case 'd':
                        instrumentor.setDebug(true);
                        break;

                    case 'm':
                        instrumentor.setAllowMonitors(true);
                        break;

                    case 'c':
                        instrumentor.setCheck(true);
                        break;

                    case 'b':
                        instrumentor.setAllowBlocking(true);
                        break;
                        
                    case 'x': {
                            final String s = parseArgBrackets(agentArguments, ++i);
                            i += s.length() + 1;

                            String[] exclusions = s.split(";", 0);
                            for (String x : exclusions) {
                                instrumentor.addExcludedPackage(x);
                            }
                        }
                        break;

                    case 'l': {
                            final String s = parseArgBrackets(agentArguments, ++i);
                            i += s.length() + 1;

                            String[] classLoaderExclusions = s.split(";",0);
                            for (String x : classLoaderExclusions) {
                                instrumentor.addExcludedClassLoader(x);
                            }
                        }
                        break;
                    default:
                        throw new IllegalStateException(USAGE);
                }
            }
        }

        Retransform.instrumentation = instrumentation;
        Retransform.instrumentor = instrumentor;
        Retransform.classLoaders = classLoaders;

        instrumentation.addTransformer(new Transformer(instrumentor), true);
    }

    public static void agentmain(String agentArguments, Instrumentation instrumentation) {
        premain(agentArguments, instrumentation);
    }

    public static boolean isActive() {
        return ACTIVE;
    }

    private static class Transformer implements ClassFileTransformer {
        private final QuasarInstrumentor instrumentor;

        public Transformer(QuasarInstrumentor instrumentor) {
            this.instrumentor = instrumentor;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
                if (loader == null) {
                    loader = ClassLoader.getSystemClassLoader();
                }
            }

            if (!instrumentor.shouldInstrument(loader)) {
                return null;
            }

            if (className != null && className.startsWith("clojure/lang/Compiler"))
                return crazyClojureOnceDisable(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

            if (!instrumentor.shouldInstrument(className))
                return null;

            Retransform.beforeTransform(className, classBeingRedefined, classfileBuffer);

            if (loader != null)
                classLoaders.add(new WeakReference<>(loader));

            try {
                final byte[] transformed = instrumentor.instrumentClass(loader, className, classfileBuffer);

                if (transformed != null)
                    Retransform.afterTransform(className, classBeingRedefined, transformed);

                return transformed;
            } catch (Throwable t) {
                instrumentor.error("while transforming " + className + ": " + t.getMessage(), t);
                return null;
            }
        }
    }

    public static byte[] crazyClojureOnceDisable(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!Boolean.parseBoolean(System.getProperty("co.paralleluniverse.pulsar.disableOnce", "false")))
            return classfileBuffer;

        final ClassReader cr = new ClassReader(classfileBuffer);
        final ClassWriter cw = new ClassWriter(cr, 0);
        final ClassVisitor cv = new ClassVisitor(ASMAPI, cw) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {

                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (cst instanceof String && cst.equals("once")) {
                            super.visitLdcInsn("once$disabled-by-pulsar");
                        } else
                            super.visitLdcInsn(cst);
                    }

                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    private static String parseArgBrackets(String args, int indexStart) {
        if (args.charAt(indexStart) != '(') {
            throw new IllegalStateException(USAGE);
        }
        final int indexEnd = args.indexOf(')', ++indexStart);
        if (indexEnd == -1) {
            throw new IllegalStateException(USAGE);
        }
        return args.substring(indexStart, indexEnd);
    }
}

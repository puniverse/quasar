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
package de.matthiasmann.continuations.instrument;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/*
 * Created on Nov 21, 2010
 *
 * @author Riven
 * @author Matthias Mann
 */
public class JavaAgent {
    public static void premain(String agentArguments, Instrumentation instrumentation) {
        MethodDatabase db = new MethodDatabase(Thread.currentThread().getContextClassLoader());
        boolean checkArg = false;

        if(agentArguments != null) {
            for(char c : agentArguments.toCharArray()) {
                switch(c) {
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
            public void log(LogLevel level, String msg, Object... args) {
                System.out.println("[Continuations] " + level + ": " + String.format(msg, args));
            }

            public void error(String msg, Exception exc) {
                System.out.println("[Continuations] ERROR: " + msg);

                exc.printStackTrace(System.out);
            }
        });

        instrumentation.addTransformer(new Transformer(db, checkArg));
    }

    static byte[] instrumentClass(MethodDatabase db, byte[] data, boolean check) {
        ClassReader r = new ClassReader(data);
        ClassWriter cw = new DBClassWriter(db, r);
        ClassVisitor cv = check ? new CheckClassAdapter(cw) : cw;
        InstrumentClass ic = new InstrumentClass(cv, db, false);
        r.accept(ic, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    private static class Transformer implements ClassFileTransformer {
        private final MethodDatabase db;
        private final boolean check;

        public Transformer(MethodDatabase db, boolean check) {
            this.db = db;
            this.check = check;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if(MethodDatabase.isJavaCore(className)) {
                return null;
            }
            if(className.startsWith("org/objectweb/asm/")) {
                return null;
            }

            db.log(LogLevel.INFO, "TRANSFORM: %s", className);

            try {
                return instrumentClass(db, classfileBuffer, check);
            } catch(Exception ex) {
                db.error("Unable to instrument", ex);
                return null;
            }
        }
    }
}

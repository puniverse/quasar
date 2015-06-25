/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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

import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.jar.Manifest;
import sun.misc.Resource;
import sun.misc.URLClassPath;

/**
 *
 * @author pron
 */
public class QuasarURLClassLoader extends URLClassLoader {
    private final QuasarInstrumentor instrumentor;

    public QuasarURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        this.instrumentor = newInstrumentor();
    }

    public QuasarURLClassLoader(URL[] urls) {
        super(urls);
        this.instrumentor = newInstrumentor();
    }

    public QuasarURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
        this.instrumentor = newInstrumentor();
    }

    private QuasarInstrumentor newInstrumentor() {
        QuasarInstrumentor inst = new QuasarInstrumentor(false, this);
        inst.setLog(new Log() {
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
//        inst.setVerbose(true);
//        inst.setDebug(true);
        return inst;
    }

    @Override
    protected Class<?> findClass(final String name)
            throws ClassNotFoundException {
        try {
            return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Class>() {
                        @Override
                        public Class run() throws ClassNotFoundException {
                            String path = name.replace('.', '/').concat(".class");
                            Resource res = ucp().getResource(path, false);
                            if (res != null) {
                                try {
                                    return defineClass(name, instrument(name, res));
                                } catch (IOException e) {
                                    throw new ClassNotFoundException(name, e);
                                }
                            } else {
                                throw new ClassNotFoundException(name);
                            }
                        }
                    }, acc());
        } catch (java.security.PrivilegedActionException pae) {
            throw (ClassNotFoundException) pae.getException();
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = super.getResourceAsStream(name);
        if (is != null && name.endsWith(".class")) {
            try {
                byte[] bytes = ByteStreams.toByteArray(is);
                byte[] instrumented = instrumentor.instrumentClass(name.substring(0, name.length() - ".class".length()), bytes);
                return new ByteArrayInputStream(instrumented);
            } catch (final IOException e) {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException(e);
                    }
                };
            }
        } else
            return is;
    }

    private Resource instrument(final String className, final Resource res) {
        return new Resource() {
            private byte[] instrumented;

            @Override
            public synchronized byte[] getBytes() throws IOException {
                if (instrumented == null) {
                    final byte[] bytes;
                    ByteBuffer bb = res.getByteBuffer();
                    if (bb != null) {
                        final int size = bb.remaining();
                        bytes = new byte[size];
                        bb.get(bytes);
                    } else
                        bytes = res.getBytes();

                    try {
                        this.instrumented = instrumentor.instrumentClass(className, bytes);
                    } catch (Exception ex) {
                        if (MethodDatabase.isProblematicClass(className))
                            instrumentor.log(LogLevel.INFO, "Skipping problematic class instrumentation %s - %s %s", className, ex, Arrays.toString(ex.getStackTrace()));
                        else
                            instrumentor.error("Unable to instrument " + className, ex);
                        instrumented = bytes;
                    }
                }
                return instrumented;
            }

            @Override
            public ByteBuffer getByteBuffer() throws IOException {
                return null;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new AssertionError();
            }

            @Override
            public String getName() {
                return res.getName();
            }

            @Override
            public URL getURL() {
                return res.getURL();
            }

            @Override
            public URL getCodeSourceURL() {
                return res.getCodeSourceURL();
            }

            @Override
            public int getContentLength() throws IOException {
                return res.getContentLength();
            }

            @Override
            public Manifest getManifest() throws IOException {
                return res.getManifest();
            }

            @Override
            public Certificate[] getCertificates() {
                return res.getCertificates();
            }

            @Override
            public CodeSigner[] getCodeSigners() {
                return res.getCodeSigners();
            }
        };
    }
    // private members access
    private static final Field ucpField;
    private static final Field accField;
    private static final Method defineClassMethod;

    static {
        try {
            ucpField = URLClassLoader.class.getDeclaredField("ucp");
            accField = URLClassLoader.class.getDeclaredField("acc");
            defineClassMethod = URLClassLoader.class.getDeclaredMethod("defineClass", String.class, Resource.class);
            ucpField.setAccessible(true);
            accField.setAccessible(true);
            defineClassMethod.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private Class defineClass(String name, Resource res) throws IOException {
        try {
            return (Class) defineClassMethod.invoke(this, name, res);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException)
                throw (IOException) e.getCause();
            throw new RuntimeException(e.getCause());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private URLClassPath ucp() {
        try {
            return (URLClassPath) ucpField.get(this);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private AccessControlContext acc() {
        try {
            return (AccessControlContext) accField.get(this);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}

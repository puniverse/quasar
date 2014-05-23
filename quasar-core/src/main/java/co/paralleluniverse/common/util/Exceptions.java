/*
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
package co.paralleluniverse.common.util;

/**
 *
 * @author pron
 */
public final class Exceptions {
    public static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException)
            throw ((RuntimeException) t);
        if (t instanceof Error)
            throw ((Error) t);
        else
            throw new RuntimeException(t);
    }

    /**
     * Unwraps several common wrapper exceptions and returns the underlying cause.
     *
     * @param t
     * @return
     */
    public static Throwable unwrap(Throwable t) {
        for (;;) {
            if (t == null)
                throw new NullPointerException();

            if (t instanceof java.util.concurrent.ExecutionException)
                t = t.getCause();
            else if (t instanceof java.lang.reflect.InvocationTargetException)
                t = t.getCause();
            else if (t.getClass().equals(RuntimeException.class) && t.getCause() != null)
                t = t.getCause();
            else
                return t;
        }
    }

    public static RuntimeException rethrowUnwrap(Throwable t) {
        throw rethrow(unwrap(t));
    }

    public static <X extends Throwable> RuntimeException rethrowUnwrap(Throwable t, Class<X> exceptionClass) throws X {
        Throwable t1 = unwrap(t);
        if (exceptionClass.isInstance(t1))
            throw exceptionClass.cast(t1);
        throw rethrow(t1);
    }

    public static <X1 extends Throwable, X2 extends Throwable> RuntimeException rethrowUnwrap(Throwable t, Class<X1> exceptionClass1, Class<X2> exceptionClass2) throws X1, X2 {
        Throwable t1 = unwrap(t);
        if (exceptionClass1.isInstance(t1))
            throw exceptionClass1.cast(t1);
        if (exceptionClass2.isInstance(t1))
            throw exceptionClass2.cast(t1);
        throw rethrow(t1);
    }

    static public RuntimeException sneakyThrow(Throwable t) {
        // http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
        if (t == null)
            throw new NullPointerException();
        Exceptions.<RuntimeException>sneakyThrow0(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    static private <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }

    private Exceptions() {
    }
}

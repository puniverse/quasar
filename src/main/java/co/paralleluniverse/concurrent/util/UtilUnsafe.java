package co.paralleluniverse.concurrent.util;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * Simple class to obtain access to the {@link Unsafe} object. {@link Unsafe}
 * is required to allow efficient CAS operations on arrays. Note that the
 * versions in {@link java.util.concurrent.atomic}, such as {@link
 * java.util.concurrent.atomic.AtomicLongArray}, require extra memory ordering
 * guarantees which are generally not needed in these algorithms and are also
 * expensive on most processors.
 */
public class UtilUnsafe {
    private UtilUnsafe() {
    }

    public static Unsafe getUnsafe1() {
        // Not on bootclasspath
        if (UtilUnsafe.class.getClassLoader() == null)
            return Unsafe.getUnsafe();
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(UtilUnsafe.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
        }
    }

    public static Unsafe getUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            try {
                return java.security.AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<Unsafe>() {
                    @Override
                    public Unsafe run() throws Exception {
                        final Class<sun.misc.Unsafe> k = sun.misc.Unsafe.class;
                        if (true) {
                            final Field f = k.getDeclaredField("theUnsafe");
                            f.setAccessible(true);
                            final Object x = f.get(null);
                            return k.cast(x);
                        } else {
                            for (Field f : k.getDeclaredFields()) {
                                f.setAccessible(true);
                                final Object x = f.get(null);
                                if (k.isInstance(x))
                                    return k.cast(x);
                            }
                            throw new NoSuchFieldError("the Unsafe");
                        }
                    }
                });
            } catch (java.security.PrivilegedActionException e) {
                throw new RuntimeException("Could not initialize intrinsics", e.getCause());
            }
        }
    }
}

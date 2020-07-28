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
package co.paralleluniverse.strands;

import co.paralleluniverse.common.reflection.GetAccessDeclaredMethod;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.FiberScheduler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.security.AccessController.doPrivileged;

/**
 * Easily creates {@code StrandFactory}s.
 * You can construct a new instance of this builder and use the builder pattern
 * (call the setter methods followed by {@link #build() build()}) to create a new {@code StrandFactory},
 * or use one of the static {@code from} methods to convert a {@link ThreadFactory} or {@link FiberFactory} into a {@code StrandFactory}.
 * When using the builder pattern, either {@link #setFiber(FiberScheduler) setFiber} or {@link #setThread(boolean) setThread} must be called
 * prior to calling {@link #build() build()}.
 *
 * @author pron
 */
public class StrandFactoryBuilder {
    /**
     * Converts a {@link ThreadFactory} into a {@link StrandFactory}.
     */
    public static StrandFactory from(final ThreadFactory tf) {
        checkNotNull(tf);
        return new StrandFactory() {
            @Override
            public Strand newStrand(SuspendableCallable<?> target) {
                return Strand.of(tf.newThread(Strand.toRunnable(target)));
            }
        };
    }

    /**
     * Converts a {@link FiberFactory} into a {@link StrandFactory}.
     */
    public static StrandFactory from(final FiberFactory ff) {
        checkNotNull(ff);
        return new StrandFactory() {
            @Override
            public Strand newStrand(SuspendableCallable<?> target) {
                return ff.newFiber(target);
            }
        };
    }

    private Boolean fiber;
    private boolean daemon;
    private FiberScheduler fs;
    private String nameFormat;
    private Integer stackSize;
    private Integer priority;
    private Strand.UncaughtExceptionHandler ueh;

    /**
     * Makes the resulting {@link StrandFactory} produce threads.
     *
     * @param daemon whether or not the new threads are daemon threads.
     * @return {@code this}
     */
    public StrandFactoryBuilder setThread(boolean daemon) {
        this.fiber = false;
        this.fs = null;
        this.daemon = daemon;
        return this;
    }

    /**
     * Makes the resulting {@link StrandFactory} produce fibers.
     *
     * @param fs the {@link FiberScheduler} to use for the new fibers, or {@code null} for the default scheduler.
     * @return {@code this}
     */
    public StrandFactoryBuilder setFiber(FiberScheduler fs) {
        this.fiber = true;
        this.fs = fs;
        return this;
    }

    /**
     * Sets the naming format to use when naming strands ({@link Strand#setName}) which are created with this StrandFactory.
     *
     * @param nameFormat a {@link String#format(String, Object...)}-compatible
     *                   format String, to which a unique integer (0, 1, etc.) will be supplied
     *                   as the single parameter. This integer will be unique to the built
     *                   instance of the StrandFactory and will be assigned sequentially. For
     *                   example, {@code "rpc-pool-%d"} will generate strand names like
     *                   {@code "rpc-pool-0"}, {@code "rpc-pool-1"}, {@code "rpc-pool-2"}, etc.
     * @return {@code this}
     */
    public StrandFactoryBuilder setNameFormat(String nameFormat) {
        String ignore = String.format(nameFormat, 0); // fail fast if the format is bad or null
        this.nameFormat = nameFormat;
        return this;
    }

    /**
     * Suggests a stack size of strands created by the resulting StrandFactory.
     * The stack size used might be an approximation of the given value, or even ignore the passed value completely.
     *
     * @param stackSize the stack size, in bytes
     * @return {@code this}
     */
    public StrandFactoryBuilder setStackSize(int stackSize) {
        this.stackSize = stackSize;
        return this;
    }

    /**
     * Sets the priority for strands created by the {@code StrandFactory}.
     * Priority currently applies only to thread, and even then the JVM and the OS are free to ignore it.
     *
     * @param priority the priority
     * @return {@code this}
     */
    public StrandFactoryBuilder setPriority(int priority) {
        checkArgument(priority >= Thread.MIN_PRIORITY, "Strand priority (%s) must be >= %s", priority, Thread.MIN_PRIORITY);
        checkArgument(priority <= Thread.MAX_PRIORITY, "Strand priority (%s) must be <= %s", priority, Thread.MAX_PRIORITY);
        this.priority = priority;
        return this;
    }

    /**
     * Sets the {@link Strand.UncaughtExceptionHandler} for new threads created with this
     * ThreadFactory.
     *
     * @param ueh the uncaught exception handler
     * @return {@code this}
     */
    public StrandFactoryBuilder setUncaughtExceptionHandler(Strand.UncaughtExceptionHandler ueh) {
        this.ueh = ueh;
        return this;
    }

    /**
     * Creates and returns the {@link StrandFactory} that will create new strands based on this builder's settings.
     *
     * @return a new {@link StrandFactory}.
     */
    public StrandFactory build() {
        if (fiber == null)
            throw new IllegalStateException("setFiber or setThread must be called before calling build");
        final boolean _fiber = fiber;
        final boolean _daemon = daemon;
        final FiberScheduler _fs = fs;
        final String _nameFormat = nameFormat;
        final Strand.UncaughtExceptionHandler _ueh = ueh;
        final int _stackSize = stackSize != null ? stackSize : 0;
        final Integer _priority = priority;
        final AtomicLong _count = (nameFormat != null) ? new AtomicLong(0) : null;

        return new StrandFactory() {
            @Override
            public Strand newStrand(SuspendableCallable<?> target) {
                final String name = _nameFormat != null ? String.format(_nameFormat, _count.getAndIncrement()) : null;
                final Strand s;
                if (_fiber) {
                    s = _fs != null ? new Fiber<>(name, _fs, _stackSize, target) : new Fiber<>(name, _stackSize, target);
                } else {
                    // ENT-5489, replace thread gymnastics with use of public API to set name.
                    final Thread t = new Thread(null, Strand.toRunnable(target), "", _stackSize);
                    t.setName(name != null ? name : "Thread-" + String.valueOf(t.getId()));
                    t.setDaemon(_daemon);
                    if (_priority != null) {
                        t.setPriority(_priority);
                    }
                    s = Strand.of(t);
                }
                if (_ueh != null)
                    s.setUncaughtExceptionHandler(_ueh);
                return s;
            }
        };
    }
}

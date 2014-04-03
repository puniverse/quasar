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
package co.paralleluniverse.strands.dataflow;

import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;

/**
 *
 * @author pron
 */
public class Var<T> {
    private static final byte UNKNOWN = 0;
    private static final byte VARFIBER = 1;
    private static final byte PLAIN = 2;
    private static final Object NULL = new Object();

    private final Channel<T> ch;
    private final SuspendableCallable<T> f;
    private final Set<Fiber<?>> registeredFibers = Collections.newSetFromMap(MapUtil.<Fiber<?>, Boolean>newConcurrentHashMap());

    private final ThreadLocal<TLVar> tlv = new ThreadLocal<TLVar>() {

        @Override
        protected TLVar initialValue() {
            return new TLVar();
        }
    };

    private class TLVar {
        final ReceivePort<T> c;
        byte type;
        T val;

        public TLVar() {
            this.c = Channels.newTickerConsumerFor(ch);
        }

    }

    public Var(int history, SuspendableCallable<T> f) {
        if (history <= 0)
            throw new IllegalArgumentException("history must be > 0, but is " + history);
        this.ch = Channels.newChannel(history, Channels.OverflowPolicy.DISPLACE);
        this.f = f;

        if (f != null)
            new VarFiber<T>(this).start();
    }

    public Var(SuspendableCallable<T> f) {
        this(1, f);
    }

    public Var(int history) {
        this(history, null);
    }

    public Var() {
        this(1, null);
    }

    public void set(T val) {
        try {
            if (val == null)
                val = (T) NULL;
            ch.send(val);

            notifyRegistered();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    private void notifyRegistered() {
        for (Fiber<?> f : registeredFibers)
            f.unpark();
    }

    /**
     * This method behaves differently when called from a Var's binding function or from a regular strand.
     * When called from a regular strand, this method returns the Var's next value, blocking until one is available.
     * When called from a Var's binding function, this method returns the Var's current value, unless this Var does not yet have
     * a value; only in that case will this method block.
     */
    public T get() throws SuspendExecution, InterruptedException {
        TLVar tl = tlv.get();
        if (tl.type == UNKNOWN) {
            Fiber currentFiber = Fiber.currentFiber();
            if (currentFiber != null && currentFiber instanceof VarFiber) {
                tl.type = VARFIBER;
                registeredFibers.add(currentFiber);
                ((VarFiber<?>) currentFiber).registeredVars.add(this);
            } else
                tl.type = PLAIN;
        }

        try {
            final T val;
            if (tl.type == VARFIBER) {
                if (tl.val == null)
                    val = tl.c.receive();
                else {
                    T v = tl.c.tryReceive();
                    if (v != null)
                        tl.val = v;
                    val = tl.val;
                }
            } else
                val = tl.c.receive();

            return val == NULL ? null : val;
        } catch (ProducerException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            else if (t instanceof Error)
                throw (Error) t;
            else
                throw new AssertionError(t);
        }
    }

    private static class VarFiber<T> extends Fiber<Void> {
        private final WeakReference<Var<T>> var;
        final Set<Var<?>> registeredVars = Collections.newSetFromMap(MapUtil.<Var<?>, Boolean>newConcurrentHashMap());

        VarFiber(FiberScheduler scheduler, Var<T> v) {
            super(scheduler);
            this.var = new WeakReference<Var<T>>(v);
        }

        VarFiber(Var<T> v) {
            this.var = new WeakReference<Var<T>>(v);
        }

        @Override
        protected Void run() throws SuspendExecution, InterruptedException {
            Var<T> v = null;
            try {
                for (;;) {
                    v = var.get();
                    if (v == null)
                        break;
                    v.set(v.f.run());
                    park();
                }
            } catch (Throwable t) {
                if (v != null)
                    v.ch.close(t);
            } finally {
                for (Var<?> v1 : registeredVars) {
                    v1.registeredFibers.remove(this);
                    v1.notifyRegistered();
                }
            }
            return null;
        }
    }
}

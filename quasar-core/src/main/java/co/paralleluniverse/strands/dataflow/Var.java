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

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.*;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;

/**
 * A dataflow variable.
 * Represents a variable whose value can be set multiple times and by multiple strands, and whose changing values can be monitored and
 * propagated.
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
    private final Set<VarFiber<?>> registeredFibers = Collections.newSetFromMap(MapUtil.newConcurrentHashMap());

    private final ThreadLocal<TLVar> tlv = new ThreadLocal<>() {
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

    /**
     * Creates a new {@code Var}, whose value is set to the value returned by the given function {@code f}; the
     * function will be re-applied, and the {@code Var}'s value re-set, whenever any of the {@code Var}s referenced
     * by {@code f} change value.
     *
     * @param history   how many historical values to maintain for each strand reading the var.
     * @param scheduler the {@link FiberScheduler} to use to schedule the fiber that will run, and re-run {@code f}.
     * @param f         this var's value is set to the return value of {@code f}
     * @see #get()
     */
    public Var(int history, FiberScheduler scheduler, SuspendableCallable<T> f) {
        if (history < 0)
            throw new IllegalArgumentException("history must be >= 0, but is " + history);
        this.ch = Channels.newChannel(1 + history, Channels.OverflowPolicy.DISPLACE);
        this.f = f;

        if (f != null)
            new VarFiber<>(scheduler != null ? scheduler : DefaultFiberScheduler.getInstance(), this).start();
    }

    /**
     * Creates a new {@code Var}, whose value is set to the value returned by the given function {@code f}; the
     * function will be re-applied, and the {@code Var}'s value re-set, whenever any of the {@code Var}s referenced
     * by {@code f} change value. The fiber running {@code f} will be scheduled by the default fiber scheduler.
     *
     * @param history   how many historical values to maintain for each strand reading the var.
     * @param f         this var's value is set to the return value of {@code f}
     * @see #get()
     */
    public Var(int history, SuspendableCallable<T> f) {
        this(history, null, f);
    }

    /**
     * Creates a new {@code Var} with no history, whose value is set to the value returned by the given function {@code f}; the
     * function will be re-applied, and the {@code Var}'s value re-set, whenever any of the {@code Var}s referenced
     * by {@code f} change value. The fiber running {@code f} will be scheduled by the default fiber scheduler.
     *
     * @param f         this var's value is set to the return value of {@code f}
     * @see #get()
     */
    public Var(SuspendableCallable<T> f) {
        this(0, null, f);
    }

    /**
     * Creates a new {@code Var} with no history, whose value is set to the value returned by the given function {@code f}; the
     * function will be re-applied, and the {@code Var}'s value re-set, whenever any of the {@code Var}s referenced
     * by {@code f} change value.
     *
     * @param scheduler the {@link FiberScheduler} to use to schedule the fiber that will run, and re-run {@code f}.
     * @param f         this var's value is set to the return value of {@code f}
     * @see #get()
     */
    public Var(FiberScheduler scheduler, SuspendableCallable<T> f) {
        this(0, scheduler, f);
    }

    /**
     * Creates a new {@code Var}.
     *
     * @param history how many historical values to maintain for each strand reading the var.
     * @see #get()
     */
    public Var(int history) {
        this(history, null, null);
    }

    /**
     * Creates a new {@code Var} with no history.
     */
    public Var() {
        this(0, null, null);
    }

    /**
     * Sets a new value for this {@code Var}. The Var can be set multiple times and by multiple strands.
     *
     * @param val the new value.
     */
    public void set(T val) {
        try {
            record("set", "Set %s to %s", this, val);
            ch.send(val == null ? (T) NULL : val);
            notifyRegistered();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    private void notifyRegistered() {
        for (VarFiber<?> fiber : registeredFibers)
            fiber.signalNewValue(this);
    }

    /**
     * Returns the Var's current value (more precisely: it returns the oldest value in the maintained history that has
     * not yet been returned), unless this Var does not yet have a value; only in that case will this method block.
     */
    public T get() throws SuspendExecution, InterruptedException {
        TLVar tl = tlv.get();
        if (tl.type == UNKNOWN) {
            Fiber<?> currentFiber = Fiber.currentFiber();
            if (currentFiber instanceof VarFiber) {
                final VarFiber<?> vf = (VarFiber<?>) currentFiber;
                tl.type = VARFIBER;
                registeredFibers.add(vf);
                vf.registeredVars.add(this);
            } else
                tl.type = PLAIN;
        }

        try {
            final T val;
            if (tl.val == null) {
                val = tl.c.receive();
                tl.val = val;
            } else {
                T v = tl.c.tryReceive();
                if (v != null)
                    tl.val = v;
                val = tl.val;
            }

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

    /**
     * Blocks until a new value has been set and returns it.
     */
    public T getNext() throws SuspendExecution, InterruptedException {
        TLVar tl = tlv.get();
        final T val;
        val = tl.c.receive();
        tl.val = val;
        return val;
    }

    private static class VarFiber<T> extends Fiber<Void> {
        private final WeakReference<Var<T>> var;
        final Set<Var<?>> registeredVars = Collections.newSetFromMap(MapUtil.newConcurrentHashMap());
        private volatile boolean hasNewVal;

        VarFiber(FiberScheduler scheduler, Var<T> v) {
            super(scheduler);
            this.var = new WeakReference<>(v);
        }

        VarFiber(Var<T> v) {
            this.var = new WeakReference<>(v);
        }

        void signalNewValue(Var<?> var) {
            Var.record("signalNewValue", "Fiber %s for var %s signalled by %s", this, this.var, var);
            hasNewVal = true;
            unpark(var);
        }

        @Override
        protected Void run() throws SuspendExecution, InterruptedException {
            Var<T> v = null;
            try {
                for (;;) {
                    hasNewVal = false;
                    Var.record("run", "Fiber %s for var %s computing new value", this, var);
                    v = var.get();
                    if (v == null)
                        break;
                    T newVal = v.f.run();
                    Var.record("run", "Fiber %s for var %s computed new value %s", this, var, newVal);
                    v.set(newVal);
                    while (!hasNewVal) {
                        Var.record("run", "Fiber %s for var %s parking", this, var);
                        Fiber.park(v);
                    }
                }
            } catch (Throwable t) {
                if (v != null)
                    v.ch.close(t);
            } finally {
                Var.record("run", "Fiber %s for var %s terminated", this, var);
                for (Var<?> v1 : registeredVars) {
                    v1.registeredFibers.remove(this);
                    v1.notifyRegistered();
                }
            }
            return null;
        }
    }

    public static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Var", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}

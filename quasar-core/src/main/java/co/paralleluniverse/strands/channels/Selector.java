/*
 * Quasar: lightweight strands and actors for the JVM.
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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.concurrent.util.UtilUnsafe;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class Selector<Message> {
    public static <Message> SelectAction<Message> select(boolean priority, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).select();
    }

    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).select(timeout, unit);
    }

    public static <Message> SelectAction<Message> select(boolean priority, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).select();
    }

    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).select(timeout, unit);
    }

    public static <Message> SelectAction<Message> select(SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return select(false, actions);
    }

    public static <Message> SelectAction<Message> select(long timeout, TimeUnit unit, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, unit, actions);
    }

    public static <Message> SelectAction<Message> select(List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return select(false, actions);
    }

    public static <Message> SelectAction<Message> select(long timeout, TimeUnit unit, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, unit, actions);
    }

    public static <Message> SelectAction<Message> trySelect(boolean priority, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).trySelect();
    }

    public static <Message> SelectAction<Message> trySelect(boolean priority, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).trySelect();
    }

    public static <Message> SelectAction<Message> trySelect(SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return trySelect(false, actions);
    }

    public static <Message> SelectAction<Message> trySelect(List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return trySelect(false, actions);
    }

    public static <Message> SelectAction<Message> send(SendPort<? super Message> port, Message message) {
        return new SelectAction<Message>((SendPort) port, message);
    }

    public static <Message> SelectAction<Message> receive(ReceivePort<? extends Message> port) {
        return new SelectAction(port, null);
    }
    private static final AtomicLong selectorId = new AtomicLong(); // used to break symmetry to prevent deadlock in transfer channel
    private static final Object LEASED = new Object() {
        @Override
        public String toString() {
            return "LEASED";
        }
    };
    final long id;
    private volatile Object winner;
    private Strand waiter;
    private final List<SelectAction<Message>> actions;
    private final boolean priority;

    Selector(boolean priority, List<SelectAction<Message>> actions) {
        this.id = selectorId.incrementAndGet();
        this.waiter = Strand.currentStrand();
        this.actions = actions;
        this.priority = priority;
        for (int i = 0; i < actions.size(); i++) {
            SelectAction<Message> sa = actions.get(i);
            sa.setSelector(this);
            sa.setIndex(i);
            record("<init>", "%s added %s", this, sa);
        }
    }

    private void selectInit() {
        this.waiter = Strand.currentStrand();

        if (!priority)
            Collections.shuffle(actions, ThreadLocalRandom.current());
    }

    void reset() {
        for (SelectAction<Message> sa : actions)
            sa.resetReceive();
        winner = null;
    }

    public SelectAction<Message> trySelect() {
        selectInit();
        for (int i = 0; i < actions.size(); i++) {
            SelectAction sa = actions.get(i);

            if (sa.isData()) {
                if (((SendPort) sa.port).trySend(sa.message()))
                    return sa;
            } else {
                Object m = ((ReceivePort) sa.port).tryReceive();
                if (m != null || ((ReceivePort) sa.port).isClosed()) {
                    sa.setItem(m);
                    return sa;
                }
            }
        }
        return null;
    }

    SelectAction<Message> select() throws InterruptedException, SuspendExecution {
        return select(-1, null);
    }

    SelectAction<Message> select(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        if (timeout == 0 && unit != null)
            return trySelect();

        selectInit();

        final boolean timed = (timeout > 0 && unit != null);
        long lastTime = timed ? System.nanoTime() : 0L;
        long nanos = timed ? unit.toNanos(timeout) : 0L;

        final int n = actions.size();
        SelectAction<Message> res = null;

        // register
        int lastRegistered = -1;
        for (int i = 0; i < n; i++) {
            SelectAction<Message> sa = actions.get(i);

            sa.token = sa.port.register(sa);
            lastRegistered = i;
            if (sa.isDone()) {
                assert winner == sa;
                res = sa;
                break;
            } else {
                Object w = winner;
                if (w != null & w != LEASED)
                    break;
            }
        }

        // try
        if (res == null) {
            tryloop:
            for (;;) {
                if (timed && nanos <= 0)
                    break;

                for (int i = 0; i <= lastRegistered; i++) {
                    SelectAction<Message> sa = actions.get(i);

                    if (sa.port.tryNow(sa.token)) {
                        res = sa;
                        break tryloop;
                    }
                }

                if (timed) {
                    long now = System.nanoTime();
                    if ((nanos -= now - lastTime) > 0)
                        Strand.parkNanos(this, nanos);
                    lastTime = now;
                } else
                    Strand.park(this);
            }
        }

        // unregister
        for (int i = 0; i <= lastRegistered; i++) {
            SelectAction sa = actions.get(i);
            sa.port.unregister(sa.token);
            sa.token = null; // for GC
        }
        return res;
    }
    // private volatile StackTraceElement st[];

    boolean lease() {
        record("lease", "trying lease %s", this);
        Object w;
        int i = 0;
        do {
            w = winner;
            if (w != null & w != LEASED)
                return false;
            if (i++ > 1 << 22) {
                // System.err.println(Arrays.toString(st));
                throw new RuntimeException("Unable to obtain selector lease: " + w);
            }
        } while (!casWinner(null, LEASED));
        // st = Thread.currentThread().getStackTrace();
        record("lease", "got lease %s", this);
        return true;
    }

    void setWinner(SelectAction<?> action) {
        record("setWinner", "won %s: %s", this, action);
        assert winner == LEASED;
        // st = null;
        winner = action;
    }

    void returnLease() {
        record("returnLease", "returned lease %s", this);
        assert winner == LEASED;
        // st = null;
        winner = null;
    }

    Strand getWaiter() {
        return waiter;
    }

    void signal() {
        waiter.unpark(this);
    }

    public SelectAction<?> getWinner() {
        return (SelectAction<?>) winner;
    }

    private static int[] randomIntArray(int n) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int[] a = new int[n];
        for (int i = 1; i < n; i++) {
            int x = random.nextInt(i);
            a[i] = a[x];
            a[x] = i;
        }
        return a;
    }

    @Override
    public String toString() {
        return Selector.class.getName() + '@' + Long.toHexString(id);
    }
    private static final Unsafe UNSAFE = UtilUnsafe.getUnsafe();
    private static final long winnerOffset;

    static {
        try {
            winnerOffset = UNSAFE.objectFieldOffset(Selector.class.getDeclaredField("winner"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private boolean casWinner(Object expected, Object update) {
        return UNSAFE.compareAndSwapObject(this, winnerOffset, expected, update);
    }
    static final FlightRecorder RECORDER = Debug.isDebug() ? Debug.getGlobalFlightRecorder() : null;

    boolean isRecording() {
        return RECORDER != null;
    }

    static void record(String method, String format) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, null));
    }

    static void record(String method, String format, Object arg1) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, new Object[]{arg1}));
    }

    static void record(String method, String format, Object arg1, Object arg2) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, new Object[]{arg1, arg2}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, new Object[]{arg1, arg2, arg3}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, new Object[]{arg1, arg2, arg3, arg4}));
    }

    static void record(String method, String format, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if (RECORDER != null)
            RECORDER.record(1, new FlightRecorderMessage("Selector", method, format, new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }
}

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

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
public class Selector<Message> {
    public static <Message> SelectAction<Message> select(boolean priority, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions).select();
    }

    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions).select(timeout, unit);
    }

    public static <Message> SelectAction<Message> select(boolean priority, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions.toArray(new SelectAction[actions.size()])).select();
    }

    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, List<SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions.toArray(new SelectAction[actions.size()])).select(timeout, unit);
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

    public static <Message> SelectAction<Message> send(SendPort<Message> port, Message message) {
        return new SelectAction<Message>((Selectable<Message>) port, message);
    }

    public static <Message> SelectAction<Message> receive(ReceivePort<Message> port) {
        return new SelectAction<Message>((Selectable<Message>) port, null);
    }
    private static final Object LEASED = new Object();
    private volatile Object winner;
    private final Strand waiter;
    private final SelectAction<Message>[] actions;
    private final boolean priority;
    private int[] order;

    private Selector(boolean priority, SelectAction<Message>[] actions) {
        this.waiter = Strand.currentStrand();
        this.actions = actions;
        this.priority = priority;
    }

    private void selectInit() {
        for (int i = 0; i < actions.length; i++) {
            SelectAction<Message> sa = actions[i];
            sa.setSelector(this);
            sa.setIndex(i);
        }
        if (!priority)
            order = randomIntArray(actions.length);
    }

    public SelectAction<Message> trySelect() {
        selectInit();
        for (int i = 0; i < actions.length; i++) {
            int ind = priority ? i : order[i];
            SelectAction sa = actions[ind];

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

    private SelectAction<Message> select() throws InterruptedException, SuspendExecution {
        return select(-1, null);
    }

    private SelectAction<Message> select(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        selectInit();

        final boolean timed = (timeout > 0 && unit != null);
        long lastTime = timed ? System.nanoTime() : 0L;
        long nanos = timed ? unit.toNanos(timeout) : 0L;

        final int n = actions.length;
        SelectAction<Message> res = null;

        // register
        int lastRegistered = n - 1;
        for (int i = 0; i < n; i++) {
            int ind = priority ? i : order[i];
            SelectAction<Message> sa = actions[ind];

            sa.token = sa.port.register(sa);
            assert sa.isDone() || sa.token != null;
            if (sa.isDone()) {
                assert winner == sa;
                res = sa;
                lastRegistered = i;
                break;
            }
        }

        // try
        if (res == null) {
            tryloop:
            for (;;) {
                if (timed && nanos <= 0)
                    break;

                for (int i = 0; i < n; i++) {
                    int ind = priority ? i : order[i];
                    SelectAction<Message> sa = actions[ind];

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
                } else {
                    Strand.park(this);
                    System.out.println("xxx");
                }
            }
        }

        // unregister
        for (int i = 0; i <= lastRegistered; i++) {
            int ind = priority ? i : order[i];
            SelectAction sa = actions[ind];
            sa.port.unregister(sa.token);
            sa.token = null; // for GC
        }
        return res;
    }

    boolean lease() {
        Object w;
        do {
            w = winner;
            if (w == LEASED)
                continue;
            else if (w != null)
                return false;
        } while (!casWinner(null, LEASED));
        return true;
    }

    void setWinner(SelectAction<?> action) {
        winner = action;
    }

    Strand getWaiter() {
        return waiter;
    }

    void signal() {
        waiter.unpark();
    }

    void returnLease() {
        winner = null;
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
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final long winnerOffset;

    static {
        try {
            winnerOffset = unsafe.objectFieldOffset(Selector.class.getDeclaredField("winner"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private boolean casWinner(Object expected, Object update) {
        return unsafe.compareAndSwapObject(this, winnerOffset, expected, update);
    }
}

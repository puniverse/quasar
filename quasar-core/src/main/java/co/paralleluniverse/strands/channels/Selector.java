 /*
 * Quasar: lightweight strands and actors for the JVM.
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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.monitoring.FlightRecorder;
import co.paralleluniverse.common.monitoring.FlightRecorderMessage;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Synchronization;
import co.paralleluniverse.strands.Timeout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Attempts to perform at most one channel operation (send or receive) of a given set.
 *
 * @author pron
 */
public class Selector<Message> implements Synchronization {
    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).select();
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param timeout  the maximum duration to block waiting for an operation to complete.
     * @param unit     the time unit of the given timeout
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).select(timeout, unit);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param timeout  the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, Timeout timeout, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).select(timeout);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).select();
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the specified timeout.
     *
     * @param timeout  the maximum duration to block waiting for an operation to complete.
     * @param unit     the time unit of the given timeout
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, long timeout, TimeUnit unit, List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).select(timeout, unit);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the specified timeout.
     *
     * @param timeout  the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(boolean priority, Timeout timeout, List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).select(timeout);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes.
     * Same as calling {@link #select(boolean, co.paralleluniverse.strands.channels.SelectAction[]) select(false, actions)}.
     *
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return select(false, actions);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     * Same as calling {@link #select(long, java.util.concurrent.TimeUnit, co.paralleluniverse.strands.channels.SelectAction[]) select(false, timeout, unit, actions)}.
     *
     * @param timeout the maximum duration to block waiting for an operation to complete.
     * @param unit    the time unit of the given timeout
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(long timeout, TimeUnit unit, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, unit, actions);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     * Same as calling {@link #select(long, java.util.concurrent.TimeUnit, co.paralleluniverse.strands.channels.SelectAction[]) select(false, timeout, unit, actions)}.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(Timeout timeout, SelectAction<Message>... actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, actions);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes.
     * Same as calling {@link #select(boolean, java.util.List) select(false, actions)}.
     *
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return select(false, actions);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     * Same as calling {@link #select(boolean, long, java.util.concurrent.TimeUnit, java.util.List) select(false, timeout, unit, actions)}.
     *
     * @param timeout the maximum duration to block waiting for an operation to complete.
     * @param unit    the time unit of the given timeout
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(long timeout, TimeUnit unit, List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, unit, actions);
    }

    /**
     * Performs exactly one channel operation of a given set, blocking until any of the actions completes, but no longer than the given timeout.
     * Same as calling {@link #select(boolean, long, java.util.concurrent.TimeUnit, java.util.List) select(false, timeout, unit, actions)}.
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully, or {@code null} if the timeout expired before an operation could complete.
     * @throws InterruptedException
     */
    public static <Message> SelectAction<Message> select(Timeout timeout, List<? extends SelectAction<Message>> actions) throws InterruptedException, SuspendExecution {
        return select(false, timeout, actions);
    }

    /**
     * Attempts to performs exactly one channel operation of a given set if one can be completed without blocking.
     * This method never blocks.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     */
    public static <Message> SelectAction<Message> trySelect(boolean priority, SelectAction<Message>... actions) throws SuspendExecution {
        return new Selector<Message>(priority, Arrays.asList(actions)).trySelect();
    }

    /**
     * Attempts to performs exactly one channel operation of a given set if one can be completed without blocking.
     * This method only blocks if the listeners associated with the select actions block.
     *
     * @param priority If {@code true} and more than one operation can complete at the same time, the one that appears in the given list first will be the one performed.
     *                 If {@code false} the order of the operations is ignored.
     * @param actions  a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     */
    public static <Message> SelectAction<Message> trySelect(boolean priority, List<? extends SelectAction<Message>> actions) throws SuspendExecution {
        return new Selector<Message>(priority, actions instanceof ArrayList ? actions : new ArrayList<>(actions)).trySelect();
    }

    /**
     * Attempts to performs exactly one channel operation of a given set if one can be completed without blocking.
     * This method only blocks if the listeners associated with the select actions block. Same as calling {@link #trySelect(boolean, co.paralleluniverse.strands.channels.SelectAction[]) trySelect(false, actions)}.
     *
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     */
    public static <Message> SelectAction<Message> trySelect(SelectAction<Message>... actions) throws SuspendExecution {
        return trySelect(false, actions);
    }

    /**
     * Attempts to performs exactly one channel operation of a given set if one can be completed without blocking.
     * This method only blocks if the listeners associated with the select actions block.. Same as calling {@link #trySelect(boolean, java.util.List) trySelect(false, actions)}.
     *
     * @param actions a list of actions, one of which will be performed.
     * @return the action that has completed successfully
     */
    public static <Message> SelectAction<Message> trySelect(List<? extends SelectAction<Message>> actions) throws SuspendExecution {
        return trySelect(false, actions);
    }

    //////////////////////
    /**
     * Creates a {@link SelectAction} for a send operation
     *
     * @param <Message>
     * @param ch        The channel to which the operation tries to send the message
     * @param message   the message to send.
     * @return a <i>send</i> {@link SelectAction} that can be selected by the selector.
     */
    public static <Message> SelectAction<Message> send(SendPort<? super Message> ch, Message message) {
        return send(ch, message, null);
    }

    /**
     * Creates a {@link SelectAction} for a send operation
     *
     * @param <Message>
     * @param ch        The channel to which the operation tries to send the message
     * @param message   the message to send.
     * @param listener  a {@link SelectSendListener} which will be triggered if this operation succeeds.
     * @return a <i>send</i> {@link SelectAction} that can be selected by the selector.
     */
    public static <Message> SelectAction<Message> send(SendPort<? super Message> ch, Message message, SelectSendListener<Message> listener) {
        return new SelectActionImpl<>((SendPort<Message>) ch, message, listener);
    }

    /**
     * Creates a {@link SelectAction} for a receive operation
     *
     * @param <Message>
     * @param ch        the channel from which the operation tries to receive
     * @return a <i>receive</i> {@link SelectAction} that can be selected by the selector.
     */
    public static <Message> SelectAction<Message> receive(ReceivePort<? super Message> ch) {
        return receive(ch, null);
    }

    /**
     * Creates a {@link SelectAction} for a receive operation
     *
     * @param <Message>
     * @param ch        the channel from which the operation tries to receive
     * @param listener  a {@link SelectReceiveListener} which will be triggered if this operation succeeds.
     * @return a <i>receive</i> {@link SelectAction} that can be selected by the selector.
     */
    public static <Message> SelectAction<Message> receive(ReceivePort<? super Message> ch, SelectReceiveListener<Message> listener) {
        return new SelectActionImpl<>((ReceivePort<Message>)ch, listener);
    }
    ///////////////////
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
    private final List<? extends SelectActionImpl<Message>> actions;
    private int lastRegistered;
    private final boolean priority;
    SelectAction<Message> res;

    Selector(boolean priority, List<? extends SelectAction<Message>> actions) {
        this.id = selectorId.incrementAndGet();
        this.waiter = Strand.currentStrand();
        this.actions = (List<? extends SelectActionImpl<Message>>) actions;
        this.priority = priority;
        for (int i = 0; i < actions.size(); i++) {
            SelectActionImpl<? extends Message> sa = (SelectActionImpl<? extends Message>) actions.get(i);
            sa.setSelector(this);
            sa.setIndex(i);
            record("<init>", "%s added %s", this, sa);
        }
    }

    private void selectInit() {
        if (!priority)
            Collections.shuffle(actions, ThreadLocalRandom.current());
    }

    void reset() {
        waiter = null;
        for (SelectActionImpl<Message> sa : actions)
            sa.resetReceive();
        winner = null;
    }

    SelectAction<Message> select() throws InterruptedException, SuspendExecution {
        return select(-1, null);
    }

    @Override
    public Object register() {
        Strand s = Strand.currentStrand();
        if (waiter != null && !waiter.equals(s))
            throw new IllegalMonitorStateException("A strand is already registered");
        this.waiter = Strand.currentStrand();

        final int n = actions.size();
        res = null;

        // register
        lastRegistered = -1;
        for (int i = 0; i < n; i++) {
            SelectActionImpl<Message> sa = actions.get(i);

            sa.token = sa.port.register(sa);
            lastRegistered = i;
            if (sa.isDone()) {
                assert winner == sa; // seen to have failed in co.paralleluniverse.strands.channels.GeneralSelectorTest > testFans1[5] 
                res = sa;
                break;
            } else {
                Object w = winner;
                if (w != null & w != LEASED)
                    break;
            }
        }
        return null;
    }

    @Override
    public void unregister(Object registrationToken) {
        for (int i = 0; i <= lastRegistered; i++) {
            SelectActionImpl<Message> sa = actions.get(i);
            sa.port.unregister(sa.token);
            sa.token = null; // for GC
        }
        this.waiter = null;
    }

    SelectAction<Message> select(Timeout timeout) throws InterruptedException, SuspendExecution {
        return select(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    SelectAction<Message> select(long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        if (timeout == 0 && unit != null)
            return trySelect();

        selectInit();

        final boolean timed = (timeout > 0 && unit != null);
        long lastTime = timed ? System.nanoTime() : 0L;
        long nanos = timed ? unit.toNanos(timeout) : 0L;

        Object token = register();
        try {
            if (res == null) {
                tryloop:
                for (;;) {
                    if (timed && nanos <= 0)
                        break;

                    for (int i = 0; i <= lastRegistered; i++) {
                        SelectActionImpl<Message> sa = actions.get(i);

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
        } finally {
            unregister(token);
        }
        if (res != null)
            ((SelectActionImpl<Message>) res).fire();
        return res;
    }

    public SelectAction<Message> trySelect() throws SuspendExecution {
        selectInit();
        for (int i = 0; i < actions.size(); i++) {
            SelectActionImpl<Message> sa = actions.get(i);

            if (sa.isData()) {
                if (((SendPort<Message>) sa.port).trySend(sa.message())) {
                    sa.fire();
                    return sa;
                }
            } else {
                Message m = ((ReceivePort<Message>) sa.port).tryReceive();
                if (m != null || ((ReceivePort<Message>) sa.port).isClosed()) {
                    sa.setItem(m);
                    sa.fire();
                    return sa;
                }
            }
        }
        return null;
    }
    // private volatile StackTraceElement st[];

    boolean lease() {
        record("lease", "trying lease %s", this);
        Object w;
        int i = 0;
        long start = 0;
        do {
            w = winner;
            if (w != null & w != LEASED)
                return false;

            if (i++ > (1 << 22)) {
                if (start == 0)
                    start = System.nanoTime();
                else if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) > 10000)
                    throw new RuntimeException("Unable to obtain selector lease in 10 seconds: " + w);
                i = 0;
                Thread.yield();
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
    
    private static final VarHandle WINNER;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            WINNER = l.findVarHandle(Selector.class, "winner", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    private boolean casWinner(Object expected, Object update) {
        return WINNER.compareAndSet(this, expected, update);
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

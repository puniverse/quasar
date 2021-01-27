/*
 * Quasar: lightweight strands and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
/*
 * Based on j.u.c.LinkedTransferQueue:
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Synchronization;
import co.paralleluniverse.strands.Timeout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Doug Lea
 * @author pron
 */
public class TransferChannel<Message> implements StandardChannel<Message>, Selectable<Message>, Synchronization {
    private Throwable closeException;
    private volatile boolean sendClosed;
    private boolean receiveClosed;
    private static final Object CHANNEL_CLOSED = new Object();
    private static final Object NO_MATCH = new Object();
    private static final Object LOST = new Object();

    public TransferChannel() {
    }

    @Override
    public final int capacity() {
        return 0;
    }

    @Override
    public boolean isSingleProducer() {
        return false;
    }

    @Override
    public boolean isSingleConsumer() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DelegatingEquals)
            return other.equals(this);
        return super.equals(other);
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (isSendClosed())
            return;
        if (xfer1(message, true, SYNC, 0) != null)
            throw new InterruptedException(); // failure possible only due to interrupt
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (isSendClosed())
            return true;
        if (xfer1(message, true, TIMED, unit.toNanos(timeout)) == null)
            return true;
        if (!Strand.interrupted())
            return false;
        throw new InterruptedException();
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return send(message, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean trySend(Message message) {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (isSendClosed())
            return true;
        boolean res = (trySendOrReceive(message, true) == null);
        return res;
    }

    @Override
    public void close() {
        if (!sendClosed) {
            sendClosed = true;
            signalWaitersOnClose();
        }
    }

    @Override
    public void close(Throwable t) {
        if (!sendClosed) {
            closeException = t;
            sendClosed = true;
            signalWaitersOnClose();
        }
    }

    private void setReceiveClosed() {
        if (!receiveClosed)
            this.receiveClosed = true;
    }

    private Message closeValue() {
        if (closeException != null)
            throw new ProducerException(closeException);
        return null;
    }

    @Override
    public Message tryReceive() {
        if (receiveClosed)
            return null;

        final Object m = trySendOrReceive(null, false);

        if (m == CHANNEL_CLOSED)
            return closeValue();
        return (Message) m;
    }

    @Override
    public Object register() {
        // for queues, a simple registration is always a receive
        return receive0();
    }

    @Override
    public Object register(SelectAction<Message> action) {
        return xfer0((SelectActionImpl<Message>) action);
    }

    @Override
    public boolean tryNow(Object token) {
        Token t = (Token) token;
        return t.n.isMatched();
    }

    @Override
    public void unregister(Object token) {
        Token t = (Token) token;
        if (token == null)
            return;
        Node p = t.n;
        Node pred = t.pred;
        Object x = p.item;
        if (!((x == p) || ((x == null) == p.isData))) {
            if (p.casItem(x, p))        // cancel
                unsplice(pred, p);
        }
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return closeValue();

        Object m = xfer1(null, false, SYNC, 0);

        if (m != null) {
            if (m == CHANNEL_CLOSED)
                return closeValue();
            return (Message) m;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    protected Message receiveInternal(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return closeValue();

        Object m = xfer1(null, false, TIMED, unit.toNanos(timeout));
        if (m != null || !Strand.interrupted()) {
            if (m == CHANNEL_CLOSED)
                return closeValue();
            return (Message) m;
        }
        throw new InterruptedException();
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return receiveInternal(timeout, unit);
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        return receiveInternal(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    boolean isSendClosed() {
        return sendClosed;
    }

    @Override
    public boolean isClosed() {
        if (receiveClosed)
            return true;
// racy, but that's OK because we don't guarantee anything if we return false
        if (sendClosed && size() == 0) {
            setReceiveClosed();
            return true;
        }
        return false;
    }

    private void signalWaitersOnClose() {
        for (Node p = head; p != null;) {
            if (!p.isMatched()) {
                if (!p.isData) {
                    if (p.casItem(null, CHANNEL_CLOSED)) // match waiting requesters with CHANNEL_CLOSED
                        Strand.unpark(p.waiter, this);   // ... and wake 'em up
                } else
                    p.tryMatchData();
            }
            Node n = p.next;
            if (n != p)
                p = n;
            else
                p = head;
        }
    }
    /////////////////////////////////////////
    private static final long serialVersionUID = -3223113410248163686L;
    /**
     * True if on multiprocessor
     */
    private static final boolean MP = Runtime.getRuntime().availableProcessors() > 1;
    /**
     * The number of times to spin (with randomly interspersed calls
     * to Strand.yield) on multiprocessor before blocking when a node
     * is apparently the first waiter in the queue. See above for
     * explanation. Must be a power of two. The value is empirically
     * derived -- it works pretty well across a variety of processors,
     * numbers of CPUs, and OSes.
     */
    private static final int FRONT_SPINS = 1 << 7;
    /**
     * The number of times to spin before blocking when a node is
     * preceded by another node that is apparently spinning. Also
     * serves as an increment to FRONT_SPINS on phase changes, and as
     * base average frequency for yielding during spins. Must be a
     * power of two.
     */
    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;
    /**
     * The maximum number of estimated removal failures (sweepVotes)
     * to tolerate before sweeping through the queue unlinking
     * cancelled nodes that were not unlinked upon initial
     * removal. See above for explanation. The value must be at least
     * two to avoid useless sweeps when removing trailing nodes.
     */
    static final int SWEEP_THRESHOLD = 32;

    /**
     * Queue nodes. Uses Object, not E, for items to allow forgetting
     * them after use. Relies heavily on Unsafe mechanics to minimize
     * unnecessary ordering constraints: Writes that are intrinsically
     * ordered wrt other accesses or CASes use simple relaxed forms.
     */
    static final class Node {
        final boolean isData;   // false if this is a request node
        volatile SelectActionImpl sa;
        volatile Object item;   // initially non-null if isData; CASed to match
        volatile Node next;
        volatile Strand waiter; // null until waiting

        // CAS methods for fields
        final boolean casNext(Node cmp, Node val) {
            return NEXT.compareAndSet(this, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            // assert cmp == null || cmp.getClass() != Node.class;
            return ITEM.compareAndSet(this, cmp, val);
        }

        /**
         * Constructs a new node. Uses relaxed write because item can
         * only be seen after publication via casNext.
         */
        Node(SelectActionImpl sa) {
            ITEM.set(this, sa.message()); // UNSAFE.putObject(this, itemOffset, sa.message()); // relaxed write
            SA.set(this, sa); // UNSAFE.putObject(this, saOffset, sa); // relaxed write
            this.isData = sa.isData();
        }

        Node(Object item, boolean isData) {
            ITEM.set(this, item); // UNSAFE.putObject(this, itemOffset, item); // relaxed write
            this.isData = isData;
        }

        /**
         * Links node to itself to avoid garbage retention. Called
         * only after CASing head field, so uses relaxed write.
         */
        final void forgetNext() {
            NEXT.set(this, this); // UNSAFE.putObject(this, nextOffset, this);
        }

        /**
         * Sets item to self and waiter to null, to avoid garbage
         * retention after matching or cancelling. Uses relaxed writes
         * because order is already constrained in the only calling
         * contexts: item is forgotten only after volatile/atomic
         * mechanics that extract items. Similarly, clearing waiter
         * follows either CAS or return from park (if ever parked;
         * else we don't care).
         */
        final void forgetContents() {
            ITEM.set(this, this); // UNSAFE.putObject(this, itemOffset, this);
            SA.set(this, null); // UNSAFE.putObject(this, saOffset, null);
            WAITER.set(this, null); // UNSAFE.putObject(this, waiterOffset, null);
        }

        /**
         * Returns true if this node has been matched, including the
         * case of artificial matches due to cancellation.
         */
        final boolean isMatched() {
            Object x = item;
            return (x == this) || ((x == null) == isData);
        }

        /**
         * Returns true if this is an unmatched request node.
         */
        final boolean isUnmatchedRequest() {
            return !isData && item == null;
        }

        /**
         * Returns true if a node with the given mode cannot be
         * appended to this node because this node is unmatched and
         * has opposite data mode.
         */
        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            Object x;
            return d != haveData && (x = item) != this && (x != null) == d;
        }

        /**
         * Tries to artificially match a data node -- used by remove.
         */
        final boolean tryMatchData() {
            // assert isData;
            Object x = item;
            if (x != null && x != this && casItem(x, null)) {
                Strand.unpark(waiter, this);
                return true;
            }
            return false;
        }

        boolean lease() {
            final SelectActionImpl sa = this.sa;
            if (sa == null)
                return true;
            return sa.lease();
        }

        void returnLease() {
            final SelectActionImpl sa = this.sa;
            if (sa != null)
                sa.returnLease();
        }

        void won() {
            final SelectActionImpl sa = this.sa;
            if (sa != null) {
                Object x = item;
                sa.setItem(x == CHANNEL_CLOSED ? null : x);
                sa.won();
            }
        }
        private static final long serialVersionUID = -3375979862319811754L;
        
        private static final VarHandle ITEM;
        private static final VarHandle SA;
        private static final VarHandle NEXT;
        private static final VarHandle WAITER;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                Class k = Node.class;
                ITEM   = l.findVarHandle(k, "item",   Object.class);
                SA     = l.findVarHandle(k, "sa",     SelectActionImpl.class);
                NEXT   = l.findVarHandle(k, "next",   k);
                WAITER = l.findVarHandle(k, "waiter", Strand.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
//        // Unsafe mechanics
//        private static final sun.misc.Unsafe UNSAFE;
//        private static final long itemOffset;
//        private static final long saOffset;
//        private static final long nextOffset;
//        private static final long waiterOffset;
//
//        static {
//            try {
//                UNSAFE = UtilUnsafe.getUnsafe();
//                Class k = Node.class;
//                itemOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("item"));
//                saOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("sa"));
//                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
//                waiterOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("waiter"));
//            } catch (Exception e) {
//                throw new Error(e);
//            }
//        }
    }

    static class Token {
        final Node n;
        final Node pred;

        public Token(Node n, Node pred) {
            this.n = n;
            this.pred = pred;
        }
    }
    /**
     * head of the queue; null until first enqueue
     */
    transient volatile Node head;
    /**
     * tail of the queue; null until first append
     */
    private transient volatile Node tail;
    /**
     * The number of apparent failures to unsplice removed nodes
     */
    private transient volatile int sweepVotes;

    // CAS methods for fields
    private boolean casTail(Node cmp, Node val) {
        return TAIL.compareAndSet(this, cmp, val); // UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return HEAD.compareAndSet(this, cmp, val); // UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return SWEEP_VOTES.compareAndSet(this, cmp, val); // UNSAFE.compareAndSwapInt(this, sweepVotesOffset, cmp, val);
    }

    /*
     * Possible values for "how" argument in xfer method.
     */
    private static final int NOW = 0; // for untimed poll, tryTransfer
    private static final int ASYNC = 1; // for offer, put, add
    private static final int SYNC = 2; // for transfer, take
    private static final int TIMED = 3; // for timed poll, tryTransfer

    @SuppressWarnings("unchecked")
    static <E> E cast(Object item) {
        // assert item == null || item.getClass() != Node.class;
        return (E) item;
    }

    private Object trySendOrReceive(Message e, boolean haveData) {
        if (haveData && (e == null))
            throw new NullPointerException();
        Object item = tryMatch(null, e, haveData);
        if (item != NO_MATCH)
            return item;
        return e;
    }

    /**
     * Implements all queuing methods. See above for explanation.
     *
     * @param e        the item or null for take
     * @param haveData true if this is a put, else a take
     * @param how      NOW, ASYNC, SYNC, or TIMED
     * @param nanos    timeout in nanosecs, used only if mode is TIMED
     * @return an item if matched, else e
     * @throws NullPointerException if haveData mode but e is null
     */
    private Object xfer1(Message e, boolean haveData, int how, long nanos) throws SuspendExecution {
        assert how == SYNC || how == TIMED;
        if (haveData && (e == null))
            throw new NullPointerException();

        Node s = null;                        // the node to append, if needed

        retry:
        for (;;) {                            // restart on append race
            Object item = tryMatch(null, e, haveData);
            if (item != NO_MATCH)
                return item;

            if (s == null)
                s = new Node(e, haveData);
            Node pred = tryAppend(s, haveData);
            if (pred == null)
                continue retry;           // lost race vs opposite mode

            if (!haveData && sendClosed) {
                s.item = CHANNEL_CLOSED;
                unsplice(pred, s);
                setReceiveClosed();
                return CHANNEL_CLOSED;
            }

            return awaitMatch(s, pred, e, (how == TIMED), nanos);
        }
    }

    private Token receive0() {
        Node s = new Node(null, false);      // the node to append

        retry:
        for (;;) {                            // restart on append race
            Object item = tryMatch(null, null, false);
            if (item != NO_MATCH) {
                s.item = item;
                return new Token(s, null);
            }

            Node pred = tryAppend(s, false);
            if (pred == null)
                continue retry;           // lost race vs opposite mode

            if (sendClosed) {
                s.item = CHANNEL_CLOSED;
                unsplice(pred, s);
                setReceiveClosed();
                return new Token(s, null);
            }

            requestUnpark(s, Strand.currentStrand());
            return new Token(s, pred);
        }
    }

    private Token xfer0(SelectActionImpl<Message> e) {
        final boolean haveData = e.isData();
        Node s = null;                        // the node to append, if needed

        retry:
        for (;;) {                            // restart on append race
            if (!e.lease())
                return null;
            if (isClosed() || (isSendClosed() && e.isData())) {
                e.setItem(null);
                e.won();
                return null;
            }

            Object item = tryMatch(e, e.message(), haveData);
            if (item == LOST)
                return null;

            if (item != NO_MATCH) {
                e.setItem(item == CHANNEL_CLOSED ? null : (Message) item);
                e.won();
                return null;
            }
            e.returnLease();

            if (s == null) {
                s = new Node(e);
                requestUnpark(s, e.selector().getWaiter());
            }
            Node pred = tryAppend(s, haveData);
            if (pred == null)
                continue retry;           // lost race vs opposite mode

            return new Token(s, pred);
        }
    }

    private Object tryMatch(SelectActionImpl sa, Message e, boolean haveData) {
        boolean closed = isSendClosed(); // must be read before trying to match so as not to miss puts

        for (Node h = head, p = h; p != null;) { // find & match first node
            boolean isData = p.isData;
            Object item = p.item;

            if (item != p && (item != null) == isData) { // unmatched
                if (isData == haveData) // can't match
                    break;

                // avoid deadlock by ordering lease acquisition:
                // if p requires a lease and is of lower hashCode than sa, we return sa's lease, acquire p's, and then reacquire sa's.
                SelectActionImpl sa2 = p.sa;
                boolean leasedp;
                if (sa != null && sa2 != null
                        && sa2.selector().id < sa.selector().id) {
                    sa.returnLease();
                    leasedp = sa2.lease();
                    if (!sa.lease()) {
                        if (leasedp)
                            sa2.returnLease();
                        return LOST;
                    }
                } else
                    leasedp = p.lease();

                if (leasedp) {
                    if (p.casItem(item, e)) { // match
                        p.won();
                        for (Node q = p; q != h;) {
                            Node n = q.next;  // update by 2 unless singleton
                            if (head == h && casHead(h, n == null ? q : n)) {
                                h.forgetNext();
                                break;
                            }                 // advance and retry
                            if ((h = head) == null
                                    || (q = h.next) == null || !q.isMatched())
                                break;        // unless slack < 2
                        }
                        Strand.unpark(p.waiter, this);
                        return item;
                    } else
                        p.returnLease();
                }
            }
            Node n = p.next;
            p = (p != n) ? n : (h = head); // Use head if p offlist
        }

        if (closed) {
            assert !haveData;
            setReceiveClosed();
            return CHANNEL_CLOSED;
        }
        return NO_MATCH;
    }

    /**
     * Tries to append node s as tail.
     *
     * @param s        the node to append
     * @param haveData true if appending in data mode
     * @return null on failure due to losing race with append in
     *         different mode, else s's predecessor, or s itself if no
     *         predecessor
     */
    private Node tryAppend(Node s, boolean haveData) {
        for (Node t = tail, p = t;;) {        // move p to last node and append
            Node n, u;                        // temps for reads of next & tail
            if (p == null && (p = head) == null) {
                if (casHead(null, s))
                    return s;                 // initialize
            } else if (p.cannotPrecede(haveData))
                return null;                  // lost race vs opposite mode
            else if ((n = p.next) != null)    // not last; keep traversing
                p = p != t && t != (u = tail) ? (t = u) : // stale tail
                        (p != n) ? n : null;      // restart if off list
            else if (!p.casNext(null, s))
                p = p.next;                   // re-read on CAS failure
            else {
                if (p != t) {                 // update if slack now >= 2
                    while ((tail != t || !casTail(t, s))
                            && (t = tail) != null
                            && (s = t.next) != null && // advance and retry
                            (s = s.next) != null && s != t);
                }
                return p;
            }
        }
    }

    /**
     * Spins/yields/blocks until node s is matched or caller gives up.
     *
     * @param s     the waiting node
     * @param pred  the predecessor of s, or s itself if it has no
     *              predecessor, or null if unknown (the null case does not occur
     *              in any current calls but may in possible future extensions)
     * @param e     the comparison value for checking match
     * @param timed if true, wait only until timeout elapses
     * @param nanos timeout in nanosecs, used only if timed is true
     * @return matched item, or e if unmatched on interrupt or timeout
     */
    private Message awaitMatch(Node s, Node pred, Message e, boolean timed, long nanos) throws SuspendExecution {
        long lastTime = timed ? System.nanoTime() : 0L;
        Strand w = Strand.currentStrand();
        int spins = (w.isFiber() ? 0 : -1); // no spins in fiber; otherwise, initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        if (spins == 0)
            requestUnpark(s, w);

        for (;;) {
            Object item = s.item;

            if (item == CHANNEL_CLOSED)
                setReceiveClosed();

            if (item != e) {                  // matched
                // assert item != s;
                s.forgetContents();           // avoid garbage
                return this.<Message>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0))
                    && s.casItem(e, s)) {     // cancel
                unsplice(pred, s);
                return e;
            }

            if (spins < 0) {                  // establish spins at/near front
                if ((spins = spinsFor(pred, s.isData)) > 0)
                    randomYields = ThreadLocalRandom.current();
            } else if (spins > 0) {           // spin
                --spins;
                if (randomYields.nextInt(CHAINED_SPINS) == 0)
                    Strand.yield();           // occasionally yield
            } else if (s.waiter == null) {
                requestUnpark(s, w);          // request unpark then recheck
            } else if (timed) {
                long now = System.nanoTime();
                if ((nanos -= now - lastTime) > 0)
                    Strand.parkNanos(this, nanos);
                lastTime = now;
            } else {
                Strand.park(this);
            }
        }
    }

    private void requestUnpark(Node s, Strand waiter) {
        s.waiter = waiter;
    }

    /**
     * Returns spin/yield value for a node with given predecessor and
     * data mode. See above for explanation.
     */
    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData)      // phase change
                return FRONT_SPINS + CHAINED_SPINS;
            if (pred.isMatched())             // probably at front
                return FRONT_SPINS;
            if (pred.waiter == null)          // pred apparently spinning
                return CHAINED_SPINS;
        }
        return 0;
    }

    /* -------------- Traversal methods -------------- */
    /**
     * Returns the successor of p, or the head node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node succ(Node p) {
        Node next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * Returns the first unmatched node of the given mode, or null if
     * none. Used by methods isEmpty, hasWaitingConsumer.
     */
    private Node firstOfMode(boolean isData) {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched())
                return (p.isData == isData) ? p : null;
        }
        return null;
    }

    /**
     * Traverses and counts unmatched nodes of the given mode.
     * Used by methods size and getWaitingConsumerCount.
     */
    private int countOfMode(boolean data) {
        int count = 0;
        for (Node p = head; p != null;) {
            if (!p.isMatched()) {
                if (p.isData != data)
                    return 0;
                if (++count == Integer.MAX_VALUE) // saturated
                    break;
            }
            Node n = p.next;
            if (n != p)
                p = n;
            else {
                count = 0;
                p = head;
            }
        }
        return count;
    }

    /* -------------- Removal methods -------------- */
    /**
     * Unsplices (now or later) the given deleted/cancelled node with
     * the given predecessor.
     *
     * @param pred a node that was at one time known to be the
     *             predecessor of s, or null or s itself if s is/was at head
     * @param s    the node to be unspliced
     */
    final void unsplice(Node pred, Node s) {
        s.forgetContents(); // forget unneeded fields
        /*
         * See above for rationale. Briefly: if pred still points to
         * s, try to unlink s.  If s cannot be unlinked, because it is
         * trailing node or pred might be unlinked, and neither pred
         * nor s are head or offlist, add to sweepVotes, and if enough
         * votes have accumulated, sweep.
         */
        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n == null
                    || (n != s && pred.casNext(s, n) && pred.isMatched())) {
                for (;;) {               // check if at, or could be, head
                    Node h = head;
                    if (h == pred || h == s || h == null)
                        return;          // at head or list empty
                    if (!h.isMatched())
                        break;
                    Node hn = h.next;
                    if (hn == null)
                        return;          // now empty
                    if (hn != h && casHead(h, hn))
                        h.forgetNext();  // advance head
                }
                if (pred.next != pred && s.next != s) { // recheck if offlist
                    for (;;) {           // sweep now if enough votes
                        int v = sweepVotes;
                        if (v < SWEEP_THRESHOLD) {
                            if (casSweepVotes(v, v + 1))
                                break;
                        } else if (casSweepVotes(v, 0)) {
                            sweep();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Unlinks matched (typically cancelled) nodes encountered in a
     * traversal from head.
     */
    private void sweep() {
        for (Node p = head, s, n; p != null && (s = p.next) != null;) {
            if (!s.isMatched())
                // Unmatched nodes are never self-linked
                p = s;
            else if ((n = s.next) == null) // trailing node is pinned
                break;
            else if (s == n)    // stale
                // No need to also check for p == s, since that implies s == n
                p = head;
            else
                p.casNext(s, n);
        }
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    boolean isEmpty() {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched())
                return !p.isData;
        }
        return true;
    }

    boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
    }

    int size() {
        return countOfMode(true);
    }

    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    private static final VarHandle HEAD;
    private static final VarHandle TAIL;
    private static final VarHandle SWEEP_VOTES;

    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            Class k = TransferChannel.class;
            HEAD = l.findVarHandle(k, "head", Node.class);
            TAIL = l.findVarHandle(k, "tail", Node.class);
            SWEEP_VOTES = l.findVarHandle(k, "sweepVotes", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
//    // Unsafe mechanics
//    private static final sun.misc.Unsafe UNSAFE;
//    private static final long headOffset;
//    private static final long tailOffset;
//    private static final long sweepVotesOffset;
//
//    static {
//        try {
//            UNSAFE = UtilUnsafe.getUnsafe();
//            Class k = TransferChannel.class;
//            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
//            tailOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("tail"));
//            sweepVotesOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("sweepVotes"));
//        } catch (Exception e) {
//            throw new Error(e);
//        }
//    }
}

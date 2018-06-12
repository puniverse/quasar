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
/*
 * Based on code:
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package co.paralleluniverse.strands.concurrent;

import co.paralleluniverse.common.util.UtilUnsafe;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.concurrent.util.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A capability-based lock with three modes for controlling read/write
 * access. The state of a StampedLock consists of a version and mode.
 * Lock acquisition methods return a stamp that represents and
 * controls access with respect to a lock state; "try" versions of
 * these methods may instead return the special value zero to
 * represent failure to acquire access. Lock release and conversion
 * methods require stamps as arguments, and fail if they do not match
 * the state of the lock. The three modes are:
 *
 * <ul>
 *
 * <li><b>Writing.</b> Method {@link #writeLock} possibly blocks
 * waiting for exclusive access, returning a stamp that can be used
 * in method {@link #unlockWrite} to release the lock. Untimed and
 * timed versions of {@code tryWriteLock} are also provided. When
 * the lock is held in write mode, no read locks may be obtained,
 * and all optimistic read validations will fail. </li>
 *
 * <li><b>Reading.</b> Method {@link #readLock} possibly blocks
 * waiting for non-exclusive access, returning a stamp that can be
 * used in method {@link #unlockRead} to release the lock. Untimed
 * and timed versions of {@code tryReadLock} are also provided. </li>
 *
 * <li><b>Optimistic Reading.</b> Method {@link #tryOptimisticRead}
 * returns a non-zero stamp only if the lock is not currently held
 * in write mode. Method {@link #validate} returns true if the lock
 * has not been acquired in write mode since obtaining a given
 * stamp. This mode can be thought of as an extremely weak version
 * of a read-lock, that can be broken by a writer at any time. The
 * use of optimistic mode for short read-only code segments often
 * reduces contention and improves throughput. However, its use is
 * inherently fragile. Optimistic read sections should only read
 * fields and hold them in local variables for later use after
 * validation. Fields read while in optimistic mode may be wildly
 * inconsistent, so usage applies only when you are familiar enough
 * with data representations to check consistency and/or repeatedly
 * invoke method {@code validate()}. For example, such steps are
 * typically required when first reading an object or array
 * reference, and then accessing one of its fields, elements or
 * methods. </li>
 *
 * </ul>
 *
 * <p>This class also supports methods that conditionally provide
 * conversions across the three modes. For example, method {@link
 * #tryConvertToWriteLock} attempts to "upgrade" a mode, returning
 * a valid write stamp if (1) already in writing mode (2) in reading
 * mode and there are no other readers or (3) in optimistic mode and
 * the lock is available. The forms of these methods are designed to
 * help reduce some of the code bloat that otherwise occurs in
 * retry-based designs.
 *
 * <p>StampedLocks are designed for use as internal utilities in the
 * development of strand-safe components. Their use relies on
 * knowledge of the internal properties of the data, objects, and
 * methods they are protecting. They are not reentrant, so locked
 * bodies should not call other unknown methods that may try to
 * re-acquire locks (although you may pass a stamp to other methods
 * that can use or convert it). The use of read lock modes relies on
 * the associated code sections being side-effect-free. Unvalidated
 * optimistic read sections cannot call methods that are not known to
 * tolerate potential inconsistencies. Stamps use finite
 * representations, and are not cryptographically secure (i.e., a
 * valid stamp may be guessable). Stamp values may recycle after (no
 * sooner than) one year of continuous operation. A stamp held without
 * use or validation for longer than this period may fail to validate
 * correctly. StampedLocks are serializable, but always deserialize
 * into initial unlocked state, so they are not useful for remote
 * locking.
 *
 * <p>The scheduling policy of StampedLock does not consistently
 * prefer readers over writers or vice versa. All "try" methods are
 * best-effort and do not necessarily conform to any scheduling or
 * fairness policy. A zero return from any "try" method for acquiring
 * or converting locks does not carry any information about the state
 * of the lock; a subsequent invocation may succeed.
 *
 * <p>Because it supports coordinated usage across multiple lock
 * modes, this class does not directly implement the {@link Lock} or
 * {@link ReadWriteLock} interfaces. However, a StampedLock may be
 * viewed {@link #asReadLock()}, {@link #asWriteLock()}, or {@link
 * #asReadWriteLock()} in applications requiring only the associated
 * set of functionality.
 *
 * <p><b>Sample Usage.</b> The following illustrates some usage idioms
 * in a class that maintains simple two-dimensional points. The sample
 * code illustrates some try/catch conventions even though they are
 * not strictly needed here because no exceptions can occur in their
 * bodies.<br>
 *
 * <pre>{@code
 * class Point {
 *   private double x, y;
 *   private final StampedLock sl = new StampedLock();
 *
 *   void move(double deltaX, double deltaY) { // an exclusively locked method
 *     long stamp = sl.writeLock();
 *     try {
 *       x += deltaX;
 *       y += deltaY;
 *     } finally {
 *       sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   double distanceFromOriginV1() { // A read-only method
 *     long stamp;
 *     if ((stamp = sl.tryOptimisticRead()) != 0L) { // optimistic
 *       double currentX = x;
 *       double currentY = y;
 *       if (sl.validate(stamp))
 *         return Math.sqrt(currentX * currentX + currentY * currentY);
 *     }
 *     stamp = sl.readLock(); // fall back to read lock
 *     try {
 *       double currentX = x;
 *       double currentY = y;
 *         return Math.sqrt(currentX * currentX + currentY * currentY);
 *     } finally {
 *       sl.unlockRead(stamp);
 *     }
 *   }
 *
 *   double distanceFromOriginV2() { // combines code paths
 *     double currentX = 0.0, currentY = 0.0;
 *     for (long stamp = sl.tryOptimisticRead(); ; stamp = sl.readLock()) {
 *       try {
 *         currentX = x;
 *         currentY = y;
 *       } finally {
 *         if (sl.tryConvertToOptimisticRead(stamp) != 0L) // unlock or validate
 *           break;
 *       }
 *     }
 *     return Math.sqrt(currentX * currentX + currentY * currentY);
 *   }
 *
 *   void moveIfAtOrigin(double newX, double newY) { // upgrade
 *     // Could instead start with optimistic, not read mode
 *     long stamp = sl.readLock();
 *     try {
 *       while (x == 0.0 && y == 0.0) {
 *         long ws = sl.tryConvertToWriteLock(stamp);
 *         if (ws != 0L) {
 *           stamp = ws;
 *           x = newX;
 *           y = newY;
 *           break;
 *         }
 *         else {
 *           sl.unlockRead(stamp);
 *           stamp = sl.writeLock();
 *         }
 *       }
 *     } finally {
 *       sl.unlock(stamp);
 *     }
 * }
 * }}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public class StampedLock implements java.io.Serializable {
    /*
     * Algorithmic notes:
     *
     * The design employs elements of Sequence locks
     * (as used in linux kernels; see Lameter's
     * http://www.lameter.com/gelato2005.pdf
     * and elsewhere; see
     * Boehm's http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html)
     * and Ordered RW locks (see Shirako et al
     * http://dl.acm.org/citation.cfm?id=2312015)
     *
     * Conceptually, the primary state of the lock includes a sequence
     * number that is odd when write-locked and even otherwise.
     * However, this is offset by a reader count that is non-zero when
     * read-locked.  The read count is ignored when validating
     * "optimistic" seqlock-reader-style stamps.  Because we must use
     * a small finite number of bits (currently 7) for readers, a
     * supplementary reader overflow word is used when the number of
     * readers exceeds the count field. We do this by treating the max
     * reader count value (RBITS) as a spinlock protecting overflow
     * updates.
     *
     * Waiters use a modified form of CLH lock used in
     * AbstractQueuedSynchronizer (see its internal documentation for
     * a fuller account), where each node is tagged (field mode) as
     * either a reader or writer. Sets of waiting readers are grouped
     * (linked) under a common node (field cowait) so act as a single
     * node with respect to most CLH mechanics.  By virtue of the
     * queue structure, wait nodes need not actually carry sequence
     * numbers; we know each is greater than its predecessor.  This
     * simplifies the scheduling policy to a mainly-FIFO scheme that
     * incorporates elements of Phase-Fair locks (see Brandenburg &
     * Anderson, especially http://www.cs.unc.edu/~bbb/diss/).  In
     * particular, we use the phase-fair anti-barging rule: If an
     * incoming reader arrives while read lock is held but there is a
     * queued writer, this incoming reader is queued.  (This rule is
     * responsible for some of the complexity of method acquireRead,
     * but without it, the lock becomes highly unfair.)
     *
     * These rules apply to strands actually queued. All tryLock forms
     * opportunistically try to acquire locks regardless of preference
     * rules, and so may "barge" their way in.  Randomized spinning is
     * used in the acquire methods to reduce (increasingly expensive)
     * context switching while also avoiding sustained memory
     * thrashing among many strands.  We limit spins to the head of
     * queue. A strand spin-waits up to SPINS times (where each
     * iteration decreases spin count with 50% probability) before
     * blocking. If, upon wakening it fails to obtain lock, and is
     * still (or becomes) the first waiting strand (which indicates
     * that some other strand barged and obtained lock), it escalates
     * spins (up to MAX_HEAD_SPINS) to reduce the likelihood of
     * continually losing to barging strands.
     *
     * Nearly all of these mechanics are carried out in methods
     * acquireWrite and acquireRead, that, as typical of such code,
     * sprawl out because actions and retries rely on consistent sets
     * of locally cached reads.
     *
     * As noted in Boehm's paper (above), sequence validation (mainly
     * method validate()) requires stricter ordering rules than apply
     * to normal volatile reads (of "state").  In the absence of (but
     * continual hope for) explicit JVM support of intrinsics with
     * double-sided reordering prohibition, or corresponding fence
     * intrinsics, we for now uncomfortably rely on the fact that the
     * Unsafe.getXVolatile intrinsic must have this property
     * (syntactic volatile reads do not) for internal purposes anyway,
     * even though it is not documented.
     *
     * The memory layout keeps lock state and queue pointers together
     * (normally on the same cache line). This usually works well for
     * read-mostly loads. In most other cases, the natural tendency of
     * adaptive-spin CLH locks to reduce memory contention lessens
     * motivation to further spread out contended locations, but might
     * be subject to future improvements.
     */
    private static final long serialVersionUID = -6001602636862214147L;
    /**
     * Number of processors, for spin control
     */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    /**
     * Maximum number of retries before blocking on acquisition
     */
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;
    /**
     * Maximum number of retries before re-blocking
     */
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 12 : 0;
    /**
     * The period for yielding when waiting for overflow spinlock
     */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1
    /**
     * The number of bits to use for reader count before overflowing
     */
    private static final int LG_READERS = 7;
    // Values for lock state and stamp operations
    private static final long RUNIT = 1L;
    private static final long WBIT = 1L << LG_READERS;
    private static final long RBITS = WBIT - 1L;
    private static final long RFULL = RBITS - 1L;
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS; // note overlap with ABITS
    // Initial value for lock state; avoid failure value zero
    private static final long ORIGIN = WBIT << 1;
    // Special value from cancelled acquire methods so caller can throw IE
    private static final long INTERRUPTED = 1L;
    // Values for node status; order matters
    private static final int WAITING = -1;
    private static final int CANCELLED = 1;
    // Modes for nodes (int not boolean to allow arithmetic)
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /**
     * Wait nodes
     */
    static final class WNode {
        volatile WNode prev;
        volatile WNode next;
        volatile WNode cowait;    // list of linked readers
        volatile Strand strand;   // non-null while possibly parked
        volatile int status;      // 0, WAITING, or CANCELLED
        final int mode;           // RMODE or WMODE

        WNode(int m, WNode p) {
            mode = m;
            prev = p;
        }
    }
    /**
     * Head of CLH queue
     */
    private transient volatile WNode whead;
    /**
     * Tail (last) of CLH queue
     */
    private transient volatile WNode wtail;
    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;
    /**
     * Lock sequence/state
     */
    private transient volatile long state;
    /**
     * extra reader count when state read count saturated
     */
    private transient int readerOverflow;

    /**
     * Creates a new lock, initially in unlocked state.
     */
    public StampedLock() {
        state = ORIGIN;
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    @Suspendable
    public long writeLock() {
        try {
            long s, next;  // bypass acquireWrite in fully unlocked case only
            return ((((s = state) & ABITS) == 0L
                    && U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    ? next : acquireWrite(false, 0L));
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryWriteLock() {
        long s, next;
        return ((((s = state) & ABITS) == 0L
                && U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                ? next : 0L);
    }

    /**
     * Exclusively acquires the lock if it is available within the
     * given time and the current strand has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long,TimeUnit)}.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current strand is interrupted
     * before acquiring the lock
     */
    @Suspendable
    public long tryWriteLock(long time, TimeUnit unit)
            throws InterruptedException {
        try {
            long nanos = unit.toNanos(time);
            if (!Strand.interrupted()) {
                long next, deadline;
                if ((next = tryWriteLock()) != 0L)
                    return next;
                if (nanos <= 0L)
                    return 0L;
                if ((deadline = System.nanoTime() + nanos) == 0L)
                    deadline = 1L;
                if ((next = acquireWrite(true, deadline)) != INTERRUPTED)
                    return next;
            }
            throw new InterruptedException();
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available or the current strand is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current strand is interrupted
     * before acquiring the lock
     */
    @Suspendable
    public long writeLockInterruptibly() throws InterruptedException {
        try {
            long next;
            if (!Strand.interrupted()
                    && (next = acquireWrite(true, 0L)) != INTERRUPTED)
                return next;
            throw new InterruptedException();
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    @Suspendable
    public long readLock() {
        try {
            long s, next;  // bypass acquireRead on fully unlocked case only
            return ((((s = state) & ABITS) == 0L
                    && U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    ? next : acquireRead(false, 0L));
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Non-exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryReadLock() {
        for (;;) {
            long s, m, next;
            if ((m = (s = state) & ABITS) == WBIT)
                return 0L;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next;
            } else if ((next = tryIncReaderOverflow(s)) != 0L)
                return next;
        }
    }

    /**
     * Non-exclusively acquires the lock if it is available within the
     * given time and the current strand has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long,TimeUnit)}.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current strand is interrupted
     * before acquiring the lock
     */
    @Suspendable
    public long tryReadLock(long time, TimeUnit unit)
            throws InterruptedException {
        try {
            long s, m, next, deadline;
            long nanos = unit.toNanos(time);
            if (!Strand.interrupted()) {
                if ((m = (s = state) & ABITS) != WBIT) {
                    if (m < RFULL) {
                        if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                            return next;
                    } else if ((next = tryIncReaderOverflow(s)) != 0L)
                        return next;
                }
                if (nanos <= 0L)
                    return 0L;
                if ((deadline = System.nanoTime() + nanos) == 0L)
                    deadline = 1L;
                if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                    return next;
            }
            throw new InterruptedException();
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available or the current strand is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current strand is interrupted
     * before acquiring the lock
     */
    @Suspendable
    public long readLockInterruptibly() throws InterruptedException {
        try {
            long next;
            if (!Strand.interrupted()
                    && (next = acquireRead(true, 0L)) != INTERRUPTED)
                return next;
            throw new InterruptedException();
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    /**
     * Returns a stamp that can later be validated, or zero
     * if exclusively locked.
     *
     * @return a stamp, or zero if exclusively locked
     */
    public long tryOptimisticRead() {
        long s;
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * Returns true if the lock has not been exclusively acquired
     * since issuance of the given stamp. Always returns false if the
     * stamp is zero. Always returns true if the stamp represents a
     * currently held lock. Invoking this method with a value not
     * obtained from {@link #tryOptimisticRead} or a locking method
     * for this lock has no defined effect or result.
     *
     * @return true if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    public boolean validate(long stamp) {
        // See above about current use of getLongVolatile here
        return (stamp & SBITS) == (U.getLongVolatile(this, STATE) & SBITS);
    }

    /**
     * If the lock state matches the given stamp, releases the
     * exclusive lock.
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockWrite(long stamp) {
        WNode h;
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    /**
     * If the lock state matches the given stamp, releases the
     * non-exclusive lock.
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockRead(long stamp) {
        long s, m;
        WNode h;
        for (;;) {
            if (((s = state) & SBITS) != (stamp & SBITS)
                    || (stamp & ABITS) == 0L || (m = s & ABITS) == 0L || m == WBIT)
                throw new IllegalMonitorStateException();
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            } else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    /**
     * If the lock state matches the given stamp, releases the
     * corresponding mode of the lock.
     *
     * @param stamp a stamp returned by a lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlock(long stamp) {
        long a = stamp & ABITS, m, s;
        WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L)
                break;
            else if (m == WBIT) {
                if (a != m)
                    break;
                state = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return;
            } else if (a == 0L || a >= WBIT)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            } else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        throw new IllegalMonitorStateException();
    }

    /**
     * If the lock state matches the given stamp, performs one of
     * the following actions. If the stamp represents holding a write
     * lock, returns it. Or, if a read lock, if the write lock is
     * available, releases the read lock and returns a write stamp.
     * Or, if an optimistic read, returns a write stamp only if
     * immediately available. This method returns zero in all other
     * cases.
     *
     * @param stamp a stamp
     * @return a valid write stamp, or zero on failure
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    return next;
            } else if (m == WBIT) {
                if (a != m)
                    break;
                return stamp;
            } else if (m == RUNIT && a != 0L) {
                if (U.compareAndSwapLong(this, STATE, s,
                        next = s - RUNIT + WBIT))
                    return next;
            } else
                break;
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp, performs one of
     * the following actions. If the stamp represents holding a write
     * lock, releases it and obtains a read lock. Or, if a read lock,
     * returns it. Or, if an optimistic read, acquires a read lock and
     * returns a read stamp only if immediately available. This method
     * returns zero in all other cases.
     *
     * @param stamp a stamp
     * @return a valid read stamp, or zero on failure
     */
    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        WNode h;
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                else if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next;
                } else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            } else if (m == WBIT) {
                if (a != m)
                    break;
                state = next = s + (WBIT + RUNIT);
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            } else if (a != 0L && a < WBIT)
                return stamp;
            else
                break;
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp then, if the stamp
     * represents holding a lock, releases it and returns an
     * observation stamp. Or, if an optimistic read, returns it if
     * validated. This method returns zero in all other cases, and so
     * may be useful as a form of "tryUnlock".
     *
     * @param stamp a stamp
     * @return a valid optimistic read stamp, or zero on failure
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS, m, s, next;
        WNode h;
        for (;;) {
            s = U.getLongVolatile(this, STATE); // see above
            if ((s & SBITS) != (stamp & SBITS))
                break;
            if ((m = s & ABITS) == 0L) {
                if (a != 0L)
                    break;
                return s;
            } else if (m == WBIT) {
                if (a != m)
                    break;
                state = next = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            } else if (a == 0L || a >= WBIT)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return next & SBITS;
                }
            } else if ((next = tryDecReaderOverflow(s)) != 0L)
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * Releases the write lock if it is held, without requiring a
     * stamp value. This method may be useful for recovery after
     * errors.
     *
     * @return true if the lock was held, else false
     */
    public boolean tryUnlockWrite() {
        long s;
        WNode h;
        if (((s = state) & WBIT) != 0L) {
            state = (s += WBIT) == 0L ? ORIGIN : s;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return true;
        }
        return false;
    }

    /**
     * Releases one hold of the read lock if it is held, without
     * requiring a stamp value. This method may be useful for recovery
     * after errors.
     *
     * @return true if the read lock was held, else false
     */
    public boolean tryUnlockRead() {
        long s, m;
        WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            } else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    /**
     * Returns true if the lock is currently held exclusively.
     *
     * @return true if the lock is currently held exclusively
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * Returns true if the lock is currently held non-exclusively.
     *
     * @return true if the lock is currently held non-exclusively
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        state = ORIGIN; // reset to unlocked state
    }

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #readLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v
                : (readLockView = new ReadLockView()));
    }

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #writeLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v
                : (writeLockView = new WriteLockView()));
    }

    /**
     * Returns a {@link ReadWriteLock} view of this StampedLock in
     * which the {@link ReadWriteLock#readLock()} method is mapped to
     * {@link #asReadLock()}, and {@link ReadWriteLock#writeLock()} to
     * {@link #asWriteLock()}.
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v
                : (readWriteLockView = new ReadWriteLockView()));
    }

    // view classes
    final class ReadLockView implements Lock {
        @Suspendable
        public void lock() {
            readLock();
        }

        @Suspendable
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }

        public boolean tryLock() {
            return tryReadLock() != 0L;
        }

        @Suspendable
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }

        public void unlock() {
            unstampedUnlockRead();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        @Suspendable
        public void lock() {
            writeLock();
        }

        @Suspendable
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }

        public boolean tryLock() {
            return tryWriteLock() != 0L;
        }

        @Suspendable
        public boolean tryLock(long time, TimeUnit unit)
                throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }

        public void unlock() {
            unstampedUnlockWrite();
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() {
            return asReadLock();
        }

        public Lock writeLock() {
            return asWriteLock();
        }
    }

    // Unlock methods without stamp argument checks for view classes.
    // Needed because view-class lock methods throw away stamps.
    final void unstampedUnlockWrite() {
        WNode h;
        long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (s += WBIT) == 0L ? ORIGIN : s;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    final void unstampedUnlockRead() {
        for (;;) {
            long s, m;
            WNode h;
            if ((m = (s = state) & ABITS) == 0L || m >= WBIT)
                throw new IllegalMonitorStateException();
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            } else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    // internals
    /**
     * Tries to increment readerOverflow by first setting state
     * access bits value to RBITS, indicating hold of spinlock,
     * then updating, then releasing.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                ++readerOverflow;
                state = s;
                return s;
            }
        } else if ((ThreadLocalRandom.current().nextInt()
                & OVERFLOW_YIELD_RATE) == 0)
            if (!Strand.isCurrentFiber())
                Thread.yield();//Strand.yield();
        return 0L;
    }

    /**
     * Tries to decrement readerOverflow.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                int r;
                long next;
                if ((r = readerOverflow) > 0) {
                    readerOverflow = r - 1;
                    next = s;
                } else
                    next = s - RUNIT;
                state = next;
                return next;
            }
        } else if ((ThreadLocalRandom.current().nextInt()
                & OVERFLOW_YIELD_RATE) == 0)
            if (!Strand.isCurrentFiber())
                Thread.yield();//Strand.yield();
        return 0L;
    }

    /**
     * Wakes up the successor of h (normally whead). This is normally
     * just h.next, but may require traversal from wtail if next
     * pointers are lagging. This may fail to wake up an acquiring
     * strand when one or more have been cancelled, but the cancel
     * methods themselves provide extra safeguards to ensure liveness.
     */
    private void release(WNode h) {
        if (h != null) {
            WNode q;
            Strand w;
            U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (q != null) {
                for (WNode r = q;;) {  // release co-waiters too
                    if ((w = r.strand) != null) {
                        r.strand = null;
                        w.unpark();
                    }
                    if ((r = q.cowait) == null)
                        break;
                    U.compareAndSwapObject(q, WCOWAIT, r, r.cowait);
                }
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireWrite(boolean interruptible, long deadline) throws SuspendExecution {
        WNode node = null, p;
        for (int spins = -1;;) { // spin while enqueuing
            long s, ns;
            if (((s = state) & ABITS) == 0L) {
                if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                    return ns;
            } else if (spins > 0) {
                if (ThreadLocalRandom.current().nextInt() >= 0)
                    --spins;
            } else if ((p = wtail) == null) { // initialize queue
                WNode h = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, h))
                    wtail = h;
            } else if (spins < 0)
                spins = (p == whead) ? SPINS : 0;
            else if (node == null)
                node = new WNode(WMODE, p);
            else if (node.prev != p)
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                p.next = node;
                break;
            }
        }

        for (int spins = SPINS;;) {
            WNode np, pp;
            int ps;
            long s, ns;
            Strand w;
            while ((np = node.prev) != p && np != null)
                (p = np).next = node;   // stale
            if (whead == p) {
                for (int k = spins;;) { // spin at head
                    if (((s = state) & ABITS) == 0L) {
                        if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT)) {
                            whead = node;
                            node.prev = null;
                            return ns;
                        }
                    } else if (ThreadLocalRandom.current().nextInt() >= 0
                            && --k <= 0)
                        break;
                }
                if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
            }
            if ((ps = p.status) == 0)
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            } else {
                long time; // 0 argument to park means no timeout
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                node.strand = Strand.currentStrand();
                if (node.prev == p && p.status == WAITING && // recheck
                        (p != whead || (state & ABITS) != 0L))
                    park(time);
                node.strand = null;
                if (interruptible && Strand.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) throws SuspendExecution {
        WNode node = null, group = null, p;
        for (int spins = -1;;) {
            for (;;) {
                long s, m, ns;
                WNode h, q;
                Strand w; // anti-barging guard
                if (group == null && (h = whead) != null
                        && (q = h.next) != null && q.mode != RMODE)
                    break;
                if ((m = (s = state) & ABITS) < RFULL
                        ? U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT)
                        : (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                    if (group != null) {  // help release others
                        for (WNode r = group;;) {
                            if ((w = r.strand) != null) {
                                r.strand = null;
                                w.unpark();
                            }
                            if ((r = group.cowait) == null)
                                break;
                            U.compareAndSwapObject(group, WCOWAIT, r, r.cowait);
                        }
                    }
                    return ns;
                }
                if (m >= WBIT)
                    break;
            }
            if (spins > 0) {
                if (ThreadLocalRandom.current().nextInt() >= 0)
                    --spins;
            } else if ((p = wtail) == null) {
                WNode h = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, h))
                    wtail = h;
            } else if (spins < 0)
                spins = (p == whead) ? SPINS : 0;
            else if (node == null)
                node = new WNode(WMODE, p);
            else if (node.prev != p)
                node.prev = p;
            else if (p.mode == RMODE && p != whead) {
                WNode pp = p.prev;  // become co-waiter with group p
                if (pp != null && p == wtail
                        && U.compareAndSwapObject(p, WCOWAIT,
                        node.cowait = p.cowait, node)) {
                    node.strand = Strand.currentStrand();
                    for (long time;;) {
                        if (interruptible && Strand.interrupted())
                            return cancelWaiter(node, p, true);
                        if (deadline == 0L)
                            time = 0L;
                        else if ((time = deadline - System.nanoTime()) <= 0L)
                            return cancelWaiter(node, p, false);
                        if (node.strand == null)
                            break;
                        if (p.prev != pp || p.status == CANCELLED
                                || p == whead || p.prev != pp) {
                            node.strand = null;
                            break;
                        }
                        if (node.strand == null) // must recheck
                            break;
                        park(time);
                    }
                    group = p;
                }
                node = null; // throw away
            } else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                p.next = node;
                break;
            }
        }

        for (int spins = SPINS;;) {
            WNode np, pp, r;
            int ps;
            long m, s, ns;
            Strand w;
            while ((np = node.prev) != p && np != null)
                (p = np).next = node;
            if (whead == p) {
                for (int k = spins;;) {
                    if ((m = (s = state) & ABITS) != WBIT) {
                        if (m < RFULL
                                ? U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT)
                                : (ns = tryIncReaderOverflow(s)) != 0L) {
                            whead = node;
                            node.prev = null;
                            while ((r = node.cowait) != null) {
                                if (U.compareAndSwapObject(node, WCOWAIT,
                                        r, r.cowait)
                                        && (w = r.strand) != null) {
                                    r.strand = null;
                                    w.unpark(); // release co-waiter
                                }
                            }
                            return ns;
                        }
                    } else if (ThreadLocalRandom.current().nextInt() >= 0
                            && --k <= 0)
                        break;
                }
                if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
            }
            if ((ps = p.status) == 0)
                U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
            else if (ps == CANCELLED) {
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            } else {
                long time;
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                node.strand = Strand.currentStrand();
                if (node.prev == p && p.status == WAITING
                        && (p != whead || (state & ABITS) != WBIT))
                    park(time);
                node.strand = null;
                if (interruptible && Strand.interrupted())
                    return cancelWaiter(node, node, true);
            }
        }
    }

    /**
     * If node non-null, forces cancel status and unsplices it from
     * queue if possible and wakes up any cowaiters (of the node, or
     * group, as applicable), and in any case helps release current
     * first waiter if lock is free. (Calling with null arguments
     * serves as a conditional form of release, which is not currently
     * needed but may be needed under possible future cancellation
     * policies). This is a variant of cancellation methods in
     * AbstractQueuedSynchronizer (see its detailed explanation in AQS
     * internal documentation).
     *
     * @param node if nonnull, the waiter
     * @param group either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     * @return INTERRUPTED if interrupted or Strand.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        if (node != null && group != null) {
            Strand w;
            node.status = CANCELLED;
            node.strand = null;
            // unsplice cancelled nodes from group
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED)
                    U.compareAndSwapObject(p, WNEXT, q, q.next);
                else
                    p = q;
            }
            if (group == node) {
                WNode r; // detach and wake up uncancelled co-waiters
                while ((r = node.cowait) != null) {
                    if (U.compareAndSwapObject(node, WCOWAIT, r, r.cowait)
                            && (w = r.strand) != null) {
                        r.strand = null;
                        w.unpark();
                    }
                }
                for (WNode pred = node.prev; pred != null;) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while ((succ = node.next) == null
                            || succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // don't link if succ cancelled
                        if (succ == q || // ensure accurate successor
                                U.compareAndSwapObject(node, WNEXT,
                                succ, succ = q)) {
                            if (succ == null && node == wtail)
                                U.compareAndSwapObject(this, WTAIL, node, pred);
                            break;
                        }
                    }
                    if (pred.next == node) // unsplice pred link
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    if (succ != null && (w = succ.strand) != null) {
                        succ.strand = null;
                        w.unpark();       // wake up succ to observe new pred
                    }
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while ((h = whead) != null) {
            long s;
            WNode q; // similar to release() but check eligibility
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                if (q != null && h.status == 0
                        && ((s = state) & ABITS) != WBIT && // waiter is eligible
                        (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Strand.interrupted()) ? INTERRUPTED : 0L;
    }
    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATE;
    private static final long WHEAD;
    private static final long WTAIL;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WCOWAIT;

    static {
        try {
            U = UtilUnsafe.getUnsafe();
            Class<?> k = StampedLock.class;
            Class<?> wk = WNode.class;
            STATE = U.objectFieldOffset(k.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset(k.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset(k.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset(wk.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset(wk.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset(wk.getDeclaredField("cowait"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private static void park(long time) throws SuspendExecution {
        if (time != 0)
            Strand.parkNanos(time);
        else
            Strand.park();
    }
}

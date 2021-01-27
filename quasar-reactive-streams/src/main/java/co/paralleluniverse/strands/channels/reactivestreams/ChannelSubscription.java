/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.strands.channels.reactivestreams;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.OwnedSynchronizer;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author pron
 */
class ChannelSubscription<T> implements Subscription, SuspendableCallable<Void> {
    private final ReceivePort<T> ch;
    private final Subscriber<? super T> s;
    private final Condition sync = new OwnedSynchronizer(this);
//    private final Fiber<Void> f;
    private volatile boolean cancelled;
    private volatile Throwable error;
    private final AtomicLong requested = new AtomicLong();

    public ChannelSubscription(Subscriber<? super T> s, ReceivePort<T> ch) {
        if (s == null)
            throw new NullPointerException(); // #1.9
        this.s = s;
        this.ch = ch;
//        this.f = Fiber.currentFiber();
    }

    @Override
    public void request(long n) {
        if (cancelled)
            return;
        if (n <= 0)
            cancel(new IllegalArgumentException("Requested number must not be > 0 (#3.9) but was " + n));
        else {
            long res = requested.addAndGet(n);
            if (n == Long.MAX_VALUE || res < 0 && n > 0) {
                requested.set(-1); // #3.17
                sync.signal();
            } else if (res == n)
                sync.signal();
        }
    }

    private void cancel(Throwable error) {
        if (error != null)
            this.error = error;
        else
            this.cancelled = true;
        requested.set(-1);
        ch.close();
        sync.signal();
    }

    private boolean checkCancelled() throws Throwable {
        if (error != null)
            throw error;
        return cancelled;
    }

    private boolean checkClosed() throws SuspendExecution, InterruptedException {
        if (ch.isClosed()) {
            ch.receive(); // throw exception if any
            return true;
        }
        return false;
    }

    @Override
    public void cancel() {
        cancel(null);
    }

    @Override
    public Void run() throws SuspendExecution, InterruptedException {
        // assumes all requests are done on this fiber
        try {
            s.onSubscribe(this);
            loop:
            for (;;) {
                long r = getRequested(50, TimeUnit.MILLISECONDS); // #1.4 -- we check for closed periodically
                if (checkClosed() || checkCancelled())
                    break;
                while (r != 0) {
                    T m = ch.receive();
                    if (m == null)
                        break loop;
                    s.onNext(m);
                    r = r > 0 ? r - 1 : r;
                }
            }
            if (!checkCancelled())
                s.onComplete();
            return null;
        } catch (Throwable t) {
            if (t instanceof ProducerException)
                t = t.getCause();
            s.onError(t);
        }
        return null;
    }

    private long getRequested(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        long r = requested.get();
        if (r < 0)
            return r;
        if (r == 0) {
            long left = unit.toNanos(timeout);
            final long deadline = System.nanoTime() + left;

            sync.register();
            try {
                for (int i = 0; (r = requested.get()) == 0; i++) {
                    sync.await(i, left, TimeUnit.NANOSECONDS);

                    left = deadline - System.nanoTime();
                    if (left <= 0)
                        break;
                }
            } finally {
                sync.unregister(requested);
            }
        }
        if (r > 0)
            requested.addAndGet(-r);
        return r;
    }

//    @Override
//    public void request(int n) {
//        if (Strand.currentStrand() != f)
//            throw new IllegalStateException("Not called in handler");
//        requested += n;
//    }
//
//    @Override
//    public void cancel() {
//        ch.close();
//    }
//
//    private int getRequested() {
//        int r = requested;
//        requested = 0;
//        return r;
//    }   
}

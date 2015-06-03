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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.QueueChannel;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author pron
 */
class ChannelSubscriber<T> implements Subscriber<T>, ReceivePort<T> {
    private final int capacity;
    private final QueueChannel<T> ch;
    private final long request;
    private Subscription subscription;
    private int consumed;

    public ChannelSubscriber(int bufferSize, Channels.OverflowPolicy policy) {
        this.ch = (QueueChannel<T>) Channels.newChannel(bufferSize, policy, true, true);
        this.capacity = bufferSize;
        this.request = (bufferSize < 0 || policy == Channels.OverflowPolicy.DISPLACE) ? Long.MAX_VALUE : bufferSize;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (s == null)
            throw new NullPointerException(); // #2.13
        if (subscription != null)             // #2.5 TODO: concurrency?
            s.cancel();
        else {
            this.subscription = s;
            subscription.request(request);
        }
    }

    @Override
    @Suspendable
    public void onNext(T element) {
        if (element == null)
            throw new NullPointerException(); // #2.13
        try {
            if (ch.isClosed())
                subscription.cancel();
            else
                ch.send(element);
        } catch (InterruptedException e) {
            Strand.interrupted();
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onError(Throwable cause) {
        if (cause == null)
            throw new NullPointerException(); // #2.13
        ch.close(cause);
    }

    @Override
    public void onComplete() {
        ch.close();
    }

    private void consumed() {
        if (request == Long.MAX_VALUE)
            return;

        consumed++;
        if (consumed < request)
            return;
        consumed = 0;
        subscription.request(request);
    }

    @Override
    public void close() {
        subscription.cancel();
        ch.close();
    }

    @Override
    public T receive() throws SuspendExecution, InterruptedException {
        T m = ch.receive();
        consumed();
        return m;
    }

    @Override
    public T receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        T m = ch.receive(timeout, unit);
        if (m != null)
            consumed();
        return m;
    }

    @Override
    public T receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        T m = ch.receive(timeout);
        if (m != null)
            consumed();
        return m;
    }

    @Override
    public T tryReceive() {
        T m = ch.tryReceive();
        if (m != null)
            consumed();
        return m;
    }

    @Override
    public boolean isClosed() {
        return ch.isClosed();
    }
}

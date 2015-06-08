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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableAction2;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.ProducerException;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.atomic.AtomicInteger;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 *
 * @author pron
 */
class ChannelProcessor<T, R> implements Processor<T, R> {
    private final ChannelSubscriber<T> subscriber;
    private final ChannelPublisher<R> publisher;

    private final FiberFactory ff;
    private final SuspendableAction2<? extends ReceivePort<? super T>, ? extends SendPort<? extends R>> transformer;
    private final ReceivePort<T> in;
    private final SendPort<R> out;
    private final AtomicInteger connectedEnds = new AtomicInteger();
    private volatile Subscription subscription;

    public ChannelProcessor(FiberFactory ff, boolean batch, Channel<T> in, Channel<R> out, SuspendableAction2<? extends ReceivePort<? super T>, ? extends SendPort<? extends R>> transformer) {
        this.ff = ff != null ? ff : defaultFiberFactory;
        this.transformer = transformer;
        this.subscriber = new ChannelSubscriber<T>(in, batch) {
            @Override
            protected void failedSubscribe(Subscription s) {
                super.failedSubscribe(s);
                throw new FailedSubscriptionException();
            }
        };
        this.publisher = new ChannelPublisher<R>(ff, out, true) {
            @Override
            protected void failedSubscribe(Subscriber<? super R> s, Throwable t) {
                super.failedSubscribe(s, t);
                throw new FailedSubscriptionException();
            }

            @Override
            protected ChannelSubscription<R> newChannelSubscription(Subscriber<? super R> s, Object channel) {
                return new ChannelSubscription<R>(s, (ReceivePort<R>) channel) {
                    @Override
                    public void cancel() {
                        super.cancel();
                        Subscription ms = subscription;
                        if (ms != null)
                            ms.cancel();
                    }
                };
            }
        };
        this.in = subscriber;
        this.out = out;
    }

    private void connected() {
        int connections = connectedEnds.incrementAndGet();
        if (connections == 2)
            start();
        if (connections > 2)
            throw new AssertionError();
    }

    private void start() {
        ff.newFiber(new SuspendableCallable<Void>() {
            @Override
            public Void run() throws SuspendExecution, InterruptedException {
                try {
                    ((SuspendableAction2) transformer).call(in, out);
                    out.close();
                } catch (ProducerException e) {
                    out.close(e.getCause());
                } catch (Throwable t) {
                    out.close(t);
                }
                return null;
            }
        }).start();
    }

    @Override
    public void subscribe(Subscriber<? super R> s) {
        try {
            publisher.subscribe(s);
            connected();
        } catch (FailedSubscriptionException e) {
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        try {
            subscriber.onSubscribe(s);
            this.subscription = s;
            connected();
        } catch (FailedSubscriptionException e) {
        }
    }

    @Override
    public void onNext(T element) {
        subscriber.onNext(element);
    }

    @Override
    public void onError(Throwable cause) {
        subscriber.onError(cause);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    private static final FiberFactory defaultFiberFactory = new FiberFactory() {
        @Override
        public <T> Fiber<T> newFiber(SuspendableCallable<T> target) {
            return new Fiber(target);
        }
    };

    private static class FailedSubscriptionException extends RuntimeException {
    }
}

/*
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
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.channels.SendPort;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class TestHelper {
    public static <T extends SendPort<Integer>> T startPublisherFiber(final T s, final long delay, final long elements) {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                if (delay > 0)
                    Strand.sleep(delay);

                // we only emit up to 100K elements or 100ms, the later of the two (the TCK asks for 2^31-1)
                long start = elements > 100_000 ? System.nanoTime() : 0L;
                for (long i = 0; i < elements; i++) {
                    s.send((int) (i % 10000));

                    if (start > 0) {
                        long elapsed = (System.nanoTime() - start) / 1_000_000;
                        if (elapsed > 100)
                            break;
                    }
                }
                s.close();
            }
        }).start();
        return s;
    }

    public static <T extends SendPort<Integer>> T startFailedPublisherFiber(final T s, final long delay) {
        new Fiber<Void>(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                if (delay > 0)
                    Strand.sleep(delay);
                s.close(new Exception("failure"));
            }
        }).start();
        return s;
    }
    
    public static <T> Publisher<T> createDummyFailedPublisher() {
        return new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                s.onSubscribe(new Subscription() {
                    @Override
                    public void request(long n) {
                    }

                    @Override
                    public void cancel() {
                    }
                });
                s.onError(new RuntimeException("Can't subscribe subscriber: " + s + ", because of reasons."));
            }
        };
    }
}

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
package co.paralleluniverse.strands.channels.transfer;

import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableAction2;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author circlespainter
 */
public class Pipeline<S, T> implements SuspendableCallable<Long> {
    private static final boolean closeToDefault = true;
    private static final int parallelismDefault = 0;
    private static final StrandFactory strandFactoryDefault = DefaultFiberScheduler.getInstance();

    private final SuspendableCallable<Channel<T>> resultChannelBuilderDefault =
        new SuspendableCallable<Channel<T>>() {
            @Override
            public Channel<T> run() throws SuspendExecution, InterruptedException {
                return Channels.newChannel(1, Channels.OverflowPolicy.BLOCK, true, true);
            }
        };

    private final Channel<Pair<S, Channel<Channel<T>>>> jobs;
    private final Channel<Channel<Channel<T>>> results;

    private final ReceivePort<? extends S> from;
    private final SendPort<? super T> to;
    private final int parallelism;
    private final StrandFactory strandFactory;
    private final boolean closeTo;

    private final AtomicLong transferred = new AtomicLong(0);

    private final SuspendableCallable<Channel<T>> resultChannelBuilder;
    private final SuspendableAction2<S, Channel<T>> transformer;

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final SuspendableAction2<S, Channel<T>> transformer, final int parallelism, boolean closeTo, final SuspendableCallable<Channel<T>> resultChannelBuilder, final StrandFactory strandFactory) {
        this.from = from;
        this.to = to;
        this.transformer = transformer;
        this.parallelism = parallelism <= 0 ? 0 : parallelism;
        this.jobs = Channels.newChannel(this.parallelism, Channels.OverflowPolicy.BLOCK, true, false);
        this.results = Channels.newChannel(this.parallelism, Channels.OverflowPolicy.BLOCK, false, true);
        this.closeTo = closeTo;
        this.resultChannelBuilder = resultChannelBuilder != null ? resultChannelBuilder : resultChannelBuilderDefault;
        this.strandFactory = strandFactory;
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final SuspendableAction2<S, Channel<T>> transformer, final int parallelism, boolean closeTo, final SuspendableCallable<Channel<T>> resultChannelBuilder) {
            this(from, to, transformer, parallelism, closeTo, resultChannelBuilder, strandFactoryDefault);
    }
    
    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final SuspendableAction2<S, Channel<T>> transformer, final int parallelism, boolean closeTo) {
        this(from, to, transformer, parallelism, closeTo, null, strandFactoryDefault);
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final SuspendableAction2<S, Channel<T>> transformer, final int parallelism) {
        this(from, to, transformer, parallelism, closeToDefault, null, strandFactoryDefault);
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final SuspendableAction2<S, Channel<T>> transformer) {
        this(from, to, transformer, parallelismDefault, closeToDefault, null, strandFactoryDefault);
    }

    public long getTransferred() {
        return transferred.get();
    }

    @Override
    public Long run() throws SuspendExecution, InterruptedException {
        if (parallelism == 0)
            sequentialTransfer();
        else
            parallelTransfer();

        if (closeTo)
            to.close();

        return transferred.get();
    }

    private void parallelTransfer() throws SuspendExecution, InterruptedException {

        // 1) Fire workers
        for(int i = 0 ; i < parallelism ; i++) {
            strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    // Get first job
                    Pair<S, Channel<Channel<T>>> job = jobs.receive();
                    while(job != null) {
                        // Build result channel
                        final Channel<T> res = resultChannelBuilder.run();
                        // Process
                        transformer.call(job.getFirst(), res);
                        final Channel<Channel<T>> resWrapper = job.getSecond();
                        // Send result asynchronously
                        strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                            @Override
                            public void run() throws SuspendExecution, InterruptedException {
                                resWrapper.send(res);
                            }
                        })).start();
                        // Get next job
                        job = jobs.receive();
                    }
                    // No more jobs, close results channel and quit worker
                    results.close();
                }
            })).start();
        }

        // 2) Send jobs asynchronously
        strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    // Get first input
                    S s = from.receive();
                    while (s != null) {
                        final Channel<Channel<T>> resultWrapper = Channels.newChannel(1, Channels.OverflowPolicy.BLOCK, true, true);
                        jobs.send(new Pair<>(s, resultWrapper));
                        results.send(resultWrapper);
                        // Get next input
                        s = from.receive();
                    }
                    // No more inputs, close jobs channel and quit
                    jobs.close();
                }
        })).start();

        // 3) Collect and transfer results asynchronously
        try {
            final Strand collector = strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    // Get first result
                    Channel<Channel<T>> resWrapper = results.receive();
                    while (resWrapper != null) {
                        // Get wrapper
                        Channel<T> res = resWrapper.receive();
                        // Get first actual result
                        T out = res.receive();
                        while(out != null) {
                            // Send to output channel
                            to.send(out);
                            // Increment counter
                            transferred.incrementAndGet();
                            // Get next result
                            out = res.receive();
                        }
                        resWrapper = results.receive();
                    }
                    // No more results, quit
                }
            })).start();

            // TODO solve nasty instrumentation problems on Strand.join()
            if (collector.isFiber()) {
                Fiber f = (Fiber) collector.getUnderlying();
                f.join();
            } else
                collector.join();
        } catch (ExecutionException ee) {
            throw new AssertionError(ee);
        }
    }

    private void sequentialTransfer() throws InterruptedException, SuspendExecution {
        S s = from.receive();
        while (s != null) {
            // Build result channel
            final Channel<T> res = resultChannelBuilder.run();
            // Process
            transformer.call(s, res);
            T out = res.receive();
            while(out != null) {
                // Send to output channel
                to.send(out);
                // Increment counter
                transferred.incrementAndGet();
                // Get next result
                out = res.receive();
            }
            s = from.receive();
        }
    }
}

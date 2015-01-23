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

import co.paralleluniverse.fibers.DefaultFiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.ReceivePort;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.channels.TransferChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author circlespainter
 */
public abstract class Pipeline<S, T> implements SuspendableCallable<Long> {
    private static final boolean closeToDefault = true;
    private static final int parallelismDefault = 0;
    private static final StrandFactory strandFactoryDefault = DefaultFiberScheduler.getInstance();

    private final Channel<S> internalCh = new TransferChannel<>();

    private final ReceivePort<? extends S> from;
    private final SendPort<? super T> to;
    private final int parallelism;
    private final StrandFactory strandFactory;
    private final boolean closeTo;

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final int parallelism, boolean closeTo, final StrandFactory strandFactory) {
        this.from = from;
        this.to = to;
        this.parallelism = parallelism;
        this.closeTo = closeTo;
        this.strandFactory = strandFactory;
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final int parallelism, boolean closeTo) {
        this(from, to, parallelism, closeTo, strandFactoryDefault);
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final int parallelism) {
        this(from, to, parallelism, closeToDefault, strandFactoryDefault);
    }

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to) {
        this(from, to, parallelismDefault, closeToDefault, strandFactoryDefault);
    }

    public abstract T transform(S input) throws SuspendExecution, InterruptedException;

    @Override
    public Long run() throws SuspendExecution, InterruptedException {
        bringInLoopingStrand();

        final long transferred = (parallelism <= 0 ? sequentialTransfer() : parallelTransfer());

        if (closeTo)
            to.close();

        return transferred;
    }

    private long parallelTransfer() throws SuspendExecution, InterruptedException {
        final Channel<Strand> activeWorkers = Channels.newChannel(parallelism);
        final AtomicLong transferred = new AtomicLong();

        long running = 0;

        while (!internalCh.isClosed()) {
            if (running == parallelism) {
                // Parallelism threshold reached, wait for completion
                joinWorker(activeWorkers);
                // Free slot
                running--;
            } else {
                // Book slot
                running++;
                // New transfer worker
                activeWorkers.send(strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        if (transferOne(internalCh, to))
                            // Transfer successful, increase atomic counter
                            transferred.incrementAndGet();
                    }
                })).start());
            }
        }

        // No more workers will be started/added after the input channel has been closed
        activeWorkers.close();
        // Wait for all workers
        while(!activeWorkers.isClosed())
            joinWorker(activeWorkers);

        return transferred.get();
    }

    private long sequentialTransfer() throws InterruptedException, SuspendExecution {
        long transferred = 0;

        while(transferOne(internalCh, to))
            transferred++;

        return transferred;
    }

    private void bringInLoopingStrand() {
        strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
            @Override
            public void run() throws SuspendExecution, InterruptedException {
                S msg = from.receive();
                while(msg != null) {
                    internalCh.send(msg);
                    msg = from.receive();
                }
                internalCh.close();
                // End of job, join
            }
        })).start();
    }

    private boolean transferOne(final ReceivePort<? extends S> from, final SendPort<? super T> to) throws SuspendExecution, InterruptedException {
        final S m = from.receive();
        if (m != null) {
            to.send(transform(m));
            return true;
        } else {
            if (closeTo)
                to.close();
            return false;
        }
    }

    private void joinWorker(final Channel<Strand> workers) throws SuspendExecution, InterruptedException {
        try {
            workers.receive().join();
        } catch (ExecutionException ex) {
            // It should never happen
            throw new AssertionError(ex);
        }
    }
}

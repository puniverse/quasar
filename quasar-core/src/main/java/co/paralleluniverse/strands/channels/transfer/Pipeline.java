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

    private final AtomicLong transferred = new AtomicLong(0);
    private int runningParallelWorkers = 0;

    public Pipeline(final ReceivePort<? extends S> from, final SendPort<? super T> to, final int parallelism, boolean closeTo, final StrandFactory strandFactory) {
        this.from = from;
        this.to = to;
        this.parallelism = parallelism <= 0 ? 0 : parallelism;
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

    public int getRunningParallelWorkers() {
        return runningParallelWorkers;
    }
    
    public long getTransferred() {
        return transferred.get();
    }

    protected abstract T transform(S input) throws SuspendExecution, InterruptedException;

    @Override
    public Long run() throws SuspendExecution, InterruptedException {
        bringInLoopingStrand();

        if (parallelism == 0)
            sequentialTransfer();
        else
            parallelTransfer();

        if (closeTo)
            to.close();

        return transferred.get();
    }

    private long parallelTransfer() throws SuspendExecution, InterruptedException {
        final Channel<Strand> activeWorkers = Channels.newChannel(parallelism);

        while (!internalCh.isClosed()) {
            if (runningParallelWorkers == parallelism) {
                // Parallelism threshold reached, wait for completion
                // TODO in-order join here could hinder parallelism, maybe better using signalling on condition
                final Strand f = activeWorkers.receive();
                if (f != null) {
                    try {
                        f.join();
                        // Free slot
                        runningParallelWorkers--;
                    } catch (ExecutionException ex) {
                        // It should never happen
                        throw new AssertionError(ex);
                    }
                }
            } else {
                // Book slot
                runningParallelWorkers++;
                // New transfer worker
                activeWorkers.send(strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                    @Override
                    public void run() throws SuspendExecution, InterruptedException {
                        if (transferOne())
                            // Transfer successful, increment atomic counter
                            transferred.incrementAndGet();
                    }
                })).start());
            }
        }

        // No more workers will be started/added after the input channel has been closed
        activeWorkers.close();

        // Wait for all workers
        Strand f = activeWorkers.receive();
        while(f != null) {
            try {
                f.join();
            } catch (ExecutionException ex) {
                // It should never happen
                throw new AssertionError(ex);
            }
            f = activeWorkers.receive();
        }

        return transferred.get();
    }

    private long sequentialTransfer() throws InterruptedException, SuspendExecution {
        while(transferOne())
            transferred.incrementAndGet();

        return transferred.get();
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

    private boolean transferOne() throws SuspendExecution, InterruptedException {
        final S m = internalCh.receive();
        if (m != null) {
            to.send(transform(m));
            return true;
        }

        return false;
    }
}

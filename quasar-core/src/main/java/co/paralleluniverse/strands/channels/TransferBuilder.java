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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.DefaultFiberFactory;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableCallable;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fluent API for data transfer between ports.
 * <p/>
 * @author circlespainter
 */
public class TransferBuilder<Message> implements SuspendableCallable<Integer> {
    private static final boolean closeToDefault = true;
    private static final int parallelismDefault = 0;
    private static final int countDefault = 1;
    private static final StrandFactory strandFactoryDefault = DefaultFiberFactory.instance();

    private final ReceivePort<Message> from;
    private final SendPort<Message> to;

    private int parallelism = parallelismDefault;
    private boolean parallelismSet = false;

    private int count = countDefault;
    private boolean countSet = false;

    private StrandFactory strandFactory = strandFactoryDefault;

    private boolean closeTo = closeToDefault;

    /**
     * Mandatory information to start a transfer are the source and destination ports. Other parameters are set to defaults (1 transfer, )
     */
    public TransferBuilder(final ReceivePort<Message> from, final SendPort<Message> to) {
        this.from = from;
        this.to = to;
    }

    /**
     * Sets the transfer parallelism; it can be set only once. Valid values are {@code 0} (no strands spawned) and {@code > 1}.
     * A strand factory must be provided as well, if {@code null} the {@link DefaultFiberFactory} instance will be used.
     */
    public TransferBuilder setParallelism(final int parallelism, final StrandFactory strandFactory) {
        if (parallelismSet)
            cannotChange("parallelism");

        if (parallelism < 0 || parallelism == 1)
            unsupportedValue("parallelism", Integer.toString(parallelism), "0 (no strands spawned) and greater than 1");

        this.parallelism = parallelism;
        parallelismSet = true;
        this.strandFactory = strandFactory != null ? strandFactory : strandFactoryDefault;
        return this;
    }

    /**
     * Sets the transfer parallelism; it can be set only once. Valid values are {@code 0} (no strands spawned) and {@code > 1}.
     * The strand factory will be unchanged.
     */
    public TransferBuilder setParallelism(final int parallelism) {
        return setParallelism(parallelism, this.strandFactory);
    }
    
    public int getParallelism() {
        return parallelism;
    }

    public boolean isParallelismSet() {
        return parallelismSet;
    }

    /**
     * Sets the transfer count; it can be set only once. Valid values are {@code -1} (until the {@link ReceivePort} is closed) and {@code > 0}.
     */
    public TransferBuilder setCount(final int count) {
        if (countSet)
            cannotChange("count");

        if (count == 0 || count < -1)
            unsupportedValue("count", Integer.toString(count), "-1 (until closed) and greater than 0");

        this.count = count;
        countSet = true;
        return this;
    }

    public int getCount() {
        return count;
    }

    public boolean isCountSet() {
        return countSet;
    }

    public StrandFactory getStrandFactory() {
        return strandFactory;
    }

    public TransferBuilder setCloseTo(final boolean closeTo) {
        this.closeTo = closeTo;
        return this;
    }

    public boolean getCloseTo() {
        return closeTo;
    }

    @Override
    public Integer run() throws SuspendExecution, InterruptedException {
        final AtomicInteger transferred = new AtomicInteger(0);
        if (parallelism == 0) {
            while((count == -1 || transferred.get() < count) && transfer(from, to)) {
                transferred.incrementAndGet();
            }
        } else {
            final Channel<Strand> joinable = Channels.newChannel(parallelism);
            int running = 0;
            while (transferred.get() != count && !from.isClosed()) {
                if (running == parallelism) {
                    completeOne(joinable);
                    running--;
                } else {
                    running++;
                    joinable.send(strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                        @Override
                        public void run() throws SuspendExecution, InterruptedException {
                            if (transfer(from, to))
                                transferred.incrementAndGet();
                        }
                    })).start());
                }
            }
            joinable.close();
            while(!joinable.isClosed())
                completeOne(joinable);
        }
        
        if (closeTo)
            to.close();

        return transferred.get();
    }

    private void completeOne(final Channel<Strand> joinable) throws SuspendExecution, InterruptedException {
        try {
            joinable.receive().join();
        } catch (ExecutionException ex) {
            // It should never happen
            throw new AssertionError(ex);
        }
    }

    private boolean transfer(final ReceivePort<Message> from, final SendPort<Message> to) throws SuspendExecution, InterruptedException {
        if (!from.isClosed()) {
            to.send(from.receive());
            return true;
        } else {
            if (closeTo)
                to.close();
            return false;
        }
    }

    private void cannotChange(final String paramName) {
        throw new UnsupportedOperationException("'" + paramName + "' cannot be changed once set");
    }
    
    private void unsupportedValue(final String paramName, final String value, final String supported) {
        throw new IllegalArgumentException("'" + paramName + "' cannot be set to '" + value + "': supported values are " + supported);
    }
}

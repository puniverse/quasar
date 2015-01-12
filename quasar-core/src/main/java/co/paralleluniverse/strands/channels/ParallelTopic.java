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
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;
import co.paralleluniverse.strands.Timeout;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A topic that will spawn fibers from a factory and distribute messages to subscribers in parallel
 * using strands, optionally waiting for them to complete receive before delivering the next one.
 * 
 * @author circlespainter
 */
public class ParallelTopic<Message> extends Topic<Message> {
    private static final boolean stagedDefault = true;
    private static final StrandFactory strandFactoryDefault = DefaultFiberFactory.instance();
    
    private final StrandFactory strandFactory;
    private final Channel<Message> internalChannel;
    
    private ParallelTopic(Channel<Message> internalChannel, StrandFactory strandFactory, boolean staged) {
        this.internalChannel = internalChannel;
        this.strandFactory = strandFactory;
        
        startDistributionLoop(staged);
    }
    
    /**
     * Creates a new ParallelTopic message distributor ({@link PubSub}) with the given buffer parameters, {@link StrandFactory} and staging behavior.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     * @param policy        The buffer policy of this topic.
     * @param strandFactory The {@llink StrandFactory} instance that will build the strands performing send operations to subscribers as well as the looping
     *                      receive strand.
     * @param staged        Whether all send operations to subscribers for a given message must be completed before initiating the subsequent one.
     */
    public ParallelTopic(int bufferSize, Channels.OverflowPolicy policy, StrandFactory strandFactory, boolean staged) {
        this(Channels.<Message>newChannel(bufferSize, policy), strandFactory, staged);
    }

    /**
     * Creates a new ParallelTopic staged message distributor ({@link PubSub}) with the given buffer parameters and {@link StrandFactory}.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     * @param policy        The buffer policy of this topic.
     * @param strandFactory The {@llink StrandFactory} instance that will build the strands performing send operations to subscribers as well as the looping
     *                      receive strand.
     */
    public ParallelTopic(int bufferSize, Channels.OverflowPolicy policy, StrandFactory strandFactory) {
        this(bufferSize, policy, strandFactory, stagedDefault);
    }

    /**
     * Creates a new ParallelTopic message distributor ({@link PubSub}) using a fiber-creating {@link StrandFactory} and with the given buffer parameters and staging behavior.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     * @param policy        The buffer policy of this topic.
     * @param staged        Whether all send operations to subscribers for a given message must be completed before initiating the subsequent one.
     */
    public ParallelTopic(int bufferSize, Channels.OverflowPolicy policy, boolean staged) {
        this(bufferSize, policy, strandFactoryDefault);
    }

    /**
     * Creates a new staged ParallelTopic message distributor ({@link PubSub}) using a fiber-creating {@link StrandFactory} and with the given buffer parameters.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     * @param policy        The buffer policy of this topic.
     */
    public ParallelTopic(int bufferSize, Channels.OverflowPolicy policy) {
        this(bufferSize, policy, stagedDefault);
    }

    /**
     * Creates a new ParallelTopic message distributor ({@link PubSub}) using a fiber-creating {@link StrandFactory} and with the given buffer parameters and staging behavior.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     * @param staged        Whether all send operations to subscribers for a given message must be completed before initiating the subsequent one.
     */
    public ParallelTopic(int bufferSize, boolean staged) {
        this(Channels.<Message>newChannel(bufferSize), strandFactoryDefault, staged);
    }

    /**
     * Creates a new staged ParallelTopic message distributor ({@link PubSub}) using a fiber-creating {@link StrandFactory} and with the given buffer parameters.
     * <p/>
     * @param bufferSize    The buffer size of this topic.
     */
    public ParallelTopic(int bufferSize) {
        this(Channels.<Message>newChannel(bufferSize), strandFactoryDefault, stagedDefault);
    }

    @Override
    public void send(final Message message) throws SuspendExecution, InterruptedException {
        internalChannel.send(message);
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return internalChannel.send(message, timeout);
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return internalChannel.send(message, timeout, unit);
    }

    @Override
    public boolean trySend(Message message) {
        return internalChannel.trySend(message);
    }

    private void startDistributionLoop(final boolean staged) {
        strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
            // TODO check if there are more efficient alternatives
            private final ArrayList<Strand> stage = new ArrayList<>();

            @Override
            public void run() throws SuspendExecution, InterruptedException {
                for(;;) {
                    final Message m = internalChannel.receive();
                    if (isSendClosed())
                        return;
                    if (staged)
                        stage.clear();
                    for (final SendPort<? super Message> sub : ParallelTopic.this.getSubscribers()) {
                        final Strand f = strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                            @Override
                            public void run() throws SuspendExecution, InterruptedException {
                                sub.send(m);
                            }
                        })).start();
                        if (staged)
                            stage.add(f);
                    }
                    if (staged) {
                        for(final Strand s : stage) {
                            try {
                                s.join();
                            } catch (final ExecutionException ee) {
                                // This should never happen
                                throw new AssertionError(ee);
                            }
                        }
                    }
                }
            }
        })).start();
    }
}

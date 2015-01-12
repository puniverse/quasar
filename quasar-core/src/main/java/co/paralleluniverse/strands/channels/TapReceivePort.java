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
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableRunnable;
import co.paralleluniverse.strands.SuspendableUtils;

/**
 * Receive transformer that will forward messages it receive to a target {@link SendPort}. oncrete subclasses will need to implement {@code select} yielding
 * {@link SendPort} to forward the message to.
 * <p/>
 * @author circlespainter
 */
abstract class TapReceivePort<Message> extends ReceivePortTransformer<Message, Message> implements ReceivePort<Message> {
    private static final StrandFactory strandFactoryDefault = DefaultFiberFactory.instance();

    private final StrandFactory strandFactory;

    /**
     * Subclasses will implement this method to select the target 
     * 
     * @param m
     * @return 
     */
    abstract protected SendPort<? super Message> select(Message m);
    
    /**
     * Creates a new {@code TapReceiverPort} that will tap into {@code target} and forward its messages to some {@link SendPort}.
     * It will not block if immediate forwarding is not possible but will rather create a new strand from {@code strandFactory}.
     * <p/>
     * @param target        The transformed receive port.
     * @param strandFactory The {@link StrandFactory} that will create forwarding strands when immediate forwarding is not possible.
     */
    public TapReceivePort(final ReceivePort<Message> target, final StrandFactory strandFactory) {
        super(target);
        this.strandFactory = strandFactory;
    }

    /**
     * Creates a new {@code TapReceiverPort} that will tap into {@code target} and forward its messages to some {@link SendPort}.
     * It will not block if immediate forwarding is not possible but will rather create a new fiber from the {@link DefaultFiberFactory}.
     * <p/>
     * @param target        The transformed receive port.
     */
    public TapReceivePort(final ReceivePort<Message> target) {
        this(target, strandFactoryDefault);
    }

    @Override
    protected Message transform(final Message m) {
        final SendPort<? super Message> actualForwardTo = select(m);
        if (actualForwardTo != null && !actualForwardTo.trySend(m))
            strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    actualForwardTo.send(m);
                }
            })).start();
        return m;
    }
}

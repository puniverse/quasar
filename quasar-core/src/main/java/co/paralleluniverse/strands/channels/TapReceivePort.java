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
import com.google.common.base.Function;

/**
 * Receive transformer that will forward messages it receive to a target {@link SendPort}.
 * <p/>
 * @author circlespainter
 */
class TapReceivePort<Message> extends ReceivePortTransformer<Message, Message> implements ReceivePort<Message> {
    private static final StrandFactory strandFactoryDefault = DefaultFiberFactory.instance();
    
    private final Selector<Message> selector;
    private final SendPort<Message> forwardTo;
    private final StrandFactory strandFactory;

    /**
     * Selector of forward target based on the actual {@code Message} instance being forwarded.
     */
    // @Functional
    public static interface Selector<Message> extends Function<Message, SendPort<Message>> {}

    private TapReceivePort(final ReceivePort<Message> target, final Selector<Message> selector, final SendPort<Message> forwardTo, final StrandFactory strandFactory) {
        super(target);
        this.selector = selector;
        this.forwardTo = forwardTo;
        this.strandFactory = strandFactory;
    }

    /**
     * Creates a new {@code TapReceiverPort} that will tap into {@code target} and forward its messages to {@code forwardTo}.
     * It will not block if immediate forwarding is not possible but will rather create a new strand from {@code strandFactory}.
     * <p/>
     * @param target        The transformed receive port.
     * @param selector      The {@link Selector} that will choose the {@link SendPort} the message will be forwarded to.
     * @param strandFactory The {@link StrandFactory} that will create forwarding strands when immediate forwarding is not possible.
     */
    public TapReceivePort(final ReceivePort<Message> target, final Selector<Message> selector, final StrandFactory strandFactory) {
        this(target, selector, null, strandFactory);
    }

    /**
     * Creates a new {@code TapReceiverPort} that will tap into {@code target} and forward its messages to {@code forwardTo}.
     * It will not block if immediate forwarding is not possible but will rather create a new fiber from the {@link DefaultFiberFactory}.
     * <p/>
     * @param target        The transformed receive port.
     * @param selector      The {@link Selector} that will choose the {@link SendPort} the message will be forwarded to.
     */
    public TapReceivePort(final ReceivePort<Message> target, final Selector<Message> selector) {
        this(target, selector, strandFactoryDefault);
    }
    
    /**
     * Creates a new {@code TapReceiverPort} that will tap into {@code target} and forward its messages to {@code forwardTo}.
     * It will not block if immediate forwarding is not possible but will rather create a new fiber from the {@link DefaultFiberFactory}.
     * <p/>
     * @param target        The transformed receive port.
     * @param forwardTo     The additional {@link SendPort} that will receive messages.
     */
    public TapReceivePort(final ReceivePort<Message> target, final SendPort<Message> forwardTo) {
        this(target, null, forwardTo, strandFactoryDefault);
    }

    @Override
    protected Message transform(final Message m) {
        final SendPort<Message> actualForwardTo = (selector != null ? selector.apply(m) : forwardTo);
        if (actualForwardTo != null && !actualForwardTo.trySend(m))
            strandFactory.newStrand(SuspendableUtils.runnableToCallable(new SuspendableRunnable() {
                @Override
                public void run() throws SuspendExecution, InterruptedException {
                    actualForwardTo.send(m);
                }
            }));
        return m;
    }
}

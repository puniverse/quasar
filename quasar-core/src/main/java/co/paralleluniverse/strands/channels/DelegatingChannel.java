/*
 * Quasar: lightweight strands and actors for the JVM.
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

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;


/**
 * A channel delegating send operations to a {@link SendPort} and receive operations to a {@link ReceivePort}.
 * 
 * @param <Message>
 *
 * @author circlespainter
 */
public class DelegatingChannel<Message> extends DelegatingSendPort<Message> implements Channel<Message> {
    private final DelegatingReceivePort<Message> delegateReceivePort;
    private final Port<Message> equalsTarget;

    /**
     * @param equalsTarget  When delegating to potentially distinct send port and receive port, the equals behaviour must be explicitly specified.
     */
    public DelegatingChannel(final SendPort<Message> sendPort, final ReceivePort<Message> receivePort, final Port<Message> equalsTarget) {
        super(sendPort);
        this.delegateReceivePort = new DelegatingReceivePort<>(receivePort);
        this.equalsTarget = equalsTarget;
    }

    /**
     * Convenience constructor when the delegate send and receive ports belong to the same channel.
     */
    public DelegatingChannel(final Channel<Message> channel) {
        this(channel, channel, channel);
    }
    
    @Override
    public void close() {
        // TODO check the closing order and that double-closing doesn't cause trouble when the send and receive ports belong to the same channel.
        super.close();
        delegateReceivePort.close();
    }

    @Override
    public void close(final Throwable t) {
        // TODO check the closing order and that double-closing doesn't cause trouble when the send and receive ports belong to the same channel.
        super.close(t);
        delegateReceivePort.close();
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        return delegateReceivePort.receive();
    }

    @Override
    public Message receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        return delegateReceivePort.receive();
    }

    @Override
    public Message receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return delegateReceivePort.receive(timeout);
    }

    @Override
    public Message tryReceive() {
        return delegateReceivePort.tryReceive();
    }

    @Override
    public boolean isClosed() {
        return delegateReceivePort.isClosed();
    }
    
    @Override
    public boolean equals(final Object o) {
        if (equalsTarget == super.target)
            return super.equals(o);
        else
            return Channels.delegatingEquals(o, equalsTarget);
    }

    @Override
    public int hashCode() {
        if (equalsTarget == super.target)
            return super.hashCode();
        else
            return equalsTarget.hashCode();
    }
}

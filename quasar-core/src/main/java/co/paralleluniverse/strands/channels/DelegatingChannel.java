/*
 * Quasar: lightweight strands and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
public class DelegatingChannel<Message> implements Channel<Message> {
    private final SendPort<Message> delegateSendPort;
    private final ReceivePort<Message> delegateReceivePort;
    
    public DelegatingChannel(SendPort<Message> sendPort, ReceivePort<Message> receivePort) {
        this.delegateSendPort = sendPort;
        this.delegateReceivePort = receivePort;
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        delegateSendPort.send(message);
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return delegateSendPort.send(message, timeout, unit);
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return delegateSendPort.send(message, timeout);
    }

    @Override
    public boolean trySend(Message message) {
        return delegateSendPort.trySend(message);
    }

    @Override
    public void close() {
        delegateSendPort.close();
        delegateReceivePort.close();
    }

    @Override
    public void close(Throwable t) {
        delegateSendPort.close(t);
        delegateReceivePort.close();
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        return delegateReceivePort.receive();
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return delegateReceivePort.receive();
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
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
}

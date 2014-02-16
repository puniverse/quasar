/*
 * Quasar: lightweight threads and actors for the JVM.
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
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class Topic<Message> implements SendPort<Message> {
    private final Collection<SendPort<? super Message>> subscribers;
    private volatile boolean sendClosed;

    public Topic() {
        this.subscribers = new CopyOnWriteArraySet<>();
    }

    public void subscribe(SendPort<? super Message> sub) {
        subscribers.add(sub);
    }

    public void unsubscribe(SendPort<? super Message> sub) {
        subscribers.remove(sub);
    }

    @Override
    public void send(Message message) throws SuspendExecution, InterruptedException {
        if (sendClosed)
            return;
        for (SendPort<? super Message> sub : subscribers)
            sub.send(message);
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean trySend(Message message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        sendClosed = true;
    }

    @Override
    public void close(Throwable t) {
        sendClosed = true;
    }
}

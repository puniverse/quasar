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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * A channel that forwards all messages to subscriber channels.
 *
 * @author pron
 */
public class Topic<Message> implements PubSub<Message> {
    private final Collection<SendPort<? super Message>> subscribers;

    protected volatile boolean sendClosed;
    private Throwable closeException;

    public Topic() {
        this.subscribers = new CopyOnWriteArraySet<>();
    }

    /**
     * Provides read-only access to subscribers for extentions. Not meant to be altered.
     */
    protected Collection<SendPort<? super Message>> getSubscribers() {
        // Avoiding defensive copy for the sake of efficiency.
        return subscribers;
    }

    @Override
    public <T extends SendPort<? super Message>> T subscribe(T sub) {
        if (closeChannelIfClosed(sub))
            return sub;
        subscribers.add(sub);
        if (closeChannelIfClosed(sub))
            unsubscribe(sub);
        return sub;
    }

    private boolean closeChannelIfClosed(SendPort<?> port) {
        if (sendClosed) {
            if (closeException != null)
                port.close(closeException);
            else
                port.close();
            return true;
        }
        return false;
    }

    @Override
    public void unsubscribe(SendPort<? super Message> sub) {
        subscribers.remove(sub);
    }

    public void unsubscribeAll() {
        subscribers.clear();
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
        if (sendClosed == true)
            return;

        sendClosed = true;
        for (SendPort<?> sub : subscribers)
            sub.close();
        unsubscribeAll();
    }

    @Override
    public void close(Throwable t) {
        if (sendClosed == true)
            return;
        closeException = t;
        sendClosed = true;
        for (SendPort<?> sub : subscribers)
            sub.close(t);
        unsubscribeAll();
    }
}

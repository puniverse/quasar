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
 * A channel that forwards all messages to subscriber channels.
 * @author pron
 */
public class Topic<Message> implements SendPort<Message> {
    private final Collection<SendPort<? super Message>> subscribers;
    private volatile boolean sendClosed;

    public Topic() {
        this.subscribers = new CopyOnWriteArraySet<>();
    }

    /**
     * Provides read-only access to volatile `sendClosed` field for extensions.
     */
    protected boolean isSendClosed() {
        return sendClosed;
    }

    /**
     * Provides read-only access to subscribers for extentions. Not meant to be altered.
     */
    protected Collection<SendPort<? super Message>> getSubscribers() {
        // Avoiding defensive copy for the sake of efficiency.
        return subscribers;
    }
    
    /**
     * Subscribe a channel to receive messages sent to this topic.
     * <p>
     * @param sub the channel to subscribe
     */
    public <T extends SendPort<? super Message>> T subscribe(T sub) {
        if (sendClosed) {
            sub.close();
            return sub;
        }
        subscribers.add(sub);
        if (sendClosed)
            sub.close();
        return sub;
    }

    /**
     * Unsubscribe a channel from this topic.
     * <p>
     * @param sub the channel to subscribe
     */
    public void unsubscribe(SendPort<? super Message> sub) {
        subscribers.remove(sub);
    }

    /**
     * Unsubscribe all channels from this topic.
     */
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
        sendClosed = true;
        for (SendPort<?> sub : subscribers)
            sub.close();
    }

    @Override
    public void close(Throwable t) {
        sendClosed = true;
        for (SendPort<?> sub : subscribers)
            sub.close(t);
    }
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

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
    public void send(Message message) throws SuspendExecution {
        if(sendClosed)
            return;
        for (SendPort<? super Message> sub : subscribers)
            sub.send(message);
    }

    @Override
    public void close() {
        sendClosed = true;
    }
}

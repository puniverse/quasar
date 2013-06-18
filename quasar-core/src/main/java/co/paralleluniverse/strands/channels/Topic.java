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
public class Topic<Message> implements SendChannel<Message> {
    private final Collection<SendChannel<? super Message>> subscribers;

    public Topic() {
        this.subscribers = new CopyOnWriteArraySet<>();
    }

    public void subscribe(SendChannel<? super Message> sub) {
        subscribers.add(sub);
    }

    public void unsubscribe(SendChannel<? super Message> sub) {
        subscribers.remove(sub);
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        for (SendChannel<? super Message> sub : subscribers)
            sub.send(message);
    }
}

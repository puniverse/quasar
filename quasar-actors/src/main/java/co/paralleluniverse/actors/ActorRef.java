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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class ActorRef<Message> implements SendPort<Message> {
    public abstract String getName();

    @Override
    public abstract void send(Message message) throws SuspendExecution;

    public abstract void sendSync(Message message) throws SuspendExecution;

    @Override
    public abstract boolean send(Message msg, long l, TimeUnit tu) throws SuspendExecution, InterruptedException;

    @Override
    public abstract boolean trySend(Message msg);

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    public abstract void interrupt();

    public static <T extends ActorRef<M>, M> T self() {
        final Actor a = Actor.currentActor();
        if (a == null)
            return null;
        return (T) a.ref();
    }
}

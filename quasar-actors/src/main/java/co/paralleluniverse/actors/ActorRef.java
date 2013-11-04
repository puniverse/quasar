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
    public String toString() {
        return "ActorRef{" + '}';
    }

    @Override
    public abstract void send(Message message) throws SuspendExecution;

    public abstract void sendSync(Message message) throws SuspendExecution;

    @Override
    public abstract boolean send(Message msg, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    @Override
    public abstract boolean trySend(Message msg);

    /**
     * {@inheritDoc}
     * 
     * This implementation just throws {@code UnsupportedOperationException}.
     */
    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    /**
     * Interrupts the actor's strand
     */
    public abstract void interrupt();

    /**
     * Returns the {@code ActorRef} of the actor currently running in the current strand.
     * @param <T>
     * @param <M>
     * @return 
     */
    public static <T extends ActorRef<M>, M> T self() {
        final Actor a = Actor.currentActor();
        if (a == null)
            return null;
        return (T) a.ref();
    }
}

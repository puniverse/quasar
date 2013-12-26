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
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An {@link ActorRef} which delegates all operations to another {@code ActorRef}.
 *
 * @author pron
 */
public abstract class ActorRefDelegate<Message> implements ActorRef<Message>, SendPort<Message>, java.io.Serializable {
    protected final ActorRef<Message> ref;

    /**
     * Constructs an {@code ActorRefDelegate}
     *
     * @param ref the {@link ActorRef} to which all operations will be delegated
     */
    public ActorRefDelegate(ActorRef<Message> ref) {
        this.ref = ref;
    }

    protected boolean isInActor() {
        return Objects.equals(ref, LocalActor.self());
    }

    @Override
    public String getName() {
        return ref.getName();
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        ref.send(message);
    }

    @Override
    public void sendSync(Message message) throws SuspendExecution {
        ref.sendSync(message);
    }

    @Override
    public void interrupt() {
        ref.interrupt();
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return ((SendPort<Message>) ref).send(message, timeout, unit);
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return ((SendPort<Message>) ref).send(message, timeout);
    }

    @Override
    public boolean trySend(Message message) {
        return ((SendPort<Message>) ref).trySend(message);
    }

    public void sendOrInterrupt(Object message) {
        if (ref instanceof ActorRefDelegate)
            ((ActorRefDelegate) ref).sendOrInterrupt(message);
        else
            ((ActorRefImpl) ref).sendOrInterrupt(message);
    }

    @Override
    public void close() {
        ((SendPort<Message>) ref).close();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ref);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof ActorRef))
            return false;
        ActorRef other = (ActorRef) obj;
        while (other instanceof ActorRefDelegate)
            other = ((ActorRefDelegate) other).ref;
        ActorRef me = ref;
        while (me instanceof ActorRefDelegate)
            me = ((ActorRefDelegate) me).ref;
        if (!Objects.equals(me, other))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ref.toString();
    }
}

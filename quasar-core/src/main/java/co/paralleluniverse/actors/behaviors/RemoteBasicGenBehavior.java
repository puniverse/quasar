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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorWrapper;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.Objects;

/**
 *
 * @author pron
 */
public class RemoteBasicGenBehavior implements ActorWrapper<Object>, GenBehavior, java.io.Serializable {
    protected final RemoteActor<Object> actor;

    public RemoteBasicGenBehavior(RemoteActor<Object> actor) {
        this.actor = actor;
    }

    @Override
    public ActorImpl<Object> getActor() {
        return actor;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.actor);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof RemoteBasicGenBehavior)
            return false;
        final RemoteBasicGenBehavior other = (RemoteBasicGenBehavior) obj;
        if (!Objects.equals(this.actor, other.actor))
            return false;
        return true;
    }

    @Override
    public void shutdown() {
        final ShutdownMessage message = new ShutdownMessage(LocalActor.self());
        actor.sendOrInterrupt(message);
    }

    @Override
    public Object getName() {
        return actor.getName();
    }

    @Override
    public void send(Object message) throws SuspendExecution {
        actor.send(message);
    }

    @Override
    public void sendSync(Object message) throws SuspendExecution {
        actor.sendSync(message);
    }

    @Override
    public void interrupt() {
        actor.interrupt();
    }

    public void close() {
        throw new UnsupportedOperationException();
    }
}

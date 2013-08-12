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
import java.util.Objects;

/**
 *
 * @author pron
 */
public class ActorRefDelegate<Message> implements ActorRef<Message> {
    protected final ActorRef<Message> ref;

    public ActorRefDelegate(ActorRef<Message> ref) {
        this.ref = ref;
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
    public int hashCode() {
        return Objects.hashCode(ref);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if(!(obj instanceof ActorRef))
            return false;
        ActorRef other = (ActorRef)obj;
        if (other instanceof ActorRefDelegate)
            other = ((ActorRefDelegate)other).ref;
        if (!Objects.equals(this.ref, other))
            return false;
        return true;
    }
}

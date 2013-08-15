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

import co.paralleluniverse.actors.*;
import java.util.Objects;

/**
 *
 * @author pron
 */
public class ShutdownMessage implements LifecycleMessage {
    public final ActorRef requester; // http://english.stackexchange.com/questions/29254/whats-the-difference-between-requester-and-requestor


    public ShutdownMessage(ActorRef requestor) {
        this.requester = requestor;
    }

    public ActorRef getRequester() {
        return requester;
    }

    @Override
    public String toString() {
        return "ShutdownMessage{" + "requester=" + requester + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.requester);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ExitMessage other = (ExitMessage) obj;
        if (!Objects.equals(this.requester, other.actor))
            return false;
        return true;
    }
}

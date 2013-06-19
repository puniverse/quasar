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

import java.util.Objects;

/**
 *
 * @author pron
 */
public class ExitMessage implements LifecycleMessage {
    public final Actor actor;
    public final Throwable cause;
    public final Object watch;

    public ExitMessage(Actor actor, Throwable cause) {
        this(actor, cause, null);
    }

    public ExitMessage(Actor actor, Throwable cause, Object watch) {
        this.actor = actor;
        this.cause = cause;
        this.watch = watch;
    }

    public Actor getActor() {
        return actor;
    }

    public Throwable getCause() {
        return cause;
    }

    public Object getWatch() {
        return watch;
    }

    @Override
    public String toString() {
        return "ExitMessage{" + "actor=" + actor + ", reason=" + cause + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.actor);
        hash = 59 * hash + Objects.hashCode(this.cause);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ExitMessage other = (ExitMessage) obj;
        if (!Objects.equals(this.actor, other.actor))
            return false;
        if (!Objects.equals(this.cause, other.cause))
            return false;
        return true;
    }
}

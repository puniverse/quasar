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
package co.paralleluniverse.actors;

import java.util.Objects;

/**
 * A {@link LifecycleMessage} signifying an actor's death. This message is automatically sent by a dying actor to its {@link Actor#watch(ActorRef) watchers} or
 * {@link Actor#link(ActorRef) linked actors}.
 *
 * @author pron
 */
public class ExitMessage implements LifecycleMessage {
    public final ActorRef actor;
    public final Throwable cause;
    public final Object watch;

    /**
     *
     * @param actor the dying actor
     * @param cause the exception that caused the actor's death, or {@code null} if the actor terminated normally.
     */
    public ExitMessage(ActorRef actor, Throwable cause) {
        this(actor, cause, null);
    }

    public ExitMessage(ActorRef actor, Throwable cause, Object watch) {
        this.actor = actor;
        this.cause = cause;
        this.watch = watch;
    }

    /**
     * Returns the actor that originated this message (the dying actor).
     * @return the actor that originated this message (the dying actor).
     */
    public ActorRef getActor() {
        return actor;
    }

    /**
     * Returns the actor's cause of death exception, or {@code null} if the actor terminated normally.
     * @return the actor's cause of death exception, or {@code null} if the actor terminated normally.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Returns the watch object that is the reason for this message being sent to the receiving actor, returned by the {@link Actor#watch(ActorRef) watch} method,
     * or {@code null} if the message is sent as a result of a {@link Actor#link(ActorRef) link}.
     * @return the watch object that is the reason for this message being sent or {@code null}
     */
    public Object getWatch() {
        return watch;
    }

    @Override
    public String toString() {
        return "ExitMessage{" + "actor: " + actor + ", cause: " + cause + '}';
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

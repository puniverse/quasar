/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import java.util.Objects;

/**
 *
 * @author pron
 */
public class ExitMessage implements LifecycleMessage {
    public final AbstractActor actor;
    public final Object reason;
    public final Object monitor;

    public ExitMessage(AbstractActor actor, Object reason) {
        this(actor, reason, null);
    }

    public ExitMessage(AbstractActor actor, Object reason, Object monitor) {
        this.actor = actor;
        this.reason = reason;
        this.monitor = monitor;
    }

    public AbstractActor getActor() {
        return actor;
    }

    public Object getReason() {
        return reason;
    }

    public Object getMonitor() {
        return monitor;
    }

    @Override
    public String toString() {
        return "ExitMessage{" + "actor=" + actor + ", reason=" + reason + '}';
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.actor);
        hash = 59 * hash + Objects.hashCode(this.reason);
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
        if (!Objects.equals(this.reason, other.reason))
            return false;
        return true;
    }
}

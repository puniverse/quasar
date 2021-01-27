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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;

/**
 * An interface to an {@link EventSourceActor}.
 *
 * @author pron
 */
public class EventSource<Event> extends Behavior {
    /**
     * If {@code actor} is known to be a {@link EventSourceActor}, creates a new {@link EventSource} interface to it.
     * Normally, you don't use this constructor, but the {@code EventSource} instance returned by {@link EventSourceActor#spawn() }.
     *
     * @param actor an {@link EventSourceActor}
     */
    EventSource(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Adds an {@link EventHandler} that will be notified of every event sent to this actor.
     *
     * @param handler the handler
     * @return {@code true} if the handler has been successfully added to the actor, or {@code false} if the handler was already registered.
     */
    public boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().addHandler(handler);

        return (Boolean) call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, true));
    }

    /**
     * Removes an {@link EventHandler} from the actor
     *
     * @param handler
     * @return {@code true} if the handler was registered and successfully removed, or {@code false} if the handler was not registered.
     */
    public boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().removeHandler(handler);

        return (Boolean) call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, false));
    }

    /**
     * Sends an event to the actor, which will be delivered to all registered event handlers.
     *
     * @param event the event
     */
    public void notify(Event event) throws SuspendExecution {
        send(event);
    }

    @Override
    public String toString() {
        return "EventSource{" + super.toString() + "}";
    }
}

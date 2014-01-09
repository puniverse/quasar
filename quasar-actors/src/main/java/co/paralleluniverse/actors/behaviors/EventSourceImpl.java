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
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.SuspendExecution;

/**
 * An interface to an {@link EventSourceActor}.
 *
 * @author pron
 */
class EventSourceImpl<Event> extends BehaviorImpl implements EventSource<Event> {
    /**
     * If {@code actor} is known to be a {@link EventSourceActor}, creates a new {@link EventSource} interface to it.
     * Normally, you don't use this constructor, but the {@code EventSource} instance returned by {@link EventSourceActor#spawn() }.
     *
     * @param actor an {@link EventSourceActor}
     */
    EventSourceImpl(ActorRef<Object> actor) {
        super(actor);
    }

    @Override
    public boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().addHandler(handler);

        return (Boolean) call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, true));
    }

    @Override
    public boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().removeHandler(handler);

        return (Boolean) call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, false));
    }

    @Override
    public void notify(Event event) throws SuspendExecution {
        send(event);
    }

    @Override
    public String toString() {
        return "EventSource{" + super.toString() + "}";
    }

    static final class Local<Event> extends EventSourceImpl<Event> implements LocalBehavior<EventSourceImpl<Event>> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public EventSourceImpl<Event> writeReplace() throws java.io.ObjectStreamException {
            return new EventSourceImpl<>(getRef());
        }
    }
}

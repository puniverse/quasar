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

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ActorRef;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class EventSource<Event> extends Behavior {
    EventSource(ActorRef<Object> actor) {
        super(actor);
    }

    public boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if(isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().addHandler(handler);
        
        return (Boolean)call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, true));
    }

    public boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if(isInActor())
            return EventSourceActor.<Event>currentEventSourceActor().removeHandler(handler);
        
        return (Boolean)call(this, new EventSourceActor.HandlerMessage(RequestReplyHelper.from(), null, handler, false));
    }

    public void notify(Event event) throws SuspendExecution {
        send(event);
    }

    static final class Local<Event> extends EventSource<Event> implements LocalBehavior<EventSource<Event>> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public EventSource<Event> writeReplace() throws java.io.ObjectStreamException {
            return new EventSource<>(ref);
        }

        @Override
        public Actor<Object, Void> build() {
            return ((ActorBuilder<Object, Void>) ref).build();
        }

        @Override
        public void join() throws ExecutionException, InterruptedException {
            ((Joinable<Void>) ref).join();
        }

        @Override
        public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            ((Joinable<Void>) ref).join(timeout, unit);
        }

        @Override
        public Void get() throws ExecutionException, InterruptedException {
            return ((Joinable<Void>) ref).get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            return ((Joinable<Void>) ref).get(timeout, unit);
        }

        @Override
        public boolean isDone() {
            return ((Joinable<Void>) ref).isDone();
        }
    }
}

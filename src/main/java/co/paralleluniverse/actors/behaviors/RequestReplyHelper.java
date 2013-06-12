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
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.common.util.Debug;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class RequestReplyHelper {
    public static Object makeId() {
        return ActorImpl.randtag();
    }

    public static <Message> Actor<Message> from() {
        return getCurrentActor(); // new TempActor<Message>(getCurrentActor());
    }

    public static GenResponseMessage call(final Actor actor, GenRequestMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {

        final LocalActor currentActor;
        if (m.getFrom() instanceof TempActor)
            currentActor = (LocalActor) ((TempActor) m.getFrom()).actor.get();
        else
            currentActor = LocalActor.self();

        assert currentActor != null;

        final Object watch = currentActor.watch(actor);
        
        if(m.getId() == null)
            m.setId(watch);
        
        final Object id = m.getId();

        final SelectiveReceiveHelper<Object> helper = new SelectiveReceiveHelper<Object>(currentActor) {
            @Override
            protected void handleLifecycleMessage(LifecycleMessage m) {
                if (m instanceof ExitMessage) {
                    final ExitMessage exit = (ExitMessage) m;
                    if (Objects.equals(exit.getActor(), actor) && exit.getWatch() == watch)
                        throw Exceptions.rethrow(exit.getCause());
                }
                super.handleLifecycleMessage(m);
            }
        };
        try {
            actor.sendSync(m);
            final GenResponseMessage response = (GenResponseMessage) helper.receive(timeout, unit, new MessageProcessor<Object>() {
                @Override
                public boolean process(Object m) throws SuspendExecution, InterruptedException {
                    return (m instanceof GenResponseMessage && id.equals(((GenResponseMessage) m).getId()));
                }
            });
            currentActor.unwatch(actor, watch); // no need to unwatch in case of receiver death, so not doen in finally block
            
            if (response instanceof GenErrorResponseMessage)
                throw Exceptions.rethrow(((GenErrorResponseMessage) response).getError());
            return response;
        } finally {
            if (m.getFrom() instanceof TempActor)
                ((TempActor) m.getFrom()).done();
        }
    }

    public static GenResponseMessage call(Actor actor, GenRequestMessage m) throws InterruptedException, SuspendExecution {
        try {
            return call(actor, m, 0, null);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    public static <V> void reply(GenRequestMessage req, V result) throws InterruptedException, SuspendExecution {
        req.getFrom().send(new GenValueResponseMessage<V>(req.getId(), result));
    }

    private static Actor getCurrentActor() {
        Actor actor = LocalActor.self();
        if (actor == null) {
            // create a "dummy actor" on the current strand
            actor = new LocalActor(Strand.currentStrand(), null, 5) {
                @Override
                protected Object doRun() throws InterruptedException, SuspendExecution {
                    throw new AssertionError();
                }
            };
            actor = new TempActor(actor);
        }
        return actor;
    }

    private static class TempActor<Message> implements Actor<Message> {
        private WeakReference<Actor<Message>> actor;
        private volatile boolean done = false;

        public TempActor(Actor actor) {
            this.actor = new WeakReference<Actor<Message>>(actor);
        }

        public void done() {
            this.actor = null;
            this.done = true;
        }

        private Actor getActor() {
            Actor a = null;
            if (actor != null)
                a = actor.get();
            return a;
        }

        private Actor actor() {
            Actor a = getActor();
            if (a == null)
                throw new RuntimeException("Temporary actor is out of scope");
            return a;
        }

        @Override
        public Object getName() {
            return actor().getName();
        }

        @Override
        public boolean isDone() {
            return done || actor().isDone();
        }

        @Override
        public void send(Message message) {
            final Actor a = getActor();
            if (a != null)
                a.send(message);
        }

        @Override
        public void sendSync(Message message) {
            final Actor a = getActor();
            if (a != null)
                a.sendSync(message);
        }

        @Override
        public Actor link(Actor other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Actor unlink(Actor other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object watch(Actor other) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unwatch(Actor other, Object listener) {
            throw new UnsupportedOperationException();
        }
    }
}

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
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
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
        return ActorUtil.randtag();
    }

    public static <Message> ActorRef<Message> from() {
        return getCurrentActor(); // new TempActor<Message>(getCurrentActor());
    }

    public static GenResponseMessage call(final ActorRef actor, GenRequestMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        final Actor currentActor;
        if (m.getFrom() instanceof TempActor)
            currentActor = ((TempActor<?>) m.getFrom()).actor.get();
        else
            currentActor = Actor.currentActor();

        assert currentActor != null;

        final Object watch = currentActor.watch(actor);

        if (m.getId() == null)
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

    public static GenResponseMessage call(ActorRef actor, GenRequestMessage m) throws InterruptedException, SuspendExecution {
        try {
            return call(actor, m, 0, null);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    public static <V> void reply(GenRequestMessage req, V result) throws SuspendExecution {
        req.getFrom().send(new GenValueResponseMessage<V>(req.getId(), result));
    }

    public static <V> void replyError(GenRequestMessage req, Throwable e) throws SuspendExecution {
        req.getFrom().send(new GenErrorResponseMessage(req.getId(), e));
    }

    private static ActorRef getCurrentActor() {
        ActorRef actorRef = Actor.self();
        if (actorRef == null) {
            // create a "dummy actor" on the current strand
            Actor actor = new Actor(Strand.currentStrand(), null, new MailboxConfig(5, OverflowPolicy.THROW)) {
                @Override
                protected Object doRun() throws InterruptedException, SuspendExecution {
                    throw new AssertionError();
                }
            };
            actorRef = new TempActor(actor);
        }
        return actorRef;
    }

    private static class TempActor<Message> implements ActorRef<Message> {
        private WeakReference<Actor<Message, Void>> actor;
        private volatile boolean done = false;

        public TempActor(Actor actor) {
            this.actor = new WeakReference<Actor<Message, Void>>(actor);
        }

        public void done() {
            this.actor = null;
            this.done = true;
        }

        private ActorRef getActor() {
            ActorRef a = null;
            if (actor != null)
                a = actor.get().ref();
            return a;
        }

        private ActorRef actor() {
            final ActorRef a = getActor();
            if (a == null)
                throw new RuntimeException("Temporary actor is out of scope");
            return a;
        }

        @Override
        public String getName() {
            return actor().getName();
        }

        @Override
        public void interrupt() {
            final ActorRef a = getActor();
            if (a != null)
                a.interrupt();
        }
        
        @Override
        public void send(Message message) throws SuspendExecution {
            final ActorRef a = getActor();
            if (a != null)
                a.send(message);
        }

        @Override
        public void sendSync(Message message) throws SuspendExecution {
            final ActorRef a = getActor();
            if (a != null)
                a.sendSync(message);
        }
    }
}

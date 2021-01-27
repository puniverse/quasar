/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class contains static methods that implement a request-reply pattern with actors. These methods can be used to communicate with
 * actors by other actors, or even by non-actor strands.
 *
 * @author pron
 */
public final class RequestReplyHelper {
    private static final ThreadLocal<Long> defaultTimeout = new ThreadLocal<Long>();

    /**
     * Generates a random, probably unique, message identifier. This method simply calls {@link ActorUtil#randtag() }.
     *
     * @return a newly allocated, probably unique, message identifier.
     */
    public static Object makeId() {
        return ActorUtil.randtag();
    }

    /**
     * Sets a default timeout for non-timed {@link #call(ActorRef, RequestMessage) call}s on this strand.
     * Non-timed calls that take longer than the default timeout, will throw a {@link TimeoutException}
     * wrapped in a {@link RuntimeException}. Timed calls (those that take a timeout parameter) will not be affected.
     * <p>
     * This method only affects the current strand.</p>
     *
     * @param timeout the timeout duration
     * @param unit    the time unit of the timeout, or {@code null} to unset.
     */
    public static void setDefaultTimeout(long timeout, TimeUnit unit) {
        if (unit == null)
            defaultTimeout.remove();
        else
            defaultTimeout.set(unit.toNanos(timeout));
    }

    /**
     * Returns an {@link ActorRef} that should be used as the <i>from</i> property of a {@link RequestMessage}. If called
     * from an actor strand, this method returns the current actor. If not, it creates a temporary faux-actor that will be used internally
     * to receive the response, even if the current strand is not running an actor.
     *
     * @param <Message>
     * @return an {@link ActorRef} that should be used as the <i>from</i> property of a request, even if not called from within an actor.
     */
    public static <Message> ActorRef<Message> from() {
        return getCurrentActor();
    }

    /**
     * Sends a request message to an actor, awaits a response value and returns it.
     * This method can be called by any code, even non-actor code.
     * If the actor responds with an error message, a {@link RuntimeException} will be thrown by this method.
     * <br>
     * The message's {@code id} and {@code from} properties may be left unset.
     * <p>
     * This method should be used as in the following example (assuming a {@code String} return value:</p>
     * <pre> {@code
     * String res = call(actor, new MyRequest());
     * }</pre>
     * In the example, {@code MyRequest} extends {@link RequestMessage}. Note how the result of the {@link #from() from} method is passed to the
     * request's constructor, but the message ID isn't.
     *
     * @param <V>   the return value's type
     * @param actor the actor to which the request is sent
     * @param m     the {@link RequestMessage}, whose {@code id} and {@code from} properties may be left unset.
     * @return the value sent by the actor as a response
     * @throws RuntimeException     if the actor responds with an error message, its contained exception will be thrown, possibly wrapped by a {@link RuntimeException},
     *                              or if a {@link #setDefaultTimeout(long, TimeUnit) default timeout} has been set and has expired.
     * @throws InterruptedException
     */
    public static <V, M extends RequestMessage<V>> V call(ActorRef<? super M> actor, M m) throws InterruptedException, SuspendExecution {
        Long timeout = null;
        try {
            timeout = defaultTimeout.get();
            if (timeout != null)
                return call(actor, m, timeout, TimeUnit.NANOSECONDS);
            else
                return call(actor, m, 0, null);
        } catch (TimeoutException ex) {
            if (timeout != null)
                throw new RuntimeException(ex);
            else
                throw new AssertionError(ex);
        }
    }

    /**
     * Sends a request message to an actor, awaits a response value (but no longer than the given timeout) and returns it.
     * This method can be called by any code, even non-actor code.
     * If the actor responds with an error message, a {@link RuntimeException} will be thrown by this method.
     * <br>
     * The message's {@code id} and {@code from} properties may be left unset.
     * <p>
     * This method should be used as in the following example (assuming a {@code String} return value:</p>
     * <pre> {@code
     * String res = call(actor, new MyRequest());
     * }</pre>
     * In the example, {@code MyRequest} extends {@link RequestMessage}. Note how the result of the {@link #from() from} method is passed to the
     * request's constructor, but the message ID isn't.
     *
     * @param <V>     the return value's type
     * @param actor   the actor to which the request is sent
     * @param timeout the maximum duration to wait for a response
     * @param unit    the time unit of the timeout
     * @return the value sent by the actor as a response
     * @throws RuntimeException     if the actor responds with an error message, its contained exception will be thrown, possibly wrapped by a {@link RuntimeException}.
     * @throws TimeoutException     if the timeout expires before a response is received from the actor.
     * @throws InterruptedException
     */
    public static <V> V call(final ActorRef actor, RequestMessage<V> m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        assert !actor.equals(LocalActor.self()) : "Can't \"call\" self - deadlock guaranteed";

        if (m.getFrom() == null || LocalActor.isInstance(m.getFrom(), TempActor.class))
            m.setFrom(from());

        final boolean tmpActor = m.getFrom() instanceof TempActorRef;
        final Actor currentActor = tmpActor ? (Actor) ((TempActorRef) m.getFrom()).getImpl() : Actor.currentActor();
        assert currentActor != null;

        currentActor.link(actor);

        if (m.getId() == null)
            m.setId(new Object());

        final Object id = m.getId();

        final SelectiveReceiveHelper<Object> helper = new SelectiveReceiveHelper<Object>(currentActor) {
            @Override
            protected void handleLifecycleMessage(LifecycleMessage m) {
                if (m instanceof ExitMessage) {
                    final ExitMessage exit = (ExitMessage) m;
                    if (Objects.equals(exit.getActor(), actor) && exit.getWatch() == null)
                        throw Exceptions.rethrow(exit.getCause());
                }
                super.handleLifecycleMessage(m);
            }
        };
        try {
            actor.sendSync(m);
            final ResponseMessage response = (ResponseMessage) helper.receive(timeout, unit, new MessageProcessor<Object, Object>() {
                @Override
                public Object process(Object m) throws SuspendExecution, InterruptedException {
                    return (m instanceof ResponseMessage && id.equals(((ResponseMessage) m).getId())) ? m : null;
                }
            });
            currentActor.unlink(actor); // no need to unlink in case of receiver death, so not done in finally block

            if (response instanceof ErrorResponseMessage)
                throw Exceptions.rethrow(((ErrorResponseMessage) response).getError());
            return ((ValueResponseMessage<V>) response).getValue();
        } catch (InterruptedException e) {
            if (tmpActor)
                currentActor.checkThrownIn();
            throw e;
        } finally {
//            if (tmpActor)
//                ((TempActor) m.getFrom()).done();
        }
    }

    /**
     * Sends a request message to an actor, awaits a response value (but no longer than the given timeout) and returns it.
     * This method can be called by any code, even non-actor code.
     * If the actor responds with an error message, a {@link RuntimeException} will be thrown by this method.
     * <br>
     * The message's {@code id} and {@code from} properties may be left unset.
     * <p>
     * This method should be used as in the following example (assuming a {@code String} return value:</p>
     * <pre> {@code
     * String res = call(actor, new MyRequest());
     * }</pre>
     * In the example, {@code MyRequest} extends {@link RequestMessage}. Note how the result of the {@link #from() from} method is passed to the
     * request's constructor, but the message ID isn't.
     *
     * @param <V>     the return value's type
     * @param actor   the actor to which the request is sent
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return the value sent by the actor as a response
     * @throws RuntimeException     if the actor responds with an error message, its contained exception will be thrown, possibly wrapped by a {@link RuntimeException}.
     * @throws TimeoutException     if the timeout expires before a response is received from the actor.
     * @throws InterruptedException
     */
    public static <V> V call(final ActorRef actor, RequestMessage<V> m, Timeout timeout) throws TimeoutException, InterruptedException, SuspendExecution {
        return call(actor, m, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Replies with a result to a {@link RequestMessage}.
     * If the request has been sent by a call to {@link #call(ActorRef, RequestMessage) call}, the
     * {@code result} argument will be the value returned by {@link #call(ActorRef, RequestMessage) call}.
     * This method should only be called by an actor.
     * <p>
     * Internally this method uses a {@link ValueResponseMessage} to send the reply.</p>
     *
     * @param req    the request we're responding to
     * @param result the result of the request
     */
    public static <V> void reply(RequestMessage<V> req, V result) throws SuspendExecution {
        req.getFrom().send(new ValueResponseMessage<V>(req.getId(), result));
    }

    /**
     * Replies with an exception to a {@link RequestMessage}.
     * If the request has been sent by a call to {@link #call(ActorRef, RequestMessage) call}, the
     * {@code e} argument will be the exception thrown by {@link #call(ActorRef, RequestMessage) call} (possibly wrapped by a {@link RuntimeException}).
     * This method should only be called by an actor.
     * <p>
     * Internally this method uses an {@link ErrorResponseMessage} to send the reply.</p>
     *
     * @param req the request we're responding to
     * @param e   the error the request has caused
     */
    public static void replyError(RequestMessage<?> req, Throwable e) throws SuspendExecution {
        req.getFrom().send(new ErrorResponseMessage(req.getId(), e));
    }

    private static ActorRef getCurrentActor() {
        ActorRef actorRef = LocalActor.self();
        if (actorRef == null) {
            Actor actor = new TempActor(); // create a "dummy actor" on the current strand
            actorRef = actor.ref();
        }
        return actorRef;
    }

    private static class TempActor extends Actor<Object, Void> {
        TempActor() {
            super(Strand.currentStrand(), null, new MailboxConfig(5, OverflowPolicy.THROW));
        }

        @Override
        protected Void doRun() throws InterruptedException, SuspendExecution {
            throw new AssertionError();
        }

        @Override
        protected ActorRef<Object> makeRef(ActorRef<Object> ref) {
            return new TempActorRef(ref);
        }
    }

    private static class TempActorRef extends ActorRefDelegate<Object> {
        public TempActorRef(ActorRef<Object> ref) {
            super(ref);
        }

        @Override
        protected ActorImpl<Object> getImpl() {
            return super.getImpl();
        }
    }

//    private static class TempActor<Message> extends ActorRef<Message> {
//        private WeakReference<Actor<Message, Void>> actor;
//        private volatile boolean done = false;
//
//        public TempActor(Actor actor) {
//            this.actor = new WeakReference<Actor<Message, Void>>(actor);
//        }
//
//        public void done() {
//            this.actor = null;
//            this.done = true;
//        }
//
//        @Override
//        protected ActorImpl<Message> getImpl() {
//            Actor a = null;
//            if (actor != null)
//                a = actor.get();
//            if (a == null)
//                throw new RuntimeException("Temporary actor is out of scope");
//            return a;
//        }
//    }
    private RequestReplyHelper() {
    }
}

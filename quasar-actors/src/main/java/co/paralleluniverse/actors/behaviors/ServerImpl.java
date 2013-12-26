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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.LocalActor;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.from;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.makeId;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An interface to a {@link ServerActor}.
 *
 * @author pron
 */
class ServerImpl<CallMessage, V, CastMessage> extends BehaviorImpl implements Server<CallMessage, V, CastMessage> {
    /**
     * If {@code actor} is known to be a {@link ServerActor}, creates a new {@link Server} interface to it.
     * Normally, you don't use this constructor, but the {@code Server} instance returned by {@link ServerActor#spawn() }.
     *
     * @param actor a {@link ServerActor}
     */
    public ServerImpl(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Sends a synchronous request to the actor, and awaits a response.
     * This method will wait indefinitely for the actor to respond unless a default timeout has been set for the calling 
     * strand with {@link RequestReplyHelper#setDefaultTimeout(long, TimeUnit) RequestReplyHelper.setDefaultTimeout}.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m the request
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     */
    @Override
    public final V call(CallMessage m) throws InterruptedException, SuspendExecution {
        try {
            return call(m, 0, null);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Sends a synchronous request to the actor, and awaits a response, but no longer than the given timeout.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m       the request
     * @param timeout the maximum duration to wait for a response.
     * @param unit    the time unit of the timeout
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     * @throws TimeoutException if the timeout expires before a response has been received.
     */
    @Override
    public final V call(CallMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        final V res = RequestReplyHelper.call(ref, new ServerRequest(from(), null, MessageType.CALL, m), timeout, unit);
        return res;
    }

    /**
     * Sends a synchronous request to the actor, and awaits a response, but no longer than the given timeout.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m       the request
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     * @throws TimeoutException if the timeout expires before a response has been received.
     */
    @Override
    public final V call(CallMessage m, Timeout timeout) throws TimeoutException, InterruptedException, SuspendExecution {
        return call(m, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    /**
     * Sends an asynchronous request to the actor and returns immediately (may block until there's room available in the actor's mailbox).
     *
     * @param m the request
     */
    @Override
    public final void cast(CastMessage m) throws SuspendExecution {
        ref.send(new ServerRequest(LocalActor.self(), makeId(), MessageType.CAST, m));
    }

//    public static void cast(ActorRef server, Object m) throws SuspendExecution {
//        server.send(new ServerRequest(ActorRef.self(), makeId(), MessageType.CAST, m));
//    }
    @Override
    public String toString() {
        return "Server{" + super.toString() + "}";
    }

    enum MessageType {
        CALL, CAST
    };

    static class ServerRequest extends RequestMessage {
        private final MessageType type;
        private final Object message;

        public ServerRequest(ActorRef sender, Object id, MessageType type, Object message) {
            super(sender, id);
            this.type = type;
            this.message = message;
        }

        public MessageType getType() {
            return type;
        }

        public Object getMessage() {
            return message;
        }
    }

    static final class Local<CallMessage, V, CastMessage> extends ServerImpl<CallMessage, V, CastMessage> implements LocalBehavior<ServerImpl<CallMessage, V, CastMessage>> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public ServerImpl<CallMessage, V, CastMessage> writeReplace() throws java.io.ObjectStreamException {
            return new ServerImpl<>(ref);
        }
    }
}

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
    private volatile long defaultTimeoutNanos = -1;

    /**
     * If {@code actor} is known to be a {@link ServerActor}, creates a new {@link Server} interface to it.
     * Normally, you don't use this constructor, but the {@code Server} instance returned by {@link ServerActor#spawn() }.
     *
     * @param actor a {@link ServerActor}
     */
    public ServerImpl(ActorRef<Object> actor) {
        super(actor);
    }

    @Override
    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        if (unit == null)
            defaultTimeoutNanos = -1;
        else
            defaultTimeoutNanos = unit.toNanos(timeout);
    }

    @Override
    public final V call(CallMessage m) throws InterruptedException, SuspendExecution {
        final long timeout = defaultTimeoutNanos;
        try {
            if (timeout > 0)
                return call(m, timeout, TimeUnit.NANOSECONDS);
            else
                return call(m, 0, null);
        } catch (TimeoutException ex) {
            if (timeout >= 0)
                throw new RuntimeException(ex);
            else
                throw new AssertionError(ex);
        }
    }

    @Override
    public final V call(CallMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        final V res = RequestReplyHelper.call(getRef(), new ServerRequest<V>(from(), null, MessageType.CALL, m), timeout, unit);
        return res;
    }

    @Override
    public final V call(CallMessage m, Timeout timeout) throws TimeoutException, InterruptedException, SuspendExecution {
        return call(m, timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public final void cast(CastMessage m) throws SuspendExecution {
        getRef().send(new ServerRequest(LocalActor.self(), makeId(), MessageType.CAST, m));
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

    static class ServerRequest<T> extends RequestMessage<T> {
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
            return new ServerImpl<>(getRef());
        }
    }
}

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
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.from;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.makeId;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class GenServer<CallMessage, V, CastMessage> extends GenBehavior {
    public GenServer(ActorRef<Object> actor) {
        super(actor);
    }

    public final V call(CallMessage m) throws InterruptedException, SuspendExecution {
        try {
            return call(m, 0, null);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    public final V call(CallMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        final GenResponseMessage response = RequestReplyHelper.call(ref, new GenServerRequest(from(), null, MessageType.CALL, m), timeout, unit);
        final V res = ((GenValueResponseMessage<V>) response).getValue();
        return res;
    }

    public final void cast(CastMessage m) throws SuspendExecution {
        ref.send(new GenServerRequest(ActorRef.self(), makeId(), MessageType.CAST, m));
    }

    public static void cast(ActorRef server, Object m) throws SuspendExecution {
        server.send(new GenServerRequest(ActorRef.self(), makeId(), MessageType.CAST, m));
    }

    enum MessageType {
        CALL, CAST
    };

    static class GenServerRequest extends GenRequestMessage {
        private final MessageType type;
        private final Object message;

        public GenServerRequest(ActorRef sender, Object id, MessageType type, Object message) {
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
}

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
import co.paralleluniverse.actors.LocalActor;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.from;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.makeId;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
class GenServerHelper {
    public static <Message, V> V call(Actor server, Message m) throws InterruptedException, SuspendExecution {
        try {
            return call(server, m, 0, null);
        } catch (TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }

    public static <V> V call(Actor server, Object m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        final GenResponseMessage response = RequestReplyHelper.call(server, new GenServerRequest(from(), null, MessageType.CALL, m), timeout, unit);
        final V res = ((GenValueResponseMessage<V>) response).getValue();
        return res;
    }

    public static void cast(Actor server, Object m) throws SuspendExecution {
        server.send(new GenServerRequest(LocalActor.self(), makeId(), MessageType.CAST, m));
    }

    enum MessageType {
        CALL, CAST
    };

    static class GenServerRequest extends GenRequestMessage {
        private final MessageType type;
        private final Object message;

        public GenServerRequest(Actor sender, Object id, MessageType type, Object message) {
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

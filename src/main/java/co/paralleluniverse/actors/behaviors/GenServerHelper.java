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
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.MessageProcessor;
import co.paralleluniverse.actors.SelectiveReceiveHelper;
import co.paralleluniverse.common.util.Exceptions;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
class GenServerHelper  {

    public static <Message, V> V call(Actor server, Message m) throws InterruptedException, SuspendExecution {
        return call(server, m, 0, null);
    }
    
    public static <Message, V> V call(Actor server, Message m, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution {
        final SelectiveReceiveHelper<Object> helper = new SelectiveReceiveHelper<>(LocalActor.currentActor());
        final Object id = ActorImpl.randtag();
        
        server.sendSync(new GenServerRequest<Message>(LocalActor.currentActor(), id, MessageType.CALL, m));
        final GenResponseMessage response = (GenResponseMessage)helper.receive(timeout, unit, new MessageProcessor<Object>() {

            @Override
            public boolean process(Object m) throws SuspendExecution, InterruptedException {
                return (m instanceof GenResponseMessage && id.equals(((GenResponseMessage)m).getId()));
            }
        });
        
        if(response instanceof GenErrorResponseMessage)
            throw Exceptions.rethrow(((GenErrorResponseMessage)response).getError());
        final V res = ((GenServerResponse<V>)response).getResult();
        return res;
    }
    
    public static <Message> void cast(Actor server, Message m) {
        final Object id = ActorImpl.randtag();
        server.send(new GenServerRequest<Message>(LocalActor.currentActor(), id, MessageType.CAST, m));
    }

    enum MessageType {
        CALL, CAST
    };

    static class GenServerRequest<Message> extends GenRequestMessage {
        private final MessageType type;
        private final Message message;

        public GenServerRequest(Actor sender, Object id, MessageType type, Message message) {
            super(sender, id);
            this.type = type;
            this.message = message;
        }

        public MessageType getType() {
            return type;
        }

        public Message getMessage() {
            return message;
        }
    }
    
       static class GenServerResponse<V> extends GenResponseMessage {
        private final V res;

        public GenServerResponse(Object id, V res) {
            super(id);
            this.res = res;
        }

        public V getResult() {
            return res;
        }
    }

}

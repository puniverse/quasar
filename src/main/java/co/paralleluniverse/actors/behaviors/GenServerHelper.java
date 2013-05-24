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
import co.paralleluniverse.fibers.SuspendExecution;

/**
 *
 * @author pron
 */
class GenServerHelper  {

    public static <Message, V> V call(Actor server, Message m) throws InterruptedException, SuspendExecution {
        server.sendSync(new GenServerMessage<Message>(MessageType.CALL, LocalActor.currentActor(), m));
        V res = (V) LocalActor.currentActor().receive();
        return res;
    }
    
    public static <Message> void cast(Actor server, Message m) {
        server.send(new GenServerMessage<Message>(MessageType.CAST, LocalActor.currentActor(), m));
    }

    enum MessageType {
        CALL, CAST
    };

    static class GenServerMessage<Message> {
        private final MessageType type;
        private final Actor sender;
        private final Message message;

        public GenServerMessage(MessageType type, Actor sender, Message message) {
            this.type = type;
            this.sender = sender;
            this.message = message;
        }

        public MessageType getType() {
            return type;
        }

        public Actor getSender() {
            return sender;
        }

        public Message getMessage() {
            return message;
        }
    }
}

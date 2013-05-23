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
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public abstract class GenServerImpl<Message, V> extends LocalActor<GenServerImpl.GenServerMessage<Message>, Void> implements GenServer<Message, V> {
    private final Server<Message, V> server;
    private long timeout; // nanos
    private boolean run;

    public GenServerImpl(Server<Message, V> server, String name, int mailboxSize) {
        super(name, mailboxSize);
        this.server = server;
        this.timeout = -1;
        this.run = true;
    }

    public GenServerImpl(Server<Message, V> server, Strand strand, String name, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.server = server;
        this.timeout = -1;
        this.run = true;
    }

    @Override
    public V call(Message m) throws InterruptedException, SuspendExecution {
        sendSync(new GenServerMessage<Message>(MessageType.CALL, LocalActor.currentActor(), m));
        V res = (V) currentActorReceive();
        return res;
    }

    @Override
    public void cast(Message m) {
        send(new GenServerMessage<Message>(MessageType.CAST, LocalActor.currentActor(), m));
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        while (run) {
            GenServerMessage<Message> m = receive(timeout, TimeUnit.NANOSECONDS);
            switch (m.getType()) {
                case CALL:
                    V res = server.handleCall(m.getSender(), m.getMessage());
                    if (res != null)
                        m.getSender().send(res);
                    break;

                case CAST:
                    server.handleCall(m.getSender(), m.getMessage());
                    break;
            }
        }
        return null;
    }

    protected void reply(Actor to, V message) {
        to.send(message);
    }

    protected void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = unit.toNanos(timeout);
    }

    protected void stop() {
        run = false;
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

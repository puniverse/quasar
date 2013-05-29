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
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.actors.behaviors.GenServerHelper.GenServerRequest;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class LocalGenServer<Message, V> extends LocalActor<Object, Void> implements GenServer<Message, V> {
    private final Server<Message, V> server;
    private long timeout; // nanos
    private boolean run;

    public LocalGenServer(String name, Server<Message, V> server, long timeout, TimeUnit unit, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.server = server;
        this.timeout = unit != null ? unit.toNanos(timeout) : -1;
        this.run = true;
    }

    public LocalGenServer(String name, Server<Message, V> server, int mailboxSize) {
        this(name, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(String name, Server<Message, V> server) {
        this(name, server, -1, null, null, -1);
    }

    public LocalGenServer(Server<Message, V> server, int mailboxSize) {
        this(null, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(Server<Message, V> server) {
        this(null, server, -1, null, null, -1);
    }

    public LocalGenServer(String name, int mailboxSize) {
        this(name, null, -1, null, null, mailboxSize);
    }

    public LocalGenServer(String name) {
        this(name, null, -1, null, null, -1);
    }

    public LocalGenServer(int mailboxSize) {
        this(null, null, -1, null, null, mailboxSize);
    }

    public LocalGenServer() {
        this(null, null, -1, null, null, -1);
    }

    public static <Message, V> LocalGenServer<Message, V> currentGenServer() {
        return (LocalGenServer<Message, V>) currentActor();
    }

    @Override
    public final V call(Message m) throws InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m);
    }

    @Override
    public final V call(Message m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m, timeout, unit);
    }

    @Override
    public final void cast(Message m) {
        GenServerHelper.cast(this, m);
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        try {
            while (run) {
                Object m1 = receive(timeout, TimeUnit.NANOSECONDS);
                if (m1 instanceof GenServerRequest) {
                    GenServerRequest<Message> m = (GenServerRequest<Message>) m1;
                    switch (m.getType()) {
                        case CALL:
                            try {
                                final V res = handleCall((Actor<V>) m.getFrom(), m.getId(), m.getMessage());
                                if (res != null)
                                    reply((Actor<V>) m.getFrom(), m.getId(), res);
                            } catch (Exception e) {
                                replyError((Actor<V>) m.getFrom(), m.getId(), e);
                            }
                            break;

                        case CAST:
                            handleCast((Actor<V>) m.getFrom(), m.getId(), m.getMessage());
                            break;
                    }
                } else if (m1 == null)
                    handleTimeout();
                else
                    handleInfo(m1);
            }
            terminate(null);
            return null;
        } catch (InterruptedException e) {
            if (run == false) {
                terminate(null);
                return null;
            } else {
                terminate(e);
                throw e;
            }
        } catch (Throwable e) {
            terminate(e);
            throw e;
        }
    }

    @Override
    protected void handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage) {
            stop();
            Strand.currentStrand().interrupt();
        } else
            super.handleLifecycleMessage(m);
    }

    @Override
    public void shutdown() {
        send(new ShutdownMessage(null));
    }
    
    public final void reply(Actor to, Object id, V message) {
        verifyInActor();
        to.send(new GenValueResponseMessage<V>(id, message));
    }

    public final void replyError(Actor to, Object id, Throwable error) {
        verifyInActor();
        to.send(new GenErrorResponseMessage(id, error));
    }

    public final void setTimeout(long timeout, TimeUnit unit) {
        verifyInActor();
        this.timeout = (unit != null ? unit.toNanos(timeout) : -1);
    }

    public final void stop() {
        verifyInActor();
        run = false;
    }

    @Override
    protected void init() throws SuspendExecution {
        server.init();
    }

    protected V handleCall(Actor<V> from, Object id, Message m) throws SuspendExecution {
        if (server != null)
            return server.handleCall(from, id, m);
        throw new UnsupportedOperationException(m.toString());
    }

    protected void handleCast(Actor<V> from, Object id, Message m) throws SuspendExecution {
        if (server != null)
            server.handleCast(from, id, m);
    }

    protected void handleInfo(Object m) throws SuspendExecution {
        if (server != null)
            server.handleInfo(m);
    }

    protected void handleTimeout() throws SuspendExecution {
        if (server != null)
            server.handleTimeout();
    }

    protected void terminate(Throwable cause) throws SuspendExecution {
        if (server != null)
            server.terminate(cause);
    }
}

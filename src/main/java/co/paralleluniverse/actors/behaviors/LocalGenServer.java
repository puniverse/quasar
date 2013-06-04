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
public class LocalGenServer<CallMessage, V, CastMessage> extends LocalActor<Object, Void> implements GenServer<CallMessage, V, CastMessage> {
    private final Server<CallMessage, V, CastMessage> server;
    private long timeout; // nanos
    private boolean run;

    public LocalGenServer(String name, Server<CallMessage, V, CastMessage> server, long timeout, TimeUnit unit, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.server = server;
        this.timeout = unit != null ? unit.toNanos(timeout) : -1;
        this.run = true;
    }

    public LocalGenServer(String name, Server<CallMessage, V, CastMessage> server, int mailboxSize) {
        this(name, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(String name, Server<CallMessage, V, CastMessage> server) {
        this(name, server, -1, null, null, -1);
    }

    public LocalGenServer(Server<CallMessage, V, CastMessage> server, int mailboxSize) {
        this(null, server, -1, null, null, mailboxSize);
    }

    public LocalGenServer(Server<CallMessage, V, CastMessage> server) {
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

    public static <CallMessage, V, CastMessage> LocalGenServer<CallMessage, V, CastMessage> currentGenServer() {
        return (LocalGenServer<CallMessage, V, CastMessage>) self();
    }

    @Override
    public final V call(CallMessage m) throws InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m);
    }

    @Override
    public final V call(CallMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution {
        return GenServerHelper.call(this, m, timeout, unit);
    }

    @Override
    public final void cast(CastMessage m) {
        GenServerHelper.cast(this, m);
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        try {
            init();
            while (run) {
                Object m1 = receive(timeout, TimeUnit.NANOSECONDS);
                if (m1 instanceof GenServerRequest) {
                    GenServerRequest m = (GenServerRequest) m1;
                    switch (m.getType()) {
                        case CALL:
                            try {
                                final V res = handleCall((Actor<V>) m.getFrom(), m.getId(), (CallMessage) m.getMessage());
                                if (res != null)
                                    reply((Actor<V>) m.getFrom(), m.getId(), res);
                            } catch (Exception e) {
                                replyError((Actor<V>) m.getFrom(), m.getId(), e);
                            }
                            break;

                        case CAST:
                            handleCast((Actor<V>) m.getFrom(), m.getId(), (CastMessage) m.getMessage());
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
            getStrand().interrupt();
        } else
            super.handleLifecycleMessage(m);
    }

    @Override
    public final void shutdown() {
        send(new ShutdownMessage(LocalActor.self()));
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

    protected void init() throws SuspendExecution {
        server.init();
    }

    protected V handleCall(Actor<V> from, Object id, CallMessage m) throws Exception, SuspendExecution {
        if (server != null)
            return server.handleCall(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    protected void handleCast(Actor<V> from, Object id, CastMessage m) throws SuspendExecution {
        if (server != null)
            server.handleCast(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
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

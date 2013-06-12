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
import co.paralleluniverse.actors.behaviors.GenServerHelper.GenServerRequest;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class LocalGenServer<CallMessage, V, CastMessage> extends BasicGenBehavior implements GenServer<CallMessage, V, CastMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalGenServer.class);
    private long timeout; // nanos

    public LocalGenServer(String name, Server<CallMessage, V, CastMessage> server, long timeout, TimeUnit unit, Strand strand, int mailboxSize) {
        super(name, server, strand, mailboxSize);
        this.timeout = unit != null ? unit.toNanos(timeout) : -1;
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
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
    //</editor-fold>

    protected Server<CallMessage, V, CastMessage> server() {
        return (Server<CallMessage, V, CastMessage>) getInitializer();
    }

    @Override
    protected Logger log() {
        return LOG;
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
    protected final void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            Object m1 = receive(timeout, TimeUnit.NANOSECONDS);
            if (m1 == null)
                handleTimeout();
            else
                handleMessage(m1);
        }
    }

    @Override
    protected void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
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
        } else
            handleInfo(m1);
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

    protected V handleCall(Actor<V> from, Object id, CallMessage m) throws Exception, SuspendExecution {
        if (server() != null)
            return server().handleCall(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    protected void handleCast(Actor<V> from, Object id, CastMessage m) throws SuspendExecution {
        if (server() != null)
            server().handleCast(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    protected void handleInfo(Object m) throws SuspendExecution {
        if (server() != null)
            server().handleInfo(m);
    }

    protected void handleTimeout() throws SuspendExecution {
        if (server() != null)
            server().handleTimeout();
    }
}

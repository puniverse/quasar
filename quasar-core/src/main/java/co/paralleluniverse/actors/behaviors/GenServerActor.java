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
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.GenBehaviorActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.behaviors.GenServer.GenServerRequest;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class GenServerActor<CallMessage, V, CastMessage> extends GenBehaviorActor {
    private static final Logger LOG = LoggerFactory.getLogger(GenServerActor.class);
    private TimeUnit timeoutUnit;
    private long timeout;

    public GenServerActor(String name, Server<CallMessage, V, CastMessage> server, long timeout, TimeUnit unit, Strand strand, MailboxConfig mailboxConfig) {
        super(name, server, strand, mailboxConfig);
        this.timeoutUnit = timeout > 0 ? unit : null;
        this.timeout = timeout;
    }

    @Override
    protected GenServer<CallMessage, V, CastMessage> makeRef(ActorRef<Object> ref) {
        return new GenServer<CallMessage, V, CastMessage>(ref);
    }

    @Override
    public GenServer<CallMessage, V, CastMessage> ref() {
        return (GenServer<CallMessage, V, CastMessage>) super.ref();
    }

    @Override
    public GenServer<CallMessage, V, CastMessage> spawn(ForkJoinPool fjPool) {
        return (GenServer<CallMessage, V, CastMessage>) super.spawn(fjPool);
    }

    @Override
    public GenServer<CallMessage, V, CastMessage> spawn() {
        return (GenServer<CallMessage, V, CastMessage>) super.spawn();
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public GenServerActor(String name, Server<CallMessage, V, CastMessage> server, MailboxConfig mailboxConfig) {
        this(name, server, -1, null, null, mailboxConfig);
    }

    public GenServerActor(String name, Server<CallMessage, V, CastMessage> server) {
        this(name, server, -1, null, null, null);
    }

    public GenServerActor(Server<CallMessage, V, CastMessage> server, MailboxConfig mailboxConfig) {
        this(null, server, -1, null, null, mailboxConfig);
    }

    public GenServerActor(Server<CallMessage, V, CastMessage> server) {
        this(null, server, -1, null, null, null);
    }

    public GenServerActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, -1, null, null, mailboxConfig);
    }

    public GenServerActor(String name) {
        this(name, null, -1, null, null, null);
    }

    public GenServerActor(MailboxConfig mailboxConfig) {
        this(null, null, -1, null, null, mailboxConfig);
    }

    public GenServerActor() {
        this(null, null, -1, null, null, null);
    }
    //</editor-fold>

    protected Server<CallMessage, V, CastMessage> server() {
        return (Server<CallMessage, V, CastMessage>) getInitializer();
    }

    @Override
    public Logger log() {
        return LOG;
    }

    public static <CallMessage, V, CastMessage> GenServerActor<CallMessage, V, CastMessage> currentGenServer() {
        return (GenServerActor<CallMessage, V, CastMessage>) (Actor)currentActor();
    }

    @Override
    protected final void behavior() throws InterruptedException, SuspendExecution {
        while (isRunning()) {
            Object m1 = receive(timeout, timeoutUnit);
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
                        final V res = handleCall((ActorRef<V>) m.getFrom(), m.getId(), (CallMessage) m.getMessage());
                        if (res != null)
                            reply((ActorRef<V>) m.getFrom(), m.getId(), res);
                    } catch (Exception e) {
                        replyError((ActorRef<V>) m.getFrom(), m.getId(), e);
                    }
                    break;

                case CAST:
                    handleCast((ActorRef<V>) m.getFrom(), m.getId(), (CastMessage) m.getMessage());
                    break;
            }
        } else
            handleInfo(m1);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    public final void reply(ActorRef to, Object id, V message) throws SuspendExecution {
        verifyInActor();
        to.send(new GenValueResponseMessage<V>(id, message));
    }

    public final void replyError(ActorRef to, Object id, Throwable error) throws SuspendExecution {
        verifyInActor();
        to.send(new GenErrorResponseMessage(id, error));
    }

    public final void setTimeout(long timeout, TimeUnit unit) {
        verifyInActor();
        this.timeoutUnit = timeout > 0 ? unit : null;
        this.timeout = timeout;
    }

    protected V handleCall(ActorRef<V> from, Object id, CallMessage m) throws Exception, SuspendExecution {
        if (server() != null)
            return server().handleCall(from, id, m);
        else
            throw new UnsupportedOperationException(m.toString());
    }

    protected void handleCast(ActorRef<V> from, Object id, CastMessage m) throws SuspendExecution {
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

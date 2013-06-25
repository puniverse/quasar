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
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.RemoteActor;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.reply;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.replyError;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class LocalGenEvent<Event> extends BasicGenBehavior implements GenEvent<Event> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalGenEvent.class);
    private final List<EventHandler<Event>> handlers = new ArrayList<>();

    public LocalGenEvent(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(name, initializer, strand, mailboxConfig);
    }

    @Override
    protected RemoteBasicGenBehavior getRemote(RemoteActor remote) {
        return new RemoteGenEvent(remote);
    }

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    public LocalGenEvent(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    public LocalGenEvent(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    public LocalGenEvent(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    public LocalGenEvent(Initializer initializer) {
        this(null, initializer, null, null);
    }

    public LocalGenEvent(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    public LocalGenEvent(String name) {
        this(name, null, null, null);
    }

    public LocalGenEvent(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    public LocalGenEvent() {
        this(null, null, null, null);
    }
    //</editor-fold>
    
    @Override
    public Logger log() {
        return LOG;
    }

    @Override
    public boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
            LOG.info("{} adding handler {}", this, handler);
            return handlers.add(handler);
        } else {
            final GenResponseMessage res = call(this, new HandlerMessage(RequestReplyHelper.from(), null, handler, true));
            return ((GenValueResponseMessage<Boolean>) res).getValue();
        }
    }

    @Override
    public boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
            LOG.info("{} removing handler {}", this, handler);
            return handlers.remove(handler);
        } else {
            final GenResponseMessage res = call(this, new HandlerMessage(RequestReplyHelper.from(), null, handler, false));
            return ((GenValueResponseMessage<Boolean>) res).getValue();
        }
    }

    @Override
    public void notify(Event event) throws SuspendExecution {
        send(event);
    }

    @Override
    protected final void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
        if (m1 instanceof GenRequestMessage) {
            final GenRequestMessage req = (GenRequestMessage) m1;
            try {
                if (m1 instanceof HandlerMessage) {
                    final HandlerMessage m = (HandlerMessage) m1;
                    if (m.add)
                        reply(req, addHandler(m.handler));
                    else
                        reply(req, removeHandler(m.handler));
                }
            } catch (Exception e) {
                replyError(req, e);
            }
        } else
            notifyHandlers((Event) m1);
    }

    @Override
    protected void onTerminate(Throwable cause) throws SuspendExecution, InterruptedException {
        super.onTerminate(cause);
        handlers.clear();
    }

    private void notifyHandlers(Event event) {
        LOG.debug("{} Got event {}", this, event);
        for (EventHandler<Event> handler : handlers)
            handler.handleEvent(event);
    }

    static class HandlerMessage<Event> extends GenRequestMessage {
        final EventHandler<Event> handler;
        final boolean add;

        public HandlerMessage(Actor<?> from, Object id, EventHandler<Event> handler, boolean add) {
            super(from, id);
            this.handler = handler;
            this.add = add;
        }
    }
}

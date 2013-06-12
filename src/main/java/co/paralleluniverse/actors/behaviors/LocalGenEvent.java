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
import co.paralleluniverse.common.util.Exceptions;
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
public class LocalGenEvent<Event> extends LocalActor<Object, Void> implements GenEvent<Event> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalGenEvent.class);
    private final List<EventHandler<Event>> handlers = new ArrayList<>();

    public LocalGenEvent(String name, Strand strand, int mailboxSize) {
        super(strand, name, mailboxSize);
    }

    public LocalGenEvent(String name, int mailboxSize) {
        this(name, null, mailboxSize);
    }

    public LocalGenEvent(String name) {
        this(name, null, -1);
    }

    public LocalGenEvent(int mailboxSize) {
        this(null, null, mailboxSize);
    }

    public LocalGenEvent() {
        this(null, null, -1);
    }

    @Override
    public boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
            LOG.info("{} adding handler {}", this, handler);
            return handlers.add(handler);
        } else {
            final GenResponseMessage res = RequestReplyHelper.call(this, new HandlerMessage(RequestReplyHelper.from(), null, handler, true));
            return ((GenValueResponseMessage<Boolean>) res).getValue();
        }
    }

    @Override
    public boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        if (isInActor()) {
            LOG.info("{} removing handler {}", this, handler);
            return handlers.remove(handler);
        } else {
            final GenResponseMessage res = RequestReplyHelper.call(this, new HandlerMessage(RequestReplyHelper.from(), null, handler, false));
            return ((GenValueResponseMessage<Boolean>) res).getValue();
        }
    }

    @Override
    public void notify(Event event) {
        send(event);
    }

    @Override
    public void shutdown() {
        send(new ShutdownMessage(LocalActor.self()));
    }

    @Override
    protected void handleLifecycleMessage(LifecycleMessage m) {
        if (m instanceof ShutdownMessage) {
            getStrand().interrupt();
        } else
            super.handleLifecycleMessage(m);
    }

    @Override
    protected Void doRun() throws InterruptedException, SuspendExecution {
        try {
            for (;;) {
                final Object m1 = receive(); // we care only about lifecycle messages
                if (m1 instanceof ShutdownMessage)
                    break;
                else if (m1 instanceof GenRequestMessage) {
                    final GenRequestMessage req = (GenRequestMessage) m1;
                    try {
                        if (m1 instanceof HandlerMessage) {
                            final HandlerMessage m = (HandlerMessage) m1;
                            if(m.add)
                                RequestReplyHelper.reply(m, addHandler(m.handler));
                            else
                                RequestReplyHelper.reply(m, removeHandler(m.handler));
                        }
                    } catch (Exception e) {
                        req.getFrom().send(new GenErrorResponseMessage(req.getId(), e));
                    }
                } else
                    notifyHandlers((Event)m1);
            }
        } catch (InterruptedException e) {
        } catch (Exception e) {
            LOG.info("Exception!", e);
            throw Exceptions.rethrow(e);
        } finally {
            LOG.info("GenEvent {} shutting down.", this);
            handlers.clear();
        }

        return null;
    }

    private void notifyHandlers(Event event) {
        LOG.debug("{} Got event {}", this, event);
        for(EventHandler<Event> handler : handlers)
            handler.handleEvent(event);
    }
    
    private static class HandlerMessage<Event> extends GenRequestMessage {
        final EventHandler<Event> handler;
        final boolean add;

        public HandlerMessage(Actor<?> from, Object id, EventHandler<Event> handler, boolean add) {
            super(from, id);
            this.handler = handler;
            this.add = add;
        }
    }
}

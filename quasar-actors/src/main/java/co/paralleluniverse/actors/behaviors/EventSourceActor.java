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
import co.paralleluniverse.actors.MailboxConfig;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.reply;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.replyError;
import co.paralleluniverse.fibers.FiberScheduler;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A behavior actor that can be notified of *event* messages, which are delivered to *event handlers* which may be registered with the actor.
 * The event handlers are called synchronously on the same strand as the actor's, so they may delay processing by other handlers if they block the strand.
 *
 * @author pron
 */
public class EventSourceActor<Event> extends BehaviorActor {
    private static final Logger LOG = LoggerFactory.getLogger(EventSourceActor.class);
    private final List<EventHandler<Event>> handlers = new ArrayList<>();

    /**
     * Creates a new event-source actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param strand        this actor's strand.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public EventSourceActor(String name, Initializer initializer, Strand strand, MailboxConfig mailboxConfig) {
        super(name, initializer, strand, mailboxConfig);
    }

    //<editor-fold defaultstate="collapsed" desc="Behavior boilerplate">
    /////////// Behavior boilerplate ///////////////////////////////////
    @Override
    protected EventSource<Event> makeRef(ActorRef<Object> ref) {
        return new EventSourceImpl.Local<Event>(ref);
    }

    @Override
    public EventSource<Event> ref() {
        return (EventSource<Event>) super.ref();
    }

    @Override
    protected EventSource<Event> self() {
        return ref();
    }

    @Override
    public EventSource<Event> spawn(FiberScheduler scheduler) {
        return (EventSource<Event>) super.spawn(scheduler);
    }

    @Override
    public EventSource<Event> spawn() {
        return (EventSource<Event>) super.spawn();
    }

    @Override
    public EventSource<Event> spawnThread() {
        return (EventSource<Event>) super.spawnThread();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /////////// Constructors ///////////////////////////////////
    /**
     * Creates a new event-source actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public EventSourceActor(String name, Initializer initializer, MailboxConfig mailboxConfig) {
        this(name, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public EventSourceActor(String name, Initializer initializer) {
        this(name, initializer, null, null);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param initializer   an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     * @param mailboxConfig this actor's mailbox settings.
     */
    public EventSourceActor(Initializer initializer, MailboxConfig mailboxConfig) {
        this(null, initializer, null, mailboxConfig);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
     */
    public EventSourceActor(Initializer initializer) {
        this(null, initializer, null, null);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig this actor's mailbox settings.
     */
    public EventSourceActor(String name, MailboxConfig mailboxConfig) {
        this(name, null, null, mailboxConfig);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param name the actor name (may be {@code null}).
     */
    public EventSourceActor(String name) {
        this(name, null, null, null);
    }

    /**
     * Creates a new event-source actor.
     *
     * @param mailboxConfig this actor's mailbox settings.
     */
    public EventSourceActor(MailboxConfig mailboxConfig) {
        this(null, null, null, mailboxConfig);
    }

    /**
     * Creates a new event-source actor.
     */
    public EventSourceActor() {
        this(null, null, null, null);
    }
    //</editor-fold>

    @Override
    public Logger log() {
        return LOG;
    }

    protected boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        verifyInActor();
        LOG.info("{} adding handler {}", this, handler);
        return handlers.add(handler);
    }

    protected boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        verifyInActor();
        LOG.info("{} removing handler {}", this, handler);
        return handlers.remove(handler);
    }

    protected void notify(Event event) throws SuspendExecution {
        ref().send(event);
    }

    @Override
    protected final void handleMessage(Object m1) throws InterruptedException, SuspendExecution {
        if (m1 instanceof RequestMessage) {
            final RequestMessage req = (RequestMessage) m1;
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

    public static <Event> EventSourceActor<Event> currentEventSourceActor() {
        return (EventSourceActor<Event>) Actor.<Object, Void>currentActor();
    }

    private void notifyHandlers(Event event) {
        LOG.debug("{} Got event {}", this, event);
        for (EventHandler<Event> handler : handlers)
            handler.handleEvent(event);
    }

    static class HandlerMessage<Event> extends RequestMessage {
        final EventHandler<Event> handler;
        final boolean add;

        public HandlerMessage(ActorRef<?> from, Object id, EventHandler<Event> handler, boolean add) {
            super(from, id);
            this.handler = handler;
            this.add = add;
        }

        @Override
        protected String contentString() {
            return super.contentString() + " handler: " + handler + " add: " + add;
        }
    }
}

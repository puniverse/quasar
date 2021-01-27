/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.actors.ActorLoader;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.MailboxConfig;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.reply;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.replyError;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
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
    private final List<EventHandler<Event>> nonUpgradedHandlers = new ArrayList<>();

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
        return new EventSource<Event>(ref);
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
    public EventSource<Event> spawn(StrandFactory sf) {
        return (EventSource<Event>) super.spawn(sf);
    }

    @Override
    public EventSource<Event> spawn(FiberFactory ff) {
        return (EventSource<Event>) super.spawn(ff);
    }

    @Override
    public EventSource<Event> spawn() {
        return (EventSource<Event>) super.spawn();
    }

    @Override
    public EventSource<Event> spawnThread() {
        return (EventSource<Event>) super.spawnThread();
    }

    public static <Event> EventSourceActor<Event> currentEventSourceActor() {
        return (EventSourceActor<Event>) Actor.<Object, Void>currentActor();
    }

    @Override
    public Logger log() {
        return LOG;
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
     * @param name        the actor name (may be {@code null}).
     * @param initializer an optional delegate object that will be run upon actor initialization and termination. May be {@code null}.
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

    protected boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        verifyInActor();

        EventHandler<Event> _handler = ActorLoader.getReplacementFor(handler);
        log().info("{} adding handler {}", this, _handler);
        boolean res = handlers.add(_handler);
        nonUpgradedHandlers.add(handler);
        return res;
    }

    protected boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException {
        verifyInActor();
        log().info("{} removing handler {}", this, handler);
        int index = -1;
        index = handlers.indexOf(handler);
        if (index == -1)
            index = nonUpgradedHandlers.indexOf(handler);
        if (index == -1)
            return false;
        handlers.remove(index);
        nonUpgradedHandlers.remove(index);
        return true;
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

    private void notifyHandlers(Event event) throws InterruptedException, SuspendExecution {
        log().debug("{} Got event {}", this, event);
        for (ListIterator<EventHandler<Event>> it = handlers.listIterator(); it.hasNext();) {
            EventHandler<Event> handler = it.next();

            EventHandler<Event> _handler = ActorLoader.getReplacementFor(handler);
            if (_handler != handler) {
                log().info("Upgraded event handler implementation: {}", _handler);
                it.set(_handler);
            }

            _handler.handleEvent(event);
        }
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

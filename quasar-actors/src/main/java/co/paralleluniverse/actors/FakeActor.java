/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.actors;

import co.paralleluniverse.concurrent.util.MapUtil;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * An {@link ActorRef} which is not backed by any actual {@link Actor}.
 * Instead, this "fake actor" only has a channel that serves as a mailbox, but not no {@link Actor#doRun() doRun} method, or a private strand.
 *
 * @author pron
 */
public abstract class FakeActor<Message> extends ActorImpl<Message> {
    private static final Throwable NATURAL = new Throwable();
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(MapUtil.<LifecycleListener, Boolean>newConcurrentHashMap());
    private final Set<ActorImpl> observed = Collections.newSetFromMap(MapUtil.<ActorImpl, Boolean>newConcurrentHashMap());
    private volatile Throwable deathCause;

    public FakeActor(String name, SendPort<Message> mailbox) {
        super(name, (SendPort<Object>) mailbox, null);
    }

    /**
     * All messages sent to the mailbox are passed to this method. If this method returns a non-null value, this value will be returned
     * from the {@code receive} methods. If it returns {@code null}, then {@code receive} will keep waiting.
     * <p>
     * By default, this message passes all {@link LifecycleMessage} messages to {@link #handleLifecycleMessage(LifecycleMessage) handleLifecycleMessage}, while
     * other messages are returned (and will be returned by {@code receive}.</p>
     *
     * @param m the message
     */
    protected Message filterMessage(Object m) {
        if (m instanceof LifecycleMessage) {
            return handleLifecycleMessage((LifecycleMessage) m);
        }
        return (Message) m;
    }

    @Override
    public boolean trySend(Message message) {
        record(1, "ActorRef", "trySend", "Sending %s -> %s", message, this);
        Message msg = filterMessage(message);
        if (msg == null)
            return true;
        if (mailbox().trySend(msg))
            return true;
        record(1, "ActorRef", "trySend", "Message not sent. Mailbox is not ready.");
        return false;
    }

    /**
     * For internal use
     *
     * @param message
     */
    @Override
    protected void internalSend(Object message) throws SuspendExecution {
        record(1, "ActorRef", "send", "Sending %s -> %s", message, this);
        Message msg = filterMessage(message);
        if (msg == null)
            return;
        try {
            mailbox().send(message);
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
        }
    }

    @Override
    protected void internalSendNonSuspendable(Object message) {
        record(1, "ActorRef", "internalSendNonSuspendable", "Sending %s -> %s", message, this);
        Message msg = filterMessage(message);
        if (msg == null)
            return;
        if (!mailbox().trySend(msg))
            throw new QueueCapacityExceededException();
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        final Throwable cause = getDeathCause();
        if (isDone()) {
            listener.dead(ref(), cause);
            return;
        }
        lifecycleListeners.add(listener);
        if (isDone())
            listener.dead(ref(), cause);
    }

    /**
     * Returns this actor's cause of death
     *
     * @return the {@link Throwable} that caused this actor's death, or {@code null} if it died by natural causes, or if it not dead.
     */
    protected final Throwable getDeathCause() {
        return deathCause == NATURAL ? null : deathCause;
    }

    /**
     * Tests whether this fake actor has terminated.
     */
    protected final boolean isDone() {
        return deathCause != null;
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }

    @Override
    protected void removeObserverListeners(ActorRef actor) {
        for (Iterator<LifecycleListener> it = lifecycleListeners.iterator(); it.hasNext();) {
            LifecycleListener lifecycleListener = it.next();
            if (lifecycleListener instanceof ActorLifecycleListener)
                if (((ActorLifecycleListener) lifecycleListener).getObserver().equals(actor))
                    it.remove();
        }
    }

    /**
     * Makes this fake actor watch another actor.
     *
     * When the other actor dies, this actor receives an {@link ExitMessage}, that is
     * handled by {@link #handleLifecycleMessage(LifecycleMessage) handleLifecycleMessage}. This message does not cause an exception to be thrown,
     * unlike the case where it is received as a result of a linked actor's death.
     * <p>
     * Unlike a link, a watch is asymmetric, and it is also composable, namely, calling this method twice with the same argument would result in two different values
     * returned, and in an {@link ExitMessage} to be received twice.</p>
     *
     * @param other the other actor
     * @return a {@code watchId} object that identifies this watch in messages, and used to remove the watch by the {@link #unwatch(ActorRef, Object) unwatch} method.
     * @see #unwatch(ActorRef, Object)
     */
    public final Object watch(ActorRef other) {
        final Object id = ActorUtil.randtag();

        final ActorImpl other1 = getActorRefImpl(other);
        final LifecycleListener listener = new ActorLifecycleListener(ref(), id);
        record(1, "Actor", "watch", "Actor %s to watch %s (listener: %s)", this, other1, listener);

        other1.addLifecycleListener(listener);
        observed.add(other1);
        return id;
    }

    /**
     * Un-watches another actor.
     *
     * @param other   the other actor
     * @param watchId the object returned from the call to {@link #watch(ActorRef) watch(other)}
     * @see #watch(ActorRef)
     */
    public final void unwatch(ActorRef other, Object watchId) {
        final ActorImpl other1 = getActorRefImpl(other);
        final LifecycleListener listener = new ActorLifecycleListener(ref(), watchId);
        record(1, "Actor", "unwatch", "Actor %s to stop watching %s (listener: %s)", this, other1, listener);
        other1.removeLifecycleListener(listener);
        observed.remove(getActorRefImpl(other));
    }

    protected abstract Message handleLifecycleMessage(LifecycleMessage m);

    protected void die(Throwable cause) {
        record(1, "Actor", "die", "Actor %s is dying of cause %s", this, cause);
        this.deathCause = (cause == null ? NATURAL : cause);

        for (LifecycleListener listener : lifecycleListeners) {
            record(1, "Actor", "die", "Actor %s notifying listener %s of death.", this, listener);
            try {
                listener.dead(ref(), cause);
            } catch (Exception e) {
                record(1, "Actor", "die", "Actor %s notifying listener %s of death failed with excetpion %s", this, listener, e);
            }
        }

        // avoid memory leaks:
        lifecycleListeners.clear();
        observed.clear();
    }

    /////////// Serialization ///////////////////////////////////
    protected final Object writeReplace() throws java.io.ObjectStreamException {
        return RemoteActorProxyFactoryService.create(ref(), null);
    }
}

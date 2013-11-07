/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public abstract class FakeActor<Message> extends ActorRefImpl<Message> {
    private static final Throwable NATURAL = new Throwable();
    private final Set<LifecycleListener> lifecycleListeners = Collections.newSetFromMap(new ConcurrentHashMapV8<LifecycleListener, Boolean>());
    private final Set<ActorRefImpl> observed = Collections.newSetFromMap(new ConcurrentHashMapV8<ActorRefImpl, Boolean>());
    private volatile Throwable deathCause;

    public FakeActor(String name, SendPort<Message> mailbox) {
        super(name, (SendPort<Object>)mailbox);
    }

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
        if(msg == null)
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
        if(msg == null)
            return;
        try {
            mailbox().send(message);
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
        }
    }

    @Override
    protected void internalSendNonSuspendable(Object message) {
        if (!mailbox().trySend(message))
            throw new QueueCapacityExceededException();
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        final Throwable cause = getDeathCause();
        if (isDone()) {
            listener.dead(this, cause);
            return;
        }
        lifecycleListeners.add(listener);
        if (isDone())
            listener.dead(this, cause);
    }

    protected final Throwable getDeathCause() {
        return deathCause == NATURAL ? null : deathCause;
    }

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

    public final Object watch(ActorRef other1) {
        final Object id = ActorUtil.randtag();

        final ActorRefImpl other = getActorRefImpl(other1);
        final LifecycleListener listener = new ActorLifecycleListener(this, id);
        record(1, "Actor", "watch", "Actor %s to watch %s (listener: %s)", this, other, listener);

        other.addLifecycleListener(listener);
        observed.add(other);
        return id;
    }

    public final void unwatch(ActorRef other1, Object watchId) {
        final ActorRefImpl other = getActorRefImpl(other1);
        final LifecycleListener listener = new ActorLifecycleListener(this, watchId);
        record(1, "Actor", "unwatch", "Actor %s to stop watching %s (listener: %s)", this, other, listener);
        other.removeLifecycleListener(listener);
        observed.remove(getActorRefImpl(other1));
    }

    protected abstract Message handleLifecycleMessage(LifecycleMessage m);

    protected void die(Throwable cause) {
        record(1, "Actor", "die", "Actor %s is dying of cause %s", this, cause);
        this.deathCause = (cause == null ? NATURAL : cause);

        for (LifecycleListener listener : lifecycleListeners) {
            record(1, "Actor", "die", "Actor %s notifying listener %s of death.", this, listener);
            try {
                listener.dead(this, cause);
            } catch (Exception e) {
                record(1, "Actor", "die", "Actor %s notifying listener %s of death failed with excetpion %s", this, listener, e);
            }

            // avoid memory leak in links:
            if (listener instanceof ActorLifecycleListener) {
                ActorLifecycleListener l = (ActorLifecycleListener) listener;
                if (l.getId() == null) // link
                    l.getObserver().removeObserverListeners(this);
            }
        }

        // avoid memory leaks:
        lifecycleListeners.clear();
        for (ActorRefImpl a : observed)
            a.removeObserverListeners(this);
        observed.clear();
    }
}

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

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;
import co.paralleluniverse.strands.channels.SendPort;
import co.paralleluniverse.strands.queues.QueueCapacityExceededException;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An actor's external API (for use by code not part of the actor).
 *
 * @author pron
 */
public class ActorRef<Message> implements SendPort<Message>, java.io.Serializable {
    private volatile ActorImpl<Message> impl;

    protected ActorRef(ActorImpl<Message> impl) {
        setImpl(impl);
    }

    protected ActorRef() {
    }

    public String getName() {
        return getImpl().getName();
    }

    protected ActorImpl<Message> getImpl() {
        return impl;
    }

    void setImpl(ActorImpl<Message> impl) {
        this.impl = impl;

        ActorRef<Message> r = impl.ref;
        if (r != null && ActorRefDelegate.stripDelegates(r) != this)
            throw new IllegalStateException("Actor " + impl + " already has a ref: " + ActorRefDelegate.stripDelegates(r));
    }

    /**
     * Sends a message to the actor, possibly blocking until there's room available in the mailbox.
     *
     * If the mailbox is full, this method may block or silently drop the message.
     * The behavior is determined by the mailbox's {@link co.paralleluniverse.strands.channels.Channels.OverflowPolicy OverflowPolicy}, set at construction time.
     * However, unlike regular channels, this method never throws {@link co.paralleluniverse.strands.queues.QueueCapacityExceededException QueueCapacityExceededException}.
     * If the mailbox overflows, and has been configured with the {@link co.paralleluniverse.strands.channels.Channels.OverflowPolicy#THROW THROW} policy,
     * the exception will be thrown <i>into</i> the actor.
     *
     * @param message
     * @throws SuspendExecution
     */
    @Override
    public void send(Message message) throws SuspendExecution {
        try {
            MutabilityTester.testMutability(message);
            ActorImpl<Message> x = getImpl();
            try {
                x.internalSend(message);
            } catch (QueueCapacityExceededException e) {
                x.throwIn(e);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            LostActor.instance.ref().send(message);
            LostActor.instance.throwIn(e);
        }
    }

    /**
     * Sends a message to the actor, and attempts to schedule the actor's strand for immediate execution.
     * This method may be called when a response message is expected from this actor; in this case, this method might provide
     * better latency than {@link #send(java.lang.Object)}.
     *
     * @param message
     * @throws SuspendExecution
     */
    public void sendSync(Message message) throws SuspendExecution {
        try {
            MutabilityTester.testMutability(message);
            getImpl().sendSync(message);
        } catch (RuntimeException e) {
            LostActor.instance.ref().sendSync(message);
            LostActor.instance.throwIn(e);
        }
    }

    /**
     * Sends a message to the actor, possibly blocking until there's room available in the mailbox, but never longer than the
     * specified timeout.
     *
     * If the mailbox is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the mailbox's {@link OverflowPolicy OverflowPolicy}, set at construction time.
     * <p>
     * <b>Currently, this behavior is not yet supported. The message will be sent using {@link #send(Object)} and the timeout argument
     * will be disregarded</b></p>
     *
     * @param message the message
     * @param timeout the maximum duration this method is allowed to wait.
     * @param unit    the timeout's time unit
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has elapsed.
     * @throws SuspendExecution
     */
    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    /**
     * Sends a message to the actor, possibly blocking until there's room available in the mailbox, but never longer than the
     * specified timeout.
     *
     * If the channel is full, this method may block, throw an exception, silently drop the message, or displace an old message from
     * the channel. The behavior is determined by the mailbox's {@link OverflowPolicy OverflowPolicy}, set at construction time.
     * <p>
     * <b>Currently, this behavior is not yet supported. The message will be sent using {@link #send(Object)} and the timeout argument
     * will be disregarded</b></p>
     *
     * @param message the message
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return {@code true} if the message has been sent successfully; {@code false} if the timeout has elapsed.
     * @throws SuspendExecution
     */
    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        send(message);
        return true;
    }

    /**
     * Sends a message to the actor if the channel has mailbox available. This method never blocks.
     *
     * @param msg the message
     * @return {@code true} if the message has been sent; {@code false} otherwise.
     */
    @Override
    public boolean trySend(Message msg) {
        try {
            return getImpl().trySend(msg);
        } catch (RuntimeException e) {
            LostActor.instance.ref().trySend(msg);
            LostActor.instance.throwIn(e);
            return false;
        }
    }

    /**
     * This implementation throws {@code UnsupportedOperationException}.
     */
    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    /**
     * This implementation throws {@code UnsupportedOperationException}.
     */
    @Override
    public void close(Throwable t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Interrupts the actor's strand
     */
    protected void interrupt() {
        getImpl().interrupt();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj == this)
            return true;
        if (obj instanceof DelegatingEquals)
            return obj.equals(this);
        if (getImpl() == null)
            return false;
        if (!(obj instanceof ActorRef))
            return false;
        ActorRef other = (ActorRef) obj;
        return getImpl().equals(other.getImpl());
    }

    @Override
    public int hashCode() {
        return 581 + Objects.hashCode(getImpl());
    }

    @Override
    public String toString() {
        return "ActorRef@" + Integer.toHexString(System.identityHashCode(this)) + "{" + getImpl() + '}';
    }

    Object readResolve() {
        if (impl == null)
            return null;
        ActorRef<Message> ref = ActorRefCanonicalizerService.getRef(impl, this);
        if (impl.ref == null)
            setRef(impl, ref);
        return ref;
    }

    private static final Field actorImplRefField;

    static {
        try {
            actorImplRefField = ActorImpl.class.getDeclaredField("ref");
            actorImplRefField.setAccessible(true);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static <T> void setRef(ActorImpl<T> impl, ActorRef<T> ref) {
        try {
            actorImplRefField.set(impl, ref);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
}

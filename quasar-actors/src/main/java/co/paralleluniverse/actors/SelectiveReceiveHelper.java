/*
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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.queues.QueueIterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs selective receive on behalf of an actor.
 *
 * @author pron
 */
public class SelectiveReceiveHelper<Message> implements java.io.Serializable {
    private transient Actor<Message, ?> actor;
    private int currentMessageIndex = -1; // this works because channel is single-consumer

    /**
     * Creates a {@code SelectiveReceiveHelper} to add selective receive to an actor
     *
     * @param actor the actor
     */
    public SelectiveReceiveHelper(Actor<Message, ?> actor) {
        if (actor == null)
            throw new NullPointerException("actor is null");
        this.actor = actor;
    }

    /**
     * used only during deserialization
     */
    public void setActor(Actor<Message, ?> actor) {
        this.actor = actor;
    }

    /**
     * Performs a selective receive. This method blocks until a message that is {@link MessageProcessor#process(java.lang.Object) selected} by
     * the given {@link MessageProcessor} is available in the mailbox, and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <T>  The type of the returned value
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}
     * @throws InterruptedException
     */
    public final <T> T receive(MessageProcessor<? super Message, T> proc) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, null, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Performs a selective receive. This method blocks (but for no longer than the given timeout) until a message that is
     * {@link MessageProcessor#process(java.lang.Object) selected} by the given {@link MessageProcessor} is available in the mailbox,
     * and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <T>     The type of the returned value
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit    timeout's time unit.
     * @param proc    performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if the timeout expired.
     * @throws InterruptedException
     */
    public final <T> T receive(long timeout, TimeUnit unit, MessageProcessor<? super Message, T> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        assert Actor.currentActor() == null || Actor.currentActor() == actor;

        final Mailbox<Object> mailbox = actor.mailbox();

        actor.checkThrownIn0();
        mailbox.maybeSetCurrentStrandAsOwner();

        final long start = timeout > 0 ? System.nanoTime() : 0;
        long now;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        final long deadline = start + left;

        actor.monitorResetSkippedMessages();
        QueueIterator<Object> it = mailbox.queue().iterator();
        for (int i = 0;; i++) {
            if (actor.flightRecorder != null)
                actor.record(1, "SelctiveReceiveHelper", "receive", "%s waiting for a message. %s", this, timeout > 0 ? "millis left: " + TimeUnit.MILLISECONDS.convert(left, TimeUnit.NANOSECONDS) : "");

            mailbox.lock();

            if (it.hasNext()) {
                final Object m;
                try {
                    m = it.next();
                } finally {
                    mailbox.unlock();
                }
                if (i == currentMessageIndex) {
                    it.remove();
                    i--;
                    currentMessageIndex = -1;
                    continue;
                }

                actor.record(1, "SelctiveReceiveHelper", "receive", "Received %s <- %s", this, m);
                actor.monitorAddMessage();
                if (m instanceof ExitMessage) {
                    final ExitMessage em = (ExitMessage) m;
                    if (em.getWatch() == null) {
                        // Delay all lifecycle messages except link death signals
                        it.remove();
                        i--;
                        handleLifecycleMessage((LifecycleMessage) m);
                    }
                } else {
                    final Message msg = (Message) m;
                    currentMessageIndex = i;
                    try {
                        T res = proc.process(msg);
                        if (res != null) {
                            if (currentMessageIndex == i) // another call to receive from within the processor may have deleted msg
                                it.remove();
                            return res;
                        }
                    } catch (Exception e) {
                        if (currentMessageIndex == i) // another call to receive from within the processor may have deleted msg
                            it.remove();
                        throw e;
                    } finally {
                        currentMessageIndex = -1;
                    }
                    actor.record(1, "SelctiveReceiveHelper", "receive", "%s skipped %s", this, m);
                    actor.monitorSkippedMessage();
                }
            } else {
                try {
                    if (unit == null)
                        mailbox.await(i);
                    else if (timeout > 0) {
                        mailbox.await(i, left, TimeUnit.NANOSECONDS);

                        now = System.nanoTime();
                        left = deadline - now;
                        if (left <= 0) {
                            actor.record(1, "Actor", "receive", "%s timed out.", this);
                            throw new TimeoutException();
                        }
                    } else {
                        return null;
                    }
                } finally {
                    mailbox.unlock();
                }
            }
        }
    }

    /**
     * Performs a selective receive. This method blocks (but for no longer than the given timeout) until a message that is
     * {@link MessageProcessor#process(java.lang.Object) selected} by the given {@link MessageProcessor} is available in the mailbox,
     * and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <T>     The type of the returned value
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param proc    performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if the timeout expired.
     * @throws InterruptedException
     */
    public final <T> T receive(Timeout timeout, MessageProcessor<? super Message, T> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS, proc);
    }

    /**
     * Tries to perform a selective receive. If a message {@link MessageProcessor#process(java.lang.Object) selected} by
     * the given {@link MessageProcessor} is immediately available in the mailbox, returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * This method never blocks.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <T>  The type of the returned value
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if no message was slected.
     */
    public final <T> T tryReceive(MessageProcessor<? super Message, T> proc) {
        try {
            return receive(0, TimeUnit.NANOSECONDS, proc);
        } catch (TimeoutException e) {
            throw new AssertionError(e);
        } catch (SuspendExecution | InterruptedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Creates a {@link MessageProcessor} that selects messages of the given class.
     *
     * @param <M>
     * @param <Message>
     * @param type      The class of the messages to select.
     * @return a new {@link MessageProcessor} that selects messages of the given class.
     */
    public static <Message, M extends Message> MessageProcessor<Message, M> ofType(final Class<M> type) {
        return new MessageProcessor<Message, M>() {
            @Override
            public M process(Message m) {
                return type.isInstance(m) ? type.cast(m) : null;
            }
        };
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <M>     The type of the returned value
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit    timeout's time unit.
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(long timeout, TimeUnit unit, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return receive(timeout, unit, ofType(type));
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <M>     The type of the returned value
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(Timeout timeout, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS, type);
    }

    /**
     * Performs a selective receive based on type. This method blocks until a message of the given type is available in the mailbox,
     * and returns it.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <M>  The type of the returned value
     * @param type the type of the messages to select
     * @return The next message of the wanted type.
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(final Class<M> type) throws SuspendExecution, InterruptedException {
        try {
            return receive(0, null, type);
        } catch (TimeoutException ex) {
            throw new AssertionError();
        }
    }

    /**
     * Tries to performs a selective receive based on type. If a message of the given type is immediately found in the mailbox, it is returned.
     * Otherwise this method returns {@code null}.
     * This method never blocks.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <M>  The type of the returned value
     * @param type the type of the messages to select
     * @return The next message of the wanted type if immediately found; {@code null} otherwise.
     */
    public final <M extends Message> M tryReceive(final Class<M> type) {
        return tryReceive(ofType(type));
    }

    protected void handleLifecycleMessage(LifecycleMessage m) {
        actor.handleLifecycleMessage(m);
    }
}

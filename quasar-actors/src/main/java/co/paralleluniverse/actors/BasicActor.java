/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A subclass of {@link Actor} that provides selective receive capabilities.
 *
 * @author pron
 */
public abstract class BasicActor<Message, V> extends Actor<Message, V> {
    private final SelectiveReceiveHelper<Message> helper;

    /**
     * Creates a new actor.
     *
     * @param name          the actor name (may be {@code null}).
     * @param mailboxConfig the actor's mailbox settings; if {@code null}, the default config - unbounded mailbox - will be used.
     */
    public BasicActor(String name, MailboxConfig mailboxConfig) {
        super(name, mailboxConfig);
        this.helper = new SelectiveReceiveHelper<>(this);
    }

    /**
     * Creates a new, unnamed actor.
     *
     * @param mailboxConfig the actor's mailbox settings; if {@code null}, the default config - unbounded mailbox - will be used.
     */
    public BasicActor(MailboxConfig mailboxConfig) {
        this((String) null, mailboxConfig);
    }

    /**
     * Creates a new actor.
     * The default mailbox config - unbounded mailbox - will be used.
     *
     * @param name the actor name (may be {@code null}).
     */
    public BasicActor(String name) {
        this(name, null);
    }

    /**
     * Creates a new unnamed actor.
     */
    public BasicActor() {
        this((String) null, null);
    }

    public BasicActor(Strand strand, String name, MailboxConfig mailboxConfig) {
        super(strand, name, mailboxConfig);
        this.helper = new SelectiveReceiveHelper<>(this);
    }

    public BasicActor(Strand strand, MailboxConfig mailboxConfig) {
        this(strand, (String) null, mailboxConfig);
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
        return helper.receive(proc);
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
        return helper.receive(timeout, unit, proc);
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
        return helper.receive(timeout, proc);
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
        return helper.tryReceive(proc);
    }

    /**
     * Performs a selective receive based on type. This method blocks until a message of the given type is available in the mailbox,
     * and returns it.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param type the type of the messages to select
     * @return The next message of the wanted type.
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(final Class<M> type) throws SuspendExecution, InterruptedException {
        return helper.receive(SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit    timeout's time unit.
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(long timeout, TimeUnit unit, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, unit, SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @param type    the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <M extends Message> M receive(Timeout timeout, final Class<M> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, SelectiveReceiveHelper.ofType(type));
    }

    /**
     * Tries to performs a selective receive based on type. If a message of the given type is immediately found in the mailbox, it is returned.
     * Otherwise this method returns {@code null}.
     * This method never blocks.
     * <p>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.</p>
     *
     * @param <M>  The type of the returned message
     * @param type the type of the messages to select
     * @return The next message of the wanted type if immediately found; {@code null} otherwise.
     */
    public final <M extends Message> M tryReceive(final Class<M> type) {
        return helper.tryReceive(SelectiveReceiveHelper.ofType(type));
    }

    @Override
    public final String getName() {
        return (String) super.getName();
    }

    @Override
    protected Object readResolve() throws java.io.ObjectStreamException {
        Object x = super.readResolve();
        assert x == this;
        helper.setActor(this);
        return this;
    }
}

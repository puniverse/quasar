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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A subclass of {@link Actor} that provides selective receive capabilities.
 *
 * @author pron
 */
public abstract class BasicActor<Message, V> extends Actor<Message, V> {
    private final SelectiveReceiveHelper<Message> helper;

    public BasicActor(String name, MailboxConfig mailboxConfig) {
        super(name, mailboxConfig);
        this.helper = new SelectiveReceiveHelper<>(this);
    }

    public BasicActor(MailboxConfig mailboxConfig) {
        this((String) null, mailboxConfig);
    }

    public BasicActor(String name) {
        this(name, null);
    }

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
     * <p/>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.
     *
     * @param <T> The type of the returned value
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <T> T receive(MessageProcessor<Message, T> proc) throws SuspendExecution, InterruptedException {
        return helper.receive(proc);
    }

    /**
     * Performs a selective receive. This method blocks (but for no longer than the given timeout) until a message that is
     * {@link MessageProcessor#process(java.lang.Object) selected} by the given {@link MessageProcessor} is available in the mailbox,
     * and returns the value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}.
     * If the given timeout expires, this method returns {@code null}.
     * <p/>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.
     *
     * @param <T> The type of the returned value
     * @param timeout the duration to wait for a matching message to arrive.
     * @param unit timeout's time unit.
     * @param proc performs the selection.
     * @return The non-null value returned by {@link MessageProcessor#process(java.lang.Object) MessageProcessor.process}, or {@code null} if the timeout expired.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <T> T receive(long timeout, TimeUnit unit, MessageProcessor<Message, T> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return helper.receive(timeout, unit, proc);
    }

    /**
     * Performs a selective receive based on type. This method blocks until a message of the given type is available in the mailbox, 
     * and returns it.
     * <p/>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.
     *
     * @param <T> The type of the returned value
     * @param type the type of the messages to select
     * @return The next message of the wanted type.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <T extends Message> T receive(final Class<T> type) throws SuspendExecution, InterruptedException {
        return helper.receive(type);
    }

    /**
     * Performs a selective receive based on type. This method blocks (but for no longer than the given timeout) until a message of the given type
     * is available in the mailbox, and returns it. If the given timeout expires, this method returns {@code null}.
     * <p/>
     * Messages that are not selected, are temporarily skipped. They will remain in the mailbox until another call to receive (selective or
     * non-selective) retrieves them.
     *
     * @param <T> The type of the returned value
     * @param type the type of the messages to select
     * @return The next message of the wanted type, or {@code null} if the timeout expires.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <T extends Message> T receive(long timeout, TimeUnit unit, final Class<T> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, unit, type);
    }

    @Override
    public final String getName() {
        return (String) super.getName();
    }
}

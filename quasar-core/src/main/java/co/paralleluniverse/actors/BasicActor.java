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
     *
     * @param proc
     * @param timeout
     * @param unit
     * @throws TimeoutException
     * @throws LwtInterruptedException
     */
    public final Message receive(long timeout, TimeUnit unit, MessageProcessor<Message> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return helper.receive(timeout, unit, proc);
    }

    public final Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return helper.receive(proc);
    }

    public final <T extends Message> T receive(long timeout, TimeUnit unit, final Class<T> type) throws SuspendExecution, InterruptedException, TimeoutException {
        return helper.receive(timeout, unit, type);
    }

    public final <T extends Message> T receive(final Class<T> type) throws SuspendExecution, InterruptedException {
        return helper.receive(type);
    }

    @Override
    public final String getName() {
        return (String) super.getName();
    }
}

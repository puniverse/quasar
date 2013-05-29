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
public abstract class BasicActor<Message, V> extends LocalActor<Message, V> {
    private final SelectiveReceiveHelper<Message> helper;

    public BasicActor(String name, int mailboxSize) {
        super(name, mailboxSize);
        this.helper = new SelectiveReceiveHelper<>(this);
    }

    public BasicActor(int mailboxSize) {
        this((String) null, mailboxSize);
    }

    public BasicActor() {
        this((String) null, -1);
    }

    public BasicActor(Strand strand, String name, int mailboxSize) {
        super(strand, name, mailboxSize);
        this.helper = new SelectiveReceiveHelper<>(this);
    }

    public BasicActor(Strand strand, int mailboxSize) {
        this(strand, (String) null, mailboxSize);
    }

    /**
     *
     * @param proc
     * @param timeout
     * @param unit
     * @throws TimeoutException
     * @throws LwtInterruptedException
     */
    public Message receive(long timeout, TimeUnit unit, MessageProcessor<Message> proc) throws TimeoutException, SuspendExecution, InterruptedException {
        return helper.receive(timeout, unit, proc);
    }

    public Message receive(MessageProcessor<Message> proc) throws SuspendExecution, InterruptedException {
        return helper.receive(proc);
    }
}

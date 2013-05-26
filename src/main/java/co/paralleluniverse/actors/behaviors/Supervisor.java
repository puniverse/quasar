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

import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class Supervisor extends LocalActor<Void, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(Supervisor.class);

    public Supervisor(String name, int mailboxSize) {
        super(name, mailboxSize);
    }

    public Supervisor(Strand strand, String name, int mailboxSize) {
        super(strand, name, mailboxSize);
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        for (;;)
            receive(); // we care only about lifecycle messages
    }

    @Override
    protected void handleLifecycleMessage(LifecycleMessage m) {
        super.handleLifecycleMessage(m);
    }
}

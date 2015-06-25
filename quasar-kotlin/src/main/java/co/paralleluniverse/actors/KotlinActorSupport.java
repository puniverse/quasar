/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.strands.queues.SingleConsumerQueue;

/**
 * @author circlespainter
 */
public abstract class KotlinActorSupport<Message, V> extends BasicActor<Message, V> {
    // Needed to get access to these package-level facility in Kotlin's inlines

    protected void checkThrownIn1() {
        checkThrownIn0();
    }

    protected SingleConsumerQueue<Object> mailboxQueue() {
        return mailbox().queue();
    }
}

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
import co.paralleluniverse.strands.channels.SendChannel;

/**
 *
 * @author pron
 */
public interface Actor<Message> extends SendChannel<Message> {
    Object getName();

    boolean isDone();

    @Override
    void send(Message message) throws SuspendExecution;

    void sendSync(Message message) throws SuspendExecution;

    Actor link(Actor other);

    Actor unlink(Actor other);

    Object watch(Actor other);

    void unwatch(Actor other, Object listener);

    void interrupt();
}

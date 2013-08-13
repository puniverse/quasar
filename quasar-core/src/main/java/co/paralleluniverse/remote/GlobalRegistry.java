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
package co.paralleluniverse.remote;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.fibers.SuspendExecution;

/**
 *
 * @author pron
 */
public interface GlobalRegistry {
    Object register(ActorRef<?> actor) throws SuspendExecution;

    void unregister(ActorRef<?> actor) throws SuspendExecution;
    
    <Message> ActorRef<Message> getActor(String name) throws SuspendExecution;
}

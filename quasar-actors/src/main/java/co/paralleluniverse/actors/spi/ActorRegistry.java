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
package co.paralleluniverse.actors.spi;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public interface ActorRegistry {
    Object register(ActorRef<?> actor, Object globalId) throws SuspendExecution;

    void unregister(ActorRef<?> actor);

    <Message> ActorRef<Message> getActor(String name) throws InterruptedException, SuspendExecution;

    <Message> ActorRef<Message> getActor(String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution;

    <Message> ActorRef<Message> tryGetActor(String name) throws SuspendExecution;

    <Message> ActorRef<Message> getOrRegisterActor(String name, Callable<ActorRef<Message>> factory) throws SuspendExecution;

    void shutdown();
}

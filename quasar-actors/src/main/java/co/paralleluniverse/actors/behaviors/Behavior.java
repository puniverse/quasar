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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;

/**
 * A general behavior-actor interface
 *
 * @author pron
 */
public class Behavior extends ActorRefDelegate<Object> {
    protected Behavior(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Asks this actor to shut down. Works by sending a {@link ShutdownMessage} via {@link ActorUtil#sendOrInterrupt(ActorRef, Object) ActorUtil.sendOrInterrupt}.
     */
    public void shutdown() {
        final ShutdownMessage message = new ShutdownMessage(LocalActor.self());
        ActorUtil.sendOrInterrupt(this, message);
    }
}

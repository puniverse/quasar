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

/**
 *
 * @author pron
 */
public class GenBehavior extends ActorRefDelegate<Object> implements java.io.Serializable {
    public GenBehavior(ActorRef<Object> actor) {
        super(actor);
    }

    public void shutdown() {
        final ShutdownMessage message = new ShutdownMessage(Actor.self());
        ((ActorRefImpl)ref).sendOrInterrupt(message);
    }

    public void close() {
        throw new UnsupportedOperationException();
    }
}

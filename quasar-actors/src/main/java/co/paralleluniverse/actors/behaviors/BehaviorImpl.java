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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;

class BehaviorImpl extends ActorRefDelegate<Object> implements Behavior, java.io.Serializable {
    protected BehaviorImpl(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Asks this actor to shut down. Works by sending a {@link ShutdownMessage} via {@link ActorUtil#sendOrInterrupt(ActorRef, Object) ActorUtil.sendOrInterrupt}.
     */
    @Override
    public void shutdown() {
        final ShutdownMessage message = new ShutdownMessage(LocalActor.self());
        ActorUtil.sendOrInterrupt(ref, message);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    static final class Local extends BehaviorImpl implements LocalBehavior<BehaviorImpl> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public BehaviorImpl writeReplace() throws java.io.ObjectStreamException {
            return new BehaviorImpl(ref);
        }
    }
}

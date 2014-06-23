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

package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;

/**
 *
 * @author pron
 */
class LostActor extends ActorRefImpl<Object> {
    public static final ActorRefImpl<Object> instance = new LostActor();
    
    private final ActorRef<Object> ref = new ActorRef<Object>(this);
    
    private LostActor() {
        super("Lost", null);
    }

    @Override
    public ActorRef<Object> ref() {
        return ref;
    }

    @Override
    void interrupt() {
    }

    @Override
    protected boolean trySend(Object message) {
        record(1, "LostActor", "trySend", "Message: %s" , message);
        return true;
    }

    @Override
    protected void internalSend(Object message) throws SuspendExecution {
        record(1, "LostActor", "internalSend", "Message: %s" , message);
    }

    @Override
    protected void internalSendNonSuspendable(Object message) {
        record(1, "LostActor", "internalSendNonSuspendable", "Message: %s" , message);
    }

    @Override
    protected void throwIn(RuntimeException e) {
        record(1, "LostActor", "throwIn", "Exception: %s" , e);
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void removeObserverListeners(ActorRef actor) {
        throw new UnsupportedOperationException();
    }
    
}

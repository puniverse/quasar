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

import co.paralleluniverse.actors.RemoteActor.RemoteActorRegisterListenerAdminMessage;
import co.paralleluniverse.actors.RemoteActor.RemoteActorUnregisterListenerAdminMessage;

/**
 * Handles {@link LifecycleListener} registration on a remote actor.
 * 
 * @author pron
 */
public abstract class LifecycleListenerProxy {
    public void addLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorRegisterListenerAdminMessage(listener));
    }

    public void removeLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(listener));
    }

    public void removeLifecycleListeners(RemoteActor actor, ActorRef observer) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(observer));
    }
    
    protected static ActorImpl getImpl(ActorRef<?> actor) {
        return actor.getImpl();
    }
}

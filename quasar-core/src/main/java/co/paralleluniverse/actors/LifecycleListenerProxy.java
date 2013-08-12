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

import co.paralleluniverse.actors.RemoteActorRef.RemoteActorRegisterListenerAdminMessage;
import co.paralleluniverse.actors.RemoteActorRef.RemoteActorUnregisterListenerAdminMessage;

/**
 *
 * @author pron
 */
abstract public class LifecycleListenerProxy {
    public void addLifecycleListener(RemoteActorRef actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorRegisterListenerAdminMessage((ActorRefImpl.ActorLifecycleListener) listener));
    }

    public void removeLifecycleListener(RemoteActorRef actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(listener));
    }

    public void removeLifecycleListeners(RemoteActorRef actor, ActorRefImpl observer) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(observer));
    }
}

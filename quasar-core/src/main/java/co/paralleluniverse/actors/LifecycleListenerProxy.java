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

import co.paralleluniverse.actors.RemoteActor.RemoteActorRegisterListenerAdminMessage;
import co.paralleluniverse.actors.RemoteActor.RemoteActorUnregisterListenerAdminMessage;

/**
 *
 * @author pron
 */
abstract public class LifecycleListenerProxy {
    public void addLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorRegisterListenerAdminMessage((ActorImpl.ActorLifecycleListener) listener));
    }

    public void removeLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(listener));
    }

    public void removeLifecycleListeners(RemoteActor actor, ActorImpl observer) {
        actor.internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(observer));
    }
}

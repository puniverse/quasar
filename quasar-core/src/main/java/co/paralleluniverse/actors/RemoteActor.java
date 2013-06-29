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

/**
 *
 * @author pron
 */
public class RemoteActor<Message> extends ActorImpl<Message> {
    private final transient LocalActor<Message, ?> actor;

    public RemoteActor(LocalActor<Message, ?> actor) {
        super(actor.getName(), actor.mailbox());
        this.actor = actor;
    }

    protected void handleAdminMessage(RemoteActorAdminMessage msg) {
        if (msg instanceof RemoteActorListenerAdminMessage) {
            final RemoteActorListenerAdminMessage m = (RemoteActorListenerAdminMessage) msg;
            if (m.isRegister())
                actor.addLifecycleListener(m.getListener());
            else
                actor.removeLifecycleListener(m.getListener());
        } else if (msg instanceof RemoteActorInterruptAdminMessage) {
            actor.interrupt();
        } else if (msg instanceof RemoteActorThrowInAdminMessage) {
            actor.throwIn(((RemoteActorThrowInAdminMessage)msg).getException());
        }
    }

    @Override
    protected void internalSend(Object message) throws SuspendExecution {
        actor.internalSend(message);
    }

    @Override
    protected void internalSendNonSuspendable(Object message) {
        actor.internalSendNonSuspendable(message);
    }

    @Override
    public boolean trySend(Message message) {
        return actor.trySend(message);
    }
    
    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        internalSendNonSuspendable(new RemoteActorListenerAdminMessage((ActorLifecycleListener) listener, true));
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        internalSendNonSuspendable(new RemoteActorListenerAdminMessage((ActorLifecycleListener) listener, false));
    }

    @Override
    protected void throwIn(RuntimeException e) {
        internalSendNonSuspendable(new RemoteActorThrowInAdminMessage(e));
    }

    @Override
    public void interrupt() {
        internalSendNonSuspendable(new RemoteActorInterruptAdminMessage());
    }

    protected static abstract class RemoteActorAdminMessage implements java.io.Serializable {
    }

    private static class RemoteActorListenerAdminMessage extends RemoteActorAdminMessage {
        private final ActorLifecycleListener listener;
        private final boolean register;

        public RemoteActorListenerAdminMessage(ActorLifecycleListener listener, boolean register) {
            this.listener = listener;
            this.register = register;
        }

        public ActorLifecycleListener getListener() {
            return listener;
        }

        public boolean isRegister() {
            return register;
        }
    }

    private static class RemoteActorInterruptAdminMessage extends RemoteActorAdminMessage {
    }

    private static class RemoteActorThrowInAdminMessage extends RemoteActorAdminMessage {
        private RuntimeException e;

        public RemoteActorThrowInAdminMessage(RuntimeException e) {
            this.e = e;
        }

        public RuntimeException getException() {
            return e;
        }
    }
}

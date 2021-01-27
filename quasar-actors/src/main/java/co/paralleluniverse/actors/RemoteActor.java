/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.remote.RemoteChannelProxyFactoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class should be extended by implementations of remote actors.
 *
 * @author pron
 */
public abstract class RemoteActor<Message> extends ActorImpl<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteActor.class);
    private final transient ActorImpl<Message> actor;

    protected RemoteActor(ActorRef<Message> actor) {
        super(actor.getName(),
                RemoteChannelProxyFactoryService.create(actor.getImpl().mailbox(), ((Actor) actor.getImpl()).getGlobalId()),
                actor);
        this.actor = actor.getImpl();
    }

    protected void handleAdminMessage(RemoteActorAdminMessage msg) {
        if (msg instanceof RemoteActorRegisterListenerAdminMessage) {
            final RemoteActorRegisterListenerAdminMessage reg = (RemoteActorRegisterListenerAdminMessage)msg;
            if (reg.isLink())
                actor.linked(((ActorLifecycleListener)reg.getListener()).getObserver());
            else
                actor.addLifecycleListener(reg.getListener());
        } else if (msg instanceof RemoteActorUnregisterListenerAdminMessage) {
            final RemoteActorUnregisterListenerAdminMessage unreg = (RemoteActorUnregisterListenerAdminMessage) msg;
            if (unreg.isLink())
                actor.unlinked(((ActorLifecycleListener)unreg.getListener()).getObserver());
            else if (unreg.getObserver() != null)
                actor.removeObserverListeners(unreg.getObserver());
            else
                actor.removeLifecycleListener(unreg.getListener());
        } else if (msg instanceof RemoteActorInterruptAdminMessage) {
            actor.interrupt();
        } else if (msg instanceof RemoteActorThrowInAdminMessage) {
            actor.throwIn(((RemoteActorThrowInAdminMessage) msg).getException());
        }
    }

    public ActorImpl<Message> getActor() {
        return actor;
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
        internalSendNonSuspendable(message);
        return true;
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        internalSendNonSuspendable(new RemoteActorRegisterListenerAdminMessage(listener, false));
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(listener, false));
    }

    @Override
    protected void linked(ActorRef actor) {
        internalSendNonSuspendable(new RemoteActorRegisterListenerAdminMessage(getActorRefImpl(actor).getLifecycleListener(), true));
    }

    @Override
    protected void unlinked(ActorRef actor) {
        internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(getActorRefImpl(actor).getLifecycleListener(), true));
    }

    @Override
    protected void removeObserverListeners(ActorRef observer) {
        internalSendNonSuspendable(new RemoteActorUnregisterListenerAdminMessage(observer, false));
    }

    @Override
    protected void throwIn(RuntimeException e) {
        internalSendNonSuspendable(new RemoteActorThrowInAdminMessage(e));
    }

    @Override
    public void interrupt() {
        internalSendNonSuspendable(new RemoteActorInterruptAdminMessage());
    }

    protected static ActorImpl getImpl(ActorRef<?> actor) {
        return actor.getImpl();
    }

    protected static abstract class RemoteActorAdminMessage implements java.io.Serializable {
    }

    private static class RemoteActorRegisterListenerAdminMessage extends RemoteActorAdminMessage {
        private final LifecycleListener listener;
        private final boolean link;

        public RemoteActorRegisterListenerAdminMessage(LifecycleListener listener, boolean link) {
            this.listener = listener;
            this.link = link;
        }

        public LifecycleListener getListener() {
            return listener;
        }

        public boolean isLink() {
            return link;
        }

        @Override
        public String toString() {
            return "RemoteActorListenerAdminMessage{" + "listener=" + listener + ", link=" + link + '}';
        }
    }

    private static class RemoteActorUnregisterListenerAdminMessage extends RemoteActorAdminMessage {
        private final ActorRef observer;
        private final LifecycleListener listener;
        private final boolean link;

        public RemoteActorUnregisterListenerAdminMessage(ActorRef observer, boolean link) {
            this.observer = observer;
            this.listener = null;
            this.link = link;
        }

        public RemoteActorUnregisterListenerAdminMessage(LifecycleListener listener, boolean link) {
            this.listener = listener;
            this.observer = null;
            this.link = link;
        }

        public ActorRef getObserver() {
            return observer;
        }

        public LifecycleListener getListener() {
            return listener;
        }

        public boolean isLink() {
            return link;
        }

        @Override
        public String toString() {
            return "RemoteActorUnregisterListenerAdminMessage{" + "observer=" + observer + ", listener=" + listener + ", link=" + link + '}';
        }
    }

    private static class RemoteActorInterruptAdminMessage extends RemoteActorAdminMessage {
    }

    private static class RemoteActorThrowInAdminMessage extends RemoteActorAdminMessage {
        private final RuntimeException e;

        public RemoteActorThrowInAdminMessage(RuntimeException e) {
            this.e = e;
        }

        public RuntimeException getException() {
            return e;
        }
    }
}

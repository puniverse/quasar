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

import co.paralleluniverse.common.util.ServiceUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class should be extended by implementations of remote actors.
 *
 * @author pron
 */
public class RemoteActor<Message> extends ActorRefImpl<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteActor.class);
    private static LifecycleListenerProxy lifecycleListenerProxy = ServiceUtil.loadSingletonService(LifecycleListenerProxy.class);
    private final transient ActorRefImpl<Message> actor;

    protected RemoteActor(ActorRef<Message> actor) {
        super(actor.getName(), actor.getImpl().mailbox());
        this.actor = actor.getImpl();
    }

    protected void handleAdminMessage(RemoteActorAdminMessage msg) {
        if (msg instanceof RemoteActorRegisterListenerAdminMessage) {
            final RemoteActorRegisterListenerAdminMessage m = (RemoteActorRegisterListenerAdminMessage) msg;
            actor.addLifecycleListener(((RemoteActorRegisterListenerAdminMessage) msg).getListener());
        } else if (msg instanceof RemoteActorUnregisterListenerAdminMessage) {
            RemoteActorUnregisterListenerAdminMessage unreg = (RemoteActorUnregisterListenerAdminMessage) msg;
            if (unreg.getObserver() != null)
                actor.removeObserverListeners(unreg.getObserver());
            else
                actor.removeLifecycleListener(unreg.getListener());
        } else if (msg instanceof RemoteActorInterruptAdminMessage) {
            actor.interrupt();
        } else if (msg instanceof RemoteActorThrowInAdminMessage) {
            actor.throwIn(((RemoteActorThrowInAdminMessage) msg).getException());
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
        lifecycleListenerProxy.addLifecycleListener(this, listener);
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        lifecycleListenerProxy.removeLifecycleListener(this, listener);
    }

    @Override
    protected void removeObserverListeners(ActorRef actor) {
        lifecycleListenerProxy.removeLifecycleListeners(this, actor);
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

    static class RemoteActorRegisterListenerAdminMessage extends RemoteActorAdminMessage {
        private final ActorLifecycleListener listener;

        @Override
        public String toString() {
            return "RemoteActorListenerAdminMessage{" + "listener=" + listener + '}';
        }

        public RemoteActorRegisterListenerAdminMessage(ActorLifecycleListener listener) {
            this.listener = listener;
        }

        public ActorLifecycleListener getListener() {
            return listener;
        }
    }

    static class RemoteActorUnregisterListenerAdminMessage extends RemoteActorAdminMessage {
        private final ActorRef observer;
        private final LifecycleListener listener;

        public RemoteActorUnregisterListenerAdminMessage(ActorRef observer) {
            this.observer = observer;
            this.listener = null;
        }

        public RemoteActorUnregisterListenerAdminMessage(LifecycleListener listener) {
            this.listener = listener;
            this.observer = null;
        }

        public ActorRef getObserver() {
            return observer;
        }

        public LifecycleListener getListener() {
            return listener;
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

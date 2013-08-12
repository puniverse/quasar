/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.remote.RemoteProxyFactoryService;
import co.paralleluniverse.strands.channels.SendPort;

/**
 *
 * @author pron
 */
class LocalActorRef<Message> extends ActorRefImpl<Message> implements java.io.Serializable {
    private final Actor<Message, ?> actor;

    public LocalActorRef(Actor<Message, ?> actor, String name, SendPort<Object> mailbox) {
        super(name, mailbox);
        this.actor = actor;
    }

    @Override
    public boolean trySend(Message message) {
        return actor.trySend(message);
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
    public void sendSync(Message message) throws SuspendExecution {
        actor.sendSync(message);
    }

    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        actor.addLifecycleListener(listener);
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        actor.addLifecycleListener(listener);
    }

    @Override
    protected void removeObserverListeners(ActorRefImpl actor) {
        actor.addLifecycleListener(null);
    }

    @Override
    protected void throwIn(RuntimeException e) {
        actor.throwIn(e);
    }

    @Override
    public void interrupt() {
        actor.interrupt();
    }

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    protected Object writeReplace() throws java.io.ObjectStreamException {
        final RemoteActorRef<Message> repl = RemoteProxyFactoryService.create((ActorRef<Message>)this, actor.getGlobalId());
        return repl;
    }
    //</editor-fold>
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;

/**
 *
 * @author pron
 */
class LocalActorRef<Message, V> extends ActorRefImpl<Message> implements ActorBuilder<Message, V>, java.io.Serializable {
    private Actor<Message, V> actor;

    public LocalActorRef(Actor<Message, V> actor, String name, SendPort<Object> mailbox) {
        super(name, mailbox);
        this.actor = actor;
    }

    Actor<Message, ?> getActor() {
        return actor;
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
        actor.removeLifecycleListener(listener);
    }

    @Override
    protected void removeObserverListeners(ActorRef observer) {
        actor.removeObserverListeners(observer);
    }

    @Override
    protected void throwIn(RuntimeException e) {
        actor.throwIn(e);
    }

    @Override
    public void interrupt() {
        actor.interrupt();
    }

    //////////////////////////////////////
    @Override
    public final Actor<Message, V> build() {
        if (!actor.isDone())
            throw new IllegalStateException("Actor " + this + " isn't dead. Cannot build a copy");

        final Actor newInstance = actor.reinstantiate();

        if (newInstance.getName() == null)
            newInstance.setName(this.getName());
        newInstance.setStrand(null);

        ActorMonitor monitor = actor.getMonitor();
        newInstance.setMonitor(monitor);
        if (getName() != null && ActorRegistry.getActor(getName()) == this)
            newInstance.register();
        return newInstance;
    }

    //<editor-fold desc="Serialization">
    /////////// Serialization ///////////////////////////////////
    protected Object writeReplace() throws java.io.ObjectStreamException {
        final RemoteActorRef<Message> repl = RemoteActorProxyFactoryService.create((ActorRef<Message>) this, actor.getGlobalId());
        return repl;
    }
    //</editor-fold>

    @Override
    public String toString() {
        return "LocalActorRef{" + "actor: " + actor + '}';
    }
}

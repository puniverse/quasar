/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
final class LocalActorRef<Message, V> extends ActorRefImpl<Message> implements ActorBuilder<Message, V>, Joinable<V>, java.io.Serializable {
    private volatile Actor<Message, V> actor;

    public LocalActorRef(String name, SendPort<Object> mailbox) {
        super(name, mailbox);
    }

    final Actor<Message, ?> getActor() {
        return actor;
    }

    final void setActor(Actor<Message, V> actor) {
        this.actor = actor;
        if(mailbox() instanceof Mailbox)
            ((Mailbox)mailbox()).setActor(actor);
    }
    
    @Override
    public final boolean trySend(Message message) {
        return actor.trySend(message);
    }

    @Override
    protected final void internalSend(Object message) throws SuspendExecution {
        actor.internalSend(message);
    }

    @Override
    protected final void internalSendNonSuspendable(Object message) {
        actor.internalSendNonSuspendable(message);
    }

    @Override
    public final void sendSync(Message message) throws SuspendExecution {
        actor.sendSync(message);
    }

    @Override
    protected final void addLifecycleListener(LifecycleListener listener) {
        actor.addLifecycleListener(listener);
    }

    @Override
    protected final void removeLifecycleListener(LifecycleListener listener) {
        actor.removeLifecycleListener(listener);
    }

    @Override
    protected final void removeObserverListeners(ActorRef observer) {
        actor.removeObserverListeners(observer);
    }

    @Override
    protected final void throwIn(RuntimeException e) {
        actor.throwIn(e);
    }

    @Override
    public final void interrupt() {
        actor.interrupt();
    }

    /////////// Joinable ///////////////////////////////////
    @Override
    public final void join() throws ExecutionException, InterruptedException {
        actor.join();
    }

    @Override
    public final void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        actor.join(timeout, unit);
    }

    @Override
    public final V get() throws ExecutionException, InterruptedException {
        return actor.get();
    }

    @Override
    public final V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return actor.get(timeout, unit);
    }

    @Override
    public final boolean isDone() {
        return actor.isDone();
    }

    /////////// ActorBuilder ///////////////////////////////////
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

    /////////// Serialization ///////////////////////////////////
    protected final Object writeReplace() throws java.io.ObjectStreamException {
        final RemoteActorRef<Message> repl = RemoteActorProxyFactoryService.create((ActorRef<Message>) this, actor.getGlobalId());
        return repl;
    }

    @Override
    public String toString() {
        return "LocalActorRef{" + "actor: " + actor + '}';
    }
}

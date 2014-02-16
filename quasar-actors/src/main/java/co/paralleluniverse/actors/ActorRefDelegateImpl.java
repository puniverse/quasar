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

import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import co.paralleluniverse.strands.channels.SendPort;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An {@link ActorRef} which delegates all operations to another {@code ActorRef}.
 *
 * @author pron
 */
public abstract class ActorRefDelegateImpl<Message> implements ActorRef<Message>, ActorRefDelegate<Message>, SendPort<Message>, java.io.Serializable {
    private final ActorRef<Message> ref;

    /**
     * Constructs an {@code ActorRefDelegate}
     *
     * @param ref the {@link ActorRef} to which all operations will be delegated
     */
    public ActorRefDelegateImpl(ActorRef<Message> ref) {
        this.ref = ref;
    }

    @Override
    public ActorRef<Message> getRef() {
        return ref;
    }
    
    protected boolean isInActor() {
        return Objects.equals(ref, LocalActor.self());
    }

    @Override
    public String getName() {
        return ref.getName();
    }

    @Override
    public void send(Message message) throws SuspendExecution {
        ref.send(message);
    }

    @Override
    public void sendSync(Message message) throws SuspendExecution {
        ref.sendSync(message);
    }

    @Override
    public void interrupt() {
        ref.interrupt();
    }

    @Override
    public boolean send(Message message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return ((SendPort<Message>) ref).send(message, timeout, unit);
    }

    @Override
    public boolean send(Message message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return ((SendPort<Message>) ref).send(message, timeout);
    }

    @Override
    public boolean trySend(Message message) {
        return ((SendPort<Message>) ref).trySend(message);
    }

    public void sendOrInterrupt(Object message) {
        ((ActorRefImpl) stripDelegates(ref)).sendOrInterrupt(message);
    }

    @Override
    public void close() {
        ((SendPort<Message>) ref).close();
    }

    @Override
    public void close(Throwable t) {
        ((SendPort<Message>) ref).close(t);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ref);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof ActorRef))
            return false;
        ActorRef other = (ActorRef) obj;
        while (other instanceof ActorRefDelegateImpl)
            other = ((ActorRefDelegateImpl) other).getRef();
        ActorRef me = ref;
        while (me instanceof ActorRefDelegateImpl)
            me = ((ActorRefDelegateImpl) me).getRef();
        if (!Objects.equals(me, other))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    // The following will throw an exception if the actor is not local.
    // However, if the ref is known to be local, a subclass can simply 
    // declare to implement ActorBuilder and Joinable, and the implementations are already provided here
    
    public Actor<Object, Void> build() {
        return ((ActorBuilder<Object, Void>) ref).build();
    }

    public void join() throws ExecutionException, InterruptedException {
        ((Joinable<Void>) ref).join();
    }

    public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        ((Joinable<Void>) ref).join(timeout, unit);
    }

    public Void get() throws ExecutionException, InterruptedException {
        return ((Joinable<Void>) ref).get();
    }

    public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return ((Joinable<Void>) ref).get(timeout, unit);
    }

    public boolean isDone() {
        return ((Joinable<Void>) ref).isDone();
    }
    
    static <M> ActorRef<M> stripDelegates(ActorRef<M> ar) {
        while (ar instanceof ActorRefDelegate)
            ar = ((ActorRefDelegate) ar).getRef();
        return ar;
    }
}

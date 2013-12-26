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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ActorRef;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An interface to a {@link SupervisorActor}.
 *
 * @author pron
 */
public class SupervisorImpl extends BehaviorImpl implements Supervisor {
    /**
     * If {@code actor} is known to be a {@link ServerActor}, creates a new {@link Server} interface to it.
     * Normally, you don't use this constructor, but the {@code Supervisor} instance returned by {@link SupervisorActor#spawn() }.
     *
     * @param actor a {@link ServerActor}
     */
    public SupervisorImpl(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Adds a new child actor to the supervisor. If the child has not been started, it will be started by the supervisor.
     *
     * @param spec the {@link ChildSpec child's spec}.
     * @return the actor (possibly after it has been started by the supervisor).
     * @throws InterruptedException
     */
    @Override
    public final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().addChild(spec);

        return (T) call(this, new AddChildMessage(RequestReplyHelper.from(), null, spec));
    }

    /**
     * Retrieves a child actor by its {@link ChildSpec#getId() id}
     *
     * @param id the child's {@link ChildSpec#getId() id} in the supervisor.
     * @return the child, if found; {@code null} if the child was not found
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    @Override
    public final <T extends ActorRef<M>, M> T getChild(Object id) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().getChild(id);

        return (T) call(this, new GetChildMessage(RequestReplyHelper.from(), null, id));
    }

    /**
     * Removes a child actor from the supervisor.
     *
     * @param id        the child's {@link ChildSpec#getId() id} in the supervisor.
     * @param terminate whether or not the supervisor should terminate the actor
     * @return {@code true} if the actor has been successfully removed from the supervisor; {@code false} if the child was not found.
     * @throws InterruptedException
     */
    @Override
    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().removeChild(id, terminate);

        return (Boolean) call(this, new RemoveChildMessage(RequestReplyHelper.from(), null, id, terminate));
    }

    @Override
    public String toString() {
        return "Supervisor{" + super.toString() + "}";
    }

    ///////// Messages
    static class AddChildMessage extends RequestMessage {
        final ChildSpec spec;

        public AddChildMessage(ActorRef from, Object id, ChildSpec info) {
            super(from, id);
            this.spec = info;
        }

        @Override
        protected String contentString() {
            return super.contentString() + " spec: " + spec;
        }
    }

    static class GetChildMessage extends RequestMessage {
        final Object name;

        public GetChildMessage(ActorRef from, Object id, Object name) {
            super(from, id);
            this.name = name;
        }

        @Override
        protected String contentString() {
            return super.contentString() + " name: " + name;
        }
    }

    static class RemoveChildMessage extends RequestMessage {
        final Object name;
        final boolean terminate;

        public RemoveChildMessage(ActorRef from, Object id, Object name, boolean terminate) {
            super(from, id);
            this.name = name;
            this.terminate = terminate;
        }

        @Override
        protected String contentString() {
            return super.contentString() + " name: " + name + " terminate: " + terminate;
        }
    }

    static final class Local extends SupervisorImpl implements LocalBehavior<SupervisorImpl> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public SupervisorImpl writeReplace() throws java.io.ObjectStreamException {
            return new SupervisorImpl(ref);
        }

        @Override
        public Actor<Object, Void> build() {
            return ((ActorBuilder<Object, Void>) ref).build();
        }

        @Override
        public void join() throws ExecutionException, InterruptedException {
            ((Joinable<Void>) ref).join();
        }

        @Override
        public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            ((Joinable<Void>) ref).join(timeout, unit);
        }

        @Override
        public Void get() throws ExecutionException, InterruptedException {
            return ((Joinable<Void>) ref).get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            return ((Joinable<Void>) ref).get(timeout, unit);
        }

        @Override
        public boolean isDone() {
            return ((Joinable<Void>) ref).isDone();
        }
    }
}

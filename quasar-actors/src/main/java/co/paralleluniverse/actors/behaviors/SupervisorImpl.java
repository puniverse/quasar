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

import co.paralleluniverse.actors.ActorRef;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.SuspendExecution;

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

    @Override
    public final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().addChild(spec);

        return (T) call(this, new AddChildMessage(RequestReplyHelper.from(), null, spec));
    }

    @Override
    public final <T extends ActorRef<M>, M> T getChild(Object id) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().getChild(id);

        return (T) call(this, new GetChildMessage(RequestReplyHelper.from(), null, id));
    }

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
            return new SupervisorImpl(getRef());
        }
    }
}

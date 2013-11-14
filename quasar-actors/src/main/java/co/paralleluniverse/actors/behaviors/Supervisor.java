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
import co.paralleluniverse.actors.LocalActorUtil;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public class Supervisor extends GenBehavior {
    Supervisor(ActorRef<Object> actor) {
        super(actor);
    }

    public final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().addChild(spec);

        return (T) call(this, new AddChildMessage(RequestReplyHelper.from(), null, spec));
    }

    public final <T extends ActorRef<M>, M> T getChild(Object id) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().getChild(id);

        return (T) call(this, new GetChildMessage(RequestReplyHelper.from(), null, id));
    }

    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().removeChild(id, terminate);

        return (Boolean) call(this, new RemoveChildMessage(RequestReplyHelper.from(), null, id, terminate));
    }

    public enum ChildMode {
        PERMANENT, TRANSIENT, TEMPORARY
    };

    public static class ChildSpec {
        final String id;
        final ActorBuilder<?, ?> builder;
        final ChildMode mode;
        final int maxRestarts;
        final long duration;
        final TimeUnit unit;
        final long shutdownDeadline;

        public ChildSpec(String id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorBuilder<?, ?> builder) {
            this.id = id;
            this.builder = builder;
            this.mode = mode;
            this.maxRestarts = maxRestarts;
            this.duration = duration;
            this.unit = unit;
            this.shutdownDeadline = shutdownDeadline;
        }

        public ChildSpec(String id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorRef<?> actor) {
            this(id, mode, maxRestarts, duration, unit, shutdownDeadline, LocalActorUtil.toActorBuilder(actor));
        }

        public Object getId() {
            return id;
        }

        public ActorBuilder<?, ?> getBuilder() {
            return builder;
        }

        public ChildMode getMode() {
            return mode;
        }

        public int getMaxRestarts() {
            return maxRestarts;
        }

        public long getDuration() {
            return duration;
        }

        public TimeUnit getDurationUnit() {
            return unit;
        }

        public long getShutdownDeadline() {
            return shutdownDeadline;
        }

        @Override
        public String toString() {
            return "ChildSpec{" + "builder: " + builder + ", mode: " + mode + ", maxRestarts: " + maxRestarts + ", duration: " + duration + ", unit: " + unit + ", shutdownDeadline: " + shutdownDeadline + '}';
        }
    }

    ///////// Messages
    static class AddChildMessage extends GenRequestMessage {
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

    static class GetChildMessage extends GenRequestMessage {
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

    static class RemoveChildMessage extends GenRequestMessage {
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

    static final class Local extends Supervisor implements LocalBehavior<Supervisor> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public Supervisor writeReplace() throws java.io.ObjectStreamException {
            return new Supervisor(ref);
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

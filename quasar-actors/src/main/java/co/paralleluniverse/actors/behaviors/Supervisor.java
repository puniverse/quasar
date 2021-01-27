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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An interface to a {@link SupervisorActor}.
 *
 * @author pron
 */
public class Supervisor extends Behavior {
    /**
     * If {@code actor} is known to be a {@link ServerActor}, creates a new {@link Server} interface to it.
     * Normally, you don't use this constructor, but the {@code Supervisor} instance returned by {@link SupervisorActor#spawn() }.
     *
     * @param actor a {@link ServerActor}
     */
    public Supervisor(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Adds a new child actor to the supervisor. If the child has not been started, it will be started by the supervisor.
     * This method does not block when called from within the Supervisor's context, so, in particular, it may be called by
     * an actor constructor, constructed by the supervisor.
     *
     * @param spec the {@link ChildSpec child's spec}.
     * @return the actor (possibly after it has been started by the supervisor).
     * @throws InterruptedException
     */
    public final <T extends ActorRef<M>, M> T addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().addChild(spec);

        return (T) call(this, new AddChildMessage(RequestReplyHelper.from(), null, spec));
    }

    /**
     * Retrieves a child actor by its {@link ChildSpec#getId() id}
     * This method does not block when called from within the Supervisor's context, so, in particular, it may be called by
     * an actor constructor, constructed by the supervisor.
     *
     * @param id the child's {@link ChildSpec#getId() id} in the supervisor.
     * @return the child, if found; {@code null} if the child was not found
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final <T extends ActorRef<M>, M> T getChild(Object id) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return (T) SupervisorActor.currentSupervisor().getChild(id);

        return (T) call(this, new GetChildMessage(RequestReplyHelper.from(), null, id));
    }

    /**
     * Retrieves the children actor references as an immutable list.
     * This method does not block when called from within the Supervisor's context, so, in particular, it may be called by
     * an actor constructor, constructed by the supervisor.
     *
     * @return the children {@link ActorRef}s.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    public final List<? extends ActorRef<?>> getChildren() throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().getChildren();

        return (List) call(this, new GetChildrenMessage(RequestReplyHelper.from(), null));
    }
    /**
     * Removes a child actor from the supervisor.
     * This method does not block when called from within the Supervisor's context, so, in particular, it may be called by
     * an actor constructor, constructed by the supervisor.
     *
     * @param id        the child's {@link ChildSpec#getId() id} in the supervisor.
     * @param terminate whether or not the supervisor should terminate the actor
     * @return {@code true} if the actor has been successfully removed from the supervisor; {@code false} if the child was not found.
     * @throws InterruptedException
     */
    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().removeChild(id, terminate);

        return (Boolean) call(this, new RemoveChildMessage(RequestReplyHelper.from(), null, id, terminate));
    }

    /**
     * Removes a child actor from the supervisor.
     * This method does not block when called from within the Supervisor's context, so, in particular, it may be called by
     * an actor constructor, constructed by the supervisor.
     *
     * @param actor     the child actor
     * @param terminate whether or not the supervisor should terminate the actor
     * @return {@code true} if the actor has been successfully removed from the supervisor; {@code false} if the child was not found.
     * @throws InterruptedException
     */
    public boolean removeChild(ActorRef<?> actor, boolean terminate) throws SuspendExecution, InterruptedException {
        if (isInActor())
            return SupervisorActor.currentSupervisor().removeChild(actor, terminate);

        return (Boolean) call(this, new RemoveChildMessage(RequestReplyHelper.from(), null, actor, terminate));
    }

    /**
     * Determines whether a child (supervised) actor should be restarted if the supervisor's {@link SupervisorActor.RestartStrategy restart strategy}
     * states that it should be restarted.
     */
    public enum ChildMode {
        /**
         * The child actor should be restarted if it dies for whatever reason if the supervisor's {@link SupervisorActor.RestartStrategy restart strategy}
         * states that it should be restarted.
         */
        PERMANENT,
        /**
         * The child actor should be restarted if it dies of unnatural causes (an exception) if the supervisor's {@link SupervisorActor.RestartStrategy restart strategy}
         * states that it should be restarted.
         */
        TRANSIENT,
        /**
         * The child actor should never be restarted.
         */
        TEMPORARY
    };

    /**
     * Describes a child actor's configuration in a supervisor
     */
    public static class ChildSpec {
        final String id;
        final ActorBuilder<?, ?> builder;
        final ChildMode mode;
        final int maxRestarts;
        final long duration;
        final TimeUnit unit;
        final long shutdownDeadline;

        /**
         * A new spec.
         *
         * @param id               the child's (optional) identifier (name)
         * @param mode             the child's {@link ChildMode mode}.
         * @param maxRestarts      the maximum number of times the child actor is allowed to restart within the given {@code duration} before
         *                         the supervisor gives up and kills itself.
         * @param duration         the duration in which the number of restarts is counted towards {@code maxRestarts}.
         * @param unit             the {@link TimeUnit time unit} of the {@code duration} for {@code maxRestarts}.
         * @param shutdownDeadline the time in milliseconds the supervisor should wait for the child actor to terminate from the time it was requested to shutdown;
         *                         after the deadline expires, the child actor is terminated forcefully.
         * @param builder          the child's {@link ActorBuilder builder}
         */
        public ChildSpec(String id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorBuilder<?, ?> builder) {
            this.id = id;
            this.builder = builder;
            this.mode = mode;
            this.maxRestarts = maxRestarts;
            this.duration = duration;
            this.unit = unit;
            this.shutdownDeadline = shutdownDeadline;
        }

        /**
         * A new spec.
         * This constructor takes an {@link ActorRef} to the actor rather than an {@link ActorBuilder}. If the {@link ActorRef} also implements
         * {@link ActorBuilder} it will be used to restart the actor. {@code ActorRef}s to local actors implement {@link ActorBuilder} using
         * {@code Actor}'s {@link Actor#reinstantiate() reinstantiate} method.
         *
         * @param id               the child's (optional) identifier (name)
         * @param mode             the child's {@link ChildMode mode}.
         * @param maxRestarts      the maximum number of times the child actor is allowed to restart within the given {@code duration} before
         *                         the supervisor gives up and kills itself.
         * @param duration         the duration in which the number of restarts is counted towards {@code maxRestarts}.
         * @param unit             the {@link TimeUnit time unit} of the {@code duration} for {@code maxRestarts}.
         * @param shutdownDeadline the time in milliseconds the supervisor should wait for the child actor to terminate from the time it was requested to shutdown;
         *                         after the deadline expires, the child actor is terminated forcefully.
         * @param actor            the child actor
         */
        public ChildSpec(String id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorRef<?> actor) {
            this(id, mode, maxRestarts, duration, unit, shutdownDeadline, LocalActor.toActorBuilder(actor));
        }

        /**
         * The child's (optional) identifier (name)
         */
        public String getId() {
            return id;
        }

        /**
         * The child's {@link ActorBuilder builder}
         */
        public ActorBuilder<?, ?> getBuilder() {
            return builder;
        }

        /**
         * The child's {@link ChildMode mode}.
         */
        public ChildMode getMode() {
            return mode;
        }

        /**
         * The maximum number of times the child actor is allowed to restart within a given {@link #getDuration() duration} before
         * the supervisor gives up and kills itself.
         */
        public int getMaxRestarts() {
            return maxRestarts;
        }

        /**
         * The duration in which the number of restarts is counted towards the {@link #getMaxRestarts() max restarts}.
         */
        public long getDuration() {
            return duration;
        }

        /**
         * The {@link TimeUnit time unit} of the {@link #getDuration() duration} for {@link #getMaxRestarts() max restarts}.
         */
        public TimeUnit getDurationUnit() {
            return unit;
        }

        /**
         * The time in milliseconds the supervisor should wait for the child actor to terminate from the time it was requested to shutdown;
         * after the deadline expires, the child actor is terminated forcefully.
         */
        public long getShutdownDeadline() {
            return shutdownDeadline;
        }

        @Override
        public String toString() {
            return "ChildSpec{" + "builder: " + builder + ", mode: " + mode + ", maxRestarts: " + maxRestarts + ", duration: " + duration + ", unit: " + unit + ", shutdownDeadline: " + shutdownDeadline + '}';
        }
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

    static class GetChildrenMessage extends RequestMessage {
        public GetChildrenMessage(ActorRef from, Object id) {
            super(from, id);
        }

        @Override
        protected String contentString() {
            return super.contentString();
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
}

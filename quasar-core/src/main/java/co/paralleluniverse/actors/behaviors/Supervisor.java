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
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.GenBehavior;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class Supervisor extends GenBehavior {
    public Supervisor(ActorRef<Object> actor) {
        super(actor);
    }

    public final ActorRef addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        final GenResponseMessage res = call(this, new AddChildMessage(RequestReplyHelper.from(), null, spec));
        return ((GenValueResponseMessage<ActorRef>) res).getValue();
    }

    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {

        final GenResponseMessage res = call(this, new RemoveChildMessage(RequestReplyHelper.from(), null, id, terminate));
        return ((GenValueResponseMessage<Boolean>) res).getValue();
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

    static class AddChildMessage extends GenRequestMessage {
        final ChildSpec info;

        public AddChildMessage(ActorRef from, Object id, ChildSpec info) {
            super(from, id);
            this.info = info;
        }
    }

    static class RemoveChildMessage extends GenRequestMessage {
        final Object id;
        final boolean terminate;

        public RemoveChildMessage(ActorRef from, Object id, Object name, boolean terminate) {
            super(from, id);
            this.id = name;
            this.terminate = terminate;
        }
    }
}

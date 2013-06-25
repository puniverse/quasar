/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public interface Supervisor extends GenBehavior {
    Actor<Object> addChild(ChildSpec spec) throws SuspendExecution, InterruptedException;

    boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException;

    public enum ChildMode {
        PERMANENT, TRANSIENT, TEMPORARY
    };

    public static class ChildSpec {
        final Object id;
        final ActorBuilder<?, ?> builder;
        final LocalSupervisor.ChildMode mode;
        final int maxRestarts;
        final long duration;
        final TimeUnit unit;
        final long shutdownDeadline;

        public ChildSpec(Object id, ChildMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline, ActorBuilder<?, ?> builder) {
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

        public LocalSupervisor.ChildMode getMode() {
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
}

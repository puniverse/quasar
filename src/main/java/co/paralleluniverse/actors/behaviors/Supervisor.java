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
import co.paralleluniverse.actors.ExitMessage;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class Supervisor extends LocalActor<Void, Void> {
    private static final Logger LOG = LoggerFactory.getLogger(Supervisor.class);
    private final RestartStrategy restartStrategy;
    private final List<ActorEntry> children = new ArrayList<ActorEntry>();

    public Supervisor(Strand strand, String name, int mailboxSize, RestartStrategy restartStrategy, List<ActorInfo> childrenSpecs) {
        super(strand, name, mailboxSize);
        this.restartStrategy = restartStrategy;
        for(ActorInfo childSpec : childrenSpecs)
            children.add(new ActorEntry(childSpec, childSpec.builder instanceof Actor ? (LocalActor)childSpec.builder : null));
    }

    @Override
    protected final Void doRun() throws InterruptedException, SuspendExecution {
        for (ActorEntry child : children) {
            if (child.actor == null)
                restart(child);
        }

        try {
            for (;;)
                receive(); // we care only about lifecycle messages
        } catch (InterruptedException e) {
        } finally {
            for (ActorEntry child : children)
                child.actor = null; // avoid memory leaks
        }

        return null;
    }

    protected void addChild(LocalActor actor, ActorMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadlineMillis) {
        verifyInActor();
        if (findEntry(actor) != null)
            throw new SupervisorException("Supervisor " + this + " already supervises actor " + actor);
        final ActorEntry entry = new ActorEntry(new ActorInfo(actor, mode, maxRestarts, duration, unit, shutdownDeadlineMillis), actor);
        entry.watch = watch(actor);
        children.add(entry);
    }

    @Override
    protected void handleLifecycleMessage(LifecycleMessage m) {
        boolean handled = false;
        try {
            if (m instanceof ExitMessage) {
                final ExitMessage death = (ExitMessage) m;
                if (death.getWatch() != null && death.actor instanceof LocalActor) {
                    final LocalActor actor = (LocalActor) death.actor;
                    final ActorEntry child = findEntry(actor);

                    if (child != null) {
                        restartStrategy.onChildDeath(this, child, death.cause);
                        handled = true;
                    }
                }
            } else if (m instanceof ShutdownMessage) {
                shutdownChildren();
                handled = true;
                Strand.currentStrand().interrupt();
            }
            if (!handled)
                super.handleLifecycleMessage(m);
        } catch (InterruptedException e) {
            Strand.currentStrand().interrupt();
        }
    }

    private boolean tryRestart(ActorEntry child, Throwable cause, long now) throws InterruptedException {
        verifyInActor();
        switch (child.info.mode) {
            case TRANSIENT:
                if (cause != null)
                    return true;
            // fall through
            case PERMANENT:
                final Actor actor = child.actor;
                if (actor != null)
                    shutdownChild(child);
                child.restartHistory.addRestart(now);
                if (child.restartHistory.numRestarts(now - child.info.unit.toMillis(child.info.duration)) > child.info.maxRestarts) {
                    LOG.warn(this + ": too many restarts for child {}", actor);
                    return false;
                }
                restart(child);
                return true;
            case TEMPORARY:
                return true;
            default:
                throw new AssertionError();
        }
    }

    private void restart(ActorEntry child) {
        assert child.actor == null || child.actor.isDone();
        if(child.actor != null && !child.actor.isDone())
            throw new IllegalStateException("Actor " + child.actor + " cannot be restarted because it is not dead");

        final LocalActor actor = child.info.builder.build();
        if (actor.getMonitor() != null)
            actor.getMonitor().addRestart();

        Strand strand = createStrandForActor(child.actor != null ? child.actor.getStrand() : null, actor);

        child.actor = actor;
        child.watch = watch(actor);
        strand.start();
    }

    private void shutdownChild(ActorEntry child) throws InterruptedException {
        if (child.actor != null) {
            unwatch(child);
            ((Actor) child.actor).send(new ShutdownMessage(this));
            joinChild(child);
        }
    }

    private void shutdownChildren() throws InterruptedException {
        for (ActorEntry child : children) {
            if (child.actor != null) {
                unwatch(child);
                ((Actor) child.actor).send(new ShutdownMessage(this));
            }
        }

        for (ActorEntry child : children) {
            if (child.actor != null)
                joinChild(child);
        }
    }

    private boolean joinChild(ActorEntry child) throws InterruptedException {
        if (child.actor != null) {
            try {
                child.actor.join(child.info.shutdownDeadline, TimeUnit.MILLISECONDS);
                return true;
            } catch (ExecutionException ex) {
                LOG.info(this + ": child {} died with exception {}", child.actor, ex.getCause());
                return true;
            } catch (TimeoutException ex) {
                LOG.warn(this + ": child {} shutdown timeout", child.actor);
                // is this the best we can do?
                child.actor.getStrand().interrupt();
                return false;
            } finally {
                child.actor = null;
            }
        } else
            return true;
    }

    private void unwatch(ActorEntry child) {
        unwatch(child.actor, child.watch);
        child.watch = null;
    }

    private Strand createStrandForActor(Strand oldStrand, LocalActor actor) {
        final Strand strand;
        if (oldStrand != null)
            strand = Strand.clone(oldStrand, actor);
        else
            strand = new Fiber(actor);
        actor.setStrand(strand);
        return strand;
    }

    private ActorEntry findEntry(LocalActor actor) {
        for (ActorEntry child : children) {
            if (child.actor == actor)
                return child;
        }
        return null;
    }

    private long now() {
        return System.currentTimeMillis() / 1000000;
    }

    public enum ActorMode {
        PERMANENT, TRANSIENT, TEMPORARY
    };

    public enum RestartStrategy {
        ONE_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ActorEntry child, Throwable cause) throws InterruptedException {
                return supervisor.tryRestart(child, cause, supervisor.now());
            }
        },
        ALL_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ActorEntry child, Throwable cause) throws InterruptedException {
                supervisor.shutdownChildren();
                for (ActorEntry c : supervisor.children) {
                    if (!supervisor.tryRestart(c, cause, supervisor.now()))
                        return false;
                }
                return true;
            }
        },
        REST_FOR_ONE {
            @Override
            boolean onChildDeath(Supervisor supervisor, ActorEntry child, Throwable cause) throws InterruptedException {
                boolean found = false;
                for (ActorEntry c : supervisor.children) {
                    if (c == child)
                        found = true;

                    if (found && !supervisor.tryRestart(c, cause, supervisor.now()))
                        return false;
                }
                return true;
            }
        };

        abstract boolean onChildDeath(Supervisor supervisor, ActorEntry child, Throwable cause) throws InterruptedException;
    }

    public static class ActorInfo {
        final ActorBuilder<?, ?> builder;
        final ActorMode mode;
        final int maxRestarts;
        final long duration;
        final TimeUnit unit;
        final long shutdownDeadline;

        public ActorInfo(ActorBuilder<?, ?> builder, ActorMode mode, int maxRestarts, long duration, TimeUnit unit, long shutdownDeadline) {
            this.builder = builder;
            this.mode = mode;
            this.maxRestarts = maxRestarts;
            this.duration = duration;
            this.unit = unit;
            this.shutdownDeadline = shutdownDeadline;
        }

        public ActorMode getMode() {
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
    }

    private static class ActorEntry {
        final ActorInfo info;
        final RestartHistory restartHistory;
        Object watch;
        LocalActor<?, ?> actor;

        public ActorEntry(ActorInfo info) {
            this(info, null);
        }

        public ActorEntry(ActorInfo info, LocalActor<?, ?> actor) {
            this.info = info;
            this.restartHistory = new RestartHistory(info.maxRestarts);

            this.actor = actor;
        }
    }

    private static class RestartHistory {
        private final long[] restarts;
        private int index;

        public RestartHistory(int windowSize) {
            this.restarts = new long[windowSize];
            this.index = 0;
        }

        public void addRestart(long now) {
            restarts[index] = now;
            index = mod(index + 1);
        }

        public int numRestarts(long since) {
            int count = 0;
            for (int i = mod(index - 1); i != index; i = mod(i - 1)) {
                if (restarts[i] < since) // || restarts[i] == 0L is implied
                    break;
                count++;
            }
            return count;
        }

        private int mod(int i) {
            // could be made fast by forcing restarts.length to be a power of two, but for now, we don't need this to be fast.
            if (i >= restarts.length)
                return i - restarts.length;
            if (i < 0)
                return i + restarts.length;
            return i;
        }
    }
}

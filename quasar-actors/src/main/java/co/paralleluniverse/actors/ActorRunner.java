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

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.Joinable;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Responsible for running an actor in a strand, and responding to hot code swaps.
 *
 * @author pron
 */
class ActorRunner<V> implements SuspendableCallable<V>, Stranded, Joinable<V>, java.io.Serializable {
    private /*final*/ transient ActorRef<?> actorRef;
    private volatile Actor<?, V> actor;
    private Strand strand;

    ActorRunner(ActorRef<?> actorRef) {
        this.actorRef = actorRef;
    }

    @Override
    public V run() throws SuspendExecution, InterruptedException {
        if (strand == null)
            setStrand(Strand.currentStrand());
        if (actor == null) {
            this.actor = (Actor<?, V>) actorRef.getImpl();
            assert actor != null && actor == actorRef.getImpl();
        }
        for (;;) {
            try {
                return actor.run0();
            } catch (CodeSwap e) {
                Actor<?, V> newActor = ActorLoader.getReplacementFor(actor);
                if (newActor != actor) {
                    newActor.setStrand0(strand);
                    newActor.onCodeChange0();
                    actor.defunct();
                    this.actor = newActor;
                    assert actor != null && actor == actorRef.getImpl();
                }
            } catch (ActorAbort e) {
                return null;
            }
        }
    }

    Actor<?, V> getActor() {
        return actor;
    }

    @Override
    public void setStrand(Strand strand) {
        if (strand == this.strand)
            return;
        if (this.strand != null)
            throw new IllegalStateException("Strand already set to " + strand);
        this.strand = strand;
        if (actor == null) {
            this.actor = (Actor<?, V>) actorRef.getImpl();
            assert actor != null : "actor == null";
            assert actor == actorRef.getImpl() : "actor (" + actor + ") != actorRef.getImpl() (" + actorRef.getImpl() + ")";
        }
        actor.setStrand0(strand);
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    public final boolean isStarted() {
        return strand != null && strand.getState().compareTo(Strand.State.STARTED) >= 0;
    }

    @Override
    @Suspendable
    public void join() throws ExecutionException, InterruptedException {
        strand.join();
    }

    @Override
    @Suspendable
    public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        strand.join(timeout, unit);
    }

    @Override
    @Suspendable
    public final V get() throws InterruptedException, ExecutionException {
        final Strand s = strand;
        if (s == null)
            throw new IllegalStateException("Actor strand not set (not started?)");
        if (s instanceof Fiber)
            return ((Fiber<V>) s).get();
        else {
            s.join();
            return actor.getResult();
        }
    }

    @Override
    @Suspendable
    public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (strand instanceof Fiber)
            return ((Fiber<V>) strand).get(timeout, unit);
        else {
            strand.join(timeout, unit);
            return actor.getResult();
        }
    }

    @Override
    public boolean isDone() {
        return actor.getDeathCause0() != null || strand.isTerminated();
    }

    private Object readResolve() {
        this.actorRef = actor.ref;
        return this;
    }
}

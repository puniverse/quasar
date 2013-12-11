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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.strands.SuspendableCallable;

/**
 * @author pron
 */
class ActorRunner<V> implements SuspendableCallable<V>, Stranded {
    private Actor<?, V> actor;
    private Strand strand;
    
    ActorRunner(Actor<?, V> actor) {
        this.actor = actor;
    }

    @Override
    public V run() throws SuspendExecution, InterruptedException {
        for (;;) {
            try {
                return actor.run0();
            } catch (CodeSwap e) {
                Actor<?, V> newActor = ActorLoader.getReplacementFor(actor);
                if (newActor != actor) {
                    newActor.setStrand(strand);
                    newActor.onCodeChange();
                    this.actor = newActor;
                }
            }
        }
    }

    Actor<?, V> getActor() {
        return actor;
    }

    @Override
    public void setStrand(Strand strand) {
        this.strand = strand;
        actor.setStrand(strand);
    }

    @Override
    public Strand getStrand() {
        return strand;
    }
}

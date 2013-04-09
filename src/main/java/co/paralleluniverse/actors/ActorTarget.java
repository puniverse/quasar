/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.*;

/**
 * Used only by Pulsar
 * @author pron
 * @param <V> 
 */
public interface ActorTarget<V> {
    /**
     * Entry point for LightweightThread execution.
     *
     * This method should never be called directly.
     *
     * @throws SuspendExecution This exception should never be caught
     */
    V run(Actor self) throws SuspendExecution, InterruptedException;
}

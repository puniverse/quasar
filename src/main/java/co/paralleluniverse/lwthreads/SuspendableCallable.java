/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

public interface SuspendableCallable<V> {
    /**
     * Entry point for LightweightThread execution.
     *
     * This method should never be called directly.
     *
     * @throws SuspendExecution This exception should never be caught
     */
    V run() throws SuspendExecution;
}

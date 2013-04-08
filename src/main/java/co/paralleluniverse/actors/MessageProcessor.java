/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;

/**
 *
 * @author pron
 */
public interface MessageProcessor<Message> {
    boolean process(Message m) throws SuspendExecution, InterruptedException;
}

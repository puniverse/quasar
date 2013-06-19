/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels.galaxy;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendChannel;

/**
 *
 * @author pron
 */
public class RemoteChannel<Message> implements SendChannel<Message> {

    @Override
    public void send(Message message) throws SuspendExecution {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

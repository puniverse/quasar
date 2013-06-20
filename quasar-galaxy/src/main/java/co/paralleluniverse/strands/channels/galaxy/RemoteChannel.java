/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels.galaxy;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;
import java.util.Map;

/**
 *
 * @author pron
 */
public class RemoteChannel<Message> implements SendChannel<Message> {
    
    public RemoteChannel(Channel channel) {
        new RemoteChannelReceiver<Message>(channel);
    }

    
    @Override
    public void send(Message message) throws SuspendExecution {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

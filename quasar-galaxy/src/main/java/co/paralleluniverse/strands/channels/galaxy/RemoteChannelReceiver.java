/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels.galaxy;

import co.paralleluniverse.galaxy.MessageListener;

/**
 * This class listens to messages received from remote ends of a channel, and forwards them to the right channel.
 * 
 */
public class RemoteChannelReceiver implements MessageListener{

    @Override
    public void messageReceived(short fromNode, byte[] message) {
        // XXXX
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}

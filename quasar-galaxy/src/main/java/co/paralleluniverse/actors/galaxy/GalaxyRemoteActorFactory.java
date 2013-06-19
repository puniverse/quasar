/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.remote.RemoteProxyFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;

/**
 *
 * @author pron
 */
public class GalaxyRemoteActorFactory implements RemoteProxyFactory {
    @Override
    public <Message> RemoteActor<Message> create(LocalActor<Message, ?> actor) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <Message> SendChannel<Message> create(Channel channel) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

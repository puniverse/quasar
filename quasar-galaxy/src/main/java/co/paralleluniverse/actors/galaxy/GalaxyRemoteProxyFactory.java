/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.remote.RemoteProxyFactory;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.SendChannel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannel;

/**
 *
 * @author pron
 */
public class GalaxyRemoteProxyFactory implements RemoteProxyFactory {
    @Override
    public <Message> RemoteActor<Message> create(LocalActor<Message, ?> actor, Object globalId) {
        return new RemoteActor<Message>(actor, globalId);
    }

    @Override
    public <Message> SendChannel<Message> create(Channel channel, Object globalId) {
        return new RemoteChannel<Message>(channel, globalId);
    }
}

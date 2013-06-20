/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels.galaxy;

import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.strands.channels.Channel;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import jsr166e.ConcurrentHashMapV8;

/**
 * This class listens to messages received from remote ends of a channel, and forwards them to the right channel.
 *
 */
public class RemoteChannelReceiver<Message> implements MessageListener {
    private static final ConcurrentMap<WeakReference<Channel<?>>, RemoteChannelReceiver<?>> receivers = new ConcurrentHashMapV8<>();
    private static final ReferenceQueue<Channel> refQueue = new ReferenceQueue<>();
    
    public static <Message> RemoteChannelReceiver<Message> getReceiver(Channel<Message> channel) {
        RemoteChannelReceiver<Message> receiver = (RemoteChannelReceiver<Message>) receivers.get(new WeakReference<>(channel));
        if (receiver == null) {
            receiver = new RemoteChannelReceiver<>(channel);
            RemoteChannelReceiver<Message> tmp = null;// (RemoteChannelReceiver<Message>) receivers.putIfAbsent(new WeakReference<>(channel), receiver);
            if (tmp == null) {
                // register to Galaxy
            } else
                receiver = tmp;
        }
        return receiver;
    }

    public RemoteChannelReceiver(Channel<Message> channel) {
    }

    @Override
    public void messageReceived(short fromNode, byte[] message) {
        // XXXX
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

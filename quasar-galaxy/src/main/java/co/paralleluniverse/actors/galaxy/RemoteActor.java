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
package co.paralleluniverse.actors.galaxy;

import co.paralleluniverse.actors.LifecycleListener;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannel;
import co.paralleluniverse.strands.channels.galaxy.RemoteChannelReceiver;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 * @author pron
 */
public class RemoteActor<Message> extends co.paralleluniverse.actors.RemoteActor<Message> {
    
    public RemoteActor(final LocalActor<Message, ?> actor, Object globalId) {
        super(actor, globalId);
        final RemoteChannelReceiver<Object> receiver = RemoteChannelReceiver.getReceiver((Channel<Object>)actor.getMailbox(), globalId != null);
        receiver.setFilter(new RemoteChannelReceiver.MessageFilter<Object> () {

            @Override
            public boolean shouldForwardMessage(Object msg) {
                if(msg instanceof RemoteActorAdminMessage) {
                    
                    // call actor method
                    
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected boolean isBackpressure() {
        return false;
    }

    @Override
    protected void internalSend(Object message) {
        ((RemoteChannel)mailbox()).send(message);
    }
    
    @Override
    protected void addLifecycleListener(LifecycleListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeLifecycleListener(LifecycleListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    protected void throwIn(RuntimeException e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void interrupt() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static class RemoteActorAdminMessage implements Streamable {

        @Override
        public int size() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void write(DataOutput out) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void read(DataInput in) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
}

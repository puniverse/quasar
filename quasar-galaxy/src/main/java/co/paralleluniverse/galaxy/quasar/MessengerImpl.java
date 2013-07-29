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
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.futures.AsyncListenableFuture;
import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.strands.Strand;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author pron
 */
public class MessengerImpl implements Messenger {
    private final co.paralleluniverse.galaxy.Messenger messenger;

    public MessengerImpl(co.paralleluniverse.galaxy.Messenger messenger) {
        this.messenger = messenger;
    }

    @Override
    public void addMessageListener(long topic, MessageListener listener) {
        messenger.addMessageListener(topic, listener);
    }

    @Override
    public void addMessageListener(String topic, MessageListener listener) {
        messenger.addMessageListener(topic, listener);
    }

    @Override
    public void removeMessageListener(long topic, MessageListener listener) {
        messenger.removeMessageListener(topic, listener);
    }

    @Override
    public void removeMessageListener(String topic, MessageListener listener) {
        messenger.removeMessageListener(topic, listener);
    }

    @Override
    public void send(short node, String topic, byte[] data) {
        messenger.send(node, topic, data);
    }

    @Override
    public void send(short node, long topic, Streamable data) {
        messenger.send(node, topic, data);
    }

    @Override
    public void send(short node, String topic, Streamable data) {
        messenger.send(node, topic, data);
    }

    @Override
    public void send(short node, long topic, byte[] data) {
        messenger.send(node, topic, data);
    }

    @Override
    public void sendToOwnerOf(long ref, long topic, byte[] data) throws TimeoutException, SuspendExecution {
        result(messenger.sendToOwnerOfAsync(ref, topic, data));
    }

    @Override
    public void sendToOwnerOf(long ref, String topic, byte[] data) throws TimeoutException, SuspendExecution {
        result(messenger.sendToOwnerOfAsync(ref, topic, data));
    }

    @Override
    public void sendToOwnerOf(long ref, long topic, Streamable data) throws TimeoutException, SuspendExecution {
        result(messenger.sendToOwnerOfAsync(ref, topic, data));
    }

    @Override
    public void sendToOwnerOf(long ref, String topic, Streamable data) throws TimeoutException, SuspendExecution {
        result(messenger.sendToOwnerOfAsync(ref, topic, data));
    }

    private <V> V result(ListenableFuture<V> future) throws TimeoutException, SuspendExecution {
        try {
            return AsyncListenableFuture.get(future);
        } catch (ExecutionException e) {
            Throwable ex = e.getCause();
            if (ex instanceof TimeoutException)
                throw (TimeoutException) ex;
            Throwables.propagateIfPossible(ex);
            throw Throwables.propagate(ex);
        } catch (InterruptedException ex) {
            Strand.currentStrand().interrupt();
            return null;
        }
    }
}

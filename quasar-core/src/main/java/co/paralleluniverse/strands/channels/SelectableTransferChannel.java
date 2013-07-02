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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Condition;
import co.paralleluniverse.strands.SimpleConditionSynchronizer;
import co.paralleluniverse.strands.queues.BoxQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class is only intended to be used by async's (chan). For a better transfer chnnel use {@link TransferChannel}.
 *
 * @author pron
 */
public class SelectableTransferChannel<Message> implements Channel<Message>, SelectableReceive, SelectableSend {
    private final Condition writers = new SimpleConditionSynchronizer();
    private final Condition readers = new SimpleConditionSynchronizer();
    private final BoxQueue<Message> q = new BoxQueue<Message>(false, false);
    private volatile boolean sendClosed;
    private boolean receiveClosed;

    @Override
    public void send(Message message) throws SuspendExecution {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (sendClosed)
            return;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean trySend(Message message) {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (sendClosed)
            return true;
        
        if (q.enq(message)) {
            readers.signal();
            return true;
        } else
            return false;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return null;


        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Message tryReceive() {
        if (receiveClosed)
            return null;


        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (receiveClosed)
            return null;


        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() {
        if (!sendClosed) {
            sendClosed = true;
            readers.signal();
        }
    }

    @Override
    public boolean isClosed() {
        return receiveClosed;
    }

    @Override
    public Condition receiveSelector() {
        return readers;
    }

    @Override
    public Condition sendSelector() {
        return writers;
    }
}

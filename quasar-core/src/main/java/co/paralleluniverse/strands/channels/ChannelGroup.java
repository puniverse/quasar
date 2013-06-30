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
import co.paralleluniverse.strands.ConditionSelector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * A group of channels allowing receiving from all member channels at once.
 *
 * @author pron
 */
public class ChannelGroup<Message> implements ReceivePort<Message> {
    private Object owner;
    private final ReceivePort<? extends Message>[] ports;
    private final ConditionSelector selector;
    private volatile boolean closed;

    /**
     * Creates a channel group
     *
     * @param channels The member channels
     */
    public ChannelGroup(ReceivePort<? extends Message>... ports) {
        this.ports = ports;
        final Condition[] conds = new Condition[ports.length];
        for(int i=0; i<conds.length; i++) 
            conds[i] = ((SelectableReceive)ports[i]).receiveSelector();
        this.selector = new ConditionSelector(conds);
    }

    /**
     * Creates a channel group
     *
     * @param channels The member channels
     */
    public ChannelGroup(Collection<? extends Message> channels) {
        this((QueueChannel<? extends Message>[]) channels.toArray(new QueueChannel[channels.size()]));
    }

    /**
     * Returns the member channels.
     *
     * @return An unmodifiable collection of all channels in this group.
     */
    public Collection<ReceivePort<? extends Message>> getChannels() {
        return Collections.unmodifiableList(Arrays.asList(ports));
    }

    public Object getOwner() {
        return owner;
    }

    /**
     * Blocks until one of the member channels receives a message, and returns it.
     *
     * @return A message received from one of the channels.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        if (closed)
            return null;

        selector.register();
        try {
            for (;;) {
                for (ReceivePort<? extends Message> c : ports) {
                    Message m = c.tryReceive();
                    if (m != null)
                        return m;
                }
                selector.await();
            }
        } finally {
            selector.unregister();
        }
    }

    @Override
    public Message tryReceive() {
        if (closed)
            return null;
        for (;;) {
            for (ReceivePort<? extends Message> c : ports) {
                Message m = c.tryReceive();
                if (m != null)
                    return m;
            }

            return null;
        }
    }

    /**
     * Blocks up to a given timeout until one of the member channels receives a message, and returns it.
     *
     * @param timeout
     * @param unit
     * @return A message received from one of the channels, or null if the timeout has expired.
     * @throws SuspendExecution
     * @throws InterruptedException
     */
    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        if (unit == null)
            return receive();
        if (timeout <= 0)
            return tryReceive();

        if (closed)
            return null;
        final long start = System.nanoTime();
        long left = unit.toNanos(timeout);

        selector.register();
        try {
            for (;;) {
                for (ReceivePort<? extends Message> c : ports) {
                    Message m = c.tryReceive();
                    if (m != null)
                        return m;
                }

                if (left <= 0)
                    return null;
                selector.await(left, TimeUnit.NANOSECONDS);
                left = start + unit.toNanos(timeout) - System.nanoTime();
                if (left <= 0)
                    return null;
            }
        } finally {
            selector.unregister();
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
import co.paralleluniverse.strands.Timeout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class ReceivePortGroup<Message> implements ReceivePort<Message> {
    private final Collection<? extends ReceivePort<? extends Message>> ports;
    private final Selector<Message> selector;

    public ReceivePortGroup(Collection<? extends ReceivePort<? extends Message>> ports) {
        this.ports = ports;
        ArrayList<SelectAction<Message>> actions = new ArrayList<>(ports.size());
        for (ReceivePort<? extends Message> port : ports)
            actions.add(Selector.receive(port));
        this.selector = new Selector(false, actions);
    }

    public ReceivePortGroup(ReceivePort<? extends Message>... ports) {
        this(Arrays.asList(ports));
    }

    @Override
    public Message tryReceive() {
        for (ReceivePort<? extends Message> port : ports) {
            Message m = port.tryReceive();
            if (m != null)
                return m;
        }
        return null;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        selector.reset();
        return selector.select().message();
    }

    @Override
    public Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        selector.reset();
        SelectAction<? extends Message> sa = selector.select(timeout, unit);
        if (sa != null)
            return sa.message();
        return null;
    }

    @Override
    public Message receive(Timeout timeout) throws SuspendExecution, InterruptedException {
        selector.reset();
        SelectAction<? extends Message> sa = selector.select(timeout);
        if (sa != null)
            return sa.message();
        return null;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public TransformingReceivePort<Message> transform() {
        return Channels.transform(this);
    }
}

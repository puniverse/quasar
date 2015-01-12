/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Serial, potentially blocking and indefinitely waiting port operations. Especially useful when used with suitable buffers.
 * <p/>
 * @author circlespainter
 */
public class ChannelBasicOps {
    private static final boolean closeToDefault = true;

    /**
     * Receives at most {@code n} messages from a {@link ReceivePort}.
     * <p/>
     * @return The collection of the received elements.
     */
    public static <Message> List<Message> receive(final ReceivePort<? extends Message> from, int n) throws SuspendExecution, InterruptedException {
        final List<Message> ret = new ArrayList<>();
        for(int i = 0 ; i < n ; i++) {
            final Message val = from.receive();
            if (val == null)
                break;
            else
                ret.add(val);
        }
        return ret;
    }

    /**
     * Sends all messages in an {@link Iterator} to a {@link SendPort}.
     * <p/>
     * @return The number of sent elements.
     */
    public static <Message> int send(Iterator<Message> it, final SendPort<? super Message> to) throws SuspendExecution, InterruptedException {
        int sent = 0;
        while(it.hasNext()) {
            to.send(it.next());
            sent++;
        }
        return sent;
    }
}
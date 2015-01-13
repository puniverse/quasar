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
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * {@link SendPort} that will discard all messages sent to it.
 *
 * @author circlespainter
 */
public class NullSendPort implements SendPort {

    @Override
    public void send(Object message) throws SuspendExecution, InterruptedException {
        // NOP
    }

    @Override
    public boolean send(Object message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
        return true; // NOP
    }

    @Override
    public boolean send(Object message, Timeout timeout) throws SuspendExecution, InterruptedException {
        return true; // NOP
    }

    @Override
    public boolean trySend(Object message) {
        return true; // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public void close(Throwable t) {
        // NOP
    }
}

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
import java.util.concurrent.TimeUnit;

/**
 * <b>All methods of this interface must only be called by the channel's owner.</b>
 *
 * @author pron
 */
public interface ReceivePort<Message> {
    Message receive() throws SuspendExecution, InterruptedException;

    Message tryReceive() throws SuspendExecution, InterruptedException;

    Message receive(long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException;

    void close();

    boolean isClosed();

    public static class EOFException extends RuntimeException {
        public EOFException() {
        }
    }
}

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
package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * An interface that is used by {@link BasicActor#receive(co.paralleluniverse.actors.MessageProcessor) BasicActor.receive} for selective receive.
 * @author pron
 */
public interface MessageProcessor<Message, T> {
    /**
     * An implementation of this method is used to select messages off an actor's queue for the purpose of selective receive. If the message
     * is selected (i.e. it should be processed now), then this method should return a non-null value. If the message
     * is to be skipped, this method should return {@code null}. The value returned by this method, will be returned by the {@code receive} method
     * this instance has been passed to.
     * <p>
     * If the message is selected, this method may process it (and may even call {@code receive} for nested selective receives), 
     * or it may choose to return the message and have it processed when it is returned from the enclosing {@link BasicActor#receive(co.paralleluniverse.actors.MessageProcessor) receive}.</p>
     * 
     * @param m The message
     * @return A non-null value if the message is selected; {@code null} if the message is to be skipped.
     * @throws SuspendExecution
     * @throws InterruptedException 
     */
    T process(Message m) throws SuspendExecution, InterruptedException;
}

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

/**
 * This exception is thrown by a {@link ReceivePort}'s {@code receive} or {@code tryReceive} methods if the channel has been
 * {@link SendPort#close(Throwable) closed with an exception}. The exception passed to {@link SendPort#close(Throwable)} is the {@link #getCause() cause}
 * of this exception.
 */
public class ProducerException extends RuntimeException {

    public ProducerException(Throwable cause) {
        super(cause);
    }
    
}

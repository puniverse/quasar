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

import co.paralleluniverse.strands.queues.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class PrimitiveChannel<T> extends Channel<T> {

    public PrimitiveChannel(Object owner, SingleConsumerQueue<T, ?> queue) {
        super(owner, queue);
    }

    public PrimitiveChannel(SingleConsumerQueue<T, ?> queue) {
        super(queue);
    }

}

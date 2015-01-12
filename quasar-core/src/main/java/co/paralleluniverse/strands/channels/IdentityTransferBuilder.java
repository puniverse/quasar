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

import com.google.common.base.Function;

/**
 * A non-transforming {@link TransferBuilder}.
 *
 * @author circlespainter
 */
public class IdentityTransferBuilder<Message> extends TransferBuilder<Message, Message> {
    public IdentityTransferBuilder(ReceivePort<? extends Message> from, SendPort<? super Message> to) {
        super(from, to, new Function<Message, Message>() {
            @Override
            public Message apply(Message m) {
                return m;
            }
        });
    }
}

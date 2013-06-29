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

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.queues.SingleConsumerArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerLinkedArrayObjectQueue;
import co.paralleluniverse.strands.queues.SingleConsumerQueue;

/**
 *
 * @author pron
 */
public class QueueObjectChannel<Message> extends QueueChannel<Message> {
    public static <Message> QueueObjectChannel<Message> create(Strand owner, int mailboxSize, OverflowPolicy policy) {
        return new QueueObjectChannel(owner, 
                mailboxSize > 0 
                ? new SingleConsumerArrayObjectQueue<Message>(mailboxSize) 
                : new SingleConsumerLinkedArrayObjectQueue<Message>(),
                policy);
    }

    public static <Message> QueueObjectChannel<Message> create(Strand owner, int mailboxSize) {
        return create(owner, mailboxSize, OverflowPolicy.THROW);
    }

    public static <Message> QueueObjectChannel<Message> create(int mailboxSize, OverflowPolicy policy) {
        return create(null, mailboxSize, policy);
    }

    public static <Message> QueueObjectChannel<Message> create(int mailboxSize) {
        return create(null, mailboxSize, OverflowPolicy.THROW);
    }

    public QueueObjectChannel(Strand owner, SingleConsumerQueue<Message, ?> queue, OverflowPolicy policy) {
        super(owner, queue, policy);
    }
}

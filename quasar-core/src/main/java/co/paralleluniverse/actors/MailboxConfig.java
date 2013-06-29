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
package co.paralleluniverse.actors;

import co.paralleluniverse.strands.channels.QueueChannel;

/**
 *
 * @author pron
 */
public class MailboxConfig {
    private final int mailboxSize;
    private final QueueChannel.OverflowPolicy policy;

    public MailboxConfig(int mailboxSize, QueueChannel.OverflowPolicy policy) {
        this.mailboxSize = mailboxSize;
        this.policy = policy;
    }

    public MailboxConfig() {
        this(-1, null);
    }

    public int getMailboxSize() {
        return mailboxSize;
    }

    public QueueChannel.OverflowPolicy getPolicy() {
        return policy;
    }
}

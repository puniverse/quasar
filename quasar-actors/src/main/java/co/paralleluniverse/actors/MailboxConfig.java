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

import co.paralleluniverse.strands.channels.Channels.OverflowPolicy;

/**
 *
 * @author pron
 */
public class MailboxConfig {
    private final int mailboxSize;
    private final OverflowPolicy policy;

    /**
     * Specifies a mailbox configuration with a given size and overflow policy.
     * @param mailboxSize The number of messages that can wait in the mailbox channel, with {@code -1} specifying an unbounded mailbox.
     * @param policy Specifies what to do when the mailbox is full and a new message is added.
     */
    public MailboxConfig(int mailboxSize, OverflowPolicy policy) {
        this.mailboxSize = mailboxSize;
        this.policy = policy;
    }

    /**
     * Specifies a default mailbox configuration - an unbounded mailbox.
     */
    public MailboxConfig() {
        this(-1, null);
    }

    public int getMailboxSize() {
        return mailboxSize;
    }

    public OverflowPolicy getPolicy() {
        return policy;
    }
}

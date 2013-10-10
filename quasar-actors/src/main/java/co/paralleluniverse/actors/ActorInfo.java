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

import java.beans.ConstructorProperties;
//import javax.management.openmbean.CompositeData;

/**
 *
 * @author pron
 */
public class ActorInfo {    
    private final long id;
    private final String name;
    private final boolean fiber;
    private final long receivedMessages;
    private final int queueLength;
    private final int restarts;
    private final String[] lastDeathCauses;
    private final String[] mailbox;
    private final String stackTrace;

    @ConstructorProperties({"id", "name", "fiber", "receivedMessages", "queueLength", "restarts", "lastDeathCauses", "mailbox", "stackTrace"})
    public ActorInfo(long id, String name, boolean fiber, long receivedMessages, int queueLength, int restarts, String[] lastDeathCauses, String[] mailbox, String stackTrace) {
        this.id = id;
        this.name = name;
        this.fiber = fiber;
        this.receivedMessages = receivedMessages;
        this.queueLength = queueLength;
        this.restarts = restarts;
        this.lastDeathCauses = lastDeathCauses;
        this.mailbox = mailbox;
        this.stackTrace = stackTrace;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isFiber() {
        return fiber;
    }

    public long getReceivedMessages() {
        return receivedMessages;
    }

    public int getQueueLength() {
        return queueLength;
    }

    public int getRestarts() {
        return restarts;
    }

    public String[] getLastDeathCauses() {
        return lastDeathCauses;
    }

    public String[] getMailbox() {
        return mailbox;
    }

    public String getStackTrace() {
        return stackTrace;
    }
}

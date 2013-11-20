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

import co.paralleluniverse.actors.behaviors.SupervisorActor;
import java.beans.ConstructorProperties;
//import javax.management.openmbean.CompositeData;

/**
 * Information about an actor returned by {@link ActorsMXBean}.
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

    /**
     * The actor's strand's ID
     */
    public long getId() {
        return id;
    }

    /**
     * The actor's name
     */
    public String getName() {
        return name;
    }

    /**
     * Whether the actor's strand is a fiber
     */
    public boolean isFiber() {
        return fiber;
    }

    /**
     * The number of messages this actor has received.
     */
    public long getReceivedMessages() {
        return receivedMessages;
    }

    /**
     * The number of messages currently waiting in the actor's mailbox.
     */
    public int getQueueLength() {
        return queueLength;
    }

    /**
     * The number of times this actor has been restarted by a {@link SupervisorActor}.
     */
    public int getRestarts() {
        return restarts;
    }

    /**
     * The latest few death-causes for this actor (relevant if it's been restarted by a {@link SupervisorActor}.
     */
    public String[] getLastDeathCauses() {
        return lastDeathCauses;
    }

    /**
     * The messages currently waiting in the actor's mailbox
     */
    public String[] getMailbox() {
        return mailbox;
    }

    /**
     * The actor's current call-stack
     */
    public String getStackTrace() {
        return stackTrace;
    }
}

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

import java.util.List;

/**
 * An MXBean that monitors all actors.
 *
 * @author pron
 */
public interface ActorsMXBean {
    void refresh();

    /**
     * The number of currently active actors.
     */
    int getNumActiveActors();

    /**
     * The strand IDs of all currently running actors.
     */
    long[] getAllActorIds();

    /**
     * Returns the stack trace of the strand running a given actor.
     *
     * @param actorId the strand ID of the actor
     * @return the current class-stack of the actor
     */
    String getStackTrace(long actorId);

    /**
     * Returns all messages currently awaiting in a given actor's mailbox.
     * @param actorId the strand ID of the actor
     * @return an array containing all messages currently awaiting in the actor's mailbox.
     */
    String[] getMailbox(long actorId);

    /**
     * Add an actor to the list of <i>watched actors</i>. Information about all watched actors is accessed via the {@link #getWatchedActorsInfo() watchedActorsInfo property}.
     * @param actorId the actor's strand ID.
     */
    void addWatch(long actorId);

    /**
     * Removes an actor from the set of <i>watched actors</i>.
     * @param actorId the actor's strand ID.
     * @see #addWatch(long) 
     */
    void removeWatch(long actorId);

    /**
     * {@link ActorInfo Actor information} for all {@link #addWatch(long) watched} actors.
     */
    List<ActorInfo> getWatchedActorsInfo();
}

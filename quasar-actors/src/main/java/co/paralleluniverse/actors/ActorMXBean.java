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

/**
 * An MXBean monitoring a single actor. This MBean is created for the actor when it is {@link Actor#register() registered}, or when its
 * {@link Actor#monitor() monitor} method is called.
 *
 * @author pron
 */
public interface ActorMXBean {
    void refresh();

    /**
     * The number of messages this actor has received.
     */
    long getReceivedMessages();

    /**
     * The number of messages currently waiting in the actor's mailbox.
     */
    int getQueueLength();

    /**
     * The number of times this actor has been restarted by a {@link co.paralleluniverse.actors.behaviors.SupervisorActor SupervisorActor}.
     */
    int getRestarts();

    /**
     * The latest few death-causes for this actor (relevant if it's been restarted by a {@link co.paralleluniverse.actors.behaviors.SupervisorActor SupervisorActor}.
     */
    String[] getLastDeathCauses();

    /**
     * The messages currently waiting in the actor's mailbox
     */
    String[] mailbox();

    /**
     * The actor's current call-stack
     */
    String stackTrace();
}

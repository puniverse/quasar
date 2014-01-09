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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * An interface to an {@link EventSourceActor}.
 *
 * @author pron
 */
public interface EventSource<Event> extends Behavior {
    /**
     * Adds an {@link EventHandler} that will be notified of every event sent to this actor.
     *
     * @param handler the handler
     * @return {@code true} if the handler has been successfully added to the actor, or {@code false} if the handler was already registered.
     */
    boolean addHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException;
    
    /**
     * Removes an {@link EventHandler} from the actor
     *
     * @param handler
     * @return {@code true} if the handler was registered and successfully removed, or {@code false} if the handler was not registered.
     */
    boolean removeHandler(EventHandler<Event> handler) throws SuspendExecution, InterruptedException;

    /**
     * Sends an event to the actor, which will be delivered to all registered event handlers.
     *
     * @param event the event
     */
    public void notify(Event event) throws SuspendExecution;
}

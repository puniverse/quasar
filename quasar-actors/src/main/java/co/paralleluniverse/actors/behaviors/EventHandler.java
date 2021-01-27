/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2016, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * A handler that can be registered with an {@link EventSource} actor to receive all events {@link EventSource#notify(java.lang.Object) sent}
 * to the actor.
 *
 * @author pron
 */
public interface EventHandler<Event> {
    void handleEvent(Event event) throws SuspendExecution, InterruptedException;
}

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

import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * An object that can construct a new actor
 *
 * @author pron
 */
public interface ActorBuilder<Message, V> {
    /**
     * Constructs a new actor
     *
     * @return a newly created actor.
     */
    Actor<Message, V> build() throws SuspendExecution;
}

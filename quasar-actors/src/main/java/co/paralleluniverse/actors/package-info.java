/*
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
/**
 * ## Quasar Actors
 *
 * An actor implementation extends the {@link co.paralleluniverse.actors.Actor Actor} or {@link co.paralleluniverse.actors.BasicActor BasicActor} class, and is spawned like so:
 *
 * ```java
 * ActorRef actor = new MyActor().spawn();
 * ```
 * 
 * You interact with actors through the {@link co.paralleluniverse.actors.ActorRef ActorRef} class.
 */
package co.paralleluniverse.actors;

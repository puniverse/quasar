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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An interface to a {@link ServerActor}.
 *
 * @author pron
 */
public interface Server<CallMessage, V, CastMessage> extends Behavior {
    /**
     * Sends a synchronous request to the actor, and awaits a response.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m the request
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     */
    V call(CallMessage m) throws InterruptedException, SuspendExecution;

    /**
     * Sends a synchronous request to the actor, and awaits a response, but no longer than the given timeout.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m       the request
     * @param timeout the maximum duration to wait for a response.
     * @param unit    the time unit of the timeout
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     * @throws TimeoutException if the timeout expires before a response has been received.
     */
    V call(CallMessage m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution;

    /**
     * Sends a synchronous request to the actor, and awaits a response, but no longer than the given timeout.
     * <p/>
     * This method may be safely called by actors and non-actor strands alike.
     *
     * @param m       the request
     * @param timeout the method will not block for longer than the amount remaining in the {@link Timeout}
     * @return the value sent as a response from the actor
     * @throws RuntimeException if the actor encountered an error while processing the request
     * @throws TimeoutException if the timeout expires before a response has been received.
     */
    V call(CallMessage m, Timeout timeout) throws TimeoutException, InterruptedException, SuspendExecution;

    /**
     * Sends an asynchronous request to the actor and returns immediately (may block until there's room available in the actor's mailbox).
     *
     * @param m the request
     */
    void cast(CastMessage m) throws SuspendExecution;

//    public static void cast(ActorRef server, Object m) throws SuspendExecution {
//        server.send(new ServerRequest(ActorRef.self(), makeId(), MessageType.CAST, m));
//    }
}

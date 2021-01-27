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

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * A delegate object that can be used instead of subclassing {@link ServerActor} and overriding its methods.
 *
 * @author pron
 */
public interface ServerHandler<CallMessage, V, CastMessage> extends Initializer {
    /**
     * Called to handle a synchronous request (one waiting for a response).
     * <ul>
     * <li>If this method returns a non-null value, it will be sent back to the sender of the request wrapped by an {@link ErrorResponseMessage};
     * if the request was sent via {@link Server#call(Object) Server.call} (which is how it's usually done), this value will be returned
     * by the {@link Server#call(java.lang.Object) call} method.</li>
     * <li>If this method throws an exception, it will be sent back to the sender of the request wrapped by an {@link ErrorResponseMessage};
     * if the request was sent via {@link Server#call(Object) Server.call}, the exception will be thrown by the {@link Server#call(java.lang.Object) call}
     * method, possibly wrapped in a {@link RuntimeException}.</li>
     * <li>If this method returns {@code null}, then a reply is not immediately sent, and the {@link Server#call(java.lang.Object) call} method
     * will remain blocked until a reply is sent manually with {@link ServerActor#reply(ActorRef, Object, Object) reply} or
     * {@link ServerActor#replyError(ActorRef, Object, Throwable) replyError}.</li>
     * </ul>
     *
     * @param from the sender of the request
     * @param id   the request's unique id
     * @param m    the request
     * @return a value that will be sent as a response to the sender of the request.
     * @throws Exception if thrown, it will be sent back to the sender of the request.
     */
    V handleCall(ActorRef<?> from, Object id, CallMessage m) throws Exception, SuspendExecution;

    /**
     * Called to handle an asynchronous request (one that does not for a response).
     *
     * @param from the sender of the request
     * @param id   the request's unique id
     * @param m    the request
     */
    void handleCast(ActorRef<?> from, Object id, CastMessage m) throws SuspendExecution;

    /**
     * Called to handle any message sent to this actor that is neither a {@link #handleCall(ActorRef, Object, Object) call} nor a {@link #handleCast(ActorRef, Object, Object) cast}.
     *
     * @param m the message
     */
    void handleInfo(Object m) throws SuspendExecution;

    /**
     * Called whenever the timeout set with {@link ServerActor#setTimeout(long, TimeUnit) setTimeout} or supplied at construction expires without any message
     * received. The countdown is reset after every received message. This method will be triggered multiple times if a message is not received
     * for a period of time longer than multiple timeout durations.
     */
    void handleTimeout() throws SuspendExecution;
}

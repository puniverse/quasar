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

import co.paralleluniverse.actors.ActorRef;
import java.beans.ConstructorProperties;

/**
 * A message that contains a sender reference (the {@code from} property} and a unique identifier (the {@code id} property) and may be used
 * as a request by {@link RequestReplyHelper#call(ActorRef, RequestMessage) RequestReplyHelper.call()}.
 *
 * @author pron
 */
public abstract class RequestMessage extends ActorMessage implements FromMessage, IdMessage {
    private ActorRef from;
    private Object id;

    /**
     * Constructs a new {@code RequestMessage}.
     * If the message is sent via {@link RequestReplyHelper#call(ActorRef, RequestMessage) RequestReplyHelper.call()}, both argument may be set to {@code null},
     * or, preferably, the {@link #RequestMessage() default constructor} should be used instead.
     *
     * @param from the actor sending the request. Usually you should pass the result of {@link RequestReplyHelper#from() }.
     * @param id   a unique message identifier. Usually you should pass the result of {@link RequestReplyHelper#makeId() }.
     */
    @ConstructorProperties({"from", "id"})
    public RequestMessage(ActorRef<?> from, Object id) {
        this.from = from;
        this.id = id;
    }

    /**
     * Constructs a new {@code RequestMessage}.<br/>
     * <i>This constructor may only be used if the message is to be sent via {@link RequestReplyHelper#call(ActorRef, RequestMessage) RequestReplyHelper.call()}<i>
     *
     * @param from the actor sending the request. Usually you should pass the result of {@link RequestReplyHelper#from() }.
     */
    @ConstructorProperties({"from"})
    public RequestMessage(ActorRef<?> from) {
        this.from = from;
        this.id = null;
    }

    /**
     * Constructs a new {@code RequestMessage}.<br/>
     * <i>This constructor may only be used if the message is to be sent via {@link RequestReplyHelper#call(ActorRef, RequestMessage) RequestReplyHelper.call()}<i>
     *
     * @param id a unique message identifier. Usually you should pass the result of {@link RequestReplyHelper#makeId() }.
     */
    @ConstructorProperties({"id"})
    public RequestMessage(Object id) {
        this.from = null;
        this.id = id;
    }

    /**
     * Constructs a new {@code RequestMessage}.<br/>
     * <i>This constructor may only be used if the message is to be sent via {@link RequestReplyHelper#call(ActorRef, RequestMessage) RequestReplyHelper.call()}<i>
     */
    public RequestMessage() {
        this.from = null;
        this.id = null;
    }

    /**
     * The actor that sent the request.
     */
    @Override
    public ActorRef getFrom() {
        return from;
    }

    /**
     * A unique message identifier.
     */
    @Override
    public Object getId() {
        return id;
    }

    /**
     * Called only by RequestReplyHelper
     */
    void setId(Object id) {
        this.id = id;
    }

    /**
     * Called only by RequestReplyHelper
     */
    void setFrom(ActorRef from) {
        this.from = from;
    }

    @Override
    protected String contentString() {
        return super.contentString() + "from: " + from + " id: " + id;
    }
}

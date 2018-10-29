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

import javax.management.ConstructorParameters;

/**
 * A message type used as a superclass by responses to {@link RequestMessage}.
 *
 * @author pron
 */
public abstract class ResponseMessage extends ActorMessage implements IdMessage {
    private final Object id;

    /**
     * Constructs a {@code ResponseMessage}.
     *
     * @param id the {@link RequestMessage#getId() id} of the {@link RequestMessage} this is a response to.
     */
    @ConstructorParameters({"id"})
    public ResponseMessage(Object id) {
        this.id = id;
    }

    /**
     * The {@link RequestMessage#getId() id} of the {@link RequestMessage} this is a response to.
     */
    @Override
    public Object getId() {
        return id;
    }

    @Override
    protected String contentString() {
        return super.contentString() + "id: " + id;
    }
}

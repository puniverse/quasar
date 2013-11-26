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

import java.beans.ConstructorProperties;

/**
 * A simple subclass of {@link ResponseMessage} that encapsulates a single response value.
 *
 * @author pron
 */
public class ValueResponseMessage<V> extends ResponseMessage implements IdMessage {
    private final V value;

    /**
     * Constructs a {@code ValueResponseMessage}.
     *
     * @param id    the {@link RequestMessage#getId() id} of the {@link RequestMessage} this is a response to.
     * @param value the response value, i.e. the result of the request
     */
    @ConstructorProperties({"id", "value"})
    public ValueResponseMessage(Object id, V value) {
        super(id);
        this.value = value;
    }

    /**
     * The response value, i.e. the result of the request.
     */
    public V getValue() {
        return value;
    }

    @Override
    protected String contentString() {
        return super.contentString() + " value: " + value;
    }
}

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
 * A simple subclass of {@link ResponseMessage} that represents an error in processing the request.
 *
 * @author pron
 */
public class ErrorResponseMessage extends ResponseMessage implements ErrorMessage {
    private final Throwable error;

    /**
     * Constructs a {@code ErrorResponseMessage}.
     *
     * @param id    the {@link RequestMessage#getId() id} of the {@link RequestMessage} this is a response to.
     * @param error the error that occurred while processing the request.
     */
    @ConstructorProperties({"id", "error"})
    public ErrorResponseMessage(Object id, Throwable error) {
        super(id);
        this.error = error;
    }

    /**
     * The error that occurred while processing the request.
     */
    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    protected String contentString() {
        return super.contentString() + " error: " + error;
    }
}

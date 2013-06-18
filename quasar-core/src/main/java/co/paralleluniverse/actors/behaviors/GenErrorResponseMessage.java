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
 *
 * @author pron
 */
public class GenErrorResponseMessage extends GenResponseMessage implements ErrorMessage {
    private final Throwable error;
    
    @ConstructorProperties({"id", "error"})
    public GenErrorResponseMessage(Object id, Throwable error) {
        super(id);
        this.error = error;
    }

    @Override
    public Throwable getError() {
        return error;
    }

    @Override
    protected String contentString() {
        return super.contentString() + " error: " + error;
    }
}

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

import co.paralleluniverse.actors.Actor;
import java.beans.ConstructorProperties;

/**
 *
 * @author pron
 */
public abstract class GenRequestMessage extends GenFromMessage implements IdMessage {
    private Object id;
    
    @ConstructorProperties({"from", "id"})
    public GenRequestMessage(Actor<?> from, Object id) {
        super(from);
        this.id = id;
    }

    @ConstructorProperties({"from", "id"})
    public GenRequestMessage(Actor<?> from) {
        super(from);
        this.id = null;
    }
    
    /**
     * Called only by RequestReplyHelper
     * @param id 
     */
    void setId(Object id) {
        this.id = id;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    protected String contentString() {
        return super.contentString() + " id: " + id;
    }
}

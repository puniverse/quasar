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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.common.util.DelegatingEquals;

/**
 * This is the superclass for all {@link ReceivePort} transformations.
 * @author pron
 */
public abstract class DelegatingReceivePort<S, T> implements ReceivePort<T>, DelegatingEquals {
    protected final ReceivePort<S> target;

    public DelegatingReceivePort(ReceivePort<S> target) {
        if (target == null)
            throw new IllegalArgumentException("target can't be null");
        this.target = target;
    }
    
    @Override
    public void close() {
        target.close();
    }

    @Override
    public boolean isClosed() {
        return target.isClosed();
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DelegatingEquals)
            return obj.equals(target);
        else
            return target.equals(obj);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) + "{" + target + "}";
    }
}

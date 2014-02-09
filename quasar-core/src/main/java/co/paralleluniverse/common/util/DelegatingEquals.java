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
package co.paralleluniverse.common.util;

/**
 * Represents an object that delegates its {@code equals} method to another, delegate, object.
 * 
 * @author pron
 */
public interface DelegatingEquals {
    /**
     * The {@code equals} method of a {@code DelegatingEquals} object, will usually be implemented thus:
     * 
     * <pre>{@code
     * boolean equals(Object o) {
     *     if (o instanceof DelegatingEquals)
     *         return o.equals(delegate);
     *     else
     *         return delegate.equals(o);
     * }</pre>
     */
    @Override
    boolean equals(Object o);
}

/*
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import java.util.Objects;

/**
 * @author circlespainter
 */
public final class Box<T> extends Tuple<Object> {
    private final T value;

    public Box(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Object get(int index) {
        switch(index) {
            case 0:
                return value;
            default:
                throw new IndexOutOfBoundsException("" + index);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Box<T> other = (Box<T>) obj;
        if (this.value != other.value && (this.value == null || !this.value.equals(other.value)))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.value);
        return hash;
    }

    @Override
    public String toString() {
        return "(" + value + ")";
    }
}

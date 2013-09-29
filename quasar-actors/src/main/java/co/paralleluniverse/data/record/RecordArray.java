/*
 * Copyright (c) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.data.record;

/**
 *
 * @author pron
 */
public interface RecordArray<R> extends Iterable<Record<R>> {
    public interface Accessor {
    }

    Accessor newAccessor();

    Accessor reset(Accessor accessor);

    Record<R> at(Accessor accessor, int index);

    /**
     * Same as {@code at(newAccessor(), index)}
     *
     * @param index
     * @return
     */
    Record<R> at(int index);

    RecordArray<R> slice(int from, int to);
}

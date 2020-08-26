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
package co.paralleluniverse.data.record;

/**
 * Represents an array of {@link Record}s. This interface allows for efficient (packed) storage of the records.
 *
 * @author pron
 */
public interface RecordArray<R> extends Iterable<Record<R>> {
    /**
     * An opaque pointer to an element in a {@link RecordArray}.
     * An accessor is mutable, and must not be shared by different strands.
     */
    public interface Accessor {
    }

    /**
     * The elements' {@link RecordType type}.
     */
    RecordType<R> type();

    /**
     * Allocates a new {@link Accessor accessor} into this array.
     *
     * @return a new {@link Accessor accessor} into this array.
     */
    Accessor newAccessor();

    /**
     * Resets the position of the given {@link Accessor accessor} so that it no longer points to any element.
     *
     * @param accessor the accessor
     * @return the accessor
     */
    Accessor reset(Accessor accessor);

    /**
     * Returns the record element at the given index, using the given {@link Accessor accessor}.
     * <p/>
     * <b>
     * The returned record is tied to the accessor, so it can only be used until the accessor is {@link #reset(Accessor) reset} or
     * {@link #at(Accessor, int) set to} a different element.
     * </b>
     * @param index the element's index
     * @return the record element at the given index
     */
    Record<R> at(Accessor accessor, int index);

    /**
     * Returns the record element at the given index, using the given {@link Accessor accessor}. 
     * Same as {@code at(newAccessor(), index)}.
     *
     * @param index the element's index
     * @return the record element at the given index
     */
    Record<R> at(int index);

    /**
     * Returns a {@link RecordArray} slice of this array.
     * The elements are not copied, and changes to the slice are reflected in this array and vice versa.
     *
     * @param from the first element index of this array to include in the slice, inclusive
     * @param to   the last element index of this array to include in the slice, exclusive (the element at this index is <i>not</i> included in the slice.
     * @return a slice of this array
     */
    RecordArray<R> slice(int from, int to);
}

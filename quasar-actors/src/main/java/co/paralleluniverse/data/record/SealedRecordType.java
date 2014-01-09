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
 *
 * @author pron
 */
public interface SealedRecordType<R> {
    /**
     * This type's name
     */
    String getName();

    /**
     * Test's whether a record is an instance of this type (or one of its subtypes).
     *
     * @param record the record to test
     * @return {@code true} if {@code record} is an instance of this type (or one of its subtypes); {@code false} otherwise.
     */
    boolean isInstance(Record<?> record);

    /**
     * Creates an new record instance of this type.
     * The returned implementation stores the record in an efficient memory representation.
     *
     * @return a newly constructed record of this type.
     */
    Record<R> newInstance();

    /**
     * Wraps a given object with a record instance of this type.
     * The record's fields are mapped to the target's fields or getters/setters of the same name.
     * Changes to the record will be reflected in the target object and vice versa.
     * <p/>
     * The record's implementation {@link Mode} mode will be the best (fastest) one available for the target's class.
     *
     * @param target the POJO to wrap as a record
     * @return a newly constructed record of this type, which reflects {@code target}.
     */
    Record<R> wrap(Object target);

    /**
     * Wraps a given object with a record instance of this type.
     * The record's fields are mapped to the target's fields or getters/setters of the same name.
     * Changes to the record will be reflected in the target object and vice versa.
     *
     * @param target the POJO to wrap as a record
     * @param mode   the record's implementation {@link Mode} mode.
     * @return a newly constructed record of this type, which reflects {@code target}.
     */
    Record<R> wrap(Object target, RecordType.Mode mode);

    /**
     * Creates an new {@link RecordArray} instance of this type.
     * The returned implementation stores the record array in an efficient memory representation.
     *
     * @return a newly constructed record array of this type.
     */
    RecordArray<R> newArray(int size);
}

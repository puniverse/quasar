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
 * Thrown by a {@link Record} when trying to get or set a field that is not in the record.
 *
 * @author pron
 */
public class FieldNotFoundException extends RecordException {
    public FieldNotFoundException(Field field, Object record) {
        super("Field " + field + " was not found in record " + record);
    }
}

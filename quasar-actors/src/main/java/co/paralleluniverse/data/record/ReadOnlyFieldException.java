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
 * Thrown by a {@link Record} when trying to set a field that is read-only.
 *
 * @author pron
 */
public class ReadOnlyFieldException extends RecordException {
    public ReadOnlyFieldException(Field field, Object record) {
        super("Field " + field + " can only be read, not set in " + record);
    }

    public ReadOnlyFieldException(String field, Object record) {
        super("Field " + field + " can only be read, not set in " + record);
    }

    public ReadOnlyFieldException() {
    }
}

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
public final class Records {
    public static <R> Record<R> delegate(Object owner, Record<R> record) {
        return new RecordDelegate<R>(owner, record);
    }

    public static <R> void setDelegate(Record<R> record, Object owner, Record<R> newDelegate) {
        if(!(record instanceof RecordDelegate))
            throw new UnsupportedOperationException("Record " + record + " is not a record delegate");
        ((RecordDelegate<R>)record).setDelegate(owner, newDelegate);
    }

    private Records() {
    }
}

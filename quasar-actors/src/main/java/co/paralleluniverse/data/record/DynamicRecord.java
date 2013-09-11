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

import java.util.Set;

/**
 *
 * @author pron
 */
public abstract class DynamicRecord<R> extends AbstractRecord<R> {
    private final DynamicRecordType.Entry[] vtable;
    private final Set<Field<? super R, ?>> fieldSet;
    final Object obj;

    DynamicRecord(DynamicRecordType<R> recordType, Object target) {
        this.vtable = recordType.getClassInfo(target.getClass()).table;
        this.fieldSet = recordType.fieldSet();
        this.obj = target;
    }

    protected DynamicRecord(DynamicRecordType<R> recordType) {
        this.vtable = recordType.getClassInfo(this.getClass()).table;
        this.fieldSet = recordType.fieldSet();
        this.obj = this;
    }

    @Override
    public Set<Field<? super R, ?>> fields() {
        return fieldSet;
    }
    
    DynamicRecordType.Entry entry(Field<? super R, ?> field) {
        return vtable[field.id()];
    }
}

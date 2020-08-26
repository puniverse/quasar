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

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 * @author pron
 */
class SimpleRecordArray<R> implements RecordArray<R>, Iterable<Record<R>>, Cloneable {
    public final RecordType<R> type;
    public final int length;
    private final int osize;
    private final int bsize;
    private final int[] offsets;
    private final Object[] oa;
    private final byte[] ba;
    private final int offset;

    public SimpleRecordArray(RecordType<R> recordType, int length) {
        this.type = recordType;
        this.length = length;
        this.offsets = recordType.getOffsets();
        this.osize = recordType.getObjectIndex() > 0 ? recordType.getObjectOffset() : 0;
        this.bsize = recordType.getPrimitiveIndex() > 0 ? recordType.getPrimitiveOffset() : 0;
        this.oa = osize > 0 ? new Object[osize * length] : null;
        this.ba = bsize > 0 ? new byte[bsize * length] : null;
        this.offset = 0;
    }

    private SimpleRecordArray(SimpleRecordArray<R> other, int offset, int length, boolean copy) {
        this.type = other.type;
        this.length = length;
        this.offsets = other.offsets;
        this.osize = other.osize;
        this.bsize = other.bsize;
        if (copy) {
            this.oa = other.oa != null ? Arrays.copyOfRange(other.oa, offset * osize, (offset + length) * osize) : null;
            this.ba = other.ba != null ? Arrays.copyOfRange(other.ba, offset * bsize, (offset + length) * bsize) : null;
            this.offset = 0;
        } else {
            this.oa = other.oa;
            this.ba = other.ba;
            this.offset = offset;
        }
    }

    @Override
    public RecordType<R> type() {
        return type;
    }
    
    @Override
    protected SimpleRecordArray<R> clone() {
        return new SimpleRecordArray<R>(this, offset, length, true);
    }

    @Override
    public Accessor newAccessor() {
        return new AccessorRecord(type, offsets, oa, ba, osize, bsize, offset);
    }

    @Override
    public Accessor reset(Accessor accessor) {
        ((AccessorRecord) accessor).index = -1;
        return accessor;
    }

    @Override
    public Record<R> at(Accessor accessor, int index) {
        if (index < 0 || index >= length)
            throw new ArrayIndexOutOfBoundsException(index);

        final AccessorRecord<R> ar = (AccessorRecord<R>) accessor;
        ar.index = index;
        return ar;
    }

    @Override
    public Record<R> at(int index) {
        return at(newAccessor(), index);
    }
    
    @Override
    public RecordArray<R> slice(int from, int to) {
        if(from > to)
            throw new IllegalArgumentException("fromIndex(" + from + ") > toIndex(" + to + ")");
        return new SimpleRecordArray<R>(this, offset + from, to - from, false);
    }
    
    public RecordArray<R> clear() {
        Arrays.fill(oa, offset * osize, (offset + length) * osize, null);
        Arrays.fill(ba, offset * bsize, (offset + length) * bsize, (byte)0);
        return this;
    }

    @Override
    public Iterator<Record<R>> iterator() {
        return new Iterator<Record<R>>() {
            private final AccessorRecord<R> acc = (AccessorRecord<R>) newAccessor();

            @Override
            public boolean hasNext() {
                return (acc.index + 1) < length;
            }

            @Override
            public Record<R> next() {
                acc.index++;
                return acc;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // public Record<R> at(Record<R> accessor)
    private static class AccessorRecord<R> extends SimpleRecord<R> implements Accessor {
        private final int osize;
        private final int bsize;
        private final int offset;
        int index;

        public AccessorRecord(RecordType<R> recordType, int[] offsets, Object[] oa, byte[] ba, int osize, int bsize, int offset) {
            super(recordType, offsets, oa, ba);
            this.osize = osize;
            this.bsize = bsize;
            this.index = -1;
            this.offset = offset;
        }

        @Override
        int fieldOffset(Field<? super R, ?> field) {
            try {
                final int stride = (field.type() == Field.OBJECT || field.type() == Field.OBJECT_ARRAY) ? osize : bsize;
                return (index + offset) * stride + offsets[field.id];
            } catch (IndexOutOfBoundsException e) {
                if (index < 0)
                    throw new IllegalStateException("Accessor not pointing at an element");
                else
                    throw new FieldNotFoundException(field, this);
            }
        }
    }
}

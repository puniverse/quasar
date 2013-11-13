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

import co.paralleluniverse.common.util.DelegatingEquals;
import co.paralleluniverse.data.record.Field.ArrayField;
import co.paralleluniverse.data.record.Field.BooleanArrayField;
import co.paralleluniverse.data.record.Field.BooleanField;
import co.paralleluniverse.data.record.Field.ByteArrayField;
import co.paralleluniverse.data.record.Field.ByteField;
import co.paralleluniverse.data.record.Field.CharArrayField;
import co.paralleluniverse.data.record.Field.CharField;
import co.paralleluniverse.data.record.Field.DoubleArrayField;
import co.paralleluniverse.data.record.Field.DoubleField;
import co.paralleluniverse.data.record.Field.FloatArrayField;
import co.paralleluniverse.data.record.Field.FloatField;
import co.paralleluniverse.data.record.Field.IntArrayField;
import co.paralleluniverse.data.record.Field.IntField;
import co.paralleluniverse.data.record.Field.LongArrayField;
import co.paralleluniverse.data.record.Field.LongField;
import co.paralleluniverse.data.record.Field.ObjectArrayField;
import co.paralleluniverse.data.record.Field.ObjectField;
import co.paralleluniverse.data.record.Field.ShortArrayField;
import co.paralleluniverse.data.record.Field.ShortField;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 *
 * @author pron
 */
final class RecordDelegate<R> implements Record<R>, DelegatingEquals {
    private final Object owner;
    private volatile Record<R> r;

    public RecordDelegate(Object owner, Record<R> delegate) {
        this.owner = owner;
        this.r = delegate;
    }

    void setDelegate(Object owner, Record<R> delegate) {
        if (this.owner == null || this.owner != owner)
            throw new IllegalAccessError("Object " + owner + " is not this record's owner");
        this.r = delegate;
    }

    Record<R> getDelegate(Object owner) {
        if (this.owner == null || this.owner != owner)
            throw new IllegalAccessError("Object " + owner + " is not this record's owner");
        return r;
    }

    @Override
    public RecordType<R> type() {
        return r.type();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if(obj == this)
            return true;
        if(r == null)
            return false;
        return obj instanceof DelegatingEquals ? obj.equals(r) : r.equals(obj);
    }
    
    @Override
    public String toString() {
        return r.toString();
    }

    @Override
    public Set<Field<? super R, ?>> fields() {
        return r.fields();
    }

    @Override
    public void write(ObjectOutput out) throws IOException {
        r.write(out);;
    }

    @Override
    public void read(ObjectInput in) throws IOException {
        r.read(in);
    }

    @Override
    public boolean get(BooleanField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(BooleanField<? super R> field, boolean value) {
        r.set(field, value);
    }

    @Override
    public byte get(ByteField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(ByteField<? super R> field, byte value) {
        r.set(field, value);
    }

    @Override
    public short get(ShortField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(ShortField<? super R> field, short value) {
        r.set(field, value);
    }

    @Override
    public int get(IntField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(IntField<? super R> field, int value) {
        r.set(field, value);
    }

    @Override
    public long get(LongField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(LongField<? super R> field, long value) {
        r.set(field, value);
    }

    @Override
    public float get(FloatField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(FloatField<? super R> field, float value) {
        r.set(field, value);
    }

    @Override
    public double get(DoubleField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(DoubleField<? super R> field, double value) {
        r.set(field, value);
    }

    @Override
    public char get(CharField<? super R> field) {
        return r.get(field);
    }

    @Override
    public void set(CharField<? super R> field, char value) {
        r.set(field, value);
    }

    @Override
    public <V> V get(ObjectField<? super R, V> field) {
        return r.get(field);
    }

    @Override
    public <V> void set(ObjectField<? super R, V> field, V value) {
        r.set(field, value);
    }

    @Override
    public boolean get(BooleanArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(BooleanArrayField<? super R> field, int index, boolean value) {
        r.set(field, index, value);
    }

    @Override
    public void get(BooleanArrayField<? super R> field, boolean[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(BooleanArrayField<? super R> field, boolean[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(BooleanArrayField<? super R> field, Record<S> source, BooleanArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public byte get(ByteArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(ByteArrayField<? super R> field, int index, byte value) {
        r.set(field, index, value);
    }

    @Override
    public void get(ByteArrayField<? super R> field, byte[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(ByteArrayField<? super R> field, byte[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(ByteArrayField<? super R> field, Record<S> source, ByteArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public short get(ShortArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(ShortArrayField<? super R> field, int index, short value) {
        r.set(field, index, value);
    }

    @Override
    public void get(ShortArrayField<? super R> field, short[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(ShortArrayField<? super R> field, short[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(ShortArrayField<? super R> field, Record<S> source, ShortArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public int get(IntArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(IntArrayField<? super R> field, int index, int value) {
        r.set(field, index, value);
    }

    @Override
    public void get(IntArrayField<? super R> field, int[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(IntArrayField<? super R> field, int[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(IntArrayField<? super R> field, Record<S> source, IntArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public long get(LongArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(LongArrayField<? super R> field, int index, long value) {
        r.set(field, index, value);
    }

    @Override
    public void get(LongArrayField<? super R> field, long[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(LongArrayField<? super R> field, long[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(LongArrayField<? super R> field, Record<S> source, LongArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public float get(FloatArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(FloatArrayField<? super R> field, int index, float value) {
        r.set(field, index, value);
    }

    @Override
    public void get(FloatArrayField<? super R> field, float[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(FloatArrayField<? super R> field, float[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(FloatArrayField<? super R> field, Record<S> source, FloatArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public double get(DoubleArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(DoubleArrayField<? super R> field, int index, double value) {
        r.set(field, index, value);
    }

    @Override
    public void get(DoubleArrayField<? super R> field, double[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(DoubleArrayField<? super R> field, double[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(DoubleArrayField<? super R> field, Record<S> source, DoubleArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public char get(CharArrayField<? super R> field, int index) {
        return r.get(field, index);
    }

    @Override
    public void set(CharArrayField<? super R> field, int index, char value) {
        r.set(field, index, value);
    }

    @Override
    public void get(CharArrayField<? super R> field, char[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public void set(CharArrayField<? super R> field, char[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S> void set(CharArrayField<? super R> field, Record<S> source, CharArrayField<? super S> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public <V> V get(ObjectArrayField<? super R, V> field, int index) {
        return r.get(field, index);
    }

    @Override
    public <V> void set(ObjectArrayField<? super R, V> field, int index, V value) {
        r.set(field, index, value);
    }

    @Override
    public <V> void get(ObjectArrayField<? super R, V> field, V[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public <V> void set(ObjectArrayField<? super R, V> field, V[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S, V> void set(ObjectArrayField<? super R, V> field, Record<S> source, ObjectArrayField<? super S, V> sourceField) {
        r.set(field, source, sourceField);
    }

    @Override
    public <V> V get(Field<? super R, V> field) {
        return r.get(field);
    }

    @Override
    public <V> void set(Field<? super R, V> field, V value) {
        r.set(field, value);
    }

    @Override
    public <V> V get(ArrayField<? super R, V> field, int index) {
        return r.get(field, index);
    }

    @Override
    public <V> void set(ArrayField<? super R, V> field, int index, V value) {
        r.set(field, index, value);
    }

    @Override
    public <V> void get(ArrayField<? super R, V> field, V[] target, int offset) {
        r.get(field, target, offset);
    }

    @Override
    public <V> void set(ArrayField<? super R, V> field, V[] source, int offset) {
        r.set(field, source, offset);
    }

    @Override
    public <S, V> void set(ArrayField<? super R, V> field, Record<S> source, ArrayField<? super S, V> sourceField) {
        r.set(field, source, sourceField);
    }
}

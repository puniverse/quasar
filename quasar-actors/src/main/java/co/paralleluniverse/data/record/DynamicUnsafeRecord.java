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

import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
final class DynamicUnsafeRecord<R> extends DynamicRecord<R> {
    static final Unsafe unsafe = DynamicRecordType.unsafe;
    
    DynamicUnsafeRecord(DynamicRecordType<R> recordType, Object target) {
        super(recordType, target);
    }

    protected DynamicUnsafeRecord(DynamicRecordType<R> recordType) {
        super(recordType);
    }
    
    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        return unsafe.getBoolean(obj, entry(field).offset);
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        unsafe.putBoolean(obj, entry(field).offset, value);
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        return unsafe.getByte(obj, entry(field).offset);
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        unsafe.putByte(obj, entry(field).offset, value);
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        return unsafe.getShort(obj, entry(field).offset);
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        unsafe.putShort(obj, entry(field).offset, value);
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        return unsafe.getInt(obj, entry(field).offset);
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        unsafe.putInt(obj, entry(field).offset, value);
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        return unsafe.getLong(obj, entry(field).offset);
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        unsafe.putLong(obj, entry(field).offset, value);
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        return unsafe.getFloat(obj, entry(field).offset);
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        unsafe.putFloat(obj, entry(field).offset, value);
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        return unsafe.getDouble(obj, entry(field).offset);
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        unsafe.putDouble(obj, entry(field).offset, value);
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        return unsafe.getChar(obj, entry(field).offset);
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        unsafe.putChar(obj, entry(field).offset, value);
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        return (V)unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        unsafe.putObject(obj, entry(field).offset, value);
    }

    boolean[] get(Field.BooleanArrayField<? super R> field) {
        return (boolean[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    byte[] get(Field.ByteArrayField<? super R> field) {
        return (byte[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    short[] get(Field.ShortArrayField<? super R> field) {
        return (short[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    int[] get(Field.IntArrayField<? super R> field) {
        return (int[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    long[] get(Field.LongArrayField<? super R> field) {
        return (long[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    float[] get(Field.FloatArrayField<? super R> field) {
        return (float[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    double[] get(Field.DoubleArrayField<? super R> field) {
        return (double[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    char[] get(Field.CharArrayField<? super R> field) {
        return (char[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        return get(field)[index];
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        get(field)[index] = value;
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        source.get(sourceField, get(field), 0);
    }

    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        return (V[])unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        return get(field)[index];
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        get(field)[index] = value;
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        System.arraycopy(get(field), 0, target, offset, field.length);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        System.arraycopy(source, offset, get(field), 0, field.length);
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        source.get(sourceField, get(field), 0);
    }
}

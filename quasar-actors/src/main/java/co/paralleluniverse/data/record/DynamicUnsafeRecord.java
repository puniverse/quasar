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

import co.paralleluniverse.common.util.UtilUnsafe;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
final class DynamicUnsafeRecord<R> extends DynamicRecord<R> {
    static final Unsafe unsafe = UtilUnsafe.getUnsafe();
    private static final int base;
    private static final int baseLong;
    private static final int shift;

    static {
        try {
            if (unsafe.arrayIndexScale(boolean[].class) != 1)
                throw new AssertionError("Strange boolean array scale: " + unsafe.arrayIndexScale(boolean[].class));
            if (unsafe.arrayIndexScale(byte[].class) != 1)
                throw new AssertionError("Strange byte array scale: " + unsafe.arrayIndexScale(byte[].class));
            if (unsafe.arrayIndexScale(short[].class) != 2)
                throw new AssertionError("Strange short array scale: " + unsafe.arrayIndexScale(short[].class));
            if (unsafe.arrayIndexScale(char[].class) != 2)
                throw new AssertionError("Strange char array scale: " + unsafe.arrayIndexScale(char[].class));
            if (unsafe.arrayIndexScale(int[].class) != 4)
                throw new AssertionError("Strange int array scale: " + unsafe.arrayIndexScale(int[].class));
            if (unsafe.arrayIndexScale(float[].class) != 4)
                throw new AssertionError("Strange float array scale: " + unsafe.arrayIndexScale(float[].class));
            if (unsafe.arrayIndexScale(long[].class) != 8)
                throw new AssertionError("Strange long array scale: " + unsafe.arrayIndexScale(long[].class));
            if (unsafe.arrayIndexScale(double[].class) != 8)
                throw new AssertionError("Strange double array scale: " + unsafe.arrayIndexScale(double[].class));

            base = unsafe.arrayBaseOffset(byte[].class);
            baseLong = unsafe.arrayBaseOffset(long[].class);

            if (unsafe.arrayBaseOffset(boolean[].class) != base)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(short[].class) != base)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(char[].class) != base)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(int[].class) != base)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(float[].class) != base)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(long[].class) != baseLong)
                throw new AssertionError("different array base");
            if (unsafe.arrayBaseOffset(double[].class) != baseLong)
                throw new AssertionError("different array base");

            int scale = unsafe.arrayIndexScale(byte[].class);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            shift = 31 - Integer.numberOfLeadingZeros(scale);
            if (scale != 1 || shift != 0)
                throw new AssertionError("Strange byte array alignment");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    static long getFieldOffset(Class<?> type, java.lang.reflect.Field f) {
        return unsafe.objectFieldOffset(f);
    }

    DynamicUnsafeRecord(RecordType<R> recordType, Object target) {
        super(recordType, target);
    }

//    protected DynamicUnsafeRecord(DynamicRecordType<R> recordType) {
//        super(recordType);
//    }
    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        return unsafe.getBoolean(obj, entry(field).offset);
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putBoolean(obj, entry.offset, value);
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        return unsafe.getByte(obj, entry(field).offset);
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putByte(obj, entry.offset, value);
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        return unsafe.getShort(obj, entry(field).offset);
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putShort(obj, entry.offset, value);
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        return unsafe.getInt(obj, entry(field).offset);
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putInt(obj, entry.offset, value);
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        return unsafe.getLong(obj, entry(field).offset);
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putLong(obj, entry.offset, value);
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        return unsafe.getFloat(obj, entry(field).offset);
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putFloat(obj, entry.offset, value);
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        return unsafe.getDouble(obj, entry(field).offset);
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putDouble(obj, entry.offset, value);
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        return unsafe.getChar(obj, entry(field).offset);
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putChar(obj, entry.offset, value);
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        return (V) unsafe.getObject(obj, entry(field).offset);
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        RecordType.Entry entry = entry(field);
        checkReadOnly(entry, field);
        unsafe.putObject(obj, entry(field).offset, value);
    }

    @Override
    boolean[] get(Field.BooleanArrayField<? super R> field) {
        return (boolean[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    byte[] get(Field.ByteArrayField<? super R> field) {
        return (byte[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    short[] get(Field.ShortArrayField<? super R> field) {
        return (short[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    int[] get(Field.IntArrayField<? super R> field) {
        return (int[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    long[] get(Field.LongArrayField<? super R> field) {
        return (long[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    float[] get(Field.FloatArrayField<? super R> field) {
        return (float[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    double[] get(Field.DoubleArrayField<? super R> field) {
        return (double[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    char[] get(Field.CharArrayField<? super R> field) {
        return (char[]) unsafe.getObject(obj, entry(field).offset);
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

    @Override
    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        return (V[]) unsafe.getObject(obj, entry(field).offset);
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

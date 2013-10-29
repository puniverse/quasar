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

import co.paralleluniverse.concurrent.util.UtilUnsafe;
import java.util.Arrays;
import java.util.Set;
import sun.misc.Unsafe;

/**
 *
 * @author pron
 */
class SimpleRecord<R> extends AbstractRecord<R> implements Record<R>, Cloneable {
    final int[] offsets;
    private final Object[] oa;
    private final byte[] ba;

    public SimpleRecord(RecordType<R> recordType) {
        super(recordType);
        this.offsets = recordType.getOffsets();
        this.oa = recordType.getObjectIndex() > 0 ? new Object[recordType.getObjectOffset()] : null;
        this.ba = recordType.getPrimitiveIndex() > 0 ? new byte[recordType.getPrimitiveOffset()] : null;
    }

    /**
     * Used by SimpleRecordArray
     * @param fieldSet
     * @param offsets
     * @param oa
     * @param ba 
     */
    SimpleRecord(RecordType<R> recordType, int[] offsets, Object[] oa, byte[] ba) {
        super(recordType);
        this.offsets = offsets;
        this.oa = oa;
        this.ba = ba;
    }

    private SimpleRecord(SimpleRecord<R> other) {
        super(other.type);
        this.offsets = other.offsets;
        this.oa = other.oa != null ? Arrays.copyOf(other.oa, other.oa.length) : null;
        this.ba = other.ba != null ? Arrays.copyOf(other.ba, other.ba.length) : null;
    }

    @Override
    protected SimpleRecord<R> clone() {
        return new SimpleRecord<R>(this);
    }

    int fieldOffset(Field<? super R, ?> field) {
        try {
            return offsets[field.id];
        } catch (IndexOutOfBoundsException e) {
            throw new FieldNotFoundException(field, this);
        }
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        return (V) oa[fieldOffset(field)];
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        oa[fieldOffset(field)] = value;
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        if (index < 0 || index > field.length)
            throw new ArrayIndexOutOfBoundsException(index);
        return (V) oa[fieldOffset(field) + index];
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        if (index < 0 || index > field.length)
            throw new ArrayIndexOutOfBoundsException(index);
        oa[fieldOffset(field) + index] = value;
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        System.arraycopy(oa, fieldOffset(field), target, offset, field.length);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        System.arraycopy(source, offset, oa, fieldOffset(field), field.length);
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord) source).oa, ((SimpleRecord<S>) source).fieldOffset(sourceField), oa, fieldOffset(field), field.length);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            System.arraycopy(array, 0, oa, fieldOffset(field), field.length);
        else {
            for (int i = 0; i < field.length; i++)
                oa[fieldOffset(field) + i] = source.get(sourceField, i);
        }
        for (int i = 0; i < field.length; i++)
            oa[fieldOffset(field) + i] = source.get(sourceField, i);
    }

    private void boundsCheck(Field field) {
        //            if (field.id < 0)
        //                throw new IllegalArgumentException("Incompatible field");
        //            if (fieldOffset(field) + field.size() > ba.length)
        //                throw new IllegalArgumentException("Incompatible field");
        //            if (field.id < 0)
        //                throw new IllegalArgumentException("Incompatible field");
        //            if (fieldOffset(field) + field.size() > ba.length)
        //                throw new IllegalArgumentException("Incompatible field");
    }

    private void boundsCheck(Field.ArrayField field, int index) {
        boundsCheck(field);
        if (index < 0 || index >= field.length)
            throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        boundsCheck(field);
        return getBoolean(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        boundsCheck(field);
        setBoolean(ba, fieldOffset(field), value);
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        boundsCheck(field);
        return getByte(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        boundsCheck(field);
        setByte(ba, fieldOffset(field), value);
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        boundsCheck(field);
        return getShort(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        boundsCheck(field);
        setShort(ba, fieldOffset(field), value);
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        boundsCheck(field);
        return getInt(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        boundsCheck(field);
        setInt(ba, fieldOffset(field), value);
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        boundsCheck(field);
        return getLong(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        boundsCheck(field);
        setLong(ba, fieldOffset(field), value);
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        boundsCheck(field);
        return getFloat(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        boundsCheck(field);
        setFloat(ba, fieldOffset(field), value);
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        boundsCheck(field);
        return getDouble(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        boundsCheck(field);
        setDouble(ba, fieldOffset(field), value);
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        boundsCheck(field);
        return getChar(ba, fieldOffset(field));
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        boundsCheck(field);
        setChar(ba, fieldOffset(field), value);
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getBoolean(ba, fieldOffset(field) + offset(index, BOOLEAN_SHIFT));
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        boundsCheck(field, index);
        setBoolean(ba, fieldOffset(field) + offset(index, BOOLEAN_SHIFT), value);
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, BOOLEAN_SHIFT);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, BOOLEAN_SHIFT);
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << BOOLEAN_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, BOOLEAN_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setBoolean(ba, fieldOffset(field) + offset(i, BOOLEAN_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getByte(ba, fieldOffset(field) + offset(index, BYTE_SHIFT));
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        boundsCheck(field, index);
        setByte(ba, fieldOffset(field) + offset(index, BYTE_SHIFT), value);
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, BYTE_SHIFT);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, BYTE_SHIFT);
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << BYTE_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, BYTE_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setByte(ba, fieldOffset(field) + offset(i, BYTE_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getShort(ba, fieldOffset(field) + offset(index, SHORT_SHIFT));
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        boundsCheck(field, index);
        setShort(ba, fieldOffset(field) + offset(index, SHORT_SHIFT), value);
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, SHORT_SHIFT);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, SHORT_SHIFT);
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << SHORT_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, SHORT_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setShort(ba, fieldOffset(field) + offset(i, SHORT_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getInt(ba, fieldOffset(field) + offset(index, INT_SHIFT));
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        boundsCheck(field, index);
        setInt(ba, fieldOffset(field) + offset(index, INT_SHIFT), value);
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, INT_SHIFT);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, INT_SHIFT);
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << INT_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, INT_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setInt(ba, fieldOffset(field) + offset(i, INT_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getLong(ba, fieldOffset(field) + offset(index, LONG_SHIFT));
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        boundsCheck(field, index);
        setLong(ba, fieldOffset(field) + offset(index, LONG_SHIFT), value);
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        boundsCheck(field);
        getArrayLong(ba, fieldOffset(field), target, offset, field.length, LONG_SHIFT);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        boundsCheck(field);
        setArrayLong(ba, fieldOffset(field), source, offset, field.length, LONG_SHIFT);
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << LONG_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArrayLong(ba, fieldOffset(field), array, 0, field.length, LONG_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setLong(ba, fieldOffset(field) + offset(i, LONG_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getFloat(ba, fieldOffset(field) + offset(index, FLOAT_SHIFT));
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        boundsCheck(field, index);
        setFloat(ba, fieldOffset(field) + offset(index, FLOAT_SHIFT), value);
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, FLOAT_SHIFT);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, FLOAT_SHIFT);
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << FLOAT_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, FLOAT_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setFloat(ba, fieldOffset(field) + offset(i, FLOAT_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getDouble(ba, fieldOffset(field) + offset(index, DOUBLE_SHIFT));
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        boundsCheck(field, index);
        setDouble(ba, fieldOffset(field) + offset(index, DOUBLE_SHIFT), value);
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        boundsCheck(field);
        getArrayLong(ba, fieldOffset(field), target, offset, field.length, DOUBLE_SHIFT);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        boundsCheck(field);
        setArrayLong(ba, fieldOffset(field), source, offset, field.length, DOUBLE_SHIFT);
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << DOUBLE_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArrayLong(ba, fieldOffset(field), array, 0, field.length, DOUBLE_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setDouble(ba, fieldOffset(field) + offset(i, DOUBLE_SHIFT), source.get(sourceField, i));
        }
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        boundsCheck(field, index);
        return getChar(ba, fieldOffset(field) + offset(index, CHAR_SHIFT));
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        boundsCheck(field, index);
        setChar(ba, fieldOffset(field) + offset(index, CHAR_SHIFT), value);
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        boundsCheck(field);
        getArray(ba, fieldOffset(field), target, offset, field.length, CHAR_SHIFT);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        boundsCheck(field);
        setArray(ba, fieldOffset(field), source, offset, field.length, CHAR_SHIFT);
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        boundsCheck(field);
        Object array = null;
        if (source instanceof SimpleRecord)
            System.arraycopy(((SimpleRecord<S>) source).ba, ((SimpleRecord<S>) source).fieldOffset(sourceField), ba, fieldOffset(field), field.length << CHAR_SHIFT);
        else if (source instanceof DynamicRecord && (array = ((DynamicRecord) source).get(sourceField)) != null)
            setArray(ba, fieldOffset(field), array, 0, field.length, CHAR_SHIFT);
        else {
            for (int i = 0; i < field.length; i++)
                setChar(ba, fieldOffset(field) + offset(i, CHAR_SHIFT), source.get(sourceField, i));
        }
    }
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

    private static long byteOffset(long i) {
        return base + i;
    }

    private static long offset(long index, int typeSizeShift) {
        return index << typeSizeShift;
    }

    private static boolean getBoolean(byte[] array, long i) {
        return unsafe.getBoolean(array, byteOffset(i));
    }

    private static void setBoolean(byte[] array, long i, boolean value) {
        unsafe.putBoolean(array, byteOffset(i), value);
    }

    private static byte getByte(byte[] array, long i) {
        return unsafe.getByte(array, byteOffset(i));
    }

    private static void setByte(byte[] array, long i, byte value) {
        unsafe.putByte(array, byteOffset(i), value);
    }

    private static short getShort(byte[] array, long i) {
        return unsafe.getShort(array, byteOffset(i));
    }

    private static void setShort(byte[] array, long i, short value) {
        unsafe.putShort(array, byteOffset(i), value);
    }

    private static int getInt(byte[] array, long i) {
        return unsafe.getInt(array, byteOffset(i));
    }

    private static void setInt(byte[] array, long i, int value) {
        unsafe.putInt(array, byteOffset(i), value);
    }

    private static long getLong(byte[] array, long i) {
        return unsafe.getLong(array, byteOffset(i));
    }

    private static void setLong(byte[] array, long i, long value) {
        unsafe.putLong(array, byteOffset(i), value);
    }

    private static char getChar(byte[] array, long i) {
        return unsafe.getChar(array, byteOffset(i));
    }

    private static void setChar(byte[] array, long i, char value) {
        unsafe.putChar(array, byteOffset(i), value);
    }

    private static float getFloat(byte[] array, long i) {
        return unsafe.getFloat(array, byteOffset(i));
    }

    private static void setFloat(byte[] array, long i, float value) {
        unsafe.putFloat(array, byteOffset(i), value);
    }

    private static double getDouble(byte[] array, long i) {
        return unsafe.getDouble(array, byteOffset(i));
    }

    private static void setDouble(byte[] array, long i, double value) {
        unsafe.putDouble(array, byteOffset(i), value);
    }

    private static void getArray(byte[] array, long i, Object target, int offset, int length, int shift) {
        unsafe.copyMemory(array, byteOffset(i), target, base + (offset << shift), length << shift);
    }

    private static void setArray(byte[] array, long i, Object source, int offset, int length, int shift) {
        unsafe.copyMemory(source, base + (offset << shift), array, byteOffset(i), length << shift);
    }
    private static void getArrayLong(byte[] array, long i, Object target, int offset, int length, int shift) {
        unsafe.copyMemory(array, byteOffset(i), target, baseLong + (offset << shift), length << shift);
    }

    private static void setArrayLong(byte[] array, long i, Object source, int offset, int length, int shift) {
        unsafe.copyMemory(source, baseLong + (offset << shift), array, byteOffset(i), length << shift);
    }
    private static final int BOOLEAN_SHIFT = 0;
    private static final int BYTE_SHIFT = 0;
    private static final int SHORT_SHIFT = 1;
    private static final int CHAR_SHIFT = 1;
    private static final int INT_SHIFT = 2;
    private static final int FLOAT_SHIFT = 2;
    private static final int LONG_SHIFT = 3;
    private static final int DOUBLE_SHIFT = 3;
}

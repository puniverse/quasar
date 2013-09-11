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
final class DynamicGeneratedRecord<R> extends DynamicRecord<R> {
    
    static Accessor generateAccessor(Class<?> type, Field<?, ?> field) {
        return null;
    }
    
    DynamicGeneratedRecord(DynamicRecordType<R> recordType, Object target) {
        super(recordType, target);
    }

    protected DynamicGeneratedRecord(DynamicRecordType<R> recordType) {
        super(recordType);
    }

    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        return ((BooleanAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        ((BooleanAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        return ((ByteAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        ((ByteAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        return ((ShortAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        ((ShortAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        return ((IntAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        ((IntAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        return ((LongAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        ((LongAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        return ((FloatAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        ((FloatAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        return ((DoubleAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        ((DoubleAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        return ((CharAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        ((CharAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        return (V) ((ObjectAccessor) entry(field).accessor).get(obj);
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        ((ObjectAccessor) entry(field).accessor).set(obj, value);
    }

    @Override
    boolean[] get(Field.BooleanArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((BooleanArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((BooleanIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((BooleanArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((BooleanIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((BooleanArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((BooleanArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((BooleanArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((BooleanArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    byte[] get(Field.ByteArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((ByteArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((ByteIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((ByteArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ByteIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ByteArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((ByteArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ByteArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((ByteArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    short[] get(Field.ShortArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((ShortArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((ShortIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((ShortArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ShortIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ShortArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((ShortArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ShortArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((ShortArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    int[] get(Field.IntArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((IntArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((IntIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((IntArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((IntIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((IntArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((IntArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((IntArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((IntArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    long[] get(Field.LongArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((LongArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((LongIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((LongArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((LongIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((LongArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((LongArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((LongArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((LongArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    float[] get(Field.FloatArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((FloatArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((FloatIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((FloatArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((FloatIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((FloatArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((FloatArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((FloatArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((FloatArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    double[] get(Field.DoubleArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((DoubleArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((DoubleIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((DoubleArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((DoubleIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((DoubleArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((DoubleArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((DoubleArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((DoubleArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    char[] get(Field.CharArrayField<? super R> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((CharArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((CharIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((CharArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((CharIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((CharArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((CharArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((CharArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((CharArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return (V[]) ((ObjectArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            return (V) ((ObjectIndexedAccessor) entry.accessor).get(obj, index);
        else
            return (V) ((ObjectArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ObjectIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ObjectArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = (V)accessor.get(obj, i);
        } else
            System.arraycopy(((ObjectArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ObjectArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        final DynamicRecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, (V[])((ObjectArrayAccessor) entry.accessor).get(obj), 0);
    }

    static abstract class Accessor {
    }

    static abstract class BooleanAccessor extends Accessor {
        public abstract boolean get(Object target);

        public abstract void set(Object target, boolean value);
    }

    static abstract class BooleanArrayAccessor extends Accessor {
        public abstract boolean[] get(Object target);
    }

    static abstract class BooleanIndexedAccessor extends Accessor {
        public abstract boolean get(Object target, int index);

        public abstract void set(Object target, int index, boolean value);
    }

    static abstract class ByteAccessor extends Accessor {
        public abstract byte get(Object target);

        public abstract void set(Object target, byte value);
    }

    static abstract class ByteArrayAccessor extends Accessor {
        public abstract byte[] get(Object target);
    }

    static abstract class ByteIndexedAccessor extends Accessor {
        public abstract byte get(Object target, int index);

        public abstract void set(Object target, int index, byte value);
    }

    static abstract class ShortAccessor extends Accessor {
        public abstract short get(Object target);

        public abstract void set(Object target, short value);
    }

    static abstract class ShortArrayAccessor extends Accessor {
        public abstract short[] get(Object target);
    }

    static abstract class ShortIndexedAccessor extends Accessor {
        public abstract short get(Object target, int index);

        public abstract void set(Object target, int index, short value);
    }

    static abstract class IntAccessor extends Accessor {
        public abstract int get(Object target);

        public abstract void set(Object target, int value);
    }

    static abstract class IntArrayAccessor extends Accessor {
        public abstract int[] get(Object target);
    }

    static abstract class IntIndexedAccessor extends Accessor {
        public abstract int get(Object target, int index);

        public abstract void set(Object target, int index, int value);
    }

    static abstract class LongAccessor extends Accessor {
        public abstract long get(Object target);

        public abstract void set(Object target, long value);
    }

    static abstract class LongArrayAccessor extends Accessor {
        public abstract long[] get(Object target);
    }

    static abstract class LongIndexedAccessor extends Accessor {
        public abstract long get(Object target, int index);

        public abstract void set(Object target, int index, long value);
    }

    static abstract class FloatAccessor extends Accessor {
        public abstract float get(Object target);

        public abstract void set(Object target, float value);
    }

    static abstract class FloatArrayAccessor extends Accessor {
        public abstract float[] get(Object target);
    }

    static abstract class FloatIndexedAccessor extends Accessor {
        public abstract float get(Object target, int index);

        public abstract void set(Object target, int index, float value);
    }

    static abstract class DoubleAccessor extends Accessor {
        public abstract double get(Object target);

        public abstract void set(Object target, double value);
    }

    static abstract class DoubleArrayAccessor extends Accessor {
        public abstract double[] get(Object target);
    }

    static abstract class DoubleIndexedAccessor extends Accessor {
        public abstract double get(Object target, int index);

        public abstract void set(Object target, int index, double value);
    }

    static abstract class CharAccessor extends Accessor {
        public abstract char get(Object target);

        public abstract void set(Object target, char value);
    }

    static abstract class CharArrayAccessor extends Accessor {
        public abstract char[] get(Object target);
    }

    static abstract class CharIndexedAccessor extends Accessor {
        public abstract char get(Object target, int index);

        public abstract void set(Object target, int index, char value);
    }

    static abstract class ObjectAccessor extends Accessor {
        public abstract Object get(Object target);

        public abstract void set(Object target, Object value);
    }

    static abstract class ObjectArrayAccessor extends Accessor {
        public abstract Object[] get(Object target);
    }

    static abstract class ObjectIndexedAccessor extends Accessor {
        public abstract Object get(Object target, int index);

        public abstract void set(Object target, int index, Object value);
    }
}

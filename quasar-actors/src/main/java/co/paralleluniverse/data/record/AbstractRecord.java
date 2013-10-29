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
import java.util.Set;

/**
 *
 * @author pron
 */
public abstract class AbstractRecord<R> implements Record<R> {
    public final RecordType<R> type;

    protected AbstractRecord(RecordType<R> type) {
        this.type = type;
    }

    @Override
    public RecordType<R> type() {
        return type;
    }
    
    @Override
    public final Set<Field<? super R, ?>> fields() {
        return type.fields();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        return obj instanceof DelegatingEquals ? obj.equals(this) : obj == this;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Record@").append(Integer.toHexString(System.identityHashCode(this))).append('{');
        for (Field<? super R, ?> field : fields()) {
            sb.append(field.name).append(": ");
            switch (field.type()) {
                case Field.BOOLEAN:
                    sb.append(get((Field.BooleanField<? super R>) field));
                    break;
                case Field.BYTE:
                    sb.append(get((Field.ByteField<? super R>) field));
                    break;
                case Field.SHORT:
                    sb.append(get((Field.ShortField<? super R>) field));
                    break;
                case Field.INT:
                    sb.append(get((Field.IntField<? super R>) field));
                    break;
                case Field.LONG:
                    sb.append(get((Field.LongField<? super R>) field));
                    break;
                case Field.FLOAT:
                    sb.append(get((Field.FloatField<? super R>) field));
                    break;
                case Field.DOUBLE:
                    sb.append(get((Field.DoubleField<? super R>) field));
                    break;
                case Field.CHAR:
                    sb.append(get((Field.CharField<? super R>) field));
                    break;
                case Field.OBJECT:
                    sb.append(get((Field.ObjectField<? super R, ?>) field));
                    break;
                case Field.BOOLEAN_ARRAY: {
                    Field.BooleanArrayField<? super R> f = (Field.BooleanArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.BYTE_ARRAY: {
                    Field.ByteArrayField<? super R> f = (Field.ByteArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.SHORT_ARRAY: {
                    Field.ShortArrayField<? super R> f = (Field.ShortArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.INT_ARRAY: {
                    Field.IntArrayField<? super R> f = (Field.IntArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.LONG_ARRAY: {
                    Field.LongArrayField<? super R> f = (Field.LongArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.FLOAT_ARRAY: {
                    Field.FloatArrayField<? super R> f = (Field.FloatArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.DOUBLE_ARRAY: {
                    Field.DoubleArrayField<? super R> f = (Field.DoubleArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.CHAR_ARRAY: {
                    Field.CharArrayField<? super R> f = (Field.CharArrayField<? super R>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                case Field.OBJECT_ARRAY: {
                    Field.ObjectArrayField<? super R, ?> f = (Field.ObjectArrayField<? super R, ?>) field;
                    sb.append('[');
                    if (f.length > 0) {
                        for (int i = 0; i < f.length; i++)
                            sb.append(get(f, i)).append(", ");
                        sb.delete(sb.length() - 2, sb.length());
                    }
                    sb.append(']');
                    break;
                }
                default:
                    throw new AssertionError();
            }
            sb.append(", ");
        }
        if (fields().size() > 0)
            sb.delete(sb.length() - 2, sb.length());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public <V> V get(Field<? super R, V> field) {
        return field.get(this);
    }

    @Override
    public <V> void set(Field<? super R, V> field, V value) {
        field.set(this, value);
    }

    @Override
    public <V> V get(Field.ArrayField<? super R, V> field, int index) {
        return field.get(this, index);
    }

    @Override
    public <V> void set(Field.ArrayField<? super R, V> field, int index, V value) {
        field.set(this, index, value);
    }

    @Override
    public <V> void get(Field.ArrayField<? super R, V> field, V[] target, int offset) {
        field.get(this, target, offset);
    }

    @Override
    public <V> void set(Field.ArrayField<? super R, V> field, V[] source, int offset) {
        field.set(this, source, offset);
    }

    @Override
    public <S, V> void set(Field.ArrayField<? super R, V> field, Record<S> source, Field.ArrayField<? super S, V> sourceField) {
        field.set(this, sourceField.get(source));
    }

    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        throw new FieldNotFoundException(field, this);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        throw fields().contains(field) ? new ReadOnlyFieldException(field, this) : new FieldNotFoundException(field, this);
    }
}

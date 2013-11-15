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
public interface Record<R> {
    RecordType<R> type();

    Set<Field<? super R, ?>> fields();

    void write(java.io.ObjectOutput out) throws java.io.IOException;

    void read(java.io.ObjectInput in) throws java.io.IOException;

    void read(java.io.ObjectInput in, int numFields) throws java.io.IOException;

    <V> V get(Field<? super R, V> field);

    <V> void set(Field<? super R, V> field, V value);

    <V> V get(Field.ArrayField<? super R, V> field, int index);

    <V> void set(Field.ArrayField<? super R, V> field, int index, V value);

    <V> void get(Field.ArrayField<? super R, V> field, V[] target, int offset);

    <V> void set(Field.ArrayField<? super R, V> field, V[] source, int offset);

    <S, V> void set(Field.ArrayField<? super R, V> field, Record<S> source, Field.ArrayField<? super S, V> sourceField);

    boolean get(Field.BooleanField<? super R> field);

    void set(Field.BooleanField<? super R> field, boolean value);

    byte get(Field.ByteField<? super R> field);

    void set(Field.ByteField<? super R> field, byte value);

    short get(Field.ShortField<? super R> field);

    void set(Field.ShortField<? super R> field, short value);

    int get(Field.IntField<? super R> field);

    void set(Field.IntField<? super R> field, int value);

    long get(Field.LongField<? super R> field);

    void set(Field.LongField<? super R> field, long value);

    float get(Field.FloatField<? super R> field);

    void set(Field.FloatField<? super R> field, float value);

    double get(Field.DoubleField<? super R> field);

    void set(Field.DoubleField<? super R> field, double value);

    char get(Field.CharField<? super R> field);

    void set(Field.CharField<? super R> field, char value);

    <V> V get(Field.ObjectField<? super R, V> field);

    <V> void set(Field.ObjectField<? super R, V> field, V value);

    boolean get(Field.BooleanArrayField<? super R> field, int index);

    void set(Field.BooleanArrayField<? super R> field, int index, boolean value);

    void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset);

    void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset);

    <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField);

    byte get(Field.ByteArrayField<? super R> field, int index);

    void set(Field.ByteArrayField<? super R> field, int index, byte value);

    void get(Field.ByteArrayField<? super R> field, byte[] target, int offset);

    void set(Field.ByteArrayField<? super R> field, byte[] source, int offset);

    <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField);

    short get(Field.ShortArrayField<? super R> field, int index);

    void set(Field.ShortArrayField<? super R> field, int index, short value);

    void get(Field.ShortArrayField<? super R> field, short[] target, int offset);

    void set(Field.ShortArrayField<? super R> field, short[] source, int offset);

    <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField);

    int get(Field.IntArrayField<? super R> field, int index);

    void set(Field.IntArrayField<? super R> field, int index, int value);

    void get(Field.IntArrayField<? super R> field, int[] target, int offset);

    void set(Field.IntArrayField<? super R> field, int[] source, int offset);

    <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField);

    long get(Field.LongArrayField<? super R> field, int index);

    void set(Field.LongArrayField<? super R> field, int index, long value);

    void get(Field.LongArrayField<? super R> field, long[] target, int offset);

    void set(Field.LongArrayField<? super R> field, long[] source, int offset);

    <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField);

    float get(Field.FloatArrayField<? super R> field, int index);

    void set(Field.FloatArrayField<? super R> field, int index, float value);

    void get(Field.FloatArrayField<? super R> field, float[] target, int offset);

    void set(Field.FloatArrayField<? super R> field, float[] source, int offset);

    <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField);

    double get(Field.DoubleArrayField<? super R> field, int index);

    void set(Field.DoubleArrayField<? super R> field, int index, double value);

    void get(Field.DoubleArrayField<? super R> field, double[] target, int offset);

    void set(Field.DoubleArrayField<? super R> field, double[] source, int offset);

    <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField);

    char get(Field.CharArrayField<? super R> field, int index);

    void set(Field.CharArrayField<? super R> field, int index, char value);

    void get(Field.CharArrayField<? super R> field, char[] target, int offset);

    void set(Field.CharArrayField<? super R> field, char[] source, int offset);

    <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField);

    <V> V get(Field.ObjectArrayField<? super R, V> field, int index);

    <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value);

    <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset);

    <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset);

    <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField);
}

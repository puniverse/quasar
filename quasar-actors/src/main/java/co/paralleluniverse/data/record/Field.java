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

import java.lang.reflect.Array;
import java.util.Objects;

/**
 *
 * @author pron
 */
public abstract class Field<R, V> {
    public static <R> BooleanField<R> booleanField(String name, int id) {
        return new BooleanField<R>(name, id);
    }

    public static <R> ByteField<R> byteField(String name, int id) {
        return new ByteField<R>(name, id);
    }

    public static <R> ShortField<R> shortField(String name, int id) {
        return new ShortField<R>(name, id);
    }

    public static <R> IntField<R> intField(String name, int id) {
        return new IntField<R>(name, id);
    }

    public static <R> LongField<R> longField(String name, int id) {
        return new LongField<R>(name, id);
    }

    public static <R> FloatField<R> floatField(String name, int id) {
        return new FloatField<R>(name, id);
    }

    public static <R> DoubleField<R> doubleField(String name, int id) {
        return new DoubleField<R>(name, id);
    }

    public static <R> CharField<R> charField(String name, int id) {
        return new CharField<R>(name, id);
    }

    public static <R, V> ObjectField<R, V> objectField(String name, Class<? extends V> type, int id) {
        return new ObjectField<R, V>(name, type, id);
    }

    public static <R> BooleanArrayField<R> booleanArrayField(String name, int length, int id) {
        return new BooleanArrayField<R>(name, length, id);
    }

    public static <R> ByteArrayField<R> byteArrayField(String name, int length, int id) {
        return new ByteArrayField<R>(name, length, id);
    }

    public static <R> ShortArrayField<R> shortArrayField(String name, int length, int id) {
        return new ShortArrayField<R>(name, length, id);
    }

    public static <R> IntArrayField<R> intArrayField(String name, int length, int id) {
        return new IntArrayField<R>(name, length, id);
    }

    public static <R> LongArrayField<R> longArrayField(String name, int length, int id) {
        return new LongArrayField<R>(name, length, id);
    }

    public static <R> FloatArrayField<R> floatArrayField(String name, int length, int id) {
        return new FloatArrayField<R>(name, length, id);
    }

    public static <R> DoubleArrayField<R> doubleArrayField(String name, int length, int id) {
        return new DoubleArrayField<R>(name, length, id);
    }

    public static <R> CharArrayField<R> charArrayField(String name, int length, int id) {
        return new CharArrayField<R>(name, length, id);
    }

    public static <R, V> ObjectArrayField<R, V> objectArrayField(String name, Class<V> type, int length, int id) {
        return new ObjectArrayField<R, V>(name, type, length, id);
    }

    ///////////////////////////////////
    static final int BOOLEAN = 1;
    static final int BYTE = 2;
    static final int SHORT = 3;
    static final int INT = 4;
    static final int LONG = 5;
    static final int FLOAT = 6;
    static final int DOUBLE = 7;
    static final int CHAR = 8;
    static final int OBJECT = 9;
    static final int BOOLEAN_ARRAY = 11;
    static final int BYTE_ARRAY = 12;
    static final int SHORT_ARRAY = 13;
    static final int INT_ARRAY = 14;
    static final int LONG_ARRAY = 15;
    static final int FLOAT_ARRAY = 16;
    static final int DOUBLE_ARRAY = 17;
    static final int CHAR_ARRAY = 18;
    static final int OBJECT_ARRAY = 19;
    //
    static final int BOOLEAN_SIZE = 1;
    static final int BYTE_SIZE = 1;
    static final int SHORT_SIZE = 2;
    static final int CHAR_SIZE = 2;
    static final int INT_SIZE = 4;
    static final int FLOAT_SIZE = 4;
    static final int LONG_SIZE = 8;
    static final int DOUBLE_SIZE = 8;
    //
    final String name;
    final int id;

    Field(String name, int id) {
        this.name = name;
        this.id = id;
    }

    Field(String name) {
        this(name, -1);
    }

    public final int id() {
        if (id == -1)
            throw new UnsupportedOperationException("id not set");
        return id;
    }

    public final String name() {
        return name;
    }

    abstract int type();

    abstract int size();

    abstract Class<?> typeClass();

    abstract void set(Record<? extends R> record, V value);

    abstract V get(Record<? extends R> record);

    @Override
    public final int hashCode() {
        int hash = 5;
        hash = 97 * hash + type();
        hash = 97 * hash + id;
        hash = 97 * hash + Objects.hashCode(name);
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Field))
            return false;
        final Field other = (Field) obj;
        return id == other.id && type() == other.type() && Objects.equals(name(), other.name());
    }

    @Override
    public String toString() {
        return name;
    }

    //////////////
    public static abstract class ScalarField<R, V> extends Field<R, V> {
        ScalarField(String name, int id) {
            super(name, id);
        }
    }

    public static abstract class ArrayField<R, V> extends Field<R, V[]> {
        public final int length;

        ArrayField(String name, int length, int id) {
            super(name, id);
            this.length = length;
        }

        abstract V get(Record<? extends R> record, int index);

        abstract void set(Record<? extends R> record, int index, V value);

        abstract void get(Record<? extends R> record, V[] target, int offset);

        abstract void set(Record<? extends R> record, V[] source, int offset);

        @Override
        final V[] get(Record<? extends R> record) {
            final V[] array = (V[]) new Object[length];
            get(record, array, 0);
            return array;
        }

        @Override
        final void set(Record<? extends R> record, V[] array) {
            set(record, array, 0);
        }
    }

    ////////////////
    public static final class BooleanField<R> extends ScalarField<R, Boolean> {
        BooleanField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return BOOLEAN;
        }

        @Override
        int size() {
            return BOOLEAN_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return boolean.class;
        }

        @Override
        void set(Record<? extends R> record, Boolean value) {
            record.set(this, value.booleanValue());
        }

        @Override
        Boolean get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class ByteField<R> extends ScalarField<R, Byte> {
        ByteField(String name, int id) {
            super(name, id);
        }
        
        @Override
        int type() {
            return BYTE;
        }

        @Override
        int size() {
            return BYTE_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return byte.class;
        }

        @Override
        void set(Record<? extends R> record, Byte value) {
            record.set(this, value.byteValue());
        }

        @Override
        Byte get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class ShortField<R> extends ScalarField<R, Short> {
        ShortField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return SHORT;
        }

        @Override
        int size() {
            return SHORT_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return short.class;
        }

        @Override
        void set(Record<? extends R> record, Short value) {
            record.set(this, value.shortValue());
        }

        @Override
        Short get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class IntField<R> extends ScalarField<R, Integer> {
        IntField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return INT;
        }

        @Override
        int size() {
            return INT_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return int.class;
        }

        @Override
        void set(Record<? extends R> record, Integer value) {
            record.set(this, value.intValue());
        }

        @Override
        Integer get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class LongField<R> extends ScalarField<R, Long> {
        LongField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return LONG;
        }

        @Override
        int size() {
            return LONG_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return long.class;
        }

        @Override
        void set(Record<? extends R> record, Long value) {
            record.set(this, value.longValue());
        }

        @Override
        Long get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class FloatField<R> extends ScalarField<R, Float> {
        FloatField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return FLOAT;
        }

        @Override
        int size() {
            return FLOAT_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return float.class;
        }

        @Override
        void set(Record<? extends R> record, Float value) {
            record.set(this, value.floatValue());
        }

        @Override
        Float get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class DoubleField<R> extends ScalarField<R, Double> {
        DoubleField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return DOUBLE;
        }

        @Override
        int size() {
            return DOUBLE_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return double.class;
        }

        @Override
        void set(Record<? extends R> record, Double value) {
            record.set(this, value.doubleValue());
        }

        @Override
        Double get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static final class CharField<R> extends ScalarField<R, Character> {
        CharField(String name, int id) {
            super(name, id);
        }

        @Override
        int type() {
            return CHAR;
        }

        @Override
        int size() {
            return CHAR_SIZE;
        }

        @Override
        Class<?> typeClass() {
            return char.class;
        }

        @Override
        void set(Record<? extends R> record, Character value) {
            record.set(this, value.charValue());
        }

        @Override
        Character get(Record<? extends R> record) {
            return record.get(this);
        }
    }

    public static class ObjectField<R, V> extends ScalarField<R, V> {
        private final Class<? extends V> clazz;

        ObjectField(String name, Class<? extends V> clazz, int id) {
            super(name, id);
            this.clazz = clazz;
        }

        @Override
        int type() {
            return OBJECT;
        }

        @Override
        int size() {
            return 0;
        }

        @Override
        Class<?> typeClass() {
            return clazz;
        }

        @Override
        void set(Record<? extends R> record, V value) {
            record.set(this, value);
        }

        @Override
        V get(Record<? extends R> record) {
            return record.get(this);
        }
    }
    //////////////////////////

    public static final class BooleanArrayField<R> extends ArrayField<R, Boolean> {
        BooleanArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return BOOLEAN_ARRAY;
        }

        @Override
        int size() {
            return BOOLEAN_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return boolean[].class;
        }

        @Override
        Boolean get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Boolean value) {
            record.set(this, index, value.booleanValue());
        }

        @Override
        void get(Record<? extends R> record, Boolean[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Boolean[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].booleanValue());
        }
    }

    public static final class ByteArrayField<R> extends ArrayField<R, Byte> {
        ByteArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return BYTE_ARRAY;
        }

        @Override
        int size() {
            return BYTE_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return byte[].class;
        }

        @Override
        Byte get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Byte value) {
            record.set(this, index, value.byteValue());
        }

        @Override
        void get(Record<? extends R> record, Byte[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Byte[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].byteValue());
        }
    }

    public static final class ShortArrayField<R> extends ArrayField<R, Short> {
        ShortArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return SHORT_ARRAY;
        }

        @Override
        int size() {
            return SHORT_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return short[].class;
        }

        @Override
        Short get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Short value) {
           record.set(this, index, value.shortValue());
        }

        @Override
        void get(Record<? extends R> record, Short[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Short[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].shortValue());
        }
    }

    public static final class IntArrayField<R> extends ArrayField<R, Integer> {
        IntArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return INT_ARRAY;
        }

        @Override
        int size() {
            return INT_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return int[].class;
        }

        @Override
        Integer get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Integer value) {
            record.set(this, index, value.intValue());
        }

        @Override
        void get(Record<? extends R> record, Integer[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Integer[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].intValue());
        }
    }

    public static final class LongArrayField<R> extends ArrayField<R, Long> {
        LongArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return LONG_ARRAY;
        }

        @Override
        int size() {
            return LONG_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return long[].class;
        }

        @Override
        Long get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Long value) {
            record.set(this, index, value.longValue());
        }

        @Override
        void get(Record<? extends R> record, Long[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Long[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].longValue());
        }
    }

    public static final class FloatArrayField<R> extends ArrayField<R, Float> {
        FloatArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return FLOAT_ARRAY;
        }

        @Override
        int size() {
            return FLOAT_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return float[].class;
        }

        @Override
        Float get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Float value) {
            record.set(this, index, value.floatValue());
        }

        @Override
        void get(Record<? extends R> record, Float[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Float[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].floatValue());
        }
    }

    public static final class DoubleArrayField<R> extends ArrayField<R, Double> {
        DoubleArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return DOUBLE_ARRAY;
        }

        @Override
        int size() {
            return DOUBLE_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return double[].class;
        }

        @Override
        Double get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Double value) {
            record.set(this, index, value.doubleValue());
        }

        @Override
        void get(Record<? extends R> record, Double[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Double[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].doubleValue());
        }
    }

    public static final class CharArrayField<R> extends ArrayField<R, Character> {
        CharArrayField(String name, int length, int id) {
            super(name, length, id);
        }

        @Override
        int type() {
            return CHAR_ARRAY;
        }

        @Override
        int size() {
            return CHAR_SIZE * length;
        }

        @Override
        Class<?> typeClass() {
            return char[].class;
        }

        @Override
        Character get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, Character value) {
            record.set(this, index, value.charValue());
        }

        @Override
        void get(Record<? extends R> record, Character[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, Character[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i].charValue());
        }
    }

    public static final class ObjectArrayField<R, V> extends ArrayField<R, V> {
        private final Class<V[]> clazz;

        ObjectArrayField(String name, Class<V> elemClazz, int length, int id) {
            super(name, length, id);
            this.clazz = (Class<V[]>) Array.newInstance(elemClazz, 0).getClass();
        }

        @Override
        int type() {
            return OBJECT_ARRAY;
        }

        @Override
        int size() {
            return 0;
        }

        @Override
        Class<?> typeClass() {
            return clazz;
        }

        @Override
        V get(Record<? extends R> record, int index) {
            return record.get(this, index);
        }

        @Override
        void set(Record<? extends R> record, int index, V value) {
            record.set(this, index, value);
        }

        @Override
        void get(Record<? extends R> record, V[] target, int offset) {
            for (int i = 0; i < length; i++)
                target[offset + i] = record.get(this, i);
        }

        @Override
        void set(Record<? extends R> record, V[] source, int offset) {
            for (int i = 0; i < length; i++)
                record.set(this, i, source[offset + i]);
        }
    }
}

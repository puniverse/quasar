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

import com.google.common.reflect.TypeToken;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * A {@link Record record}'s field.
 *
 * @param <R> The {@link RecordType}
 * @param <V> The field {@link #typeClass() type}.
 * @author pron
 */
public abstract class Field<R, V> {
    public static final int TRANSIENT = 1;

    public static <R> BooleanField<R> booleanField(String name, int id, int flags) {
        return new BooleanField<R>(name, id, flags);
    }

    public static <R> BooleanField<R> booleanField(String name, int id) {
        return booleanField(name, id, 0);
    }

    public static <R> ByteField<R> byteField(String name, int id, int flags) {
        return new ByteField<R>(name, id, flags);
    }

    public static <R> ByteField<R> byteField(String name, int id) {
        return byteField(name, id, 0);
    }

    public static <R> ShortField<R> shortField(String name, int id, int flags) {
        return new ShortField<R>(name, id, flags);
    }

    public static <R> ShortField<R> shortField(String name, int id) {
        return shortField(name, id, 0);
    }

    public static <R> IntField<R> intField(String name, int id, int flags) {
        return new IntField<R>(name, id, flags);
    }

    public static <R> IntField<R> intField(String name, int id) {
        return intField(name, id, 0);
    }

    public static <R> LongField<R> longField(String name, int id, int flags) {
        return new LongField<R>(name, id, flags);
    }

    public static <R> LongField<R> longField(String name, int id) {
        return longField(name, id, 0);
    }

    public static <R> FloatField<R> floatField(String name, int id, int flags) {
        return new FloatField<R>(name, id, flags);
    }

    public static <R> FloatField<R> floatField(String name, int id) {
        return floatField(name, id, 0);
    }

    public static <R> DoubleField<R> doubleField(String name, int id, int flags) {
        return new DoubleField<R>(name, id, flags);
    }

    public static <R> DoubleField<R> doubleField(String name, int id) {
        return doubleField(name, id, 0);
    }

    public static <R> CharField<R> charField(String name, int id, int flags) {
        return new CharField<R>(name, id, flags);
    }

    public static <R> CharField<R> charField(String name, int id) {
        return charField(name, id, 0);
    }

    public static <R, V> ObjectField<R, V> objectField(String name, TypeToken<V> type, int id, int flags) {
        return new ObjectField<R, V>(name, type.getRawType(), id, flags);
    }

    public static <R, V> ObjectField<R, V> objectField(String name, TypeToken<V> type, int id) {
        return objectField(name, type, id, 0);
    }

    public static <R, V> ObjectField<R, V> objectField(String name, Class<V> type, int id, int flags) {
        return new ObjectField<R, V>(name, type, id, flags);
    }

    public static <R, V> ObjectField<R, V> objectField(String name, Class<V> type, int id) {
        return objectField(name, type, id, 0);
    }

    public static <R> BooleanArrayField<R> booleanArrayField(String name, int length, int id, int flags) {
        return new BooleanArrayField<R>(name, length, id, flags);
    }

    public static <R> BooleanArrayField<R> booleanArrayField(String name, int length, int id) {
        return booleanArrayField(name, length, id, 0);
    }

    public static <R> ByteArrayField<R> byteArrayField(String name, int length, int id, int flags) {
        return new ByteArrayField<R>(name, length, id, flags);
    }

    public static <R> ByteArrayField<R> byteArrayField(String name, int length, int id) {
        return byteArrayField(name, length, id, 0);
    }

    public static <R> ShortArrayField<R> shortArrayField(String name, int length, int id, int flags) {
        return new ShortArrayField<R>(name, length, id, flags);
    }

    public static <R> ShortArrayField<R> shortArrayField(String name, int length, int id) {
        return shortArrayField(name, length, id, 0);
    }

    public static <R> IntArrayField<R> intArrayField(String name, int length, int id, int flags) {
        return new IntArrayField<R>(name, length, id, flags);
    }

    public static <R> IntArrayField<R> intArrayField(String name, int length, int id) {
        return intArrayField(name, length, id, 0);
    }

    public static <R> LongArrayField<R> longArrayField(String name, int length, int id, int flags) {
        return new LongArrayField<R>(name, length, id, flags);
    }

    public static <R> LongArrayField<R> longArrayField(String name, int length, int id) {
        return longArrayField(name, length, id, 0);
    }

    public static <R> FloatArrayField<R> floatArrayField(String name, int length, int id, int flags) {
        return new FloatArrayField<R>(name, length, id, flags);
    }

    public static <R> FloatArrayField<R> floatArrayField(String name, int length, int id) {
        return floatArrayField(name, length, id, 0);
    }

    public static <R> DoubleArrayField<R> doubleArrayField(String name, int length, int id, int flags) {
        return new DoubleArrayField<R>(name, length, id, flags);
    }

    public static <R> DoubleArrayField<R> doubleArrayField(String name, int length, int id) {
        return doubleArrayField(name, length, id, 0);
    }

    public static <R> CharArrayField<R> charArrayField(String name, int length, int id, int flags) {
        return new CharArrayField<R>(name, length, id, flags);
    }

    public static <R> CharArrayField<R> charArrayField(String name, int length, int id) {
        return charArrayField(name, length, id, 0);
    }

    public static <R, V> ObjectArrayField<R, V> objectArrayField(String name, Class<V> type, int length, int id, int flags) {
        return new ObjectArrayField<R, V>(name, type, length, id, flags);
    }

    public static <R, V> ObjectArrayField<R, V> objectArrayField(String name, Class<V> type, int length, int id) {
        return ObjectArrayField.objectArrayField(name, type, length, id, 0);
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
    final int flags;

    Field(String name, int id, int flags) {
        this.name = name;
        this.id = id;
        this.flags = flags;
    }

    /**
     * The field's id. Ids are unique for fields of the same type in the same {@link RecordType}.
     *
     * @return the field's id
     */
    public final int id() {
        if (id == -1)
            throw new UnsupportedOperationException("id not set");
        return id;
    }

    /**
     * The field's name
     */
    public final String name() {
        return name;
    }

    /**
     * The field's flags.
     */
    public int flags() {
        return flags;
    }

    /**
     * Whether or not the field is transient.
     */
    public boolean isTransient() {
        return (flags & TRANSIENT) != 0;
    }

    abstract int type();

    abstract int size();

    /**
     * The field's type
     */
    public abstract Class<?> typeClass();

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
    public String toString() {
        return name;
    }

    //////////////
    /**
     * Represents a scalar field
     *
     * @param <R> The {@link RecordType}
     * @param <V> The field {@link #typeClass() type}.
     */
    public static abstract class ScalarField<R, V> extends Field<R, V> {
        ScalarField(String name, int id, int flags) {
            super(name, id, flags);
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
    }

    /**
     * Represents an array field
     *
     * @param <R> The {@link RecordType}
     * @param <V> The field {@link #typeClass() type}.
     */
    public static abstract class ArrayField<R, V> extends Field<R, V[]> {
        /**
         * The array length
         */
        public final int length;

        ArrayField(String name, int length, int id, int flags) {
            super(name, id, flags);
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

        @Override
        public final boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof ArrayField))
                return false;
            final ArrayField other = (ArrayField) obj;
            return id == other.id && type() == other.type() && length == other.length && Objects.equals(name(), other.name());
        }
    }

    ////////////////
    /**
     * A scalar {@code boolean} field
     *
     * @param <R> the record type
     */
    public static final class BooleanField<R> extends ScalarField<R, Boolean> {
        BooleanField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code byte} field
     *
     * @param <R> the record type
     */
    public static final class ByteField<R> extends ScalarField<R, Byte> {
        ByteField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code short} field
     *
     * @param <R> the record type
     */
    public static final class ShortField<R> extends ScalarField<R, Short> {
        ShortField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code int} field
     *
     * @param <R> the record type
     */
    public static final class IntField<R> extends ScalarField<R, Integer> {
        IntField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code long} field
     *
     * @param <R> the record type
     */
    public static final class LongField<R> extends ScalarField<R, Long> {
        LongField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code flaot} field
     *
     * @param <R> the record type
     */
    public static final class FloatField<R> extends ScalarField<R, Float> {
        FloatField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code double} field
     *
     * @param <R> the record type
     */
    public static final class DoubleField<R> extends ScalarField<R, Double> {
        DoubleField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code char} field
     *
     * @param <R> the record type
     */
    public static final class CharField<R> extends ScalarField<R, Character> {
        CharField(String name, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A scalar {@code Object} field
     *
     * @param <R> the record type
     * @param <V> the field's type
     */
    public static class ObjectField<R, V> extends ScalarField<R, V> {
        private final Class<?> clazz;

        ObjectField(String name, Class<?> clazz, int id, int flags) {
            super(name, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code boolean} array field
     *
     * @param <R> the record type
     */
    public static final class BooleanArrayField<R> extends ArrayField<R, Boolean> {
        BooleanArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code byte} array field
     *
     * @param <R> the record type
     */
    public static final class ByteArrayField<R> extends ArrayField<R, Byte> {
        ByteArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code short} array field
     *
     * @param <R> the record type
     */
    public static final class ShortArrayField<R> extends ArrayField<R, Short> {
        ShortArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * An {@code int} array field
     *
     * @param <R> the record type
     */
    public static final class IntArrayField<R> extends ArrayField<R, Integer> {
        IntArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code long} array field
     *
     * @param <R> the record type
     */
    public static final class LongArrayField<R> extends ArrayField<R, Long> {
        LongArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code float} array field
     *
     * @param <R> the record type
     */
    public static final class FloatArrayField<R> extends ArrayField<R, Float> {
        FloatArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code double} array field
     *
     * @param <R> the record type
     */
    public static final class DoubleArrayField<R> extends ArrayField<R, Double> {
        DoubleArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * A {@code char} array field
     *
     * @param <R> the record type
     */
    public static final class CharArrayField<R> extends ArrayField<R, Character> {
        CharArrayField(String name, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

    /**
     * An {@code Object} array field
     *
     * @param <R> the record type
     * @param <V> the field's element type
     */
    public static final class ObjectArrayField<R, V> extends ArrayField<R, V> {
        private final Class<V[]> clazz;

        ObjectArrayField(String name, Class<V> elemClazz, int length, int id, int flags) {
            super(name, length, id, flags);
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
        public Class<?> typeClass() {
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

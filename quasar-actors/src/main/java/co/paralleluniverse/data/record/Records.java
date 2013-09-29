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

import java.util.Collection;
import java.util.Objects;

/**
 *
 * @author pron
 */
public final class Records {
    public static <R> Record<R> delegate(Object owner, Record<R> record) {
        return new RecordDelegate<R>(owner, record);
    }

    public static <R> void setDelegate(Record<R> record, Object owner, Record<R> newDelegate) {
        if (!(record instanceof RecordDelegate))
            throw new UnsupportedOperationException("Record " + record + " is not a record delegate");
        ((RecordDelegate<R>) record).setDelegate(owner, newDelegate);
    }

    public static <R> Record<R> getDelegate(Record<R> record, Object owner) {
        if (!(record instanceof RecordDelegate))
            return record;
        return ((RecordDelegate<R>) record).getDelegate(owner);
    }

    public static <R> void copy(Record<R> source, Record<R> target) {
        copy(source, target, target.fields());
    }

    public static <R> void copy(Record<R> source, Record<R> target, Collection<Field<? super R, ?>> fields) {
        for (Field<? super R, ?> field : fields) {
            try {
                switch (field.type()) {
                    case Field.BOOLEAN:
                        target.set((Field.BooleanField) field, source.get((Field.BooleanField) field));
                        break;
                    case Field.BYTE:
                        target.set((Field.ByteField) field, source.get((Field.ByteField) field));
                        break;
                    case Field.SHORT:
                        target.set((Field.ShortField) field, source.get((Field.ShortField) field));
                        break;
                    case Field.INT:
                        target.set((Field.IntField) field, source.get((Field.IntField) field));
                        break;
                    case Field.LONG:
                        target.set((Field.LongField) field, source.get((Field.LongField) field));
                        break;
                    case Field.FLOAT:
                        target.set((Field.FloatField) field, source.get((Field.FloatField) field));
                        break;
                    case Field.DOUBLE:
                        target.set((Field.DoubleField) field, source.get((Field.DoubleField) field));
                        break;
                    case Field.CHAR:
                        target.set((Field.CharField) field, source.get((Field.CharField) field));
                        break;
                    case Field.OBJECT:
                        target.set((Field.ObjectField) field, source.get((Field.ObjectField) field));
                        break;
                    case Field.BOOLEAN_ARRAY:
                        target.set((Field.BooleanArrayField) field, source, (Field.BooleanArrayField) field);
                        break;
                    case Field.BYTE_ARRAY:
                        target.set((Field.ByteArrayField) field, source, (Field.ByteArrayField) field);
                        break;
                    case Field.SHORT_ARRAY:
                        target.set((Field.ShortArrayField) field, source, (Field.ShortArrayField) field);
                        break;
                    case Field.INT_ARRAY:
                        target.set((Field.IntArrayField) field, source, (Field.IntArrayField) field);
                        break;
                    case Field.LONG_ARRAY:
                        target.set((Field.LongArrayField) field, source, (Field.LongArrayField) field);
                        break;
                    case Field.FLOAT_ARRAY:
                        target.set((Field.FloatArrayField) field, source, (Field.FloatArrayField) field);
                        break;
                    case Field.DOUBLE_ARRAY:
                        target.set((Field.DoubleArrayField) field, source, (Field.DoubleArrayField) field);
                        break;
                    case Field.CHAR_ARRAY:
                        target.set((Field.CharArrayField) field, source, (Field.CharArrayField) field);
                        break;
                    case Field.OBJECT_ARRAY:
                        target.set((Field.ObjectArrayField) field, source, (Field.ObjectArrayField) field);
                        break;
                    default:
                        throw new AssertionError();
                }
            } catch (FieldNotFoundException e) {
            }
        }
    }

    public static <R> void clear(Record<R> record) {
        for (Field<? super R, ?> field : record.fields()) {
            try {
                switch (field.type()) {
                    case Field.BOOLEAN:
                        record.set((Field.BooleanField) field, false);
                        break;
                    case Field.BYTE:
                        record.set((Field.ByteField) field, (byte) 0);
                        break;
                    case Field.SHORT:
                        record.set((Field.ShortField) field, (short) 0);
                        break;
                    case Field.INT:
                        record.set((Field.IntField) field, 0);
                        break;
                    case Field.LONG:
                        record.set((Field.LongField) field, 0L);
                        break;
                    case Field.FLOAT:
                        record.set((Field.FloatField) field, 0.0f);
                        break;
                    case Field.DOUBLE:
                        record.set((Field.DoubleField) field, 0.0);
                        break;
                    case Field.CHAR:
                        record.set((Field.CharField) field, (char) 0);
                        break;
                    case Field.OBJECT:
                        record.set((Field.ObjectField) field, null);
                        break;
                    case Field.BOOLEAN_ARRAY: {
                        Field.BooleanArrayField f = (Field.BooleanArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, false);
                        break;
                    }
                    case Field.BYTE_ARRAY: {
                        Field.ByteArrayField f = (Field.ByteArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, (byte) 0);
                        break;
                    }
                    case Field.SHORT_ARRAY: {
                        Field.ShortArrayField f = (Field.ShortArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, (short) 0);
                        break;
                    }
                    case Field.INT_ARRAY: {
                        Field.IntArrayField f = (Field.IntArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, 0);
                        break;
                    }
                    case Field.LONG_ARRAY: {
                        Field.LongArrayField f = (Field.LongArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, 0L);
                        break;
                    }
                    case Field.FLOAT_ARRAY: {
                        Field.FloatArrayField f = (Field.FloatArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, 0.0f);
                        break;
                    }
                    case Field.DOUBLE_ARRAY: {
                        Field.DoubleArrayField f = (Field.DoubleArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, 0.0);
                        break;
                    }
                    case Field.CHAR_ARRAY: {
                        Field.CharArrayField f = (Field.CharArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, (char) 0);
                        break;
                    }
                    case Field.OBJECT_ARRAY: {
                        Field.ObjectArrayField f = (Field.ObjectArrayField) field;
                        for (int i = 0; i < f.length; i++)
                            record.set(f, i, null);
                        break;
                    }
                    default:
                        throw new AssertionError();
                }
            } catch (FieldNotFoundException e) {
            }
        }
    }

    public static <R> void clear(RecordArray<R> recordArray) {
        if(recordArray instanceof SimpleRecordArray) {
            ((SimpleRecordArray<R>)recordArray).clear();
        } else
            throw new UnsupportedOperationException();
    }
    
    public static <R> boolean deepEquals(Record<R> a, Record<R> b) {
        if (a == b)
            return true;
        if (a == null | b == null)
            return false;
        if (!a.fields().equals(b.fields()))
            return false;
        for (Field<? super R, ?> field : a.fields()) {
            try {
                switch (field.type()) {
                    case Field.BOOLEAN:
                        if (a.get((Field.BooleanField) field) != b.get((Field.BooleanField) field))
                            return false;
                        break;
                    case Field.BYTE:
                        if (a.get((Field.ByteField) field) != b.get((Field.ByteField) field))
                            return false;
                        break;
                    case Field.SHORT:
                        if (a.get((Field.ShortField) field) != b.get((Field.ShortField) field))
                            return false;
                        break;
                    case Field.INT:
                        if (a.get((Field.IntField) field) != b.get((Field.IntField) field))
                            return false;
                        break;
                    case Field.LONG:
                        if (a.get((Field.LongField) field) != b.get((Field.LongField) field))
                            return false;
                        break;
                    case Field.FLOAT:
                        if (a.get((Field.FloatField) field) != b.get((Field.FloatField) field))
                            return false;
                        break;
                    case Field.DOUBLE:
                        if (a.get((Field.DoubleField) field) != b.get((Field.DoubleField) field))
                            return false;
                        break;
                    case Field.CHAR:
                        if (a.get((Field.CharField) field) != b.get((Field.CharField) field))
                            return false;
                        break;
                    case Field.OBJECT:
                        if (!(Objects.equals(a.get((Field.ObjectField) field), b.get((Field.ObjectField) field))))
                            return false;
                        break;
                    case Field.BOOLEAN_ARRAY: {
                        Field.BooleanArrayField f = (Field.BooleanArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.BYTE_ARRAY: {
                        Field.ByteArrayField f = (Field.ByteArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.SHORT_ARRAY: {
                        Field.ShortArrayField f = (Field.ShortArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.INT_ARRAY: {
                        Field.IntArrayField f = (Field.IntArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.LONG_ARRAY: {
                        Field.LongArrayField f = (Field.LongArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.FLOAT_ARRAY: {
                        Field.FloatArrayField f = (Field.FloatArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.DOUBLE_ARRAY: {
                        Field.DoubleArrayField f = (Field.DoubleArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.CHAR_ARRAY: {
                        Field.CharArrayField f = (Field.CharArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (a.get(f, i) != b.get(f, i))
                                return false;
                        }
                        break;
                    }
                    case Field.OBJECT_ARRAY: {
                        Field.ObjectArrayField f = (Field.ObjectArrayField) field;
                        for (int i = 0; i < f.length; i++) {
                            if (Objects.equals(a.get(f, i), b.get(f, i)))
                                return false;
                        }
                        break;
                    }
                    default:
                        throw new AssertionError();
                }
            } catch (FieldNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    private Records() {
    }
}

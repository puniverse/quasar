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

import co.paralleluniverse.common.util.Exceptions;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * @author pron
 */
class DynamicMethodHandleRecord<R> extends DynamicRecord<R> {
    static MethodHandle getGetterMethodHandle(Field<?, ?> field, java.lang.reflect.Field f, Method getter) {
        try {
            if (f == null && getter == null)
                return null;
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            return fixMethodHandleType(field, getter != null ? lookup.unreflect(getter) : lookup.unreflectGetter(f));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static MethodHandle getSetterMethodHandle(Field<?, ?> field, java.lang.reflect.Field f, Method setter) {
        try {
            if (f == null && setter == null)
                return null;
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            return fixMethodHandleType(field, f != null ? (field instanceof Field.ScalarField && !Modifier.isFinal(f.getModifiers()) ? lookup.unreflectSetter(f) : null) : (setter != null ? lookup.unreflect(setter) : null));
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static MethodHandle fixMethodHandleType(Field field, MethodHandle mh) throws IllegalAccessException {
        if (mh == null)
            return null;

        final MethodType origType = mh.type();
        final Class<?>[] params = origType.parameterArray();

        params[0] = Object.class;
        for (int i = 1; i < params.length; i++) {
            if (!params[i].isPrimitive())
                params[i] = Object.class;
        }

        Class<?> rtype = origType.returnType();
        if (field instanceof Field.ArrayField && rtype.isArray()) {
            if (!rtype.getComponentType().isPrimitive())
                rtype = Object[].class;
        } else if (!rtype.isPrimitive())
            rtype = Object.class;

        final MethodType mt = MethodType.methodType(rtype, params);
        return mh.asType(mt);
    }

    DynamicMethodHandleRecord(RecordType<R> recordType, Object target) {
        super(recordType, target);
    }

//    protected DynamicMethodHandleRecord(DynamicRecordType<R> recordType) {
//        super(recordType);
//    }
    private MethodHandle setter(Field<? super R, ?> field, RecordType.Entry entry) {
        final MethodHandle mh = entry.setterHandle;
        if (mh == null)
            throw new ReadOnlyFieldException(field, obj);
        return mh;
    }

    private MethodHandle setter(Field<? super R, ?> field) {
        return setter(field, entry(field));
    }

    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        try {
            return (boolean) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        try {
            return (byte) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        try {
            return (short) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        try {
            return (int) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        try {
            return (long) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        try {
            return (float) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        try {
            return (double) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        try {
            return (char) entry(field).getterHandle.invokeExact(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        try {
            return (V) entry(field).getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        try {
            setter(field).invokeExact(obj, value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    boolean[] get(Field.BooleanArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (boolean[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (boolean) entry.getterHandle.invokeExact(obj, index);
            else
                return ((boolean[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((boolean[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (boolean) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((boolean[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((boolean[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((boolean[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    byte[] get(Field.ByteArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (byte[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (byte) entry.getterHandle.invokeExact(obj, index);
            else
                return ((byte[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((byte[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (byte) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((byte[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((byte[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((byte[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    short[] get(Field.ShortArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (short[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (short) entry.getterHandle.invokeExact(obj, index);
            else
                return ((short[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((short[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (short) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((short[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((short[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((short[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    int[] get(Field.IntArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (int[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (int) entry.getterHandle.invokeExact(obj, index);
            else
                return ((int[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((int[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (int) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((int[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((int[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((int[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    long[] get(Field.LongArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (long[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (long) entry.getterHandle.invokeExact(obj, index);
            else
                return ((long[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((long[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (long) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((long[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((long[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((long[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    float[] get(Field.FloatArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (float[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (float) entry.getterHandle.invokeExact(obj, index);
            else
                return ((float[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((float[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (float) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((float[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((float[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((float[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    double[] get(Field.DoubleArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (double[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (double) entry.getterHandle.invokeExact(obj, index);
            else
                return ((double[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((double[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (double) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((double[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((double[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((double[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    char[] get(Field.CharArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (char[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (char) entry.getterHandle.invokeExact(obj, index);
            else
                return ((char[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((char[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (char) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((char[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((char[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((char[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (V[]) entry.getterHandle.invokeExact(obj);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (V) entry.getterHandle.invokeExact(obj, index);
            else
                return ((V[]) entry.getterHandle.invokeExact(obj))[index];
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invokeExact(obj, index, value);
            else
                ((V[]) entry.getterHandle.invokeExact(obj))[index] = value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (V) entry.getterHandle.invokeExact(obj, i);
            } else
                System.arraycopy(((V[]) entry.getterHandle.invokeExact(obj)), 0, target, offset, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((V[]) entry.getterHandle.invokeExact(obj)), 0, field.length);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invokeExact(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((V[]) entry.getterHandle.invokeExact(obj)), 0);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }
}

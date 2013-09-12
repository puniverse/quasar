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

import co.paralleluniverse.common.util.Exceptions;
import java.lang.reflect.Method;

/**
 *
 * @author pron
 */
class DynamicReflectionRecord<R> extends DynamicRecord<R> {
    DynamicReflectionRecord(RecordType<R> recordType, Object target) {
        super(recordType, target);
    }

//    protected DynamicReflectionRecord(DynamicRecordType<R> recordType) {
//        super(recordType);
//    }

    private Method setter(Field<? super R, ?> field, RecordType.Entry entry) {
        final Method m = entry.setter;
        if (m == null)
            throw new ReadOnlyFieldException(field, obj);
        return m;
    }

    @Override
    public boolean get(Field.BooleanField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (boolean) entry.field.get(obj);
            else
                return (boolean) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.BooleanField<? super R> field, boolean value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public byte get(Field.ByteField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (byte) entry.field.get(obj);
            else
                return (byte) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ByteField<? super R> field, byte value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public short get(Field.ShortField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (short) entry.field.get(obj);
            else
                return (short) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.ShortField<? super R> field, short value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public int get(Field.IntField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (int) entry.field.get(obj);
            else
                return (int) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.IntField<? super R> field, int value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public long get(Field.LongField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (long) entry.field.get(obj);
            else
                return (long) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.LongField<? super R> field, long value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public float get(Field.FloatField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (float) entry.field.get(obj);
            else
                return (float) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.FloatField<? super R> field, float value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public double get(Field.DoubleField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (double) entry.field.get(obj);
            else
                return (double) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.DoubleField<? super R> field, double value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public char get(Field.CharField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (char) entry.field.get(obj);
            else
                return (char) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public void set(Field.CharField<? super R> field, char value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> V get(Field.ObjectField<? super R, V> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                return (V) entry.field.get(obj);
            else
                return (V) entry.getter.invoke(obj);
        } catch (Throwable t) {
            throw Exceptions.rethrow(t);
        }
    }

    @Override
    public <V> void set(Field.ObjectField<? super R, V> field, V value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.field != null)
                entry.field.set(obj, value);
            else
                setter(field, entry).invoke(obj, value);
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
            return (boolean[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (boolean) entry.getter.invoke(obj, index);
            else
                return ((boolean[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((boolean[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (boolean) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((boolean[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((boolean[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((boolean[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    byte[] get(Field.ByteArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (byte[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (byte) entry.getter.invoke(obj, index);
            else
                return ((byte[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((byte[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (byte) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((byte[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((byte[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((byte[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    short[] get(Field.ShortArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (short[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (short) entry.getter.invoke(obj, index);
            else
                return ((short[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((short[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (short) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((short[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((short[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((short[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    int[] get(Field.IntArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (int[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (int) entry.getter.invoke(obj, index);
            else
                return ((int[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((int[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (int) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((int[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((int[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((int[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    long[] get(Field.LongArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (long[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (long) entry.getter.invoke(obj, index);
            else
                return ((long[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((long[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (long) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((long[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((long[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((long[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    float[] get(Field.FloatArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (float[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (float) entry.getter.invoke(obj, index);
            else
                return ((float[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((float[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (float) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((float[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((float[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((float[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    double[] get(Field.DoubleArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (double[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (double) entry.getter.invoke(obj, index);
            else
                return ((double[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((double[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (double) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((double[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((double[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((double[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    char[] get(Field.CharArrayField<? super R> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (char[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (char) entry.getter.invoke(obj, index);
            else
                return ((char[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((char[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (char) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((char[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((char[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((char[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return null;
            return (V[]) entry.field.get(obj);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                return (V) entry.getter.invoke(obj, index);
            else
                return ((V[]) entry.field.get(obj))[index];
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed)
                setter(field, entry).invoke(obj, index, value);
            else
                ((V[]) entry.field.get(obj))[index] = value;
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    target[offset + i] = (V) entry.getter.invoke(obj, i);
            } else
                System.arraycopy(((V[]) entry.field.get(obj)), 0, target, offset, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source[offset + i]);
            } else
                System.arraycopy(source, offset, ((V[]) entry.field.get(obj)), 0, field.length);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        try {
            final RecordType.Entry entry = entry(field);
            if (entry.indexed) {
                for (int i = 0; i < field.length; i++)
                    setter(field, entry).invoke(obj, i, source.get(sourceField, i));
            } else
                source.get(sourceField, ((V[]) entry.field.get(obj)), 0);
        } catch (Exception e) {
            throw Exceptions.rethrow(Exceptions.unwrap(e));
        }
    }
}

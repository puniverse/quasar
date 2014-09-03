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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;

/**
 *
 * @author pron
 */
public class DynamicGeneratedRecord<R> extends DynamicRecord<R> {
    private static final String DYNAMIC_GENERATED_RECORD_TYPE = Type.getInternalName(DynamicGeneratedRecord.class);
    private static final ClassValue<MyClassLoader> myClassLoader = new ClassValue<MyClassLoader>() {
        @Override
        protected MyClassLoader computeValue(Class<?> type) {
            return new MyClassLoader(type.getClassLoader());
        }
    };

    static Accessor generateAccessor(Class<?> type, Field<?, ?> field, java.lang.reflect.Field f, Method getter, Method setter) {
        final MyClassLoader cl = myClassLoader.get(type);
        final String className = accessorClassName(type, field);
        Class<?> accessorClass;

        try {
            accessorClass = Class.forName(className, false, cl);
        } catch (ClassNotFoundException e) {
            final byte[] classData;
            if (field instanceof Field.ArrayField) {
                if (f != null)
                    classData = generateArrayFieldAccessor(type, field, f);
                else
                    classData = generateIndexedAccessor(type, field, getter, setter);
            } else {
                if (f != null)
                    classData = generateSimpleFieldAccessor(type, field, f);
                else
                    classData = generateMethodAccessor(type, field, getter, setter);
            }

            accessorClass = cl.defineClass(className, classData);
        }

        try {
            final Accessor accessor = (Accessor) accessorClass.newInstance();

            return accessor;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new AssertionError(e);
        }
    }

    private static class MyClassLoader extends ClassLoader {
        public MyClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    private static String accessorClassName(Class<?> type, Field<?, ?> field) {
        final String packageName = DynamicGeneratedRecord.class.getPackage().getName();
        final String className = type.getSimpleName() + "$" + field.name() + "Accessor$" + Integer.toHexString(type.hashCode());
        return packageName + "." + className;
    }

    private static ClassWriter generateClass(Class<?> type, Field field, String accName) {
        final String superName = DYNAMIC_GENERATED_RECORD_TYPE + "$" + accName;
        final String className = accessorClassName(type, field).replace('.', '/');

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, className, null, superName, null);

        cw.visitInnerClass(superName, DYNAMIC_GENERATED_RECORD_TYPE, accName, ACC_STATIC + ACC_ABSTRACT);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        return cw;
    }

    private static String methodSigTypeDesc(Field<?, ?> field) {
        if (field instanceof Field.ObjectField)
            return "Ljava/lang/Object;";
        if (field instanceof Field.ObjectArrayField)
            return "[Ljava/lang/Object;";
        return Type.getDescriptor(field.typeClass());
    }

    private static String methodSigComponentTypeDesc(Field<?, ?> field) {
        assert field instanceof Field.ArrayField;
        if (field instanceof Field.ObjectArrayField)
            return "Ljava/lang/Object;";
        return Type.getDescriptor(field.typeClass().getComponentType());
    }

    private static byte[] generateSimpleFieldAccessor(Class<?> type, Field<?, ?> field, java.lang.reflect.Field f) {
        final String typeName = Type.getInternalName(type);
        final String fieldTypeName = Type.getInternalName(field.typeClass());
        final String fieldTypeDesc = Type.getDescriptor(field.typeClass());
        final String accName = accessorName(field) + "Accessor";

        ClassWriter cw = generateClass(type, field, accName);
        MethodVisitor mv;

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)" + methodSigTypeDesc(field), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typeName);
        mv.visitFieldInsn(GETFIELD, typeName, field.name(), fieldTypeDesc);
        mv.visitInsn(returnOpcode(field));
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;" + methodSigTypeDesc(field) + ")V", null, null);
        mv.visitCode();
        if (Modifier.isFinal(f.getModifiers())) {
            mv.visitTypeInsn(NEW, "co/paralleluniverse/data/record/ReadOnlyFieldException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "co/paralleluniverse/data/record/ReadOnlyFieldException", "<init>", "()V", false);
            mv.visitInsn(ATHROW);
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, typeName);
            mv.visitVarInsn(loadOpcode(field), 2);
            if (field instanceof Field.ObjectField)
                mv.visitTypeInsn(CHECKCAST, fieldTypeName);
            mv.visitFieldInsn(PUTFIELD, typeName, field.name(), fieldTypeDesc);
            mv.visitInsn(RETURN);
        }
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    private static byte[] generateMethodAccessor(Class<?> type, Field<?, ?> field, Method getter, Method setter) {
        final String typeName = Type.getInternalName(type);
        final String fieldTypeName = Type.getInternalName(field.typeClass());
        final String fieldTypeDesc = Type.getDescriptor(field.typeClass());
        final String accName = accessorName(field) + "Accessor";

        ClassWriter cw = generateClass(type, field, accName);
        MethodVisitor mv;

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)" + methodSigTypeDesc(field), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typeName);
        mv.visitMethodInsn(INVOKEVIRTUAL, typeName, getter.getName(), Type.getMethodDescriptor(getter), false);
        mv.visitInsn(returnOpcode(field));
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;" + methodSigTypeDesc(field) + ")V", null, null);
        mv.visitCode();

        if (setter != null) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, typeName);
            mv.visitVarInsn(loadOpcode(field), 2);
            if (field instanceof Field.ObjectField)
                mv.visitTypeInsn(CHECKCAST, fieldTypeName);
            mv.visitMethodInsn(INVOKEVIRTUAL, typeName, setter.getName(), Type.getMethodDescriptor(setter), false);
            mv.visitInsn(RETURN);
        } else {
            mv.visitTypeInsn(NEW, Type.getInternalName(ReadOnlyFieldException.class));
            mv.visitInsn(DUP);
            mv.visitLdcInsn(field.name);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ReadOnlyFieldException.class), "<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitInsn(ATHROW);
        }
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    private static byte[] generateArrayFieldAccessor(Class<?> type, Field<?, ?> field, java.lang.reflect.Field f) {
        final String typeName = Type.getInternalName(type);
        final String typeDesc = Type.getDescriptor(field.typeClass());
        final String accName = accessorName(field) + "ArrayAccessor";

        ClassWriter cw = generateClass(type, field, accName);
        MethodVisitor mv;

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)" + methodSigTypeDesc(field), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typeName);
        mv.visitFieldInsn(GETFIELD, typeName, field.name(), typeDesc);
        mv.visitInsn(ARETURN);
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    private static byte[] generateIndexedAccessor(Class<?> type, Field<?, ?> field, Method getter, Method setter) {
        final String typeName = Type.getInternalName(type);
        final String fieldComponentTypeName = Type.getInternalName(field.typeClass().getComponentType());
        final String fieldTypeDesc = Type.getDescriptor(field.typeClass());
        final String accName = accessorName(field) + "IndexedAccessor";

        ClassWriter cw = generateClass(type, field, accName);
        MethodVisitor mv;

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;I)" + methodSigComponentTypeDesc(field), null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, typeName);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, typeName, getter.getName(), Type.getMethodDescriptor(getter), false);
        mv.visitInsn(returnOpcode(field));
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;I" + methodSigComponentTypeDesc(field) + ")V", null, null);
        mv.visitCode();
        if (setter != null) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, typeName);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(loadOpcode(field), 3);
            if (field instanceof Field.ObjectArrayField)
                mv.visitTypeInsn(CHECKCAST, fieldComponentTypeName);
            mv.visitMethodInsn(INVOKEVIRTUAL, typeName, setter.getName(), Type.getMethodDescriptor(setter), false);
            mv.visitInsn(RETURN);
        } else {
            mv.visitTypeInsn(NEW, Type.getInternalName(ReadOnlyFieldException.class));
            mv.visitInsn(DUP);
            mv.visitLdcInsn(field.name);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(ReadOnlyFieldException.class), "<init>", "(Ljava/lang/String;Ljava/lang/Object;)V", false);
            mv.visitInsn(ATHROW);
        }
        mv.visitEnd();

        cw.visitEnd();

        return cw.toByteArray();
    }

    private static String accessorName(Field field) {
        switch (field.type()) {
            case Field.BOOLEAN:
            case Field.BOOLEAN_ARRAY:
                return "Boolean";
            case Field.BYTE:
            case Field.BYTE_ARRAY:
                return "Byte";
            case Field.SHORT:
            case Field.SHORT_ARRAY:
                return "Short";
            case Field.INT:
            case Field.INT_ARRAY:
                return "Int";
            case Field.LONG:
            case Field.LONG_ARRAY:
                return "Long";
            case Field.FLOAT:
            case Field.FLOAT_ARRAY:
                return "Float";
            case Field.DOUBLE:
            case Field.DOUBLE_ARRAY:
                return "Double";
            case Field.CHAR:
            case Field.CHAR_ARRAY:
                return "Char";
            case Field.OBJECT:
            case Field.OBJECT_ARRAY:
                return "Object";
            default:
                throw new AssertionError();
        }
    }

    private static int returnOpcode(Field field) {
        switch (field.type()) {
            case Field.BOOLEAN:
            case Field.BOOLEAN_ARRAY:
            case Field.BYTE:
            case Field.BYTE_ARRAY:
            case Field.SHORT:
            case Field.SHORT_ARRAY:
            case Field.INT:
            case Field.INT_ARRAY:
            case Field.CHAR:
            case Field.CHAR_ARRAY:
                return IRETURN;
            case Field.LONG:
            case Field.LONG_ARRAY:
                return LRETURN;
            case Field.FLOAT:
            case Field.FLOAT_ARRAY:
                return FRETURN;
            case Field.DOUBLE:
            case Field.DOUBLE_ARRAY:
                return DRETURN;
            case Field.OBJECT:
            case Field.OBJECT_ARRAY:
                return ARETURN;
            default:
                throw new AssertionError();
        }
    }

    private static int loadOpcode(Field field) {
        switch (field.type()) {
            case Field.BOOLEAN:
            case Field.BOOLEAN_ARRAY:
            case Field.BYTE:
            case Field.BYTE_ARRAY:
            case Field.SHORT:
            case Field.SHORT_ARRAY:
            case Field.INT:
            case Field.INT_ARRAY:
            case Field.CHAR:
            case Field.CHAR_ARRAY:
                return ILOAD;
            case Field.LONG:
            case Field.LONG_ARRAY:
                return LLOAD;
            case Field.FLOAT:
            case Field.FLOAT_ARRAY:
                return FLOAD;
            case Field.DOUBLE:
            case Field.DOUBLE_ARRAY:
                return DLOAD;
            case Field.OBJECT:
            case Field.OBJECT_ARRAY:
                return ALOAD;
            default:
                throw new AssertionError();
        }
    }

    DynamicGeneratedRecord(RecordType<R> recordType, Object target) {
        super(recordType, target);
    }

//    protected DynamicGeneratedRecord(DynamicRecordType<R> recordType) {
//        super(recordType);
//    }
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
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((BooleanArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public boolean get(Field.BooleanArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((BooleanIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((BooleanArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, int index, boolean value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((BooleanIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((BooleanArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.BooleanArrayField<? super R> field, boolean[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((BooleanArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.BooleanArrayField<? super R> field, boolean[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((BooleanArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.BooleanArrayField<? super R> field, Record<S> source, Field.BooleanArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final BooleanIndexedAccessor accessor = ((BooleanIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((BooleanArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    byte[] get(Field.ByteArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((ByteArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public byte get(Field.ByteArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((ByteIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((ByteArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, int index, byte value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ByteIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ByteArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.ByteArrayField<? super R> field, byte[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((ByteArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ByteArrayField<? super R> field, byte[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ByteArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.ByteArrayField<? super R> field, Record<S> source, Field.ByteArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ByteIndexedAccessor accessor = ((ByteIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((ByteArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    short[] get(Field.ShortArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((ShortArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public short get(Field.ShortArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((ShortIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((ShortArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, int index, short value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ShortIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ShortArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.ShortArrayField<? super R> field, short[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((ShortArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.ShortArrayField<? super R> field, short[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ShortArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.ShortArrayField<? super R> field, Record<S> source, Field.ShortArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ShortIndexedAccessor accessor = ((ShortIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((ShortArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    int[] get(Field.IntArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((IntArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public int get(Field.IntArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((IntIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((IntArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int index, int value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((IntIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((IntArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.IntArrayField<? super R> field, int[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((IntArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.IntArrayField<? super R> field, int[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((IntArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.IntArrayField<? super R> field, Record<S> source, Field.IntArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final IntIndexedAccessor accessor = ((IntIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((IntArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    long[] get(Field.LongArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((LongArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public long get(Field.LongArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((LongIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((LongArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, int index, long value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((LongIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((LongArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.LongArrayField<? super R> field, long[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((LongArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.LongArrayField<? super R> field, long[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((LongArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.LongArrayField<? super R> field, Record<S> source, Field.LongArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final LongIndexedAccessor accessor = ((LongIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((LongArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    float[] get(Field.FloatArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((FloatArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public float get(Field.FloatArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((FloatIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((FloatArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, int index, float value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((FloatIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((FloatArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.FloatArrayField<? super R> field, float[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((FloatArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.FloatArrayField<? super R> field, float[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((FloatArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.FloatArrayField<? super R> field, Record<S> source, Field.FloatArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final FloatIndexedAccessor accessor = ((FloatIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((FloatArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    double[] get(Field.DoubleArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((DoubleArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public double get(Field.DoubleArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((DoubleIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((DoubleArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, int index, double value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((DoubleIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((DoubleArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.DoubleArrayField<? super R> field, double[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((DoubleArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.DoubleArrayField<? super R> field, double[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((DoubleArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.DoubleArrayField<? super R> field, Record<S> source, Field.DoubleArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final DoubleIndexedAccessor accessor = ((DoubleIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((DoubleArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    char[] get(Field.CharArrayField<? super R> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return ((CharArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public char get(Field.CharArrayField<? super R> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return ((CharIndexedAccessor) entry.accessor).get(obj, index);
        else
            return ((CharArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, int index, char value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((CharIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((CharArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public void get(Field.CharArrayField<? super R> field, char[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = accessor.get(obj, i);
        } else
            System.arraycopy(((CharArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public void set(Field.CharArrayField<? super R> field, char[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((CharArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S> void set(Field.CharArrayField<? super R> field, Record<S> source, Field.CharArrayField<? super S> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final CharIndexedAccessor accessor = ((CharIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, ((CharArrayAccessor) entry.accessor).get(obj), 0);
    }

    @Override
    <V> V[] get(Field.ObjectArrayField<? super R, V> field) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return null;
        return (V[]) ((ObjectArrayAccessor) entry.accessor).get(obj);
    }

    @Override
    public <V> V get(Field.ObjectArrayField<? super R, V> field, int index) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            return (V) ((ObjectIndexedAccessor) entry.accessor).get(obj, index);
        else
            return (V) ((ObjectArrayAccessor) entry.accessor).get(obj)[index];
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, int index, V value) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed)
            ((ObjectIndexedAccessor) entry.accessor).set(obj, index, value);
        else
            ((ObjectArrayAccessor) entry.accessor).get(obj)[index] = value;
    }

    @Override
    public <V> void get(Field.ObjectArrayField<? super R, V> field, V[] target, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                target[offset + i] = (V) accessor.get(obj, i);
        } else
            System.arraycopy(((ObjectArrayAccessor) entry.accessor).get(obj), 0, target, offset, field.length);
    }

    @Override
    public <V> void set(Field.ObjectArrayField<? super R, V> field, V[] source, int offset) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source[offset + i]);
        } else
            System.arraycopy(source, offset, ((ObjectArrayAccessor) entry.accessor).get(obj), 0, field.length);
    }

    @Override
    public <S, V> void set(Field.ObjectArrayField<? super R, V> field, Record<S> source, Field.ObjectArrayField<? super S, V> sourceField) {
        final RecordType.Entry entry = entry(field);
        if (entry.indexed) {
            final ObjectIndexedAccessor accessor = ((ObjectIndexedAccessor) entry.accessor);
            for (int i = 0; i < field.length; i++)
                accessor.set(obj, i, source.get(sourceField, i));
        } else
            source.get(sourceField, (V[]) ((ObjectArrayAccessor) entry.accessor).get(obj), 0);
    }

    public static abstract class Accessor {
    }

    public static abstract class BooleanAccessor extends Accessor {
        public abstract boolean get(Object target);

        public abstract void set(Object target, boolean value);
    }

    public static abstract class BooleanArrayAccessor extends Accessor {
        public abstract boolean[] get(Object target);
    }

    public static abstract class BooleanIndexedAccessor extends Accessor {
        public abstract boolean get(Object target, int index);

        public abstract void set(Object target, int index, boolean value);
    }

    public static abstract class ByteAccessor extends Accessor {
        public abstract byte get(Object target);

        public abstract void set(Object target, byte value);
    }

    public static abstract class ByteArrayAccessor extends Accessor {
        public abstract byte[] get(Object target);
    }

    public static abstract class ByteIndexedAccessor extends Accessor {
        public abstract byte get(Object target, int index);

        public abstract void set(Object target, int index, byte value);
    }

    public static abstract class ShortAccessor extends Accessor {
        public abstract short get(Object target);

        public abstract void set(Object target, short value);
    }

    public static abstract class ShortArrayAccessor extends Accessor {
        public abstract short[] get(Object target);
    }

    public static abstract class ShortIndexedAccessor extends Accessor {
        public abstract short get(Object target, int index);

        public abstract void set(Object target, int index, short value);
    }

    public static abstract class IntAccessor extends Accessor {
        public abstract int get(Object target);

        public abstract void set(Object target, int value);
    }

    public static abstract class IntArrayAccessor extends Accessor {
        public abstract int[] get(Object target);
    }

    public static abstract class IntIndexedAccessor extends Accessor {
        public abstract int get(Object target, int index);

        public abstract void set(Object target, int index, int value);
    }

    public static abstract class LongAccessor extends Accessor {
        public abstract long get(Object target);

        public abstract void set(Object target, long value);
    }

    public static abstract class LongArrayAccessor extends Accessor {
        public abstract long[] get(Object target);
    }

    public static abstract class LongIndexedAccessor extends Accessor {
        public abstract long get(Object target, int index);

        public abstract void set(Object target, int index, long value);
    }

    public static abstract class FloatAccessor extends Accessor {
        public abstract float get(Object target);

        public abstract void set(Object target, float value);
    }

    public static abstract class FloatArrayAccessor extends Accessor {
        public abstract float[] get(Object target);
    }

    public static abstract class FloatIndexedAccessor extends Accessor {
        public abstract float get(Object target, int index);

        public abstract void set(Object target, int index, float value);
    }

    public static abstract class DoubleAccessor extends Accessor {
        public abstract double get(Object target);

        public abstract void set(Object target, double value);
    }

    public static abstract class DoubleArrayAccessor extends Accessor {
        public abstract double[] get(Object target);
    }

    public static abstract class DoubleIndexedAccessor extends Accessor {
        public abstract double get(Object target, int index);

        public abstract void set(Object target, int index, double value);
    }

    public static abstract class CharAccessor extends Accessor {
        public abstract char get(Object target);

        public abstract void set(Object target, char value);
    }

    public static abstract class CharArrayAccessor extends Accessor {
        public abstract char[] get(Object target);
    }

    public static abstract class CharIndexedAccessor extends Accessor {
        public abstract char get(Object target, int index);

        public abstract void set(Object target, int index, char value);
    }

    public static abstract class ObjectAccessor extends Accessor {
        public abstract Object get(Object target);

        public abstract void set(Object target, Object value);
    }

    public static abstract class ObjectArrayAccessor extends Accessor {
        public abstract Object[] get(Object target);
    }

    public static abstract class ObjectIndexedAccessor extends Accessor {
        public abstract Object get(Object target, int index);

        public abstract void set(Object target, int index, Object value);
    }
}

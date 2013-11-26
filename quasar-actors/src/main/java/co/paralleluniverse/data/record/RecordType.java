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

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import jsr166e.ConcurrentHashMapV8;

/**
 * Represents a record type, and includes a name, and a list of fields along with their names and types.
 * <p/>
 * A new record type must be declared as a static member of a class. The class must only include the definition of a single record type,
 * and this class is called the type's <i>identifier class</i>, because it is used only to uniquely identify the record type (only its name is used internally).
 * <p/>
 * Here's an example record type definition:
 *
 * ```java
 * class A {
 *     public static final RecordType<A> aType = RecordType.newType(A.class);
 *     public static final IntField<A> $id = stateType.intField("id");
 *     public static final DoubleField<A> $foo = stateType.doubleField("id", Field.TRANSIENT);
 *     public static final ObjectField<A, String> $name = stateType.objectField("name", String.class);
 *     public static final ObjectField<A, List<String>> $emails = stateType.objectField("emails", new TypeToken<List<String>() {});
 * }
 * ```
 * {@code A} is the type's <i>identifier class</i>. The fields are, by convention, given identifiers that begin with a {@code $} to make it clear
 * that they identify fields rather than values.
 * <br/>
 * A new record is instantiated by calling one of the {@code newInstance} methods.
 *
 * @author pron
 */
public class RecordType<R> {
    public enum Mode {
        /**
         * About 2.5 times slower than REFLECTION in Java 7, but doesn't use boxing and doesn't generate garbage. The default.
         */
        METHOD_HANDLE,
        /**
         * About 8x slower than UNSAFE and GENERATION
         */
        REFLECTION,
        /**
         * Just a little slower than GENERATION. Can only work on fields; doesn't work if there are getters/setters.
         */
        UNSAFE,
        /**
         * The fastest method (as fast as direct settings of fields), but can only be used if both the target object's class
         * as well as the fields or getters/setters are public.
         */
        GENERATION
    };
    private final String name;
    private final RecordType<? super R> parent;
    private final List<Field<? super R, ?>> fields;
    private int fieldIndex;
    private boolean sealed;
    private Set<Field<? super R, ?>> fieldSet;
    private int primitiveIndex;
    private int primitiveOffset;
    private int objectIndex;
    private int objectOffset;
    private int[] offsets;
    private final ThreadLocal<Mode> currentMode = new ThreadLocal<Mode>();
    private final ClassValue<ClassInfo> vtables;
    //
    private static final ConcurrentMap<String, RecordType<?>> loadedTypes = new ConcurrentHashMapV8<>();

    /**
     * Creates a new record type, possibly extending a super type.
     * If a supertype is provided, this type inherits its fields.
     *
     * @param type   the {@link RecordType <i>identifier class</i>} of this record type.
     * @param parent the super-type of this type (may be {@code null}).
     */
    public static <R> RecordType<R> newType(Class<R> type, RecordType<? super R> parent) {
        return new RecordType<R>(type, parent);
    }

    /**
     * Creates a new record type with no super type.
     *
     * @param type the {@link RecordType <i>identifier class</i>} of this record type.
     */
    public static <R> RecordType<R> newType(Class<R> type) {
        return newType(type, null);
    }

    public static RecordType<?> forName(String name) throws ClassNotFoundException {
        return forClass(Class.forName(name));
    }

    /**
     * Returns 
     * @param <T>
     * @param type
     * @return 
     */
    public static <T> RecordType<T> forClass(Class<T> type) {
        sealClassType(type);
        return (RecordType<T>) loadedTypes.get(type.getName());
    }

    private static void sealClassType(Class<?> clazz) {
        java.lang.reflect.Field[] fs = clazz.getDeclaredFields();
        try {
            for (java.lang.reflect.Field f : fs) {
                if (Modifier.isStatic(f.getModifiers()) && RecordType.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    ((RecordType<?>) f.get(null)).seal();
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addType(String name, RecordType<?> type) {
        for (;;) {
            RecordType<?> oldType = loadedTypes.get(name);
            if (oldType == null) {
                oldType = loadedTypes.putIfAbsent(name, type);
                if (oldType == null)
                    break;
            } else if (isCompatible(oldType, type)) {
                if (loadedTypes.replace(name, oldType, type))
                    break;
            } else {
                throw new RuntimeException("RecordType " + type + " incompatible with an already loaded type of the same name: " + oldType);
            }
        }
    }

    private static boolean isCompatible(RecordType<?> oldType, RecordType<?> newType) {
        List<Field> oldFields = new ArrayList<Field>(oldType.fields());
        List<Field> newFileds = new ArrayList<Field>(newType.fields());
        if (newFileds.size() < oldFields.size())
            return false;
        for (int i = 0; i < oldFields.size(); i++) {
            if (!oldFields.get(i).equals(newFileds.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Creates a new record type, possibly extending a super type.
     * If a supertype is provided, this type inherits its fields.
     *
     * @param type   the {@link RecordType <i>identifier class</i>} of this record type.
     * @param parent the super-type of this type (may be {@code null}).
     */
    public RecordType(Class<R> type, RecordType<? super R> parent) {
        this.name = type.getName().intern();
        this.parent = parent;
        if (parent != null) {
            parent.seal();
            this.fields = new ArrayList<Field<? super R, ?>>(parent.fields);
            this.fieldIndex = parent.fieldIndex;
            this.primitiveIndex = parent.primitiveIndex;
            this.primitiveOffset = parent.primitiveOffset;
            this.objectIndex = parent.objectIndex;
            this.objectOffset = parent.objectOffset;
        } else {
            this.fields = new ArrayList<Field<? super R, ?>>();
            this.fieldIndex = 0;
        }

        this.vtables = new ClassValue<ClassInfo>() {
            @Override
            protected ClassInfo computeValue(Class<?> type) {
                seal();
                return new ClassInfo(currentMode.get(), type, RecordType.this);
            }
        };
    }

    /**
     * Creates a new record type with no super type.
     *
     * @param type the {@link RecordType <i>identifier class</i>} of this record type.
     */
    public RecordType(Class<R> type) {
        this(type, null);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RecordType))
            return false;
        final RecordType<R> other = (RecordType<R>) obj;
        if (!Objects.equals(this.name, other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name + fields.toString();
    }

    /**
     * This type's name
     */
    public String getName() {
        return name;
    }

    /**
     * Test's whether a record is an instance of this type (or one of its subtypes).
     *
     * @param record the record to test
     * @return {@code true} if {@code record} is an instance of this type (or one of its subtypes); {@code false} otherwise.
     */
    public boolean isInstance(Record<?> record) {
        for (RecordType<?> t = record.type(); t != null; t = t.parent) {
            if (this.equals(t))
                return true;
        }
        return false;
    }

    /**
     * Adds a {@code boolean} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.BooleanField<R> booleanField(String name) {
        return booleanField(name, 0);
    }

    /**
     * Adds a {@code boolean} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.BooleanField<R> booleanField(String name, int flags) {
        return addField(new Field.BooleanField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code byte} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.ByteField<R> byteField(String name) {
        return byteField(name, 0);
    }

    /**
     * Adds a {@code byte} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.ByteField<R> byteField(String name, int flags) {
        return addField(new Field.ByteField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code short} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.ShortField<R> shortField(String name) {
        return shortField(name, 0);
    }

    /**
     * Adds a {@code short} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.ShortField<R> shortField(String name, int flags) {
        return addField(new Field.ShortField<R>(name, -1, flags));
    }

    /**
     * Adds an {@code int} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.IntField<R> intField(String name) {
        return intField(name, 0);
    }

    /**
     * Adds an {@code short} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.IntField<R> intField(String name, int flags) {
        return addField(new Field.IntField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code long} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.LongField<R> longField(String name) {
        return longField(name, 0);
    }

    /**
     * Adds a {@code long} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.LongField<R> longField(String name, int flags) {
        return addField(new Field.LongField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code float} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.FloatField<R> floatField(String name) {
        return floatField(name, 0);
    }

    /**
     * Adds a {@code float} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.FloatField<R> floatField(String name, int flags) {
        return addField(new Field.FloatField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code double} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.DoubleField<R> doubleField(String name) {
        return doubleField(name, 0);
    }

    /**
     * Adds a {@code double} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.DoubleField<R> doubleField(String name, int flags) {
        return addField(new Field.DoubleField<R>(name, -1, flags));
    }

    /**
     * Adds a {@code char} scalar field to the type
     *
     * @param name the field name
     * @return the field
     */
    public Field.CharField<R> charField(String name) {
        return charField(name, 0);
    }

    /**
     * Adds a {@code char} scalar field to the type
     *
     * @param name  the field name
     * @param flags the field's flags
     * @return the field
     */
    public Field.CharField<R> charField(String name, int flags) {
        return addField(new Field.CharField<R>(name, -1, flags));
    }

    /**
     * Adds an {@code Object} scalar field to the type
     *
     * @param <V>  the fields type
     * @param name the field name
     * @param type the type of the field
     * @return the field
     */
    public <V> Field.ObjectField<R, V> objectField(String name, Class<V> type) {
        return objectField(name, type, 0);
    }

    /**
     * Adds a {@code Object} scalar field to the type
     *
     * @param <V>   the fields type
     * @param name  the field name
     * @param type  the type of the field
     * @param flags the field's flags
     * @return the field
     */
    public <V> Field.ObjectField<R, V> objectField(String name, Class<V> type, int flags) {
        return addField(new Field.ObjectField<R, V>(name, type, -1, flags));
    }

    /**
     * Adds an {@code Object} scalar field to the type
     *
     * @param <V>  the fields type
     * @param name the field name
     * @param type the type of the field (as a {@link TypeToken})
     * @return the field
     */
    public <V> Field.ObjectField<R, V> objectField(String name, TypeToken<V> type) {
        return objectField(name, type, 0);
    }

    /**
     * Adds a {@code Object} scalar field to the type
     *
     * @param <V>   the fields type
     * @param name  the field name
     * @param type  the type of the field (as a {@link TypeToken})
     * @param flags the field's flags
     * @return the field
     */
    public <V> Field.ObjectField<R, V> objectField(String name, TypeToken<V> type, int flags) {
        return addField(new Field.ObjectField<R, V>(name, type.getRawType(), -1, flags));
    }

    /**
     * Adds a {@code boolean} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.BooleanArrayField<R> booleanArrayField(String name, int length) {
        return booleanArrayField(name, length, 0);
    }

    /**
     * Adds a {@code boolean} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.BooleanArrayField<R> booleanArrayField(String name, int length, int flags) {
        return addField(new Field.BooleanArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code byte} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.ByteArrayField<R> byteArrayField(String name, int length) {
        return byteArrayField(name, length, 0);
    }

    /**
     * Adds a {@code byte} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.ByteArrayField<R> byteArrayField(String name, int length, int flags) {
        return addField(new Field.ByteArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code short} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.ShortArrayField<R> shortArrayField(String name, int length) {
        return shortArrayField(name, length, 0);
    }

    /**
     * Adds a {@code short} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.ShortArrayField<R> shortArrayField(String name, int length, int flags) {
        return addField(new Field.ShortArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds an {@code int} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.IntArrayField<R> intArrayField(String name, int length) {
        return intArrayField(name, length, 0);
    }

    /**
     * Adds an {@code int} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.IntArrayField<R> intArrayField(String name, int length, int flags) {
        return addField(new Field.IntArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code long} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.LongArrayField<R> longArrayField(String name, int length) {
        return longArrayField(name, length, 0);
    }

    /**
     * Adds a {@code long} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.LongArrayField<R> longArrayField(String name, int length, int flags) {
        return addField(new Field.LongArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code float} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.FloatArrayField<R> floatArrayField(String name, int length) {
        return floatArrayField(name, length, 0);
    }

    /**
     * Adds a {@code float} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.FloatArrayField<R> floatArrayField(String name, int length, int flags) {
        return addField(new Field.FloatArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code double} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.DoubleArrayField<R> doubleArrayField(String name, int length) {
        return doubleArrayField(name, length, 0);
    }

    /**
     * Adds a {@code double} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.DoubleArrayField<R> doubleArrayField(String name, int length, int flags) {
        return addField(new Field.DoubleArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds a {@code char} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @return the field
     */
    public Field.CharArrayField<R> charArrayField(String name, int length) {
        return charArrayField(name, length, 0);
    }

    /**
     * Adds a {@code char} array field to the type
     *
     * @param name   the field name
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public Field.CharArrayField<R> charArrayField(String name, int length, int flags) {
        return addField(new Field.CharArrayField<R>(name, length, -1, flags));
    }

    /**
     * Adds an {@code Object} array field to the type
     *
     * @param <V>    the field's element type
     * @param name   the field name
     * @param type   the type of the field
     * @param length the length of the array
     * @return the field
     */
    public <V> Field.ObjectArrayField<R, V> objectArrayField(String name, Class<V> type, int length) {
        return objectArrayField(name, type, length, 0);
    }

    /**
     * Adds an {@code Object} array field to the type
     *
     * @param <V>    the field's element type
     * @param name   the field name
     * @param type   the type of the field
     * @param length the length of the array
     * @param flags  the field's flags
     * @return the field
     */
    public <V> Field.ObjectArrayField<R, V> objectArrayField(String name, Class<V> type, int length, int flags) {
        return addField(new Field.ObjectArrayField<R, V>(name, type, length, -1, flags));
    }

    private <F extends Field<R, ?>> F addField(F field) {
        if (sealed)
            throw new IllegalStateException("Cannot add fields once a record has been instantiated");

        assert field.id < 0;
        final int id = fieldIndex;
        this.fieldIndex++;

        final Field<R, ?> f;
        switch (field.type()) {
            case Field.BOOLEAN:
                f = Field.booleanField(field.name(), id, field.flags());
                break;
            case Field.BYTE:
                f = Field.byteField(field.name(), id, field.flags());
                break;
            case Field.SHORT:
                f = Field.shortField(field.name(), id, field.flags());
                break;
            case Field.INT:
                f = Field.intField(field.name(), id, field.flags());
                break;
            case Field.LONG:
                f = Field.longField(field.name(), id, field.flags());
                break;
            case Field.FLOAT:
                f = Field.floatField(field.name(), id, field.flags());
                break;
            case Field.DOUBLE:
                f = Field.doubleField(field.name(), id, field.flags());
                break;
            case Field.CHAR:
                f = Field.charField(field.name(), id, field.flags());
                break;
            case Field.BOOLEAN_ARRAY:
                f = Field.booleanArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.BYTE_ARRAY:
                f = Field.byteArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.SHORT_ARRAY:
                f = Field.shortArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.INT_ARRAY:
                f = Field.intArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.LONG_ARRAY:
                f = Field.longArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.FLOAT_ARRAY:
                f = Field.floatArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.DOUBLE_ARRAY:
                f = Field.doubleArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.CHAR_ARRAY:
                f = Field.charArrayField(field.name(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            case Field.OBJECT:
                f = Field.objectField(field.name(), (Class) field.typeClass(), id, field.flags());
                break;
            case Field.OBJECT_ARRAY:
                f = Field.objectArrayField(field.name(), field.typeClass().getComponentType(), ((Field.ArrayField<R, ?>) field).length, id, field.flags());
                break;
            default:
                throw new AssertionError();
        }
        fields.add(f);
        return (F) f;
    }

    private void seal() {
        if (!sealed) {
            this.sealed = true;
            this.fieldSet = (Set) ImmutableSet.copyOf(fields);
            this.offsets = new int[fields.size()];
            for (Field<?, ?> field : fields) {
                final int offset;
                if (field.type() == Field.OBJECT || field.type() == Field.OBJECT_ARRAY) {
                    offset = objectOffset;
                    objectOffset += (field instanceof Field.ArrayField ? ((Field.ArrayField) field).length : 1);
                    objectIndex++;
                } else {
                    offset = primitiveOffset;
                    primitiveOffset += field.size();
                    primitiveIndex++;
                }
                offsets[field.id()] = offset;
            }
            addType(name, this);
        }
    }

    static class ClassInfo {
        final Entry[] table;
        final Mode mode;
        final Set<Field<?, ?>> fieldSet;

        private ClassInfo(Mode mode, Class<?> type, RecordType<?> recordType) {
            try {
                final Collection<? extends Field<?, ?>> fields = recordType.fields();
                if (mode == null) {
                    boolean unsafePossible = true;
                    boolean generationPossible = true;

                    for (Field<?, ?> field : fields) {
                        final Method getter = field instanceof Field.ArrayField ? getIndexedGetter(type, field) : getGetter(type, field);
                        final Method setter = field instanceof Field.ArrayField ? getIndexedSetter(type, field) : getSetter(type, field);
                        final java.lang.reflect.Field f = getter == null ? getField(type, field) : null;

                        if (f == null && getter == null)
                            throw new RuntimeException("Field " + field.name() + " defined in record type " + recordType + " is neither a field or a getter of class " + type.getName());

                        if (f == null && getter != null)
                            unsafePossible = false;

                        if (getter == null && ((f.getModifiers() & Modifier.PUBLIC) == 0))
                            generationPossible = false;
                    }

                    if (unsafePossible)
                        mode = Mode.UNSAFE;
                    else if (generationPossible)
                        mode = Mode.GENERATION;
                    else
                        mode = Mode.METHOD_HANDLE;
                }

                this.mode = mode;
                this.table = new Entry[fields.size()];
                final List<Field<?, ?>> implementedFields = new ArrayList<>();
                for (Field<?, ?> field : fields) {
                    final Method getter = field instanceof Field.ArrayField ? getIndexedGetter(type, field) : getGetter(type, field);
                    final Method setter = field instanceof Field.ArrayField ? getIndexedSetter(type, field) : getSetter(type, field);
                    final java.lang.reflect.Field f = getter == null ? getField(type, field) : null;
                    final boolean indexed = f == null && field instanceof Field.ArrayField;

                    final MethodHandle getterHandle;
                    final MethodHandle setterHandle;
                    if (mode == Mode.METHOD_HANDLE) {
                        getterHandle = DynamicMethodHandleRecord.getGetterMethodHandle(field, f, getter);
                        setterHandle = DynamicMethodHandleRecord.getSetterMethodHandle(field, f, setter);
                    } else {
                        getterHandle = null;
                        setterHandle = null;
                    }

                    final long offset;
                    if (mode == Mode.UNSAFE) {
                        if (f == null && getter != null)
                            throw new RuntimeException("Cannot use UNSAFE mode for class " + type.getName() + " because field " + field.name + " has a getter and/or a setter");
                        offset = DynamicUnsafeRecord.getFieldOffset(type, f);
                    } else
                        offset = -1L;

                    final DynamicGeneratedRecord.Accessor accessor;
                    if (mode == Mode.GENERATION) {
                        if ((type.getModifiers() & Modifier.PUBLIC) == 0)
                            throw new RuntimeException("Cannot use GENERATION mode because class " + type.getName() + " is not public.");
                        if (f != null && (f.getModifiers() & Modifier.PUBLIC) == 0)
                            throw new RuntimeException("Cannot use GENERATION mode because field " + f.getName() + " in class " + type.getName() + " is not public.");

                        accessor = DynamicGeneratedRecord.generateAccessor(type, field, f, getter, setter);
                    } else
                        accessor = null;

                    if (f != null || getter != null)
                        implementedFields.add(field);
                    table[field.id()] = new Entry(f, getter, setter, getterHandle, setterHandle, offset, accessor, indexed);
                }

                this.fieldSet = (Set) ImmutableSet.copyOf(implementedFields);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static java.lang.reflect.Field getField(Class<?> type, Field field) {
        java.lang.reflect.Field f = null;
        try {
            f = type.getField(field.name());
        } catch (NoSuchFieldException e) {
        }
        try {
            f = type.getDeclaredField(field.name());
        } catch (NoSuchFieldException e) {
        }
        if (f != null) {
            f.setAccessible(true);
        }
        return f;
    }

    private static Method getGetter(Class<?> type, Field field) {
        try {
            return type.getMethod("get" + capitalize(field.name()));
        } catch (NoSuchMethodException e) {
        }
        if (field.type() == Field.BOOLEAN) {
            try {
                return type.getMethod("is" + capitalize(field.name()));
            } catch (NoSuchMethodException e) {
            }
        }
        return null;
    }

    private static Method getSetter(Class<?> type, Field field) {
        try {
            return type.getMethod("set" + capitalize(field.name()), field.typeClass());
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private static Method getIndexedGetter(Class<?> type, Field field) {
        assert field instanceof Field.ArrayField;
        try {
            return type.getMethod("get" + capitalize(field.name()), int.class);
        } catch (NoSuchMethodException e) {
        }
        if (field.type() == Field.BOOLEAN_ARRAY) {
            try {
                return type.getMethod("is" + capitalize(field.name()), int.class);
            } catch (NoSuchMethodException e) {
            }
        }
        return null;
    }

    private static Method getIndexedSetter(Class<?> type, Field field) {
        assert field instanceof Field.ArrayField;
        try {
            return type.getMethod("set" + capitalize(field.name()), int.class, field.typeClass().getComponentType());
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    private static String capitalize(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    static class Entry {
        final java.lang.reflect.Field field;
        final Method getter;
        final Method setter;
        final MethodHandle getterHandle;
        final MethodHandle setterHandle;
        final long offset;
        final DynamicGeneratedRecord.Accessor accessor;
        final boolean indexed;

        public Entry(java.lang.reflect.Field field, Method getter, Method setter, MethodHandle getterHandle, MethodHandle setterHandle, long offset, DynamicGeneratedRecord.Accessor accessor, boolean indexed) {
            this.field = field;
            this.getter = getter;
            this.setter = setter;
            this.getterHandle = getterHandle;
            this.setterHandle = setterHandle;
            this.offset = offset;
            this.accessor = accessor;
            this.indexed = indexed;
        }
    }

    /**
     * The type's fields
     *
     * @return the type's fields
     */
    public Set<Field<? super R, ?>> fields() {
        seal();
        return fieldSet;
    }

    ClassInfo getClassInfo(Class<?> clazz) {
        return vtables.get(clazz);
    }

    int getPrimitiveIndex() {
        return primitiveIndex;
    }

    int getObjectIndex() {
        return objectIndex;
    }

    int getPrimitiveOffset() {
        return primitiveOffset;
    }

    int getObjectOffset() {
        return objectOffset;
    }

    int[] getOffsets() {
        return offsets;
    }

    /**
     * Creates an new record instance of this type.
     * The returned implementation stores the record in an efficient memory representation.
     *
     * @return a newly constructed record of this type.
     */
    public Record<R> newInstance() {
        seal();
        return new SimpleRecord<R>(this);
    }

    /**
     * Creates an new record instance of this type, which reflects the given object.
     * The record's fields are mapped to the target's fields or getters/setters of the same name.
     * Changes to the record will be reflected in the target object and vice versa.
     * <p/>
     * The record's implementation {@link Mode} mode will be the best (fastest) one available for the target's class.
     *
     * @param target the POJO to wrap as a record
     * @return a newly constructed record of this type, which reflects {@code target}.
     */
    public Record<R> newInstance(Object target) {
        return newInstance(target, null);
    }

    /**
     * Creates an new record instance of this type, which reflects the given object.
     * The record's fields are mapped to the target's fields or getters/setters of the same name.
     * Changes to the record will be reflected in the target object and vice versa.
     *
     * @param target the POJO to wrap as a record
     * @param mode   the record's implementation {@link Mode} mode.
     * @return a newly constructed record of this type, which reflects {@code target}.
     */
    public Record<R> newInstance(Object target, Mode mode) {
        seal();
        currentMode.set(mode);
        ClassInfo ci = vtables.get(target.getClass());
        if (mode == null)
            mode = ci.mode;
        if (mode != Mode.REFLECTION && ci.mode != mode)
            throw new IllegalStateException("Target's class, " + target.getClass().getName() + ", has been mirrored with a different, incompatible mode, " + ci.mode);
        switch (mode) {
            case METHOD_HANDLE:
                return new DynamicMethodHandleRecord<R>(this, target);
            case REFLECTION:
                return new DynamicReflectionRecord<R>(this, target);
            case UNSAFE:
                return new DynamicUnsafeRecord<R>(this, target);
            case GENERATION:
                return new DynamicGeneratedRecord<R>(this, target);
        }
        throw new AssertionError("unreachable");
    }

    /**
     * Creates an new {@link RecordArray} instance of this type.
     * The returned implementation stores the record array in an efficient memory representation.
     *
     * @return a newly constructed record array of this type.
     */
    public RecordArray<R> newArray(int size) {
        seal();
        return new SimpleRecordArray<R>(this, size);
    }
}

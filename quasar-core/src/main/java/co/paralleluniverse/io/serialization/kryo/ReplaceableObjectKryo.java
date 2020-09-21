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
package co.paralleluniverse.io.serialization.kryo;

import co.paralleluniverse.common.reflection.GetAccessDeclaredMethod;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedActionException;

import static java.security.AccessController.doPrivileged;

/**
 * A subclass of {@link Kryo} that respects {@link Serializable}'s {@code writeReplace} and {@code readResolve}.
 *
 * @author pron
 */
public class ReplaceableObjectKryo extends Kryo {
    private static final ClassValue<SerializationMethods> replaceMethodsCache = new ClassValue<>() {
        @Override
        protected SerializationMethods computeValue(Class<?> type) {
            return new SerializationMethods(getMethodByReflection(type, WRITE_REPLACE),
                    getMethodByReflection(type, READ_RESOLVE));
        }
    };
    private static final String WRITE_REPLACE = "writeReplace";
    private static final String READ_RESOLVE = "readResolve";

    @Override
    public void writeClassAndObject(Output output, Object object) {
        if (output == null)
            throw new IllegalArgumentException("output cannot be null.");
        if (object == null) {
            super.writeClass(output, null);
            return;
        }
        Object newObj = getReplacement(getMethods(object.getClass()).writeReplace, object);
        setAutoReset(false);
        Registration registration = super.writeClass(output, newObj.getClass());
        setAutoReset(true);
        super.writeObject(output, newObj, registration.getSerializer());
//        System.out.println("wrote an object "+newObj+" id "+registration.getId());
//        reset();
    }

    public ReplaceableObjectKryo() {
    }

    @Override
    protected Serializer<?> newDefaultSerializer(Class type) {
        final Serializer<?> s = super.newDefaultSerializer(type);
        if (s instanceof FieldSerializer)
            ((FieldSerializer<?>) s).setIgnoreSyntheticFields(false);
        return s;
    }

    @Override
    public Registration writeClass(Output output, Class type) {
        if (type == null || getMethods(type).writeReplace == null)
            return super.writeClass(output, type);
        return super.getRegistration(type); // do nothing. write object will write the class too
    }

    @Override
    public void writeObject(Output output, Object object, Serializer serializer) {
        Method m = getMethods(object.getClass()).writeReplace;
        if (m != null) {
            object = getReplacement(m, object);
            Registration reg = super.writeClass(output, object.getClass());
            serializer = reg.getSerializer();
        }
        super.writeObject(output, object, serializer);
//        System.out.println("wrote2 an object "+object+" id "+getRegistration(object.getClass()).getId());

    }

    @Override
    public <T> T readObject(Input input, Class<T> type, Serializer serializer) {
        return readReplace(super.readObject(input, type, serializer));
    }

    @Override
    public <T> T readObject(Input input, Class<T> type) {
        return readReplace(super.readObject(input, type));
    }

    @Override
    public <T> T readObjectOrNull(Input input, Class<T> type) {
        return readReplace(super.readObjectOrNull(input, type));
    }

    @Override
    public <T> T readObjectOrNull(Input input, Class<T> type, Serializer serializer) {
        return readReplace(super.readObjectOrNull(input, type, serializer));
    }

    @Override
    public Object readClassAndObject(Input input) {
        return readReplace(super.readClassAndObject(input));
    }

    private <T> T readReplace(Object obj) {
        if (obj == null)
            return null;
        return (T) getReplacement(getMethods(obj.getClass()).readResolve, obj);
    }

    private static Object getReplacement(Method m, Object object) {
        if (m == null)
            return object;
        try {
            return m.invoke(object);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause()); // Exceptions.rethrow(e.getCause());
        }
    }

    private static SerializationMethods getMethods(Class<?> clazz) {
        return replaceMethodsCache.get(clazz);
    }

    private static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... args) throws NoSuchMethodException {
        try {
            return doPrivileged(new GetAccessDeclaredMethod(clazz, methodName, args));
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static Method getMethodByReflection(Class<?> clazz, final String methodName, Class<?>... paramTypes) throws SecurityException {
        if (!Serializable.class.isAssignableFrom(clazz))
            return null;

        Method m = null;
        try {
            m = getDeclaredMethod(clazz, methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            Class<?> ancestor = clazz.getSuperclass();
            while (ancestor != null) {
                if (!Serializable.class.isAssignableFrom(ancestor))
                    return null;
                try {
                    m = getDeclaredMethod(ancestor, methodName, paramTypes);
                    if (!Modifier.isPublic(m.getModifiers()) && !Modifier.isProtected(m.getModifiers()))
                        return null;
                    break;
                } catch (NoSuchMethodException ex1) {
                    ancestor = ancestor.getSuperclass();
                }
            }
        }
        return m;
    }

    private static class SerializationMethods {
        Method writeReplace;
        Method readResolve;

        public SerializationMethods(Method writeReplace, Method readResolve) {
            this.writeReplace = writeReplace;
            this.readResolve = readResolve;
        }
    }
}

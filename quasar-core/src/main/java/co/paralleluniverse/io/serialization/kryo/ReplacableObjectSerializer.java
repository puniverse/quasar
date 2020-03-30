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
 *
 * @author pron
 */
class ReplacableObjectSerializer extends FieldSerializer<Object> {
    public ReplacableObjectSerializer(Kryo kryo, Class type) {
        super(kryo, type);
    }

    @Override
    public void write(Kryo kryo, Output output, Object object) {
        super.write(kryo, output, getReplacement(object, "writeReplace"));
    }

    @Override
    public Object read(Kryo kryo, Input input, Class<Object> type) {
        return getReplacement(super.read(kryo, input, type), "readResolve");
    }

    private static Method getDeclaredMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        try {
            return doPrivileged(new GetAccessDeclaredMethod(clazz, methodName));
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof NoSuchMethodException) {
                throw (NoSuchMethodException) t;
            }
            throw new RuntimeException(t);
        }
    }

    private static Object getReplacement(Object obj, final String replaceMethodName) {
        try {
            Class clazz = obj.getClass();
            if (!Serializable.class.isAssignableFrom(clazz))
                return obj;

            Method m = null;
            try {
                m = getDeclaredMethod(clazz, replaceMethodName);
            } catch (NoSuchMethodException ex) {
                Class ancestor = clazz.getSuperclass();
                while (ancestor != null) {
                    if (!Serializable.class.isAssignableFrom(ancestor))
                        return obj;
                    try {
                        m = getDeclaredMethod(ancestor, replaceMethodName);
                        if (!Modifier.isPublic(m.getModifiers()) && !Modifier.isProtected(m.getModifiers()))
                            return obj;
                        break;
                    } catch (NoSuchMethodException ex1) {
                        ancestor = ancestor.getSuperclass();
                    }
                }
            }
            if (m == null)
                return obj;
            return m.invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            if (ex instanceof InvocationTargetException)
                ((InvocationTargetException) ex).getTargetException().printStackTrace();
            return obj;
        }
    }
}

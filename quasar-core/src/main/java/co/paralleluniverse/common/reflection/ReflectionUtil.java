/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Based, in part, on code from apache.commons-lang, released under the Apache License 2.0
 */
package co.paralleluniverse.common.reflection;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pron
 */
public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static <T extends AccessibleObject> T accessible(T obj) {
        if (obj == null)
            return null;
        obj.setAccessible(true);
        return obj;
    }

    public static Class<?>[] getTypes(Object... vals) {
        Class<?>[] types = new Class[vals.length];
        for (int i = 0; i < vals.length; i++)
            types[i] = vals[i] != null ? vals[i].getClass() : null;

        return types;
    }

    public static boolean isAssignable(Class<?>[] classArray, Class<?>[] toClassArray, final boolean autoboxing) {
        if (classArray.length != toClassArray.length)
            return false;

        for (int i = 0; i < classArray.length; i++) {
            if (!isAssignable(classArray[i], toClassArray[i], autoboxing))
                return false;
        }
        return true;
    }

    public static <T> Constructor<T> getMatchingConstructor(final Class<T> cls, final Class<?>... parameterTypes) {
        // see if we can find the constructor directly
        // most of the time this works and it's much faster
        try {
            final Constructor<T> ctor = cls.getConstructor(parameterTypes);
            return ctor;
        } catch (final NoSuchMethodException e) {
        }

        // return best match:
        Constructor<T> result = null;
        final Constructor<?>[] ctors = cls.getConstructors();
        for (Constructor<?> ctor : ctors) {
            // compare parameters
            if (isAssignable(parameterTypes, ctor.getParameterTypes(), true)) {
                if (result == null || compareParameterTypes(ctor.getParameterTypes(), result.getParameterTypes(), parameterTypes) < 0)
                    result = (Constructor<T>) ctor;
            }
        }
        return result;
    }

    /**
     * Compares the relative fitness of two sets of parameter types in terms of
     * matching a third set of runtime parameter types, such that a list ordered
     * by the results of the comparison would return the best match first
     * (least).
     *
     * @param left   the "left" parameter set
     * @param right  the "right" parameter set
     * @param actual the runtime parameter types to match against
     *               <code>left</code>/<code>right</code>
     * @return int consistent with <code>compare</code> semantics
     */
    static int compareParameterTypes(final Class<?>[] left, final Class<?>[] right, final Class<?>[] actual) {
        final float leftCost = getTotalTransformationCost(actual, left);
        final float rightCost = getTotalTransformationCost(actual, right);
        return leftCost < rightCost ? -1 : rightCost < leftCost ? 1 : 0;
    }

    private static float getTotalTransformationCost(final Class<?>[] srcArgs, final Class<?>[] destArgs) {
        float totalCost = 0.0f;
        for (int i = 0; i < srcArgs.length; i++) {
            Class<?> srcClass, destClass;
            srcClass = srcArgs[i];
            destClass = destArgs[i];
            totalCost += getObjectTransformationCost(srcClass, destClass);
        }
        return totalCost;
    }

    private static float getObjectTransformationCost(Class<?> srcClass, final Class<?> destClass) {
        if (destClass.isPrimitive()) {
            return getPrimitivePromotionCost(srcClass, destClass);
        }
        float cost = 0.0f;
        while (srcClass != null && !destClass.equals(srcClass)) {
            if (destClass.isInterface() && isAssignable(srcClass, destClass, true)) {
                // slight penalty for interface match.
                // we still want an exact match to override an interface match,
                // but
                // an interface match should override anything where we have to
                // get a superclass.
                cost += 0.25f;
                break;
            }
            cost++;
            srcClass = srcClass.getSuperclass();
        }
        /*
         * If the destination class is null, we've travelled all the way up to
         * an Object match. We'll penalize this by adding 1.5 to the cost.
         */
        if (srcClass == null) {
            cost += 1.5f;
        }
        return cost;
    }

    private static float getPrimitivePromotionCost(final Class<?> srcClass, final Class<?> destClass) {
        float cost = 0.0f;
        Class<?> cls = srcClass;
        if (!cls.isPrimitive()) {
            // slight unwrapping penalty
            cost += 0.1f;
            cls = wrapperToPrimitive(cls);
        }
        for (int i = 0; cls != destClass && i < ORDERED_PRIMITIVE_TYPES.length; i++) {
            if (cls == ORDERED_PRIMITIVE_TYPES[i]) {
                cost += 0.1f;
                if (i < ORDERED_PRIMITIVE_TYPES.length - 1) {
                    cls = ORDERED_PRIMITIVE_TYPES[i + 1];
                }
            }
        }
        return cost;
    }

    public static boolean isAssignable(Class<?> cls, final Class<?> toClass, final boolean autoboxing) {
        if (toClass == null)
            return false;

        // have to check for null, as isAssignableFrom doesn't
        if (cls == null)
            return !toClass.isPrimitive();

        //autoboxing:
        if (autoboxing) {
            if (cls.isPrimitive() && !toClass.isPrimitive()) {
                cls = primitiveToWrapper(cls);
                if (cls == null) {
                    return false;
                }
            }
            if (toClass.isPrimitive() && !cls.isPrimitive()) {
                cls = wrapperToPrimitive(cls);
                if (cls == null) {
                    return false;
                }
            }
        }
        if (cls.equals(toClass))
            return true;

        if (cls.isPrimitive()) {
            if (!toClass.isPrimitive())
                return false;

            if (Integer.TYPE.equals(cls)) {
                return Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Long.TYPE.equals(cls)) {
                return Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Boolean.TYPE.equals(cls)) {
                return false;
            }
            if (Double.TYPE.equals(cls)) {
                return false;
            }
            if (Float.TYPE.equals(cls)) {
                return Double.TYPE.equals(toClass);
            }
            if (Character.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Short.TYPE.equals(cls)) {
                return Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            if (Byte.TYPE.equals(cls)) {
                return Short.TYPE.equals(toClass)
                        || Integer.TYPE.equals(toClass)
                        || Long.TYPE.equals(toClass)
                        || Float.TYPE.equals(toClass)
                        || Double.TYPE.equals(toClass);
            }
            // should never get here
            return false;
        }
        return toClass.isAssignableFrom(cls);
    }

    public static Class<?> primitiveToWrapper(final Class<?> cls) {
        Class<?> convertedClass = cls;
        if (cls != null && cls.isPrimitive()) {
            convertedClass = primitiveWrapperMap.get(cls);
        }
        return convertedClass;
    }

    public static Class<?> wrapperToPrimitive(final Class<?> cls) {
        return wrapperPrimitiveMap.get(cls);
    }

    public static Type getGenericParameterType(Class<?> cls, Class<?> genericSuper, int paramIndex) {
        if (!genericSuper.isAssignableFrom(cls))
            throw new IllegalArgumentException("Class " + cls.getName() + " does not implement or extend " + genericSuper.getName());
        Type res = getGenericParameterType0(cls, genericSuper, paramIndex);
        return !(res instanceof TypeVariable) ? res : null;
    }

    private static Type getGenericParameterType0(Class<?> cls, Class<?> genericSuper, int paramIndex) {
        if (!genericSuper.isAssignableFrom(cls))
            return null;
        if (genericSuper.isInterface()) {
            for (Type type : cls.getGenericInterfaces()) {
                final Class<?> clazz = getClass(type);
                if (genericSuper.isAssignableFrom(clazz)) {
                    if (genericSuper.equals(clazz))
                        return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[paramIndex] : null;
                    else {
                        for (Class<?> iface : cls.getInterfaces()) {
                            final Type res = getGenericParameterType0(iface, genericSuper, paramIndex);
                            if (res != null)
                                return res;
                        }
                    }
                }
            }
            return null;
        } else {
            Type type = cls.getGenericSuperclass();
            assert genericSuper.isAssignableFrom(getClass(type));
            if (genericSuper.equals(getClass(type)))
                return type instanceof ParameterizedType ? ((ParameterizedType) type).getActualTypeArguments()[paramIndex] : null;
            else
                return getGenericParameterType0(cls.getSuperclass(), genericSuper, paramIndex);
        }
    }

    public static Class<?> getClass(Type type) {
        if (type instanceof Class)
            return (Class<?>) type;
        if (type instanceof ParameterizedType)
            return (Class<?>) ((ParameterizedType) type).getRawType();
        if (type instanceof GenericArrayType)
            return Array.newInstance((Class<?>) ((GenericArrayType) type).getGenericComponentType(), 0).getClass();
        return null;
    }

    public static Class<?>[] getParameterTypes(Member m) {
        // necessary prior to Java 8's Executable
        if (m instanceof Method)
            return ((Method) m).getParameterTypes();
        if (m instanceof Constructor)
            return ((Constructor<?>) m).getParameterTypes();
        throw new IllegalArgumentException("Not an executable: " + m);
    }
    
    private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>();

    static {
        primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
        primitiveWrapperMap.put(Byte.TYPE, Byte.class);
        primitiveWrapperMap.put(Character.TYPE, Character.class);
        primitiveWrapperMap.put(Short.TYPE, Short.class);
        primitiveWrapperMap.put(Integer.TYPE, Integer.class);
        primitiveWrapperMap.put(Long.TYPE, Long.class);
        primitiveWrapperMap.put(Double.TYPE, Double.class);
        primitiveWrapperMap.put(Float.TYPE, Float.class);
        primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
    }
    private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<>();

    static {
        for (final Class<?> primitiveClass : primitiveWrapperMap.keySet()) {
            final Class<?> wrapperClass = primitiveWrapperMap.get(primitiveClass);
            if (!primitiveClass.equals(wrapperClass)) {
                wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
            }
        }
    }
    private static final Class<?>[] ORDERED_PRIMITIVE_TYPES = {Byte.TYPE, Short.TYPE,
        Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE};
}

/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.actors;

import co.paralleluniverse.common.reflection.ReflectionUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A specification of how to construct an actor
 *
 * @author pron
 */
public class ActorSpec<T extends Actor<Message, V>, Message, V> implements ActorBuilder<Message, V> {
    public static <Message, V, T extends Actor<Message, V>> ActorSpec<T, Message, V> of(Class<T> type, Object... params) {
        return new ActorSpec<>(type, params);
    }
    private final AtomicReference<Class<T>> classRef;
    private final String className;
    private final Object[] params;
    private Class<?>[] ctorParamTypes;
    private Constructor<T> ctor;

    /**
     * Specifies an actor of a given type and given constructor parameters.
     *
     * @param className the name of the actor class
     * @param params    the parameters to pass to the actor's constructors
     */
    public ActorSpec(String className, Object[] params) {
        this((Constructor<T>) matchingConstructor(currentClassFor(className), params), params, false);
    }

    /**
     * Specifies an actor of a given type and given constructor parameters.
     *
     * @param type   the type of the actor
     * @param params the parameters to pass to the actor's constructors
     */
    public ActorSpec(Class<T> type, Object[] params) {
        this(matchingConstructor(type, params), params, false);
    }

    /**
     * Specifies an actor with given constructor and given constructor parameters.
     *
     * @param ctor   the actor's constructor
     * @param params the parameters to pass to the actor's constructors
     */
    public ActorSpec(Constructor<T> ctor, Object[] params) {
        this(ctor, params, false);
    }

    private ActorSpec(Constructor<T> ctor, Object[] params, boolean ignore) {
        this.className = ctor.getDeclaringClass().getName();
        this.classRef = (AtomicReference<Class<T>>) (Object) ActorLoader.getClassRef(className);
        this.params = Arrays.copyOf(params, params.length);

        this.ctor = ctor;
        ctor.setAccessible(true);
    }

    private void updateConstructor() {
        try {
            this.ctor = ActorLoader.currentClassFor(ctor.getDeclaringClass()).getConstructor(ctor.getParameterTypes());
            ctor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private static <T> Constructor<T> matchingConstructor(Class<T> type, Object[] params) {
        final Constructor<T> ctor = ReflectionUtil.getMatchingConstructor(type, ReflectionUtil.getTypes(params));
        if (ctor == null)
            throw new IllegalArgumentException("No constructor for type " + type.getName() + " was found to match parameters " + Arrays.toString(params));
        return ctor;
    }

    @Override
    public T build() {
        if (classRef.get() != ctor.getDeclaringClass())
            updateConstructor();
        try {
            T instance = ctor.newInstance(params);
            instance.setSpec(this);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final String ps = Arrays.toString(params);
        return "ActorSpec{" + ctor.getName() + '(' + ps.substring(1, ps.length() - 1) + ")}";
    }

    private static Class<?> currentClassFor(String className) {
        try {
            return ActorLoader.currentClassFor(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

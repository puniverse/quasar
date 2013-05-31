/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 *
 * @author pron
 */
public class ActorSpec<T extends LocalActor<Message, V>, Message, V> implements ActorBuilder<Message, V> {
    public static <Message, V, T extends LocalActor<Message, V>> ActorSpec<T, Message, V> of(Class<T> type, Object... params) {
        return new ActorSpec<>(type, params);
    }
    final Constructor<T> ctor;
    final Object[] params;

    public ActorSpec(Class<T> type, Object[] params) {
        this(ReflectionUtil.getMatchingConstructor(type, ReflectionUtil.getTypes(params)), params);
    }

    public ActorSpec(Constructor<T> ctor, Object[] params) {
        this.ctor = ctor;
        this.params = Arrays.copyOf(params, params.length);
        ctor.setAccessible(true);
    }

    @Override
    public T build() {
        try {
            T instance = ctor.newInstance(params);
            instance.setSpec(this);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

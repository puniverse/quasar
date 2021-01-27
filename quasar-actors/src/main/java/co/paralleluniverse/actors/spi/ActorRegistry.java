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
package co.paralleluniverse.actors.spi;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import static co.paralleluniverse.common.reflection.ReflectionUtil.accessible;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author pron
 */
public abstract class ActorRegistry {
    public abstract <Message> void register(Actor<Message, ?> actor, ActorRef<Message> actorRef) throws SuspendExecution;

    public abstract <Message> void unregister(Actor<Message, ?> actor, ActorRef<Message> actorRef);

    public abstract ActorRef<?> getActor(String name) throws InterruptedException, SuspendExecution;

    public abstract ActorRef<?> getActor(String name, long timeout, TimeUnit unit) throws InterruptedException, SuspendExecution;

    public abstract ActorRef<?> tryGetActor(String name) throws SuspendExecution;

    public abstract <T extends ActorRef<?>> T getOrRegisterActor(String name, Callable<T> factory) throws SuspendExecution;

    public abstract void shutdown();

    protected Object getGlobalId(Actor<?, ?> actor) {
        try {
            return getGlobalId.invoke(actor);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    protected void setGlobalId(Actor<?, ?> actor, Object globalId) {
        try {
            setGlobalId.invoke(actor, globalId);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static final Method getGlobalId;
    private static final Method setGlobalId;

    static {
        try {
            getGlobalId = accessible(Actor.class.getDeclaredMethod("getGlobalId"));
            setGlobalId = accessible(Actor.class.getDeclaredMethod("setGlobalId", Object.class));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}

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

import co.paralleluniverse.strands.channels.QueueChannel;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public final class ActorUtil {
    public static Object randtag() {
        return new BigInteger(80, ThreadLocalRandom.current()) {
            @Override
            public String toString() {
                return toString(16);
            }
        };
    }

    public static void sendOrInterrupt(ActorRef actor, Object message) {
        ((ActorRefImpl) actor).sendOrInterrupt(message);
    }

    public static void join(ActorRef<?> actor) throws ExecutionException, InterruptedException {
        actorOf(actor).join();
    }

    public static void join(ActorRef<?> actor, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        actorOf(actor).join(timeout, unit);
    }

    public static <V> V get(ActorRef<?> actor) throws ExecutionException, InterruptedException {
        return (V) actorOf(actor).get();
    }

    public static <V> V get(ActorRef<?> actor, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return (V) actorOf(actor).get(timeout, unit);
    }

    public static QueueChannel<Object> getMailbox(ActorRef<?> actor) {
        return actorOf(actor).mailbox();
    }

    static Actor actorOf(ActorRef ar) {
        while (ar instanceof ActorRefDelegate)
            ar = ((ActorRefDelegate) ar).ref;
        if (!(ar instanceof LocalActorRef))
            throw new IllegalArgumentException("ActorRef " + ar + " is not a local actor.");
        return ((LocalActorRef) ar).getActor();
    }

    private ActorUtil() {
    }
}

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

import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.QueueChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Static methods that provide access to {@link Actor}'s functionality through an {@link ActorRef} if the actor is local.
 * Application code shouldn't normally use these methods. They are provided mainly for testing, and defining sophisticated behaviors.
 * <p/>
 * We provide these services as static methods rather than return a reference to the actor because we don't want anyone to accidentally
 * keep a reference to the actor itself.
 *
 * @author pron
 */
public final class LocalActorUtil {
    public static boolean isLocal(ActorRef<?> actor) {
        return stripDelegates(actor) instanceof LocalActorRef;
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

    public static Strand getStrand(ActorRef<?> actor) {
        return actorOf(actor).getStrand();
    }

    public static boolean isDone(ActorRef<?> actor) {
        return actorOf(actor).isDone();
    }

    public static ActorMonitor getMonitor(ActorRef<?> actor) {
        return actorOf(actor).getMonitor();
    }

    public static void stopMonitor(ActorRef<?> actor) {
        actorOf(actor).stopMonitor();
    }

    public static void unregister(ActorRef<?> actor) {
        actorOf(actor).unregister();
    }

    public static int getQueueLength(ActorRef<?> actor) {
        return actorOf(actor).getQueueLength();
    }

    public static void link(ActorRef<?> actor1, ActorRef<?> actor2) {
        actorOf(actor1).link(actor2);
    }

    public static void unlink(ActorRef<?> actor1, ActorRef<?> actor2) {
        actorOf(actor1).unlink(actor2);
    }

    public static <M, V> ActorBuilder<M, V> toActorBuilder(ActorRef<M> actor) {
        actor = stripDelegates(actor);
        if (!(actor instanceof LocalActorRef))
            throw new IllegalArgumentException("ActorRef " + actor + " is not a local actor, and cannot be used as an ActorBuilder.");
        return (ActorBuilder<M, V>) actor;
    }

    private static Actor actorOf(ActorRef ar) {
        ar = stripDelegates(ar);
        if (!(ar instanceof LocalActorRef))
            throw new IllegalArgumentException("ActorRef " + ar + " is not a local actor.");
        return ((LocalActorRef) ar).getActor();
    }

    static <M> ActorRef<M> stripDelegates(ActorRef<M> ar) {
        while (ar instanceof ActorRefDelegate)
            ar = ((ActorRefDelegate) ar).ref;
        return ar;
    }

    private LocalActorUtil() {
    }
}

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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorBuilder;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.ActorRefDelegate;
import co.paralleluniverse.actors.ActorUtil;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.actors.ShutdownMessage;
import co.paralleluniverse.fibers.Joinable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class BehaviorImpl extends ActorRefDelegate<Object> implements Behavior, java.io.Serializable {
    protected BehaviorImpl(ActorRef<Object> actor) {
        super(actor);
    }

    /**
     * Asks this actor to shut down. Works by sending a {@link ShutdownMessage} via {@link ActorUtil#sendOrInterrupt(ActorRef, Object) ActorUtil.sendOrInterrupt}.
     */
    @Override
    public void shutdown() {
        final ShutdownMessage message = new ShutdownMessage(LocalActor.self());
        ActorUtil.sendOrInterrupt(ref, message);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    static final class Local extends BehaviorImpl implements LocalBehavior<BehaviorImpl> {
        Local(ActorRef<Object> actor) {
            super(actor);
        }

        @Override
        public BehaviorImpl writeReplace() throws java.io.ObjectStreamException {
            return new BehaviorImpl(ref);
        }

        @Override
        public Actor<Object, Void> build() {
            return ((ActorBuilder<Object, Void>) ref).build();
        }

        @Override
        public void join() throws ExecutionException, InterruptedException {
            ((Joinable<Void>) ref).join();
        }

        @Override
        public void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            ((Joinable<Void>) ref).join(timeout, unit);
        }

        @Override
        public Void get() throws ExecutionException, InterruptedException {
            return ((Joinable<Void>) ref).get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
            return ((Joinable<Void>) ref).get(timeout, unit);
        }

        @Override
        public boolean isDone() {
            return ((Joinable<Void>) ref).isDone();
        }
    }
}

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

import java.util.Objects;

/**
 * An {@link ActorRef} which delegates all operations to another {@code ActorRef}.
 *
 * @author pron
 */
public class ActorRefDelegate<Message> extends ActorRef<Message> {
    private final ActorRef<Message> ref;

    /**
     * Constructs an {@code ActorRefDelegate}
     *
     * @param ref the {@link ActorRef} to which all operations will be delegated
     */
    public ActorRefDelegate(ActorRef<Message> ref) {
        this.ref = ref;
    }

    ActorRef<Message> getRef() {
        return ref;
    }

    @Override
    protected final ActorImpl<Message> getImpl() {
        return getRef().getImpl();
    }

    protected boolean isInActor() {
        return Objects.equals(getRef(), LocalActor.self());
    }

    static <T> ActorRef<T> stripDelegates(ActorRef<T> r) {
        while (r instanceof ActorRefDelegate)
            r = ((ActorRefDelegate<T>) r).getRef();
        return r;
    }
}

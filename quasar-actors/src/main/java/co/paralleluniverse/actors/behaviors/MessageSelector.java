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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.MessageProcessor;
import java.util.Objects;

/**
 * A fluent interface for creating {@link MessageProcessor}s that select messages matching a few simple criteria.
 *
 * @author pron
 */
public abstract class MessageSelector<M1, M2> implements MessageProcessor<Object, M2> {
    /**
     * Creates a new {@link MessageSelector}.
     *
     * @return A new {@code MessageSelector} that selects all messages.
     */
    public static <M> MessageSelector<?, M> select() {
        return new MessageSelector<Object, M>() {
            @Override
            public boolean matches(Object m) {
                return true;
            }
        };
    }

    /**
     * Selects messages of the given class.
     *
     * @param type The class of the messages to select.
     * @return a new {@link MessageSelector} that selects messages of the given class.
     */
    public <M extends M1> MessageSelector<M1, M> ofType(final Class<M> type) {
        return new MessageSelector<M1, M>() {
            @Override
            public boolean matches(M1 m) {
                return MessageSelector.this.matches(m) && type.isInstance(m);
            }
        };
    }

    /**
     * Creates a {@link MessageProcessor} that selects {@link FromMessage}s from the given actor.
     *
     * @param actor The sender of the message to select (should be equal to {@link FromMessage#getFrom() })
     * @return a new {@link MessageSelector} that selects messages from the given sender.
     */
    public <M extends FromMessage> MessageSelector<M, M> from(final ActorRef<?> actor) {
        return new MessageSelector<M, M>() {
            @Override
            public boolean matches(M m) {
                return MessageSelector.this.matches((M1) m) && m instanceof FromMessage && Objects.equals(actor, ((FromMessage) m).getFrom());
            }
        };
    }

    /**
     * Returns a {@link MessageSelector} that selects {@link IdMessage}s with the given id.
     *
     * @param id The id of the message to select (should be equal to {@link IdMessage#getId() })
     * @return a new {@link MessageSelector} that selects messages from the given id.
     */
    public <M extends IdMessage> MessageSelector<M, M> withId(final Object id) {
        return new MessageSelector<M, M>() {
            @Override
            public boolean matches(M m) {
                return MessageSelector.this.matches((M1) m) && m instanceof IdMessage && Objects.equals(id, ((IdMessage) m).getId());
            }
        };
    }

    protected abstract boolean matches(M1 m);

    @Override
    public final M2 process(Object m) {
        return (M2) (matches((M1) m) ? m : null);
    }

    private MessageSelector() {
    }
}

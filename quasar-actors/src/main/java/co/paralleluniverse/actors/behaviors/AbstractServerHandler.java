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
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

/**
 * A convenience class implementing the {@link ServerHandler} interface.
 * All methods do nothing, except for {@link #handleCall(ActorRef, Object, Object) handleCall} which throws an
 * {@code UnsupportedOperationException}.
 *
 * @author pron
 */
public class AbstractServerHandler<CallMessage, V, CastMessage> implements ServerHandler<CallMessage, V, CastMessage> {
    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation does nothing</b></p>
     */
    @Override
    public void init() throws SuspendExecution {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation throws an {@link UnsupportedOperationException}.</b></p>
     */
    @Override
    public V handleCall(ActorRef<?> from, Object id, CallMessage m) throws SuspendExecution {
        throw new UnsupportedOperationException(m.toString());
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation does nothing</b></p>
     */
    @Override
    public void handleCast(ActorRef<?> from, Object id, CastMessage m) throws SuspendExecution {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation does nothing</b></p>
     */
    @Override
    public void handleInfo(Object m) throws SuspendExecution {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation does nothing</b></p>
     */
    @Override
    public void handleTimeout() throws SuspendExecution {
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>This implementation does nothing</b></p>
     */
    @Override
    public void terminate(Throwable cause) throws SuspendExecution {
    }
}

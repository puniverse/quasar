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
import co.paralleluniverse.fibers.SuspendExecution;

/**
 *
 * @author pron
 */
public class AbstractServer<CallMessage, V, CastMessage> implements Server<CallMessage, V, CastMessage> {
    @Override
    public void init() throws SuspendExecution {
    }

    @Override
    public V handleCall(Actor<V> from, Object id, CallMessage m) throws SuspendExecution {
        throw new UnsupportedOperationException(m.toString());
    }

    @Override
    public void handleCast(Actor<V> from, Object id, CastMessage m) throws SuspendExecution {
    }

    @Override
    public void handleInfo(Object m) throws SuspendExecution {
    }

    @Override
    public void handleTimeout() throws SuspendExecution {
    }

    @Override
    public void terminate(Throwable cause) throws SuspendExecution {
    }
}

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
import co.paralleluniverse.actors.RemoteActor;
import static co.paralleluniverse.actors.behaviors.RequestReplyHelper.call;
import co.paralleluniverse.fibers.SuspendExecution;

/**
 *
 * @author pron
 */
public class RemoteSupervisor extends RemoteBasicGenBehavior implements Supervisor {
    public RemoteSupervisor(RemoteActor<Object> actor) {
        super(actor);
    }

    @Override
    public final Actor addChild(ChildSpec spec) throws SuspendExecution, InterruptedException {
        final GenResponseMessage res = call(this, new LocalSupervisor.AddChildMessage(RequestReplyHelper.from(), null, spec));
        return ((GenValueResponseMessage<Actor>) res).getValue();
    }

    @Override
    public final boolean removeChild(Object id, boolean terminate) throws SuspendExecution, InterruptedException {

        final GenResponseMessage res = call(this, new LocalSupervisor.RemoveChildMessage(RequestReplyHelper.from(), null, id, terminate));
        return ((GenValueResponseMessage<Boolean>) res).getValue();
    }
}

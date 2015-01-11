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
package co.paralleluniverse.strands.channels;

import co.paralleluniverse.fibers.DefaultFiberFactory;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * A topic that will spawn fibers from a factory and distribute messages to subscribers in parallel
 * using fibers, optionally waiting for them to complete receive before delivering the next one.
 * 
 * @author circlespainter
 */
public class ParallelTopic<Message> extends Topic<Message> {
    final private FiberFactory fiberFactory;
    private final boolean staged;
    
    /**
     * @param fiberFactory
     * @param staged        Will join all fibers delivering a message before initiating delivery of the next one.
     */
    public ParallelTopic(FiberFactory fiberFactory, boolean staged) {
        this.fiberFactory = fiberFactory;
        this.staged = staged;
    }

    public ParallelTopic(FiberFactory fiberFactory) {
        this(fiberFactory, true);
    }

    public ParallelTopic(boolean staged) {
        this(DefaultFiberFactory.instance());
    }
    
    public ParallelTopic() {
        this(true);
    }

    @Override
    public void send(final Message message) throws SuspendExecution, InterruptedException {
        if (isSendClosed())
            return;
        final ArrayList<Fiber> stage = new ArrayList<>(getSubscribers().size());
        for (final SendPort<? super Message> sub : getSubscribers()) {
            final Fiber f = fiberFactory.newFiber(new SuspendableCallable() {
                @Override
                public Object run() throws SuspendExecution, InterruptedException {
                    sub.send(message);
                    return null;
                }
            });
            if (staged) {
                stage.add(f);
            }
        }
        if (staged) {
            for(final Fiber f : stage) {
                try {
                    f.join();
                } catch (final ExecutionException ee) {
                    // This should never happen
                    throw new AssertionError(ee);
                }
            }
        }
    }
}

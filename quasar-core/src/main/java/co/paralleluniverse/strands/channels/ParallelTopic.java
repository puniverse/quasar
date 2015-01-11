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
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.StrandFactory;
import co.paralleluniverse.strands.SuspendableCallable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * A topic that will spawn fibers from a factory and distribute messages to subscribers in parallel
 * using strands, optionally waiting for them to complete receive before delivering the next one.
 * 
 * @author circlespainter
 */
public class ParallelTopic<Message> extends Topic<Message> {
    final ArrayList<Strand> stage = new ArrayList<>(getSubscribers().size());
    final private StrandFactory strandFactory;
    private final boolean staged;
    
    /**
     * @param staged        Will join all fibers delivering a message before initiating delivery of the next one.
     */
    public ParallelTopic(StrandFactory strandFactory, boolean staged) {
        this.strandFactory = strandFactory;
        this.staged = staged;
    }

    public ParallelTopic(StrandFactory strandFactory) {
        this(strandFactory, true);
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
        if (staged)
            stage.clear();
        for (final SendPort<? super Message> sub : getSubscribers()) {
            final Strand f = strandFactory.newStrand(new SuspendableCallable() {
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
            for(final Strand s : stage) {
                try {
                    s.join();
                } catch (final ExecutionException ee) {
                    // This should never happen
                    throw new AssertionError(ee);
                }
            }
        }
    }
}

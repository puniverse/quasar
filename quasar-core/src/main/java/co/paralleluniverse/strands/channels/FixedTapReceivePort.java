/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.strands.StrandFactory;

/**
 * Concrete {@link TapReceivePort} that will always forward to a single {@link SendPort}.
 * <p/>
 * @author circlespainter
 */
public class FixedTapReceivePort<Message> extends TapReceivePort<Message> {
    private final SendPort<? super Message> forwardTo;

    public FixedTapReceivePort(final ReceivePort<Message> target, final StrandFactory strandFactory, final SendPort<? super Message> forwardTo) {
        super(target, strandFactory);
        this.forwardTo = forwardTo;
    }

    public FixedTapReceivePort(final ReceivePort<Message> target, final SendPort<? super Message> forwardTo) {
        super(target);
        this.forwardTo = forwardTo;
    }

    @Override
    protected SendPort<? super Message> select(final Message m) {
        return forwardTo;
    }
}

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * @author circlespainter
 */
public class SelectorFiberAsync extends FiberAsync<Integer, IOException> implements SelectorCallback {
    private final SelectableChannel sc;
    private final Selector s;
    private final int ops;

    public SelectorFiberAsync(final SelectableChannel sc, final Selector s, final int ops) {
        this.sc = sc;
        this.s = s;
        this.ops = ops;
    }

    @Override
    protected void requestAsync() {
        try {
            FiberSelect.register(sc, ops, this);
        } catch (final IOException ioe) {
            asyncFailed(ioe);
        }
    }

    @Override
    public void complete(final SelectionKey key, final int readyOps) {
        try {
            FiberSelect.unregister(key);
        } catch (final IOException ioe) {
            asyncFailed(ioe);
        }
        asyncCompleted(readyOps);
    }

    @Override
    public void fail(final IOException ioe) {
        asyncFailed(ioe);
    }

    @Override
    @Suspendable
    public Integer run() throws IOException {
        try {
            return super.run();
        } catch (final SuspendExecution ex) {
            throw new AssertionError(ex);
        } catch (final InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}

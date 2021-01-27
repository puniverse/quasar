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
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.common.util.CheckedCallable;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.fibers.suspend.SuspendExecution;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
abstract class FiberAsyncIO<V> extends FiberAsync<V, IOException> {
    protected CompletionHandler<V, Fiber> makeCallback() {
        return new CompletionHandler<V, Fiber>() {
            @Override
            public void completed(V result, Fiber attachment) {
                FiberAsyncIO.this.asyncCompleted(result);
            }

            @Override
            public void failed(Throwable exc, Fiber attachment) {
                FiberAsyncIO.this.asyncFailed(exc);
            }
        };
    }

    @Override
    public V run() throws IOException, SuspendExecution {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Override
    public V run(long timeout, TimeUnit unit) throws IOException, SuspendExecution, TimeoutException {
        try {
            return super.run(timeout, unit);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    @Suspendable
    public V runSneaky() throws IOException {
        try {
            return super.run();
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }

    @Suspendable
    public static <V> V runBlockingIO(final ExecutorService exec, final CheckedCallable<V, IOException> callable) throws IOException {
        try {
            return FiberAsync.runBlocking(exec, callable);
        } catch (InterruptedException e) {
            throw new IOException(e);
        } catch (SuspendExecution e) {
            throw new AssertionError();
        }
    }
}

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
package co.paralleluniverse.fibers.futures;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/**
 *
 * @author pron
 */
public class FiberAsyncListenableFuture<V> extends FiberAsync<V, Runnable, Void, ExecutionException> {
    public static <V> V get(ListenableFuture<V> future) throws ExecutionException, InterruptedException, SuspendExecution {
        return new FiberAsyncListenableFuture<>(future).run();
    }
    
    private final ListenableFuture<V> fut;
    private final Runnable listener;
    private Fiber<?> fiber;

    private FiberAsyncListenableFuture(ListenableFuture<V> future) {
        this.fut = future;
        this.listener = new Runnable() {
            @Override
            public void run() {
                try {
                    assert fut.isDone();
                    final V res = fut.get();
                    completed(res, fiber);
                } catch (ExecutionException e) {
                    failed(e, fiber);
                } catch (InterruptedException e) {
                    throw new AssertionError(); // can't happen b/c we know future is done.
                }
            }
        };
    }

    @Override
    protected Runnable getCallback() {
        return listener;
    }

    @Override
    protected Void requestAsync(Fiber current, Runnable callback) {
        this.fiber = current;
        fut.addListener(listener, sameThreadExecutor);
        return null;
    }
    
    private static final Executor sameThreadExecutor = new Executor() {

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };
}

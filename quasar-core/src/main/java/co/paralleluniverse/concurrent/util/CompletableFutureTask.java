/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RunnableFuture;

/**
 *
 * @author pron
 */
public class CompletableFutureTask<V> extends CompletableFuture<V> implements RunnableFuture<V> {
    private final Callable<V> target;

    CompletableFutureTask(Callable<V> callable) {
        this.target = callable;
    }

    @Override
    public void run() {
        if(isCancelled())
            return;
        try {
            final V result = target.call();
            complete(result);
        } catch (Throwable ex) {
            completeExceptionally(ex);
        }
    }
}

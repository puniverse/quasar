/*
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
package co.paralleluniverse.concurrent.util;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;
import jsr166e.CompletableFuture;

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

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
package co.paralleluniverse.concurrent.forkjoin;

import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinWorkerThread;

/**
 *
 * @author pron
 */
public class ExtendedForkJoinWorkerFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final String name;

    public ExtendedForkJoinWorkerFactory(String name) {
        this.name = name;
    }
    
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = createThread(pool); // ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        final String workerNumber = thread.getName().substring(thread.getName().lastIndexOf('-') + 1);
        final String newThreadName = "ForkJoinPool-" + name + "-worker-" + workerNumber;
        thread.setName(newThreadName);
        //thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }
    
    protected ExtendedForkJoinWorkerThread createThread(ForkJoinPool pool) {
        return new ExtendedForkJoinWorkerThread(pool);
    }
}

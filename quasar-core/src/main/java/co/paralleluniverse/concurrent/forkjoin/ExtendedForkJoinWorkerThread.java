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
public class ExtendedForkJoinWorkerThread extends ForkJoinWorkerThread {
    private Object target;
    
    protected ExtendedForkJoinWorkerThread(ForkJoinPool pool) {
        super(pool);
    }

    Object getTarget() {
        return target;
    }

    void setTarget(Object target) {
        this.target = target;
    }
}

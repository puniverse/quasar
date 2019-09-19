/*
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
package co.paralleluniverse.concurrent.forkjoin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinTask;

public final class ForkJoinUtil {
    public static ExecutorService getPool() {
        return ForkJoinTask.getPool();
    }

    public static boolean inForkJoinPool() {
        return ForkJoinTask.inForkJoinPool();
    }

    private ForkJoinUtil() {

    }
}

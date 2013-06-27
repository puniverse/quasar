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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Map;

/**
 *
 * @author pron
 */
public final class ThreadUtil {
    public static void dumpThreads() {
        final Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
        for(Thread thread : threads.keySet())
            System.out.println(thread.getName() + "\t" + (thread.isDaemon() ? "DAEMON" : ""));
        
        // final ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false);
//        for(ThreadInfo thread : threads) {
//            System.out.println(thread.getThreadName() + ": ");
//        }
    }
    
    private ThreadUtil() {
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.util;

import jsr166e.ForkJoinPool;
import jsr166e.ForkJoinWorkerThread;

/**
 *
 * @author pron
 */
public class NamingForkJoinWorkerFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final String name;

    public NamingForkJoinWorkerFactory(String name) {
        this.name = name;
    }
    
    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool fjp) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(fjp);
        final String workerNumber = thread.getName().substring(thread.getName().lastIndexOf('-') + 1);
        final String newThreadName = "ForkJoinPool-" + name + "-worker-" + workerNumber;
        thread.setName(newThreadName);
        //thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
        return thread;
    }
}

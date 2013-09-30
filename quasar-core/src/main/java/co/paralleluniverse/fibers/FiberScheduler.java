/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class FiberScheduler {
    private final ForkJoinPool fjPool;
    private final FiberTimedScheduler timer;

    public FiberScheduler(ForkJoinPool fjPool, FiberTimedScheduler timeService) {
        this.fjPool = fjPool;
        this.timer = timeService;
    }
    
    public FiberScheduler(ForkJoinPool fjPool) {
        this.fjPool = fjPool;
        this.timer = new FiberTimedScheduler(fjPool);
    }

    ForkJoinPool getFjPool() {
        return fjPool;
    }

    FiberTimedScheduler getTimer() {
        return timer;
    }
}

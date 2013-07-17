/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import jsr166e.ForkJoinPool;
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
public class JMXFibersForkJoinPoolMonitor extends JMXForkJoinPoolMonitor implements FibersForkJoinPoolMXBean {
    private final LongAdder activeCount = new LongAdder();
    //private final LongAdder runnableCount = new LongAdder();
    private final LongAdder waitingCount = new LongAdder();

    public JMXFibersForkJoinPoolMonitor(String name, ForkJoinPool fjPool) {
        super(name, fjPool);
    }

    public void fiberStarted() {
        activeCount.increment();
    }

    public void fiberTerminated() {
        activeCount.decrement();
        //runnableCount.decrement();
    }

    public void fiberSuspended() {
        //runnableCount.decrement();
        waitingCount.increment();
    }

    public void fiberSubmitted(boolean start) {
        //runnableCount.increment();
        if (start)
            activeCount.increment();
        else
            waitingCount.decrement();
    }

    @Override
    public int getNumActiveFibers() {
        return activeCount.intValue();
    }

    @Override
    public int getNumRunnableFibers() {
        return getNumActiveFibers() - getNumWaitingFibers();
        //return runnableCount.intValue();
    }

    @Override
    public int getNumWaitingFibers() {
        return waitingCount.intValue();
    }
}

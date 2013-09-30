/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import co.paralleluniverse.common.monitoring.ForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MetricsForkJoinPoolMonitor;
import co.paralleluniverse.common.monitoring.MonitorType;
import co.paralleluniverse.concurrent.forkjoin.MonitoredForkJoinPool;
import co.paralleluniverse.concurrent.util.NamingForkJoinWorkerFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
        this(fjPool, createTimer(fjPool));
    }

    public FiberScheduler(String name, int parallelism, MonitorType monitorType) {
        this(createForkJoinPool(name, parallelism, monitorType));
    }

    private static ForkJoinPool createForkJoinPool(String name, int parallelism, MonitorType monitorType) {
        final MonitoredForkJoinPool fjPool = new MonitoredForkJoinPool(name, parallelism, new NamingForkJoinWorkerFactory(name), null, true);
        final ForkJoinPoolMonitor fjpMonitor;
        final FibersMonitor fibersMonitor;
        switch (monitorType) {
            case JMX:
                fjpMonitor = new JMXForkJoinPoolMonitor(name, fjPool);
                fibersMonitor = new JMXFibersMonitor(name, fjPool);
                break;
            case METRICS:
                fjpMonitor = new MetricsForkJoinPoolMonitor(name, fjPool);
                fibersMonitor = new MetricsFibersMonitor(name, fjPool);
                break;
            case NONE:
                fjpMonitor = null;
                fibersMonitor = new NoopFibersMonitor();
                break;
            default:
                throw new RuntimeException("Unsupported monitor type: " + monitorType);
        }

        fjPool.setMonitor(fjpMonitor);
        fjPool.setFibersMonitor(fibersMonitor);
        return fjPool;
    }

    private static FiberTimedScheduler createTimer(ForkJoinPool fjPool) {
        if (fjPool instanceof MonitoredForkJoinPool)
            return new FiberTimedScheduler(fjPool,
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat(((MonitoredForkJoinPool) fjPool).getName()).build());
        else
            return new FiberTimedScheduler(fjPool);
    }

    public ForkJoinPool getFjPool() {
        return fjPool;
    }

    FiberTimedScheduler getTimer() {
        return timer;
    }
}

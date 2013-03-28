/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.monitoring;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import java.util.Map;
import jsr166e.ForkJoinPool;

/**
 *
 * @author pron
 */
public class MetricsForkJoinPoolMonitor extends JMXForkJoinPoolMonitor {
    private final Histogram runsPerTask;
    
    public MetricsForkJoinPoolMonitor(String name, ForkJoinPool fjPool, Map<?, Integer> highContentionObjects) {
        super(name, fjPool, highContentionObjects);
        
        this.runsPerTask = Metrics.newHistogram(MetricsForkJoinPoolMonitor.class, "runsPerTask", name, true);
    }

    @Override
    public void doneTask(int runs) {
        runsPerTask.update(runs);
    }
}

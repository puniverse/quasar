/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.common.monitoring;

import co.paralleluniverse.common.monitoring.JMXForkJoinPoolMonitor.Status;
import java.util.Map;

/**
 *
 * @author pron
 */
public interface ForkJoinPoolMXBean {

    boolean getAsyncMode();

    ForkJoinInfo getInfo();

    int getParalellism();

    int getPoolSize();

    Status getStatus();

    void shutdown();

    void shutdownNow();
    
    Map<String, Integer> getHighContentionLocks();
}

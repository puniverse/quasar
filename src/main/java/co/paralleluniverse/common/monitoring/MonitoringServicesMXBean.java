/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.common.monitoring;

/**
 *
 * @author pron
 */
public interface MonitoringServicesMXBean {

    int getPerformanceTimerPeriod();

    int getStructuraltimerPeriod();

    boolean isPerformanceUpdates();

    boolean isStructuralUpdates();

    void setPerformanceTimerPeriod(int perfTimerPeriod);

    void setStructuraltimerPeriod(int structuraltimerPeriod);

    void startPerformanceUpdates();

    void startStructuralUpdates();

    void stopPerformanceUpdates();

    void stopStructuralUpdates();

    void setPerformanceUpdates(boolean value);

    void setStructuralUpdates(boolean value);
}

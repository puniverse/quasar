/*
 * Copyright (c) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.monitoring;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * A {@link MetricSet} that monitors CPU usage using HotSpot MBeans.
 * Some of the metrics reported may not be available on non-HotSpot JVMs.
 * <p/>
 * 
 * <ul>
 *  <li>{@code process.cpuUsage} – the CPU percentage currently used by the process (averaged between measurements).
 * the result isn't normalized to the number of cores, so it falls in the range {@code 0-100*cores}%</li>
 *  <li>{@code process.cpuLoad}</li>
 *  <li>{@code system.loadAverage}</li>
 *  <li>{@code system.cpuLoad}</li>
 * </ul>
 * @author pron
 */
public class CpuUsageGaugeSet implements MetricSet {
    private static final ObjectName RUNTIME_MBEAN;
    private static final String UPTIME_ATTR = "Uptime"; // runtime, long
    private static final ObjectName OS_MBEAN;
    private static final String AVAILABLE_PROCESSORS_ATTR = "AvailableProcessors"; // os, int
    private static final String PROCESS_CPU_TIME_ATTR = "ProcessCpuTime"; // os, long
    private static final String PROCESS_CPU_LOAD_ATTR = "ProcessCpuLoad"; // os, double
    private static final String SYSTEM_CPU_LOAD_ATTR = "SystemCpuLoad"; // os, double
    private static final String SYSTEM_LOAD_AVERAGE_ATTR = "SystemLoadAverage"; // os, double

    //private static final 
    static {
        try {
            RUNTIME_MBEAN = new ObjectName("java.lang:type=Runtime");
            OS_MBEAN = new ObjectName("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e);
        }
    }
    private final MBeanServer mbeanServer;

    public CpuUsageGaugeSet() {
        this(ManagementFactory.getPlatformMBeanServer());
    }

    public CpuUsageGaugeSet(MBeanServer beanServer) {
        this.mbeanServer = beanServer;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<String, Metric>();

        if (hasAttribute(OS_MBEAN, PROCESS_CPU_TIME_ATTR) && hasAttribute(RUNTIME_MBEAN, UPTIME_ATTR)) {
            gauges.put("process.cpuUsage", new Gauge<Double>() {
                private long prevUptime = -1;
                private long prevProcessCpuTime = -1;

                @Override
                public Double getValue() {
                    // final int processorCount = getAttributeInt(OS_MBEAN, AVAILABLE_PROCESSORS_ATTR);

                    long uptime = getAttributeLong(RUNTIME_MBEAN, UPTIME_ATTR) * 1000000;
                    long processCpuTime = getAttributeLong(OS_MBEAN, PROCESS_CPU_TIME_ATTR); //  / processorCount;

                    double cpuUsage = 0.0;
                    if (prevUptime != -1) {
                        long uptimeDiff = uptime - prevUptime;
                        long processTimeDiff = processCpuTime - prevProcessCpuTime;
                        cpuUsage = (uptimeDiff > 0 ? (double) processTimeDiff / (double) uptimeDiff : 0) * 100.0;
                    }

                    prevUptime = uptime;
                    prevProcessCpuTime = processCpuTime;

                    return cpuUsage;
                }
            });
        }

        if (hasAttribute(OS_MBEAN, PROCESS_CPU_LOAD_ATTR)) {
            gauges.put("process.cpuLoad", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return getAttributeDouble(OS_MBEAN, PROCESS_CPU_LOAD_ATTR) * 100.0; // in percents
                }
            });
        }

        if (hasAttribute(OS_MBEAN, SYSTEM_LOAD_AVERAGE_ATTR)) {
            gauges.put("system.loadAverage", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return getAttributeDouble(OS_MBEAN, SYSTEM_LOAD_AVERAGE_ATTR) * 100.0; // in percents
                }
            });
        }

        if (hasAttribute(OS_MBEAN, SYSTEM_CPU_LOAD_ATTR)) {
            gauges.put("system.cpuLoad", new Gauge<Double>() {
                @Override
                public Double getValue() {
                    return getAttributeDouble(OS_MBEAN, SYSTEM_CPU_LOAD_ATTR) * 100.0; // in percents
                }
            });
        }

        return gauges;
    }

    private boolean hasAttribute(ObjectName mbean, String attr) {
        try {
            MBeanInfo info = mbeanServer.getMBeanInfo(mbean);
            for (MBeanAttributeInfo ai : info.getAttributes()) {
                if (attr.equals(ai.getName()))
                    return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private int getAttributeInt(ObjectName mbean, String attr) {
        try {
            return (Integer) mbeanServer.getAttribute(mbean, attr);
        } catch (Exception e) {
            throw new RuntimeException("Could not get attribute " + attr + " from MBean " + mbean, e);
        }
    }

    private long getAttributeLong(ObjectName mbean, String attr) {
        try {
            return (Long) mbeanServer.getAttribute(mbean, attr);
        } catch (Exception e) {
            throw new RuntimeException("Could not get attribute " + attr + " from MBean " + mbean, e);
        }
    }

    private double getAttributeDouble(ObjectName mbean, String attr) {
        try {
            return (Double) mbeanServer.getAttribute(mbean, attr);
        } catch (Exception e) {
            throw new RuntimeException("Could not get attribute " + attr + " from MBean " + mbean, e);
        }
    }
}

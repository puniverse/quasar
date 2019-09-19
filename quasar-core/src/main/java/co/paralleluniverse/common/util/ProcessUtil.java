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
package co.paralleluniverse.common.util;

//import java.lang.management.ManagementFactory;
//import java.lang.management.RuntimeMXBean;
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import sun.management.VMManagement;

public class ProcessUtil {
    public static int getCurrentPid() {
        throw new UnsupportedOperationException();
//        try {
//            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
//            Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
//            jvmField.setAccessible(true);
//            VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
//            Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
//            getProcessIdMethod.setAccessible(true);
//            return (Integer) getProcessIdMethod.invoke(vmManagement);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
    }
}

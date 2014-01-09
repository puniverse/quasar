/*
 * Copyright (c) 2011-2014, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.logging;

/**
 *
 * @author pron
 */
public final class LoggingUtils {
    public static String hex(long num) {
        return Long.toHexString(num);
    }
    
    public static String hex(int num) {
        return Integer.toHexString(num);
    }
    
    private LoggingUtils() {
    }
}

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

/**
 * System properties utilities.
 *
 * @author pron
 */
public final class SystemProperties {
    /**
     * Returns the value of a system property which defaults to false.
     *
     * @param property the name of the system property
     * @return {@code true} iff the given property is defined and has the value {@code "true"} or the empty string.
     */
    public static boolean isEmptyOrTrue(String property) {
        final String value = System.getProperty(property);
        if (value == null)
            return false;
        return value.isEmpty() || Boolean.parseBoolean(value);
    }

    /**
     * Returns the value of a system property which defaults to true.
     *
     * @param property the name of the system property
     * @return {@code true} iff the given property is undefined, or defined and has the value {@code "true"} or the empty string.
     */
    public static boolean isNotFalse(String property) {
        final String value = System.getProperty(property);
        if (value == null)
            return true;
        if (value.isEmpty())
            return true;
        return value.isEmpty() || Boolean.parseBoolean(value);
    }

    private SystemProperties() {
    }
}

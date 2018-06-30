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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * System properties utilities.
 *
 * @author pron
 */
public final class SystemProperties {
    private static String PROPERTY_FILE_PATH = "/quasar.properties";
    private static String PROPERTY_NAME = "co.paralleluniverse.common.util.SystemProperties.name";

    static final Properties prop;
    static {
        URL resource = SystemProperties.class.getResource(PROPERTY_FILE_PATH);
        if (resource != null) {
            prop = new Properties();
            try (InputStream in = resource.openStream()){
                prop.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            prop = null;
        }
    }

    /**
     * Returns the value of a system property which defaults to false.
     *
     * @param property the name of the system property
     * @return {@code true} iff the given property is defined and has the value {@code "true"} or the empty string.
     */
    public static boolean isEmptyOrTrue(String property) {
        final String value = getLocalProperty(property);
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
        final String value = getLocalProperty(property);
        if (value == null)
            return true;
        if (value.isEmpty())
            return true;
        return value.isEmpty() || Boolean.parseBoolean(value);
    }

    private SystemProperties() {
    }

    public static String getLocalProperty(String key) {
        String value = null;
        if (prop != null) {
            value = prop.getProperty(key);
        }
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }

    public static String prefixWithName(String value) {
        String name = getLocalProperty(PROPERTY_NAME);
        if (name != null) {
            return name + "-" + value;
        } else {
            return value;
        }
    }
}

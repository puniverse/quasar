/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

/**
 *
 * @author pron
 */
public class GenericRecordingDouble {
    protected final Object clazz;
    protected final int hashCode;

    public GenericRecordingDouble(Class clazz, int hashCode) {
        this.clazz = clazz;
        this.hashCode = hashCode;
    }

    public GenericRecordingDouble(String clazz, int hashCode) {
        this.clazz = clazz;
        this.hashCode = hashCode;
    }

    public GenericRecordingDouble(Object object, String name) {
        if (object == null) {
            this.clazz = null;
            this.hashCode = -1;
        } else {
            this.clazz = name;
            this.hashCode = System.identityHashCode(object);
        }
    }

    public GenericRecordingDouble(Object object) {
        if (object == null) {
            this.clazz = null;
            this.hashCode = -1;
        } else {
            this.clazz = object.getClass();
            this.hashCode = System.identityHashCode(object);
        }
    }

    @Override
    public String toString() {
        if (clazz == null)
            return "null";
        final String name = (clazz instanceof Class ? ((Class)clazz).getSimpleName() : (String)clazz);
        return name + "@" + Integer.toHexString(hashCode);
    }
}

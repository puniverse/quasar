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
package co.paralleluniverse.actors;

/**
 *
 * @author pron
 */
public class LifecycleException extends RuntimeException {
    private final LifecycleMessage message;

    public LifecycleException(LifecycleMessage message) {
        this.message = message;
    }

    public LifecycleMessage message() {
        return message;
    }
    
    @Override    
    public String toString() {
        String s = getClass().getName();
        return (message != null) ? (s + ": " + message) : s;
    }
}

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
 * Loads actor classes
 * @author pron
 */
class ActorLoader {
    private static final ClassValue<InstanceUpgrader> instanceUpgrader = new ClassValue<InstanceUpgrader>() {

        @Override
        protected InstanceUpgrader computeValue(Class<?> type) {
            return new InstanceUpgrader(type);
        }
    };
    
    public static <T> Class<T> currentClassFor(Class<T> clazz) {
        return clazz;
    }

    public static <T> Class<T> currentClassFor(String className) {
        return null;
    }
    
    public static <T extends Actor<?, ?>> T getReplacementFor(T actor) {
        return actor;
    }
}

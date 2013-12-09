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

import com.google.common.collect.Lists;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import jsr166e.ConcurrentHashMapV8;

/**
 * Loads actor classes
 *
 * @author pron
 */
class ActorLoader extends ClassLoader {
    private static ActorLoader instance = new ActorLoader();
    
    private List<ModuleClassLoader> modules = new ArrayList<>();
    private final ConcurrentMap<String, ModuleClassLoader> classLoaders = new ConcurrentHashMapV8<String, ModuleClassLoader>();
    private final ClassValue<InstanceUpgrader> instanceUpgrader = new ClassValue<InstanceUpgrader>() {
        @Override
        protected InstanceUpgrader computeValue(Class<?> type) {
            return new InstanceUpgrader(type);
        }
    };

    public synchronized void loadModule(URL jarUrl) {
        ModuleClassLoader module = new ModuleClassLoader(jarUrl, this);
        modules.add(module);
        for(String className : module.getUpgradeClasses())
            classLoaders.put(className, module);
    }

    public static <T> Class<T> currentClassFor(Class<T> clazz) {
        return clazz;
    }

    public static <T> Class<T> currentClassFor(String className) {
        return null;
    }

    public static <T extends Actor<?, ?>> T getReplacementFor(T actor) {
        return actor;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ModuleClassLoader mcl : Lists.reverse(modules)) {
            if (mcl.getUpgradeClasses().contains(name)) {
                try {
                    return mcl.findClass(name);
                } catch (ClassNotFoundException e) {
                }
            }
        }
        return super.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }
}

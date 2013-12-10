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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads actor classes
 *
 * @author pron
 */
class ActorLoader extends ClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private static final ClassValue<AtomicInteger> classVersion = new ClassValue<AtomicInteger>() {
        @Override
        protected AtomicInteger computeValue(Class<?> type) {
            return new AtomicInteger(1);
        }
    };

    public static boolean isUpgraded(Class<?> clazz, int version) {
        return classVersion.get(clazz).get() > version;
    }
    
    public static <T extends Actor<?, ?>> Class<T> currentClassFor(Class<T> clazz) {
        return clazz;
    }

    public static <T extends Actor<?, ?>> Class<T> currentClassFor(String className) {
        return null;
    }

    public static <T extends Actor<?, ?>> T getReplacementFor(T actor) {
        return actor;
    }
    private static final Logger LOG = LoggerFactory.getLogger(ActorLoader.class);
    private static ActorLoader instance = new ActorLoader();
    //
    private final ThreadLocal<Boolean> recursive = new ThreadLocal<Boolean>();
    private List<ModuleClassLoader> modules = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<String, ModuleClassLoader> upgradedClasses = new ConcurrentHashMapV8<String, ModuleClassLoader>();
    private final ClassValue<InstanceUpgrader> instanceUpgrader = new ClassValue<InstanceUpgrader>() {
        @Override
        protected InstanceUpgrader computeValue(Class<?> type) {
            return new InstanceUpgrader(type);
        }
    };

    private ModuleClassLoader getModule(URL url) {
        for (ModuleClassLoader m : modules) {
            if (m.getURL().equals(url))
                return m;
        }
        return null;
    }

    private Map<String, Class<?>> checkModule(ModuleClassLoader module) {
        Map<String, Class<?>> oldClasses = new HashMap<>();
        try {
            for (String className : module.getUpgradeClasses()) {
                Class<?> newClass = null;
                try {
                    newClass = module.findClassInModule(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Upgraded class " + className + " is not found in module " + module);
                }
                if (!Actor.class.isAssignableFrom(newClass))
                    throw new RuntimeException("Upgraded class " + className + " in module " + module + " is not an actor");
                Class<?> oldClass = null;
                try {
                    oldClass = this.loadClass(className);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Upgraded class " + className + " does not upgrade an existing class");
                }
                if (!Actor.class.isAssignableFrom(oldClass))
                    throw new RuntimeException("Upgraded class " + className + " in module " + module + " does not upgrade an actor");
                oldClasses.put(className, oldClass);
            }
            return oldClasses;
        } catch (Exception e) {
            LOG.error("Error while loading module " + module, e);
            throw e;
        }
    }

    public synchronized void loadModule(URL jarUrl) {
        if (getModule(jarUrl) != null) {
            LOG.warn("loadModule: module {} already loaded.", jarUrl);
            return;
        }

        LOG.info("loadModule: Loading module {}.", jarUrl);
        ModuleClassLoader module = new ModuleClassLoader(jarUrl, this);
        Map<String, Class<?>> oldClasses = checkModule(module);
        modules.add(module);

        for (String className : module.getUpgradeClasses()) {
            Class<? extends Actor> oldClazz, newClazz;
            try {
                newClazz = (Class<? extends Actor>) module.loadClass(className);
                oldClazz = (Class<? extends Actor>) oldClasses.get(className);
            } catch (ClassNotFoundException e) {
                throw new AssertionError();
            }
            upgradedClasses.put(className, module);
            classVersion.get(oldClazz).incrementAndGet();
        }
    }

    public synchronized void removeModule(URL jarUrl) {
        ModuleClassLoader module = getModule(jarUrl);
        if (module == null) {
            LOG.warn("removeModule: module {} not loaded.", jarUrl);
            return;
        }
        
        LOG.info("loadModule: Removing module {}.", jarUrl);
        
        modules.remove(module);
        
        ConcurrentMap<String, ModuleClassLoader> ucs = new ConcurrentHashMapV8<String, ModuleClassLoader>();
        for (ModuleClassLoader m : modules) {
            for (String className : module.getUpgradeClasses())
                ucs.put(className, m);
        }
        //this.upgradedClasses = ucs;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (recursive.get() == Boolean.TRUE)
            throw new ClassNotFoundException(name);
        recursive.set(Boolean.TRUE);
        try {
            for (ModuleClassLoader mcl : Lists.reverse(modules)) {
                if (mcl.getUpgradeClasses().contains(name)) {
                    try {
                        return mcl.findClassInModule(name);
                    } catch (ClassNotFoundException e) {
                    }
                }
            }
        } finally {
            recursive.remove();
        }
        return super.findClass(name);
    }

    @Override
    public URL getResource(String name) {
        if (recursive.get() == Boolean.TRUE)
            return null;

        recursive.set(Boolean.TRUE);
        try {
            for (ModuleClassLoader mcl : Lists.reverse(modules)) {
                URL resource = mcl.getResource(name);
                if (resource != null)
                    return resource;
            }
        } finally {
            recursive.remove();
        }
        return super.getResource(name);
    }
}

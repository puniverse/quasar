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

import co.paralleluniverse.common.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import jsr166e.ConcurrentHashMapV8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads actor classes for hot code-swapping.
 *
 * @author pron
 */
class ActorLoader extends ClassLoader implements ActorLoaderMXBean, NotificationEmitter {
    public static final String MODULE_DIR_PROPERTY = "co.paralleluniverse.actors.moduleDir";
    private static final String DEFAULT_MODULE_DIR = "modules";
    private static final Path moduleDir;
    private static final Logger LOG = LoggerFactory.getLogger(ActorLoader.class);
    private static final ActorLoader instance;

    static {
        ClassLoader.registerAsParallelCapable();

        instance = new ActorLoader("co.parallelunierse:product=Quasar,name=ActorLoader");

        String moduleDirName = System.getProperty(MODULE_DIR_PROPERTY);
        if (moduleDirName != null) {
            Path mdir = Paths.get(moduleDirName);
            try {
                mdir = mdir.toAbsolutePath();
                Files.createDirectories(mdir);
                mdir = mdir.toRealPath();
            } catch (IOException e) {
                LOG.error("ActorLoader: Error findong/creating module directory " + mdir, e);
                mdir = null;
            }
            moduleDir = mdir;

            loadModulesInModuleDir(instance, moduleDir);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    monitorFilesystem(instance, moduleDir);
                }
            }, "actor-loader-filesystem-monitor");
            t.setDaemon(true);
            t.start();
        } else
            moduleDir = null;
    }
    private static final ClassValue<AtomicInteger> classVersion = new ClassValue<AtomicInteger>() {
        @Override
        protected AtomicInteger computeValue(Class<?> type) {
            return new AtomicInteger(0);
        }
    };

    public static int getClassVersion(Class<?> clazz) {
        return classVersion.get(clazz).get();
    }

    public static boolean isUpgraded(Class<?> clazz, int version) {
        return getClassVersion(clazz) > version;
    }

    public static <T extends Actor<?, ?>> Class<T> currentClassFor(Class<T> clazz) {
        return instance.currentClassFor0(clazz);
    }

    public static <T extends Actor<?, ?>> Class<T> currentClassFor(String className) {
        return instance.currentClassFor0(className);
    }

    public static <T extends Actor<?, ?>> T getReplacementFor(T actor) {
        return instance.getReplacementFor0(actor);
    }
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
    private final NotificationBroadcasterSupport notificationBroadcaster;
    private int notificationSequenceNumber;

    private ActorLoader(String mbeanName) {
        MBeanNotificationInfo info = new MBeanNotificationInfo(
                new String[]{ModuleNotification.NAME},
                ModuleNotification.class.getName(),
                "Actor module change");
        this.notificationBroadcaster = new NotificationBroadcasterSupport(info);
        registerMBean(mbeanName);
    }

    private void registerMBean(String mbeanName) {
        try {
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            final ObjectName mxbeanName = new ObjectName(mbeanName);
            mbs.registerMBean(this, mxbeanName);
        } catch (InstanceAlreadyExistsException ex) {
            throw new RuntimeException(ex);
        } catch (MBeanRegistrationException ex) {
            LOG.error("ActorLoader: exception while registering MBean " + mbeanName, ex);
        } catch (NotCompliantMBeanException ex) {
            throw new AssertionError(ex);
        } catch (MalformedObjectNameException ex) {
            throw new AssertionError(ex);
        }
    }

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
            LOG.error("ActorLoader: Error while loading module " + module, e);
            throw e;
        }
    }

    @Override
    public List<String> getLoadedModules() {
        return Lists.transform(modules, new Function<ModuleClassLoader, String>() {
            @Override
            public String apply(ModuleClassLoader module) {
                return module.getURL().toString();
            }
        });
    }

    @Override
    public synchronized void reloadModule(String jarURL) {
        try {
            reloadModule(new URL(jarURL));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public synchronized void loadModule(String jarURL) {
        try {
            loadModule(new URL(jarURL));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public synchronized void unloadModule(String jarURL) {
        try {
            unloadModule(new URL(jarURL));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public synchronized void reloadModule(URL jarURL) {
        ModuleClassLoader oldModule = getModule(jarURL);

        LOG.info("ActorLoader: {} module {}.", oldModule == null ? "Loading" : "Reloading", jarURL);
        ModuleClassLoader module = new ModuleClassLoader(jarURL, this);
        addModule(module);
        if (oldModule != null)
            removeModule(oldModule);
        LOG.info("ActorLoader: Module {} {}.", jarURL, oldModule == null ? "loaded" : "reloaded");
        notify(module, oldModule == null ? "loaded" : "reloaded");
    }

    public synchronized void loadModule(URL jarURL) {
        if (getModule(jarURL) != null) {
            LOG.warn("ActorLoader loadModule: module {} already loaded.", jarURL);
            return;
        }
        LOG.info("ActorLoader: Loading module {}.", jarURL);
        ModuleClassLoader module = new ModuleClassLoader(jarURL, this);
        addModule(module);
        LOG.info("ActorLoader: Module {} loaded.", jarURL);
        notify(module, "loaded");
    }

    public synchronized void unloadModule(URL jarURL) {
        ModuleClassLoader module = getModule(jarURL);
        if (module == null) {
            LOG.warn("ActorLoader removeModule: module {} not loaded.", jarURL);
            return;
        }

        LOG.info("ActorLoader: Removing module {}.", jarURL);
        removeModule(module);
        LOG.info("ActorLoader: Module {} removed.", jarURL);
        notify(module, "removed");
    }

    private synchronized void addModule(ModuleClassLoader module) {
        Map<String, Class<?>> oldClasses = checkModule(module);
        modules.add(module);

        for (String className : module.getUpgradeClasses()) {
            Class<? extends Actor> oldClass, newClass;
            try {
                newClass = (Class<? extends Actor>) module.loadClass(className);
                oldClass = (Class<? extends Actor>) oldClasses.get(className);
            } catch (ClassNotFoundException e) {
                throw new AssertionError();
            }
            ModuleClassLoader oldModule = oldClass.getClassLoader() instanceof ModuleClassLoader ? (ModuleClassLoader) oldClass.getClassLoader() : null;
            LOG.info("ActorLoader: Upgrading class {} of module {} to that in module {}", className, oldModule, module);
            upgradedClasses.put(className, module);
            classVersion.get(oldClass).incrementAndGet();
        }
    }

    private synchronized void removeModule(ModuleClassLoader module) {
        modules.remove(module);

        for (String className : module.getUpgradeClasses()) {
            if (upgradedClasses.get(className) == module) {
                Class oldClass = module.findLoadedClassInModule(className);
                ModuleClassLoader newModule = null;
                for (ModuleClassLoader mcl : Lists.reverse(modules)) {
                    if (mcl.getUpgradeClasses().contains(className)) {
                        newModule = mcl;
                        break;
                    }
                }
                LOG.info("ActorLoader: Downgrading class {} of module {} to that in module {}", className, newModule);
                if (newModule != null)
                    upgradedClasses.put(className, newModule);
                else
                    upgradedClasses.remove(className);
                if (oldClass != null)
                    classVersion.get(oldClass).incrementAndGet();
            }
        }
    }

    <T extends Actor<?, ?>> Class<T> currentClassFor0(Class<T> clazz) {
        try {
            final String className = clazz.getName();
            ModuleClassLoader module = instance.upgradedClasses.get(className);
            if (module != null)
                clazz = (Class<T>) module.loadClass(className);
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    <T extends Actor<?, ?>> Class<T> currentClassFor0(String className) {
        try {
            ModuleClassLoader module = instance.upgradedClasses.get(className);
            Class<?> clazz;
            if (module == null)
                clazz = Class.forName(className);
            else
                clazz = module.loadClass(className);
            return (Class<T>) clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    <T extends Actor<?, ?>> T getReplacementFor0(T actor) {
        Class<T> clazz = (Class<T>) actor.getClass();
        Class<? extends T> newClazz = currentClassFor0(clazz);
        if (newClazz == clazz)
            return actor;
        return (T) instanceUpgrader.get(newClazz).copy(actor);
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

    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return notificationBroadcaster.getNotificationInfo();
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        notificationBroadcaster.addNotificationListener(listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        notificationBroadcaster.removeNotificationListener(listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        notificationBroadcaster.removeNotificationListener(listener);
    }

    private synchronized void notify(ModuleClassLoader module, String action) {
        final Notification n = new ModuleNotification(this, notificationSequenceNumber++, System.currentTimeMillis(),
                "Module " + module + " has been " + action);
        notificationBroadcaster.sendNotification(n);
    }

    private static class ModuleNotification extends Notification {
        static final String NAME = "co.paralleluniverse.actors.module";

        public ModuleNotification(String type, Object source, long sequenceNumber, String message) {
            super(NAME, source, sequenceNumber, message);
        }

        public ModuleNotification(Object source, long sequenceNumber, long timeStamp, String message) {
            super(NAME, source, sequenceNumber, timeStamp, message);
        }
    }

    private static void loadModulesInModuleDir(ActorLoader instance, Path moduleDir) {
        LOG.info("ActorLoader: scanning module directory " + moduleDir + " for modules.");
        try (DirectoryStream<Path> children = Files.newDirectoryStream(moduleDir)) {
            for (Path child : children) {
                if (isValidFile(child)) {
                    try {
                        final URL jarUrl = child.toUri().toURL();
                        instance.reloadModule(jarUrl);
                    } catch (Exception e) {
                        LOG.error("ActorLoader: exception while processing " + child, e);
                    }
                } else {
                    LOG.warn("ActorLoader: A non-jar item " + child + " found in the modules directory " + moduleDir);
                }
            }
        } catch (Exception e) {
            LOG.error("ActorLoader: exception while loading modules in module directory " + moduleDir, e);
        }
    }

    private static void monitorFilesystem(ActorLoader instance, Path moduleDir) {
        try (WatchService watcher = FileSystems.getDefault().newWatchService();) {
            moduleDir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            LOG.info("ActorLoader watching module directory " + moduleDir + " for changes.");
            for (;;) {
                final WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    final WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) { // An OVERFLOW event can occur regardless of registration if events are lost or discarded.
                        LOG.warn("ActorLoader filesystem monitor: filesystem events may have been missed");
                        continue;
                    }

                    final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    final Path filename = ev.context(); // The filename is the context of the event.
                    final Path child = moduleDir.resolve(filename); // Resolve the filename against the directory.
                    if (isValidFile(child)) {
                        try {
                            final URL jarUrl = child.toUri().toURL();

                            LOG.info("ActorLoader filesystem monitor: detected module file {} {}", child,
                                    kind == ENTRY_CREATE ? "created"
                                    : kind == ENTRY_MODIFY ? "modified"
                                    : kind == ENTRY_DELETE ? "deleted"
                                    : null);

                            if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY)
                                instance.reloadModule(jarUrl);
                            else if (kind == ENTRY_DELETE)
                                instance.unloadModule(jarUrl);
                        } catch (Exception e) {
                            LOG.error("ActorLoader filesystem monitor: exception while processing " + child, e);
                        }
                    } else {
                        if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY)
                            LOG.warn("ActorLoader filesystem monitor: A non-jar item " + child + " has been placed in the modules directory " + moduleDir);
                    }
                }
                if (!key.reset())
                    throw new IOException("Directory " + moduleDir + " is no longer accessible");
            }
        } catch (Exception e) {
            LOG.error("ActorLoader filesystem monitor thread terminated with an exception", e);
            throw Exceptions.rethrow(e);
        }
    }

    private static boolean isValidFile(Path file) {
        return Files.isRegularFile(file) && file.getFileName().endsWith(".jar");
    }
}

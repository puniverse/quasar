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

import co.paralleluniverse.common.reflection.ASMUtil;
import co.paralleluniverse.common.reflection.AnnotationUtil;
import co.paralleluniverse.common.reflection.ClassLoaderUtil;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.classToResource;
import static co.paralleluniverse.common.reflection.ClassLoaderUtil.isClassFile;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module of actor code-upgrades contained in a jar file.
 *
 * @author pron
 */
class ActorModule extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private static final Logger LOG = LoggerFactory.getLogger(ActorModule.class);
    private static final String UPGRADE_CLASSES_ATTR = "Upgrade-Classes";
    private final URL url;
    private final ClassLoader parent;
    private final Set<String> upgradeClasses;

    public ActorModule(URL jarUrl, ActorLoader parent) {
        super(new URL[]{jarUrl}, null);
        this.url = jarUrl;
        this.parent = parent;
        
        // determine upgrade classes
        try {
            JarFile jar = new JarFile(new File(jarUrl.toURI()));
            final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String ucstr = attributes.getValue(UPGRADE_CLASSES_ATTR);
            if (ucstr != null && !ucstr.trim().isEmpty()) {
                if (ucstr.trim().equals("*")) {
                    ClassLoaderUtil.accept(this, new ClassLoaderUtil.Visitor() {
                        @Override
                        public void visit(String resource, URL url, ClassLoader cl) {
                            if (!isClassFile(resource))
                                return;
                            final String className = ClassLoaderUtil.resourceToClass(resource);
//                            System.out.println("className: " + className + " "
//                                    + ASMUtil.isAssignableFrom(Actor.class, className, ActorModule.this) + " "
//                                    + " " + url + " " + ActorModule.this.parent.getResource(resource) + " "
//                                    + equalContent(ActorModule.this.parent.getResource(resource), url));                  
                            if (ASMUtil.isAssignableFrom(Actor.class, className, ActorModule.this)
                                    && !equalContent(ActorModule.this.parent.getResource(resource), url))
                                builder.add(className);
                        }
                    });
                } else {
                    for (String className : ucstr.split("\\s"))
                        builder.add(className);
                }
            }

            ClassLoaderUtil.accept(this, new ClassLoaderUtil.Visitor() {
                @Override
                public void visit(String resource, URL url, ClassLoader cl) {
                    if (!isClassFile(resource))
                        return;
                    final String className = ClassLoaderUtil.resourceToClass(resource);
                    try (InputStream is = cl.getResourceAsStream(resource)) {
                        if (AnnotationUtil.hasClassAnnotation(Upgrade.class, is))
                            builder.add(className);
                    } catch (IOException e) {
                        throw new RuntimeException("Exception while scanning class " + className + " for Upgrade annotation", e);
                    }
                }
            });

            this.upgradeClasses = builder.build();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }        
    }

    public URL getURL() {
        return url;
    }

    public Set<String> getUpgradeClasses() {
        return upgradeClasses;
    }

    public Class<?> loadClassInModule(String name) throws ClassNotFoundException {
        Class<?> loaded = super.findLoadedClass(name);
        if (loaded != null)
            return loaded;
        return super.loadClass(name); // first try to use the URLClassLoader findClass
    }

    public Class<?> findLoadedClassInModule(String name) {
        return super.findLoadedClass(name);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loaded = super.findLoadedClass(name);
        if (loaded != null)
            return loaded;

        LOG.debug("findClass {} in module {}", name, this);
        boolean isUpgraded = upgradeClasses.contains(name);
        if (!isUpgraded && parent != null) {
            try {
                String resourceName = classToResource(name);
                URL parentUrl = parent.getResource(resourceName);
                if (parentUrl != null) {
                    URL myUrl = super.getResource(resourceName);
                    if (myUrl == null || equalContent(parentUrl, myUrl)) {
                        // NOTE: classes may differ only in their Java source line information; considered a difference
                        if (myUrl != null)
                            LOG.debug("Class {} in module {} is identical to that in main module", name, this);
                        else
                            LOG.debug("findClass {} in module {} - not found in module", name, this);
                        return parent.loadClass(name);
                    }
                }
            } catch (ClassNotFoundException e) {
            }
        }

        try {
            Class<?> clazz = super.findClass(name); // first try to use the URLClassLoader findClass
            LOG.info("{} loaded {} class {}", this, isUpgraded ? "upgraded" : "internal", name);
            return clazz;
        } catch (ClassNotFoundException e) {
            if (parent != null)
                return parent.loadClass(name);
            throw e;
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url != null)
            LOG.info("{} - getResource {}: {}", this, name, url);
        if (url == null && parent != null)
            url = parent.getResource(name);
        return url;
    }

    @Override
    public String toString() {
        return "ActorModule{" + "url=" + url + '}';
    }

    private static boolean equalContent(URL url1, URL url2) {
        try {
            return ByteStreams.equal(Resources.asByteSource(url1), Resources.asByteSource(url2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

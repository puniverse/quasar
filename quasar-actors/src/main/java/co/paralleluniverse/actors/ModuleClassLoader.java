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

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author pron
 */
class ModuleClassLoader extends URLClassLoader {
    private static final String UPGRADE_CLASSES_ATTR = "Upgrade-Classes";
    private final ClassLoader parent;
    private final Set<String> upgradeClasses;

    public ModuleClassLoader(URL jarUrl, ClassLoader parent) {
        super(new URL[]{jarUrl}, null);
        this.parent = parent;

        try {
            JarFile jar = new JarFile(new File(jarUrl.toURI()));
            Manifest manifest = jar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String ucstr = attributes.getValue(UPGRADE_CLASSES_ATTR);
            if (ucstr == null)
                throw new IllegalArgumentException("Module jar " + jarUrl + " does not contain a " + UPGRADE_CLASSES_ATTR + " attribute");
            if (ucstr.isEmpty())
                throw new IllegalArgumentException("Module jar " + jarUrl + " " + UPGRADE_CLASSES_ATTR + " attribute does not contain entries");

            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            for (String className : ucstr.split("\\s"))
                builder.add(className);
            this.upgradeClasses = builder.build();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getUpgradeClasses() {
        return upgradeClasses;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loaded = super.findLoadedClass(name);
        if (loaded != null)
            return loaded;
        try {
            return super.findClass(name); // first try to use the URLClassLoader findClass
        } catch (ClassNotFoundException e) {
            if (parent != null)
                return parent.loadClass(name);
            throw e;
        }
    }

    @Override
    public URL getResource(String name) {
        URL url = super.getResource(name);
        if (url == null && parent != null)
            url = parent.getResource(name);
        return url;
    }
}

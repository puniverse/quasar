/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2018, Parallel Universe Software Co. All rights reserved.
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
/*
 * Based on Guava's com.google.common.reflect.ClassPath
 */
/*
 * Copyright (c) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.paralleluniverse.common.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author pron
 */
public final class ClassLoaderUtil {
    public interface Visitor {
        void visit(String resource, URL url, ClassLoader cl) throws IOException;
    }

    private static final String CLASS_FILE_NAME_EXTENSION = ".class";

    public static boolean isClassFile(String resourceName) {
        return resourceName.endsWith(CLASS_FILE_NAME_EXTENSION) && !resourceName.endsWith("module-info.class");
    }

    public static String classToResource(String className) {
        if (className == null)
            return null;
        return className.replace('.', '/') + CLASS_FILE_NAME_EXTENSION;
    }

    public static String classToResource(Class<?> clazz) {
        if (clazz == null)
            return null;
        return classToResource(clazz.getName());
    }

    public static String classToSlashed(String className) {
        if (className == null)
            return null;
        return className.replace('.', '/');
    }

    public static String classToSlashed(Class<?> clazz) {
        if (clazz == null)
            return null;
        return classToSlashed(clazz.getName());
    }

    public static String resourceToClass(String resourceName) {
        if (resourceName == null)
            return null;
        return resourceToSlashed(resourceName).replace('/', '.');
    }

    public static String resourceToSlashed(String resourceName) {
        if (resourceName == null)
            return null;
        if (!resourceName.endsWith(CLASS_FILE_NAME_EXTENSION))
            throw new IllegalArgumentException("Resource " + resourceName + " is not a class file");
        return resourceName.substring(0, resourceName.length() - CLASS_FILE_NAME_EXTENSION.length());
    }

    public static void accept(URLClassLoader ucl, Visitor visitor) throws IOException {
        accept(ucl, ucl.getURLs(), visitor);
    }

    public static void accept(ClassLoader cl, URL[] urls, Visitor visitor) throws IOException {
        try {
            final Set<URI> scannedUris = new HashSet<>();
            for (URL entry : urls) {
                URI uri = entry.toURI();
                if (uri.getScheme().equals("file") && scannedUris.add(uri))
                    scanFrom(new File(uri), cl, scannedUris, visitor);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static void scan(URI uri, ClassLoader classloader, Set<URI> scannedUris, Visitor visitor) throws IOException {
        if (uri.getScheme().equals("file") && scannedUris.add(uri))
            scanFrom(new File(uri), classloader, scannedUris, visitor);
    }

    private static void scanFrom(File file, ClassLoader classloader, Set<URI> scannedUris, Visitor visitor) throws IOException {
        if (!file.exists())
            return;

        if (file.isDirectory())
            scanDirectory(file, classloader, visitor);
        else
            scanJar(file, classloader, scannedUris, visitor);
    }

    private static void scanDirectory(File directory, ClassLoader classloader, Visitor visitor) throws IOException {
        scanDirectory(directory, classloader, "", new HashSet<>(), visitor);
    }

    private static void scanDirectory(File directory, ClassLoader classloader, String packagePrefix, Set<File> ancestors, Visitor visitor) throws IOException {
        File canonical = directory.getCanonicalFile();
        if (ancestors.contains(canonical)) {
            // A cycle in the filesystem, for example due to a symbolic link.
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            // logger.warning("Cannot read directory " + directory);
            // IO error, just skip the directory
            return;
        }
        Set<File> newAncestors = new HashSet<>(ancestors);
        newAncestors.add(canonical);
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                scanDirectory(f, classloader, packagePrefix + name + '/', newAncestors, visitor);
            } else {
                String resourceName = packagePrefix + name;
                if (!resourceName.equals(JarFile.MANIFEST_NAME))
                    visitor.visit(resourceName, f.toURI().toURL(), classloader);
            }
        }
    }

    private static void scanJar(File file, ClassLoader classloader, Set<URI> scannedUris, Visitor visitor) throws IOException {
        JarFile jarFile;
        try {
            jarFile = new JarFile(file);
        } catch (IOException e) {
            // Not a jar file
            return;
        }
        try {
            for (URI uri : getClassPathFromManifest(file, jarFile.getManifest()))
                scan(uri, classloader, scannedUris, visitor);

            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME))
                    continue;
                try {
                    visitor.visit(entry.getName(), new URL("jar:file:" + file.getCanonicalPath() + "!/" + entry.getName()), classloader);
                } catch (IOException e) {
                    throw new IOException("Exception thrown during scanning of jar file " + file + " entry " + entry, e);
                }  catch (RuntimeException e) {
                    throw new RuntimeException("Exception thrown during scanning of jar file " + file + " entry " + entry, e);
                }
            }
        } finally {
            try {
                jarFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static URL getResource(ClassLoader cl, String resource) {
        return cl != null ? cl.getResource(resource) : ClassLoader.getSystemResource(resource);
    }
    
    public static Enumeration<URL> getResources(ClassLoader cl, String resources) throws IOException {
        return cl != null ? cl.getResources(resources) : ClassLoader.getSystemResources(resources);
    }
    
    public static InputStream getResourceAsStream(ClassLoader cl, String resource) throws IOException {
        URL url = getResource(cl, resource);
        if (url == null)
            return null;
        URLConnection uc = url.openConnection();
        uc.setUseCaches(false);
        return uc.getInputStream();
    }
    
    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. If {@code manifest} is null, it means the jar file has no
     * manifest, and an empty set will be returned.
     */
    private static Set<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
        if (manifest == null)
            return new HashSet<>();

        HashSet<URI> s = new HashSet<>();
        String classpathAttribute = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
        if (classpathAttribute != null) {
            for (String path : classpathAttribute.split("\\s")) {
                if (path != null && path.trim().length() > 0) {
                    URI uri;
                    try {
                        uri = getClassPathEntry(jarFile, path.trim());
                    } catch (URISyntaxException e) {
                        // Ignore bad entry
                        // logger.warning("Invalid Class-Path entry: " + path);
                        continue;
                    }
                    s.add(uri);
                }
            }
        }
        return s;
    }

    /**
     * Returns the absolute uri of the Class-Path entry value as specified in
     * <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. Even though the specification only talks about relative urls,
     * absolute urls are actually supported too (for example, in Maven surefire plugin).
     */
    private static URI getClassPathEntry(File jarFile, String path) throws URISyntaxException {
        URI uri = new URI(path);
        if (uri.isAbsolute())
            return uri;
        else
            return new File(jarFile.getParentFile(), path.replace('/', File.separatorChar)).toURI();
    }

    private ClassLoaderUtil() {
    }
}

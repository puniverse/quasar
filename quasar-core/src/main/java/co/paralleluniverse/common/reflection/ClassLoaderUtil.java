/*
 * Quasar: lightweight threads and actors for the JVM.
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
package co.paralleluniverse.common.reflection;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
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
        void visit(String resource, URL url, ClassLoader cl);
    }

    public enum ScanMode {
        WHOLE_CLASSPATH, WITHOUT_JARS
    }

    private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(" ").omitEmptyStrings();
    private static final String CLASS_FILE_NAME_EXTENSION = ".class";

    public static boolean isClassFile(String resourceName) {
        return resourceName.endsWith(CLASS_FILE_NAME_EXTENSION);
    }

    public static String classToResource(String className) {
        return className.replace('.', '/') + CLASS_FILE_NAME_EXTENSION;
    }

    public static String classToResource(Class<?> clazz) {
        return classToResource(clazz.getName());
    }

    public static String classToSlashed(String className) {
        return className.replace('.', '/');
    }

    public static String classToSlashed(Class<?> clazz) {
        return classToSlashed(clazz.getName());
    }

    public static String resourceToClass(String resourceName) {
        return resourceToSlashed(resourceName).replace('/', '.');
    }

    public static String resourceToSlashed(String resourceName) {
        if (!resourceName.endsWith(CLASS_FILE_NAME_EXTENSION))
            throw new IllegalArgumentException("Resource " + resourceName + " is not a class file");
        return resourceName.substring(0, resourceName.length() - CLASS_FILE_NAME_EXTENSION.length());
    }

    public static void accept(URLClassLoader ucl, Visitor visitor) throws IOException {
        accept(ucl, ScanMode.WHOLE_CLASSPATH, visitor);
    }

    public static void accept(URLClassLoader ucl, ScanMode scanMode, Visitor visitor) throws IOException {
        accept(ucl, ucl.getURLs(), scanMode, visitor);
    }

    public static void accept(ClassLoader cl, URL[] urls, ScanMode scanMode, Visitor visitor) throws IOException {
        try {
            final Set<URI> scannedUris = new HashSet<>();
            for (URL entry : urls) {
                URI uri = entry.toURI();
                if (uri.getScheme().equals("file") && scannedUris.add(uri)) {
                    final File path = new File(uri);
                    if (scanMode != ScanMode.WITHOUT_JARS || path.isDirectory())
                        scanFrom(path, cl, scannedUris, visitor);
                }
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
        scanDirectory(directory, classloader, "", ImmutableSet.<File>of(), visitor);
    }

    private static void scanDirectory(File directory, ClassLoader classloader, String packagePrefix, ImmutableSet<File> ancestors, Visitor visitor) throws IOException {
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
        ImmutableSet<File> newAncestors = ImmutableSet.<File>builder().addAll(ancestors).add(canonical).build();
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                scanDirectory(f, classloader, packagePrefix + name + "/", newAncestors, visitor);
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
                visitor.visit(entry.getName(), new URL("jar:file:" + file.getCanonicalPath() + "!/" + entry.getName()), classloader);
            }
        } finally {
            try {
                jarFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Returns the class path URIs specified by the {@code Class-Path} manifest attribute, according
     * to <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Main%20Attributes">
     * JAR File Specification</a>. If {@code manifest} is null, it means the jar file has no
     * manifest, and an empty set will be returned.
     */
    private static ImmutableSet<URI> getClassPathFromManifest(File jarFile, Manifest manifest) {
        if (manifest == null)
            return ImmutableSet.of();

        ImmutableSet.Builder<URI> builder = ImmutableSet.builder();
        String classpathAttribute = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
        if (classpathAttribute != null) {
            for (String path : CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute)) {
                URI uri;
                try {
                    uri = getClassPathEntry(jarFile, path);
                } catch (URISyntaxException e) {
                    // Ignore bad entry
                    // logger.warning("Invalid Class-Path entry: " + path);
                    continue;
                }
                builder.add(uri);
            }
        }
        return builder.build();
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

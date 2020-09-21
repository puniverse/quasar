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
package co.paralleluniverse.common.resource;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author pron
 */
public class ClassLoaderUtilTKB {
    public static void main(String[] args) throws Exception {
        File f = new File("build/libs/quasar-core-0.4.0-SNAPSHOT.jar");
        System.out.println(f.exists());
        URLClassLoader cl = new URLClassLoader(new URL[]{f.toURI().toURL()});
        ClassLoaderUtil.accept(cl, new ClassLoaderUtil.Visitor() {

            @Override
            public void visit(String resource, URL url, ClassLoader cl) {
                System.out.println("- " + resource + " " + url);
            }
        });
    }
}

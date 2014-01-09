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
package co.paralleluniverse.actors;

import java.util.List;

/**
 *
 * @author pron
 */
public interface ActorLoaderMXBean {
    /**
     * All currently loaded modules
     */
    List<String> getLoadedModules();

    /**
     * Loads or reloads the module at the given URL
     *
     * @param jarURL the URL of the JAR file containing the module
     */
    void reloadModule(String jarURL);

    /**
     * Unloads the module at the given URL
     *
     * @param jarURL the URL of the JAR file containing the module
     */
    void unloadModule(String jarURL);
}

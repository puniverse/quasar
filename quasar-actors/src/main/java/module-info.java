/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2018, Parallel Universe Software Co. All rights reserved.
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
module co.paralleluniverse.quasar.actors {
    requires java.management;
    requires jdk.unsupported; // needed for sun.reflect.ReflectionFactory in InstanceUpgrader

    requires transitive co.paralleluniverse.quasar.core;
    requires slf4j.api;
    requires static net.bytebuddy;

    exports co.paralleluniverse.actors;
    exports co.paralleluniverse.actors.behaviors;
    exports co.paralleluniverse.actors.spi;
}

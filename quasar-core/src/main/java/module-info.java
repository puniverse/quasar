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
module co.paralleluniverse.quasar.core {
    requires java.management;
    requires java.instrument;
    requires jdk.unsupported; // needed for ThreadAccess and ExtendedStackTraceHotSpot
    
    requires static org.objectweb.asm;
    requires static org.objectweb.asm.util;
    requires static org.objectweb.asm.commons;
    requires static transitive org.objectweb.asm.tree.analysis;
    requires com.google.common;
    requires static kryo;
    requires static kryo.serializers;
    requires static objenesis;
    requires static ant;
    requires static junit;
    requires static HdrHistogram;
    requires static LatencyUtils;
    requires static com.codahale.metrics;
    requires static com.codahale.metrics.jmx;
    requires static osgi.annotation;

    exports co.paralleluniverse.fibers;
    exports co.paralleluniverse.fibers.futures;
    exports co.paralleluniverse.fibers.instrument;
    exports co.paralleluniverse.fibers.io;
    exports co.paralleluniverse.fibers.suspend;
    exports co.paralleluniverse.remote;
    exports co.paralleluniverse.strands;
    exports co.paralleluniverse.strands.channels;
    exports co.paralleluniverse.strands.channels.transfer;
    exports co.paralleluniverse.strands.concurrent;
    exports co.paralleluniverse.strands.dataflow;

    exports co.paralleluniverse.common.util       to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.common.monitoring to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.common.reflection to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.common.resource   to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.common.test       to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.concurrent.util   to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.io.serialization  to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.strands.queues    to co.paralleluniverse.quasar.actors;

    // This is to appease the Java 9 module import.
    // co.paralleluniverse.asm is actually a shadowing of org.objectweb.asm.
    exports co.paralleluniverse.asm to co.paralleluniverse.quasar.actors;
    exports co.paralleluniverse.common.asm to co.paralleluniverse.quasar.actors;

    uses co.paralleluniverse.fibers.instrument.SuspendableClassifier;
}

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
package co.paralleluniverse.remote.galaxy;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author eitan
 */
public class Finalizer {
    private static final Logger LOG = LoggerFactory.getLogger(Finalizer.class);
    private static final Set<PhantomReferenceFinalizer> refs = Collections.newSetFromMap(new ConcurrentHashMap<PhantomReferenceFinalizer, Boolean>());
    private static final ReferenceQueue<Object> q = new ReferenceQueue<>();

    static {
        Thread collector = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        final PhantomReferenceFinalizer ref = (PhantomReferenceFinalizer) q.remove();
                        try {
                            ref.run();
                        } finally {
                            refs.remove(ref);
                            ref.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    LOG.info(this.toString() + " has been interrupted");
                }
            }
        }, "finalizer-collector");
        collector.setDaemon(true);
        collector.start();
    }

    private static class PhantomReferenceFinalizer extends PhantomReference<Object> implements Runnable {
        private final Runnable finalizeFunc;

        public <T> PhantomReferenceFinalizer(T referent, ReferenceQueue<? super T> q, Runnable finilizeFunc) {
            super(referent, (ReferenceQueue) q);
            this.finalizeFunc = finilizeFunc;
        }

        @Override
        public void run() {
            finalizeFunc.run();
        }
    }

    public static void register(Object t, Runnable r) {
        refs.add(new PhantomReferenceFinalizer(t, q, r));
    }
}

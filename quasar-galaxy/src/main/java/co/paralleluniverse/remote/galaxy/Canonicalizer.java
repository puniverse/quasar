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

import co.paralleluniverse.concurrent.util.MapUtil;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author pron
 */
public class Canonicalizer<K, V> {
    private final ConcurrentMap<K, WeakReference<V>> map = MapUtil.newConcurrentHashMap();

    public V get(K key, Callable<V> creator) {
        WeakReference<V> ref = map.get(key);
        V v = null;
        if (ref != null)
            v = ref.get();
        if (v == null) {
            try {
                v = creator.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            WeakReference<V> oref = map.putIfAbsent(key, new WeakReference<>(v));
            if (oref != null) {
                V ov = oref.get();
                if (ov != null)
                    v = ov;
            }
        }
        assert v != null;
        return v;
    }

    public V get(K key, final V v1) {
        return get(key, new Callable<V>() {

            @Override
            public V call() throws Exception {
                return v1;
            }
        });
    }
}

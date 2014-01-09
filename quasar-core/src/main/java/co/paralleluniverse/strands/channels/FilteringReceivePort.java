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
package co.paralleluniverse.strands.channels;

import com.google.common.base.Predicate;

/**
 *
 * @author pron
 */
public class FilteringReceivePort<M> extends TransformingReceivePort<M, M> implements ReceivePort<M> {
    private final Predicate<M> p;

    public FilteringReceivePort(ReceivePort<M> target, Predicate<M> p) {
        super(target);
        this.p = p;
    }

    public FilteringReceivePort(ReceivePort<M> target) {
        this(target, null);
    }

    @Override
    protected M transform(M m) {
        return filter(m) ? m : null;
    }

    protected boolean filter(M m) {
        if (p != null)
            return p.apply(m);
        throw new UnsupportedOperationException();
    }
}

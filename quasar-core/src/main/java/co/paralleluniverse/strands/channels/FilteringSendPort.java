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
class FilteringSendPort<M> extends SendPortTransformer<M, M> implements SendPort<M> {
    private final Predicate<M> p;

    public FilteringSendPort(SendPort<M> target, Predicate<M> p) {
        super(target);
        this.p = p;
    }

    public FilteringSendPort(SendPort<M> target) {
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

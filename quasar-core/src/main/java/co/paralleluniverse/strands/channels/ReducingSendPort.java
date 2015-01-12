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

import co.paralleluniverse.common.util.Function2;

/**
 * A transforming {@link SendPort} that will apply a reduction function to values.
 * <p/>
 * @author circlespainter
 */
class ReducingSendPort<S, T> extends SendPortTransformer<S, T> implements SendPort<S> {
    private final Function2<T, S, T> f;
    private T prev;

    public ReducingSendPort(SendPort<T> target, Function2<T, S, T> f, T init) {
        super(target);
        this.f = f;
        this.prev = init;
    }

    @Override
    protected T transform(S m) {
        return (this.prev = reduce(prev, m));
    }

    protected T reduce(T prev, S m) {
        if (f != null && prev != null)
            return f.apply(prev, m);
        throw new UnsupportedOperationException();
    }
}
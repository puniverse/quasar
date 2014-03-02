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

import com.google.common.base.Function;

/**
 *
 * @author pron
 */
class MappingReceivePort<S, T> extends ReceivePortTransformer<S, T> implements ReceivePort<T> {
    private final Function<S, T> f;

    public MappingReceivePort(ReceivePort<S> target, Function<S, T> f) {
        super(target);
        this.f = f;
    }

    public MappingReceivePort(ReceivePort<S> target) {
        this(target, null);
    }

    @Override
    protected T transform(S m) {
        return map(m);
    }

    protected T map(S m) {
        if (f != null)
            return f.apply(m);
        throw new UnsupportedOperationException();
    }

    @Override
    public TransformingReceivePort<T> transform() {
        return Channels.transform(this);
    }
}

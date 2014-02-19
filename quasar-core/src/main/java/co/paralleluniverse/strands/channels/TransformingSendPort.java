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
import com.google.common.base.Predicate;

/**
 *
 * @author pron
 */
public class TransformingSendPort<T> extends DelegatingSendPort<T> {

    TransformingSendPort(SendPort<T> target) {
        super(target);
    }

    public <S> TransformingSendPort<S> map(Function<S, T> f) {
        return Channels.transformSend(Channels.mapSend(this, f));
    }

    public TransformingSendPort<T> filter(Predicate<T> pred) {
        return Channels.transformSend(Channels.filterSend(this, pred));
    }
}

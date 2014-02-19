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

import co.paralleluniverse.strands.SuspendableAction2;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 * @author pron
 */
public class TransformingReceivePort<T> extends DelegatingReceivePort<T> {

    TransformingReceivePort(ReceivePort<T> target) {
        super(target);
    }

    public <U> TransformingReceivePort<U> map(Function<T, U> f) {
        return Channels.transform(Channels.map(this, f));
    }

    public <U> TransformingReceivePort<U> flatmap(Function<T, ReceivePort<U>> f) {
        return Channels.transform(Channels.flatmap(this, f));
    }

    public TransformingReceivePort<T> filter(Predicate<T> pred) {
        return Channels.transform(Channels.filter(this, pred));
    }

    public <U> TransformingReceivePort<U> fiberTransform(SuspendableAction2<? extends ReceivePort<? super T>, ? extends SendPort<? extends U>> transformer, Channel<U> out) {
        Channels.fiberTransform(this, out, transformer);
        return Channels.transform(out);
    }
}

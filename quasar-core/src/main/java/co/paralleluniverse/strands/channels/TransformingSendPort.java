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
import co.paralleluniverse.fibers.FiberFactory;
import co.paralleluniverse.strands.SuspendableAction2;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * A {@link SendPort} with additional functional-transform operations, usually wrapping a plain {@link SendPort}.
 *
 * @author pron
 */
public class TransformingSendPort<T> extends DelegatingSendPort<T> {

    TransformingSendPort(SendPort<T> target) {
        super(target);
    }

    /**
     * Returns a {@link TransformingSendPort} that filters messages that satisfy a predicate before sending to this channel.
     * Messages that don't satisfy the predicate will be silently discarded when sent.
     * <p/>
     * The returned {@code SendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param pred    the filtering predicate
     * @return A {@link TransformingSendPort} that will send only those messages which satisfy the predicate (i.e. the predicate returns {@code true}) to the given channel.
     */
    public TransformingSendPort<T> filter(Predicate<T> pred) {
        return Channels.transformSend(Channels.filterSend(this, pred));
    }

    /**
     * Returns a {@link TransformingSendPort} that transforms messages by applying a given mapping function before sending this channel.
     * <p/>
     * The returned {@code TransformingSendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param f       the mapping function
     * @return a {@link TransformingSendPort} that passes messages to the given channel after transforming them by applying the mapping function.
     */
    public <S> TransformingSendPort<S> map(Function<S, T> f) {
        return Channels.transformSend(Channels.mapSend(this, f));
    }

    /**
     * Returns a {@link TransformingSendPort} to which sending messages that are transformed towards a channel by a reduction function.
     * <p/>
     * The returned {@code TransformingSendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param f       The reduction function.
     * @param init    The initial input to the reduction function.
     * @return a {@link ReceivePort} that returns messages that are the result of applying the reduction function to the messages received on the given channel.
     */
    public <S> TransformingSendPort<S> reduce(Function2<T, S, T> f, T init) {
        return Channels.transformSend(Channels.reduceSend(this, f, init));
    }

    /**
     * Returns a {@link SendPort} that sends messages that are transformed by a given flat-mapping function into this channel.
     * Unlike {@link #map(Function) map}, the mapping function does not returns a single output message for every input message, but
     * a new {@code ReceivePort}. All the returned ports are concatenated and sent to the channel.
     * <p/>
     * To return a single value the mapping function can make use of {@link Channels#singletonReceivePort(Object)}. To return a collection,
     * it can make use of {@link Channels#toReceivePort(Iterable)}. To emit no values, the function can return {@link Channels#emptyReceivePort()}
     * or {@code null}.
     * <p/>
     * If multiple producers send messages into the channel, the messages from the {@code ReceivePort}s returned by the mapping function
     * may be interleaved with other messages.
     * <p/>
     * The returned {@code SendPort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param pipe    an intermediate channel used in the flat-mapping operation. Messages are first sent to this channel before being transformed.
     * @param f       the mapping function
     * @return a {@link ReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public <S> TransformingSendPort<S> flatMap(Channel<S> pipe, Function<S, ReceivePort<T>> f) {
        return Channels.transformSend(Channels.flatMapSend(pipe, this, f));
    }

    /**
     * Spawns a fiber that transforms values read from the {@code in} channel and writes values to this channel.
     * <p/>
     * When the transformation terminates. the output channel is automatically closed. If the transformation terminates abnormally 
     * (throws an exception), this channel is {@link SendPort#close(Throwable) closed with that exception}.
     * 
     * @param in          the input channel
     * @param transformer the transforming operation
     * 
     * @return A {@link TransformingSendPort} wrapping the {@code in} channel.
     */
    public <U> TransformingSendPort<U> fiberTransform(SuspendableAction2<? extends ReceivePort<? super U>, ? extends SendPort<? extends T>> transformer, Channel<U> in) {
        Channels.fiberTransform(in, this, transformer);
        return Channels.transformSend(in);
    }
    
    /**
     * Spawns a fiber that transforms values read from the {@code in} channel and writes values to this channel.
     * <p/>
     * When the transformation terminates. the output channel is automatically closed. If the transformation terminates abnormally 
     * (throws an exception), this channel is {@link SendPort#close(Throwable) closed with that exception}.
     * 
     * @param fiberFactory  will be used to create the fiber
     * @param in           the input channel
     * @param transformer  the transforming operation
     * 
     * @return A {@link TransformingSendPort} wrapping the {@code in} channel.
     */
    public <U> TransformingSendPort<U> fiberTransform(FiberFactory fiberFactory, SuspendableAction2<? extends ReceivePort<? super U>, ? extends SendPort<? extends T>> transformer, Channel<U> in) {
        Channels.fiberTransform(fiberFactory, in, this, transformer);
        return Channels.transformSend(in);
    }
}

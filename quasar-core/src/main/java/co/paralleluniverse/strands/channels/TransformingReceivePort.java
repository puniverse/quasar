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
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.SuspendableAction1;
import co.paralleluniverse.strands.SuspendableAction2;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * A {@link ReceivePort} with additional functional-transform operations, usually wrapping a plain {@link ReceivePort}.
 *
 * @author pron
 */
public class TransformingReceivePort<T> extends DelegatingReceivePort<T> {

    TransformingReceivePort(ReceivePort<T> target) {
        super(target);
    }
    
    /**
     * Returns a {@link TransformingReceivePort} that filters messages that satisfy a predicate from this given channel.
     * All messages (even those not satisfying the predicate) will be consumed from the original channel; those that don't satisfy the predicate will be silently discarded.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.</p>
     *
     * @param pred the filtering predicate
     * @return A {@link TransformingReceivePort} that will receive all those messages from the original channel which satisfy the predicate (i.e. the predicate returns {@code true}).
     */
    public TransformingReceivePort<T> filter(Predicate<T> pred) {
        return Channels.transform(Channels.filter(this, pred));
    }

    /**
     * Returns a {@link TransformingReceivePort} that receives messages that are transformed by a given mapping function from this channel.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param f the mapping function
     * @return a {@link TransformingReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public <U> TransformingReceivePort<U> map(Function<T, U> f) {
        return Channels.transform(Channels.map(this, f));
    }

     /**
     * Returns a {@link TransformingReceivePort} from which receiving messages that are transformed from a given channel by a given reduction function.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.</p>
     *
     * @param f       The reduction function.
     * @param init    The initial input to the reduction function.
     * @return a {@link ReceivePort} that returns messages that are the result of applying the reduction function to the messages received on the given channel.
     */
    public <U> TransformingReceivePort<U> reduce(Function2<U, T, U> f, U init) {
        return Channels.transform(Channels.reduce(this, f, init));
    }

    /**
     * Returns a {@link TransformingReceivePort} that maps exceptions thrown by the underlying channel
     * (by channel transformations, or as a result of {@link SendPort#close(Throwable)} )
     * into messages.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param f the exception mapping function
     */
    TransformingReceivePort<T> mapErrors(Function<Exception, T> f) {
        return Channels.transform(Channels.mapErrors(this, f));
    }

    /**
     * Returns a {@link TransformingReceivePort} that receives messages that are transformed by a given flat-mapping function from this channel.
     * Unlike {@link #map(Function) map}, the mapping function does not returns a single output message for every input message, but
     * a new {@code ReceivePort}. All the returned ports are concatenated into a single {@code ReceivePort} that receives the messages received by all
     * the ports in order.
     * <p>
     * To return a single value the mapping function can make use of {@link Channels#singletonReceivePort(Object)}. To return a collection,
     * it can make use of {@link Channels#toReceivePort(Iterable)}. To emit no values, the function can return {@link Channels#emptyReceivePort()}
     * or {@code null}.
     * <p>
     * The returned {@code TransformingReceivePort} can only be safely used by a single receiver strand.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param f the mapping function
     * @return a {@link TransformingReceivePort} that returns messages that are the result of applying the mapping function to the messages received on the given channel.
     */
    public <U> TransformingReceivePort<U> flatMap(Function<T, ReceivePort<U>> f) {
        return Channels.transform(Channels.flatMap(this, f));
    }

    /**
     * Returns a {@link TakeReceivePort} that can provide at most {@code count} messages from the underlying channel.
     * <p>
     * The returned {@code TransformingReceivePort} has the same {@link Object#hashCode() hashCode} as {@code channel} and is {@link Object#equals(Object) equal} to it.
     *
     * @param count The maximum number of messages extracted from the underlying channel.
     */
    public TransformingReceivePort<T> take(final long count) {
        return Channels.transform(Channels.take(this, count));
    }
    
    /**
     * Spawns a fiber that transforms values read from this channel and writes values to the {@code out} channel.
     * <p>
     * When the transformation terminates. the output channel is automatically closed. If the transformation terminates abnormally
     * (throws an exception), the output channel is {@link SendPort#close(Throwable) closed with that exception}.
     *
     * @param out         the output channel
     * @param transformer the transforming operation
     *
     * @return A {@link TransformingReceivePort} wrapping the {@code out} channel.
     */
    public <U> TransformingReceivePort<U> fiberTransform(SuspendableAction2<? extends ReceivePort<? super T>, ? extends SendPort<? extends U>> transformer, Channel<U> out) {
        Channels.fiberTransform(this, out, transformer);
        return Channels.transform(out);
    }

   /**
     * Spawns a fiber that transforms values read from this channel and writes values to the {@code out} channel.
     * <p>
     * When the transformation terminates. the output channel is automatically closed. If the transformation terminates abnormally
     * (throws an exception), the output channel is {@link SendPort#close(Throwable) closed with that exception}.
     *
     * @param fiberFactory will be used to create the fiber
     * @param out          the output channel
     * @param transformer  the transforming operation
     *
     * @return A {@link TransformingReceivePort} wrapping the {@code out} channel.
     */
    public <U> TransformingReceivePort<U> fiberTransform(FiberFactory fiberFactory, SuspendableAction2<? extends ReceivePort<? super T>, ? extends SendPort<? extends U>> transformer, Channel<U> out) {
        Channels.fiberTransform(fiberFactory, this, out, transformer);
        return Channels.transform(out);
    }

    /**
     * Performs the given action on each message received by this channel.
     * This method returns only after all messages have been consumed and the channel has been closed.
     *
     * @param action
     */
    public void forEach(SuspendableAction1<T> action) throws SuspendExecution, InterruptedException {
        Channels.forEach(this, action);
    }
}

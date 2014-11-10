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
package co.paralleluniverse.fibers;

// TODO circlespainter: finish & review JavaDocs + write examples

/**
 * An extension of {@link FiberAsync} implementing {@link FiberAsync#requestAsync} through a {@link Delegate} functional interface instance;
 * it is meant to extended and subclasses will implement async API callback interfaces as well.
 *
 * @param <V>       The async API's result type
 * @param <E>       The async API's exception type
 * @param <SELF>    The concrete {@link DelegatingFiberAsyncCallback} subclass (self type)
 * 
 * @see FiberAsync
 * 
 * @author circlespainter
 */
public abstract class DelegatingFiberAsyncCallback<V, E extends Throwable, SELF extends DelegatingFiberAsyncCallback<V, E, SELF>> extends FiberAsync<V, E> {
   /**
    * Functional interface representing the concrete operation to be carried out by
    * {@link DelegatingFiberAsyncCallback#requestAsync} in some extension of {@link DelegatingFiberAsyncCallback}
    * 
    * @param <V>       Return value class of the async API method being integrated
    * @param <E>       Exception class of the async API method being integrated
    * @param <DFAC>    The concrete {@link DelegatingFiberAsyncCallback} class
    */
   // @FunctionalInterface // circlespainter: only available in JDK8+
   public interface Delegate<V, E extends Throwable, DFAC extends DelegatingFiberAsyncCallback<V, E, DFAC>> {
       void requestAsync(DFAC callback);
   }
    
    protected final Delegate op;

    /**
     * Same as `DelegatingFiberAsync(op, false)`
     */
    public DelegatingFiberAsyncCallback(Delegate<V, E, SELF> op) {
        this(op, false);
    }

    /**
     * @param op            The {@link Delegate} for subclasses to call with an appropriate API callback when overriding {@link FiberAsync#requestAsync}
     * @param immediateExec See {@link FiberAsync#FiberAsync(boolean)}
     */
    public DelegatingFiberAsyncCallback(Delegate<V, E, SELF> op, boolean immediateExec) {
        super(immediateExec);
        this.op = op;
    }

    /***
     * Sealed, no further extension needed for delegation use cases
     */
    @Override
    protected final void requestAsync() {
        // Call delegate
        op.requestAsync(this);
    }
}
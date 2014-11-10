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
 * the async API callback implementation will be a separate class.
 * 
 * @param <V> The async API's result type
 * @param <E> The async API's exception type
 * 
 * @see FiberAsync
 * 
 * @author circlespainter
 */
public final class DelegatingFiberAsync<V, E extends Throwable> extends FiberAsync<V, E> {
   /**
    * Functional interface representing the concrete operation to be carried out by
    * {@link DelegatingFiberAsync#requestAsync} in some extension of {@link DelegatingFiberAsync}
    * 
    * @author circlespainter
    */
   // @FunctionalInterface // circlespainter: only available in JDK8+
   public interface Delegate {
       void requestAsync();
   }
    
    protected final Delegate op;

    /**
     * Same as `DelegatingFiberAsync(op, false)`
     */
    public DelegatingFiberAsync(Delegate op) {
        this(op, false);
    }

    /**
     * @param op            The {@link Delegate} for subclasses to call with an appropriate API callback when overriding {@link FiberAsync#requestAsync}
     * @param immediateExec See {@link FiberAsync#FiberAsync(boolean)}
     */
    public DelegatingFiberAsync(Delegate op, boolean immediateExec) {
        super(immediateExec);
        this.op = op;
    }

    /***
     * Sealed, no further extension needed for delegation use cases
     */
    @Override
    protected void requestAsync() {
        // Call delegate
        op.requestAsync();
    }
}
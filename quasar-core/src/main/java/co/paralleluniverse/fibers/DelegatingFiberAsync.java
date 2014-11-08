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

// TODO circlespainter: generalise examples in the following JavaDoc

/**
 * An extension of {@link FiberAsync} implementing {@link FiberAsync#requestAsync}
 * through a {@link FiberAsyncDelegate} functional interface instance. It will typically 
 * be extended to implement an integration-specific callback interface as well.
 *
 * This is especially convenient in Java 8+ where lambdas are allowed as a sugar to
 * anonymously extend and instantiate functional interfaces, enabling more concise
 * styles.
 * 
 * For example, given the following integration base class:
 * 
 * ```java
 * public class FiberMongoCallback{@literal <}T{@literal >}
 *     extends DelegatingFiberAsync{@literal <}T, MongoDbException, FiberMongoCallback{@literal <}T{@literal >}{@literal >}
 *     implements Callback{@literal <}T{@literal >} {
 * 
 *     // It's a shame this can't be compiler-provided
 *     public FiberMongoCallback(FiberAsyncDelegate{@literal <}T, MongoDbException, FiberMongoCallback{@literal <}T{@literal >}{@literal >} op) {
 *         super(op);
 *     }
 * 
 *     {@literal @}Override
 *     public T run() throws MongoDbException {
 *         // Handling here exceptions that would be inconvenient to handle later in integrations
 *         try {
 *             return super.run();
 *         } catch (SuspendExecution ex) {
 *             throw new AssertionError(ex);
 *         } catch (InterruptedException ex) {
 *             throw new RuntimeException(ex);
 *         }
 *     }
 *     
 *     {@literal @}Override
 *     public void callback(T success) {
 *         asyncCompleted(success);
 *     }
 * 
 *     {@literal @}Override
 *     public void exception(Throwable failure) {
 *         asyncFailed(failure);
 *     }
 * 
 *     {@literal @]Override
 *     protected void requestAsync() {
 *        // Delegate to op
 *        op.opAsync(this);
 *    }
 * }
 * ```
 * 
 * The following styles can be used:
 * 
 * ### With empty extensions as typedefs (which Java misses)
 * 
 * The class and {@code await} definitions will be reusable for all async API's methods returning values of a given class:
 * 
 *```java
 *  private static final class FiberMongoCallbackForDocsIterator extends FiberMongoCallback{@literal <}MongoIterator{@literal <}Document{@literal >}{@literal >} {
 *      public FiberMongoCallbackForDocsIterator(FiberAsyncDelegate op) {
 *          super(op);
 *      }
 *  }
 *  private interface FiberAsyncDelegateForDocsIterator extends FiberAsyncDelegate{@literal <}MongoIterator{@literal <}Document{@literal >}, MongoDbException, FiberMongoCallbackForDocsIterator{@literal >} {}
 *  private static MongoIterator{@literal <}Document{@literal {@literal >}} await(FiberAsyncDelegateForDocsIterator a) { return new FiberMongoCallbackForDocsIterator(a).run(); }
 *    
 *  {@literal @}Override
 *  {@literal @}Suspendable
 *  public MongoIterator{@literal <}Document{@literal >} aggregate(final Aggregate.Builder command) throws MongoDbException {
 *      return await(c -> aggregateAsync(c, command)); // Quite similar to Pulsar's 'await', about just as concise
 *  }
 * ```
 * 
 * ### With inline types
 * 
 *```java
 *  {@literal @}Override
 *  {@literal @}Suspendable
 *  public MongoIterator{@literal <}Document> aggregate(final Aggregate command) throws MongoDbException {
 *      return
 *          new FiberMongoCallback{@literal <}{@literal >}{ (
 *              (FiberAsyncDelegate{@literal <}MongoIterator{@literal <}Document{@literal >}, MongoDbException, FiberMongoCallback{@literal <}MongoIterator{@literal <}Document{@literal >}{@literal >}{@literal >})
 *                  callback -> aggregateAsync(callback, command)
 *          ).run();
 *  }
 * ```
 * 
 * @param <V>   The async API's result type
 * @param <E>   The async API's exception type
 * @param <C>   An async API-compatible callback type (often it will be an extention of this very class)
 * 
 * @see FiberAsync
 * 
 * @author circlespainter
 */
public abstract class DelegatingFiberAsync <V, E extends Throwable, C> extends FiberAsync<V, E> {
    protected final FiberAsyncDelegate op;

    /**
     * Same as `DelegatingFiberAsync(op, false)`
     * 
     * @param op The {@link FiberAsyncDelegate} this instance will {@link FiberAsync#run}
     */
    public DelegatingFiberAsync(FiberAsyncDelegate<V, E, C> op) {
        this(op, false);
    }

    /**
     * @param op            The {@link FiberAsyncDelegate} for subclasses to call with an appropriate API callback when overriding {@link FiberAsync#requestAsync}
     * @param immediateExec See {@link FiberAsync#FiberAsync(boolean)}
     */
    public DelegatingFiberAsync(FiberAsyncDelegate<V, E, C> op, boolean immediateExec) {
        super(immediateExec);
        this.op = op;
    }
}
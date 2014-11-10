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

// CORE IDEA: allowing integrators to hook the async API logic in FiberAsync through a `@FunctionalIterface` instead of subclassing
// `FiberAsync`. This approach in turn allows a more functional Quasar integration style with lambdas under Java 8.
//
// Unfortunately java 8 only considers valid lambda types the interface types with a single unimplemented method and not abstract
// classes with a default constructor and a single unimplemented method, else FiberAsync would be ok as it is. This is the
// only reason why a delegation mechanism is needed.
//
// PROs:
// *   Allows using lightweight lambdas (and possibly closures) under Java 8 to provide the integration implementation for async APIs.
//
// CONs:
// *   In the special (but very common and useful) case where the integration `FiberAsync` concrete class also implements the async API
//   callback interface, a catch-22 situation arises: the `FiberAsync` concrete instance must hold a reference to the delegate
//   `@FunctionalInterface` instance in order to delegate the `requestAsync` call, while at the same time the delegate `@FunctionalInterface`
//   instance must hold a reference to the `FiberAsync `concrete instance (since it is the async API callback handle as well, which it
//   must hand over to the async API call).
//     In order to break the mutual construction dependency without resorting to mutability, the delegate `@FunctionalInterface`
//   needs to receive the `FiberAsync` instance as an argument when `requestAsync` is being called. This also means that the delegate
//   must know the concrete `FiberAsync` type, which in turn means the `FiberAsync` instance must know its own concrete type: here's
//   why there's a "SELF" recursive type parameter (that's how self types are "simulated" in Java).
//     This complicates slightly Quasar code but it wouldn't be a "con" per se if type arguments could be inferred and not explicitly
//   passed at least in the Java 8 w/lambda use case, but current Java 8 type inference seems not to be smart enough. This "builder"
//   facility could help to work around the issue.
//
// Since the "con" arises only in this case (although most probably a very common one), I would prefer to separate it rather than
// complicating the case that doesn't suffer from the issue (that is, separate `FiberAsync` and async API callback classes).
// This means that the `@FunctionalInterface`s must be separated as well, so `DelegatingFiberAsync` and `DelegatingFiberAsyncCallback`
// can't be respectively parent and child classes (as their construction interface would be different and incompatible). This also means
// that `DelegatingFiberAsync` coudln't be merged back into `FiberAsync`.
// I honestly would prefer avoiding it anyway because in this way `FiberAsync` can still stay unbiased as far as styles are concerned, and
// be extended to a concrete one by just implementing freely `requestAsync`; in addition, `DelegatingFiberAsync` could be made final as
// it would becomes just an integration component with no need to be extended (because async API callback implementation would be a
// different class and concrete async request logic would be in the delegate).

// TODO circlespainter: finish & review JavaDocs
// TODO circlespainter: try out, hopefully the fluent/builder interface helps Java 8's type inferrer as well, allowing omitting more type annotations

/**
 * Builder API for {@link DelegatignFiberAsyncCallback}, starting with a call to {@code constructor}.
 * 
 * @author circlespainter
 */
public class FiberAsyncBuilder {
    /**
     * Functional interface to build concrete {@link DelegatingFiberAsyncCallback} instances
     * 
     * @param <V>       Return value class of the async API method being integrated
     * @param <E>       Exception class of the async API method being integrated
     * @param <DFAC>    The concrete {@link DelegatingFiberAsyncCallback} class
     */
    // @FunctionalInterface // circlespainter: only available in Java 8+
    public interface DelegatingFiberAsyncCallbackConstructor<V, E extends Throwable, DFAC extends DelegatingFiberAsyncCallback<V, E, DFAC>> {
        DFAC create(DelegatingFiberAsyncCallback.Delegate op, boolean immediateExec);
    }

    /**
     * Builder entry point.
     * 
     * @param <V>       Return value class of the async API method being integrated
     * @param <E>       Exception class of the async API method being integrated
     * @param <DFAC>    The concrete {@link DelegatingFiberAsyncCallback} class (self type)
     * @param <CTOR>    The class of the {@link DelegatingFiberAsyncCallbackConstructor} instance that will create the DelegatingFiberAsyncCallback
     *                   (unfortunately Java does not support bounds on constructors)
     * @param ctor      The concrete {@link DelegatingFiberAsyncCallbackConstructor} instance
     * @return The intermediate builder state
     */
    public static <V, E extends Throwable, DFAC extends DelegatingFiberAsyncCallback<V, E, DFAC>, CTOR extends DelegatingFiberAsyncCallbackConstructor<V, E, DFAC>> ConstructorSetState constructor(CTOR ctor) {
        return new ConstructorSetState<>(ctor);
    }

    /**
     * The intermediate state of the builder, when async API callback constructor has been instance set.
     * 
     * @param <V>       Return value class of the async API method being integrated
     * @param <E>       Exception class of the async API method being integrated
     * @param <DFAC>    The concrete {@link DelegatingFiberAsyncCallback} class (self type)
     * @param <CTOR>    The class of the {@link DelegatingFiberAsyncCallbackConstructor} instance that will create the DelegatingFiberAsyncCallback
     *                   (unfortunately Java does not support bounds on constructors)
     */
    public static final class ConstructorSetState<V, E extends Throwable, DFAC extends DelegatingFiberAsyncCallback<V, E, DFAC>, CTOR extends DelegatingFiberAsyncCallbackConstructor<V, E, DFAC>> {
        private final CTOR ctor;

        public ConstructorSetState(CTOR ctor) {
            this.ctor = ctor;
        }

        /**
         * Same as `requestAsync(op, false)`.
         */
        public DFAC requestAsync(DelegatingFiberAsyncCallback.Delegate<V, E, DFAC> op) {
            return requestAsync(op, false);
        }

        /**
         * @param op            The {@link Delegate} for subclasses to call with an appropriate API callback when overriding {@link FiberAsync#requestAsync}
         * @param immediateExec See {@link FiberAsync#FiberAsync(boolean)}
         * @return 
         */
        public DFAC requestAsync(DelegatingFiberAsyncCallback.Delegate<V, E, DFAC> op, boolean immediateExec) {
            return ctor.create(op, immediateExec);
        }
    }
}
package com.proxy4o.utils;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.net.UnknownHostException;
import java.util.concurrent.*;

/**
 * Created by linkerlin on 7/30/16.
 */
public class FiberExecutors<T extends Object, E extends Exception> extends FiberAsync<T, E> implements FiberCompletion<T, E> {

    private static ExecutorService es = Executors.newCachedThreadPool();

    private final Callable<T> callable;

    public FiberExecutors(Callable<T> callable) {
        this.callable = callable;
    }

    @Suspendable // without a checked Exception
    public static <T extends Object, E extends Exception> T fiberSubmit(Callable<T> callable) throws E, InterruptedException, SuspendExecution {
        return (new FiberExecutors<T,E>(callable)).run();
    }

    @Suspendable // with a checked Exception
    public static <T extends Object, E extends Exception> T fiberSubmit(Callable<T> callable, Class<E> throwE) throws E, InterruptedException, SuspendExecution {
        return (new FiberExecutors<T,E>(callable)).run();
    }

    @Suspendable
    public static <T extends Object, E extends Exception> T fiberSubmit(final Callable<T> callable, Class<E> throwE, final long timeout, final TimeUnit unit)
            throws E, TimeoutException, InterruptedException, SuspendExecution {
        return (new FiberExecutors<T,E>(new Callable<T>() {
            @Override
            public T call() throws E, TimeoutException, ExecutionException, InterruptedException {
                return es.submit(new Callable<T>() {
                    @Override
                    public T call() throws Exception {
                        return callable.call();
                    }
                }).get(timeout,unit);
            }
        })).run();
    }

    @Suspendable
    @Override
    protected void requestAsync() {
        es.submit(new Callable<Object>() {
            @Override
            public Object call() throws E {
                try {
                    success(callable.call());
                } catch (Exception e) {
                    failure((E) e);
                }
                return null;
            }
        });
    }

    @Suspendable
    @Override
    public void success(T result) {
        asyncCompleted(result);
    }

    @Suspendable
    @Override
    public void failure(E exception) {
        asyncFailed(exception);
    }
}

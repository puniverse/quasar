package com.proxy4o.utils;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;

import java.util.concurrent.*;

/**
 * Created by linkerlin on 7/30/16.
 */
public class FiberExecutors<T extends Object, E extends Exception> extends FiberAsync<T, E> implements FiberCompletion<T, E> {

    private static ExecutorService es = Executors.newCachedThreadPool();

    private Callable<T> callable;

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
    @Override
    protected void requestAsync() {
        es.submit(() -> {
            try {
                this.success(this.callable.call());
            } catch (Exception e) {
                this.failure((E) e);
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

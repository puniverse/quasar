/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public interface Joinable<V> {
    void join() throws ExecutionException, InterruptedException;

    void join(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException;

    V get() throws ExecutionException, InterruptedException;

    V get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException;

    boolean isDone();
}

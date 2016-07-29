package com.proxy4o.utils;

/**
 * Created by linkerlin on 7/30/16.
 */
public interface FiberCompletion<T extends Object, E extends Exception> {
    void success(T result);

    void failure(E exception);
}

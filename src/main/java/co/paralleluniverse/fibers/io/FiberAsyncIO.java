/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberAsync;
import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 *
 * @author pron
 */
abstract class FiberAsyncIO<V> extends FiberAsync<V, CompletionHandler<V, Fiber>, IOException> {
    @Override
    protected LightweightThreadCallback createCallback() {
        return new LightweightThreadCompletionHandler();
    }

    private class LightweightThreadCompletionHandler extends LightweightThreadCallback implements CompletionHandler<V, Fiber> {
        @Override
        public void completed(V result, Fiber lwthread) {
            super.completed(result, lwthread);
        }

        @Override
        public void failed(Throwable exc, Fiber lwthread) {
            super.failed(exc, lwthread);
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.LightweightThreadAsync;
import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 *
 * @author pron
 */
abstract class LightweightThreadAsyncIO<V> extends LightweightThreadAsync<V, CompletionHandler<V, LightweightThread>, IOException> {
    @Override
    protected LightweightThreadCallback createCallback() {
        return new LightweightThreadCompletionHandler();
    }

    private class LightweightThreadCompletionHandler extends LightweightThreadCallback implements CompletionHandler<V, LightweightThread> {
        @Override
        public void completed(V result, LightweightThread lwthread) {
            super.completed(result, lwthread);
        }

        @Override
        public void failed(Throwable exc, LightweightThread lwthread) {
            super.failed(exc, lwthread);
        }
    }
}

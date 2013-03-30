/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.io.IOException;
import java.nio.channels.CompletionHandler;
import javax.imageio.IIOException;

/**
 *
 * @author pron
 */
abstract class LightweightThreadAsyncIO<V> {
    abstract void callIO(LightweightThread current, CompletionHandler<V, LightweightThread> completionHandler);

    public V run() throws IOException, SuspendExecution {
        final LightweightThreadCompletionHandler completionHandler = new LightweightThreadCompletionHandler();

        while (!completionHandler.isCompleted())
            LightweightThread.park(this, completionHandler);
        return completionHandler.getResult();
    }

    private class LightweightThreadCompletionHandler implements CompletionHandler<V, LightweightThread>, LightweightThread.PostParkActions {
        private volatile boolean completed;
        private boolean registered;
        private Throwable exception;
        private V result;

        @Override
        public void completed(V result, LightweightThread lwthread) {
            this.result = result;
            completed = true;
            lwthread.unpark();
        }

        @Override
        public void failed(Throwable exc, LightweightThread lwthread) {
            this.exception = exc;
            completed = true;
            lwthread.unpark();
        }

        @Override
        public void run(LightweightThread current) {
            if (!registered) {
                callIO(current, this);
                registered = true;
            }
        }

        public boolean isCompleted() {
            return completed;
        }

        public V getResult() throws IOException {
            if (!completed)
                throw new IllegalStateException("Not completed");
            if (exception != null)
                throw (IIOException) exception;
            return result;
        }
    }
}

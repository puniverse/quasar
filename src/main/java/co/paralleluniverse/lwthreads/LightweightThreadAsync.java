/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads;

/**
 * A general helper class that transforms asynchronous requests to synchronous calls on a LightweightThread.
 * @author pron
 * @param <V> The value retuned by the async request
 * @param <Callback> The interface of the async callback.
 * @param <E> An exception class that could be thrown by the async request
 */
public abstract class LightweightThreadAsync<V, Callback, E extends Throwable> {

    @SuppressWarnings("empty-statement")
    public V run() throws E, SuspendExecution {
        final LightweightThreadCallback handler = new LightweightThreadCallback();
        while(!LightweightThread.park(this, handler)) // make sure we actually park and run PostParkActions
            ;
        while (!handler.isCompleted())
            LightweightThread.park(this);
        return handler.getResult();
    }

    /**
     * Calls the asynchronous request and registers the callback.
     * @param current
     * @param callback 
     */
    protected abstract void requestAsync(LightweightThread current, Callback callback);
    
    /**
     * Returns a LightweightThreadCallback that implements Callback
     */
    protected abstract LightweightThreadCallback createCallback();
    
    protected class LightweightThreadCallback implements LightweightThread.PostParkActions {
        private volatile boolean completed;
        private Throwable exception;
        private V result;

        protected void completed(V result, LightweightThread lwthread) {
            this.result = result;
            completed = true;
            lwthread.unpark();
        }

        protected void failed(Throwable exc, LightweightThread lwthread) {
            this.exception = exc;
            completed = true;
            lwthread.unpark();
        }

        @Override
        public void run(LightweightThread current) {
            requestAsync(current, (Callback)this);
        }

        public boolean isCompleted() {
            return completed;
        }

        public V getResult() throws E {
            if (!completed)
                throw new IllegalStateException("Not completed");
            if (exception != null)
                throw (E) exception;
            return result;
        }
    }
}

/*
 * This is an example implementation made for the sake fo the Reactive Streams project
 */
package co.paralleluniverse.strands.channels.reactivestreams;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Implementors must override {@code newSubscription} + {@code ThreadlessSubscription.supply(Subscriber, long)}.
 */
public abstract class ThreadlessPublisher<T> implements Publisher<T> {

    @Override
    public void subscribe(Subscriber<? super T> s) {
        newSubscription(s).start();
    }

    protected abstract ThreadlessSubscription newSubscription(Subscriber<? super T> s);

    protected abstract class ThreadlessSubscription implements Subscription {
        private final Subscriber<? super T> sr;
        private long pending;
        private boolean done;

        public ThreadlessSubscription(Subscriber<? super T> subscriber) {
            this.sr = new WrappedSubscriber<>(subscriber);
        }

        private void start() {
            sr.onSubscribe(this);
            supply();
        }

        @Override
        public final void request(long n) {
            if (n <= 0) {
                sr.onError(new IllegalArgumentException("Requested number must be positive but was " + n + " (rule 3.9)"));
                return;
            }
            
            final boolean recursive = pending == 0;
            pending += n;
            if (pending < 0)
                pending = Long.MAX_VALUE;

            if (!recursive)
                supply();
        }

        @Override
        public void cancel() {
            done = true;
        }

        private void supply() {
            while (!done & pending > 0)
                pending -= supply(sr, pending);
        }

        /**
         * Needs to supply up to n elements by calling {@code onNext}.
         * May freely call {@code onComplete} or {@code onError} as appropriate.
         *
         * @return the number of elements actually supplied
         */
        protected abstract long supply(Subscriber<? super T> subscriber, long n);

        private class WrappedSubscriber<R> implements Subscriber<R> {
            private final Subscriber<R> sr;

            public WrappedSubscriber(Subscriber<R> sr) {
                this.sr = sr;
            }

            @Override
            public void onSubscribe(Subscription s) {
                sr.onSubscribe(s);
            }

            @Override
            public void onNext(R element) {
                if (!done)
                    sr.onNext(element);
            }

            @Override
            public void onError(Throwable t) {
                if (!done) {
                    done = true;
                    sr.onError(t);
                }
            }

            @Override
            public void onComplete() {
                if (!done) {
                    done = true;
                    sr.onComplete();
                }
            }
        }
    }
}

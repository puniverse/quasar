package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import co.paralleluniverse.lwthreads.channels.IntChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

public class PrimitiveChannelRingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = 10;
    static final ForkJoinPool fjPool = new ForkJoinPool(3, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 10; i++)
            new PrimitiveChannelRingBenchmark().run();
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        LightweightThread<Integer> manager = new LightweightThreadWithAnIntChannel<Integer>(fjPool) {
            @Override
            protected Integer run() throws InterruptedException, SuspendExecution {
                IntChannel a = this.channel;
                for (int i = 0; i < N - 1; i++)
                    a = createRelayActor(a);

                a.send(1); // start things off

                int msg = 0;
                for (int i = 0; i < M; i++) {
                    msg = channel.receiveInt();
                    a.send(msg + 1);
                }

                return msg;
            }
        }.start();

        int totalCount = manager.get();
        //assert totalCount == M * N;
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount + " time (ms): " + time);
    }

    private IntChannel createRelayActor(final IntChannel prev) {
        final LightweightThreadWithAnIntChannel<Void> lwt = new LightweightThreadWithAnIntChannel<Void>(fjPool) {
            @Override
            protected Void run() throws InterruptedException, SuspendExecution {
                for (;;)
                    prev.send(channel.receiveInt() + 1);
            }
        };

        lwt.start();
        return lwt.channel;
    }

    private static abstract class LightweightThreadWithAnIntChannel<V> extends LightweightThread<V> {
        IntChannel channel = IntChannel.create(this, mailboxSize);

        public LightweightThreadWithAnIntChannel(ForkJoinPool fjPool) {
            super(fjPool);
        }
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }
}
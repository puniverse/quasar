package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

public class RingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = -1;
    static ForkJoinPool fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 10; i++)
            new PrimitiveChannelRingBenchmark().run();
    }

    private <Message, V> Actor<Message, V> spawnActor(Actor<Message, V> actor) {
        Fiber<Integer> fiber = new Fiber("actor", fjPool, actor).start();
        return actor;
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        Actor<Message, Integer> manager = spawnActor(new Actor<Message, Integer>(mailboxSize) {
            @Override
            protected Integer doRun() throws InterruptedException, SuspendExecution {
                Actor a = this;
                for (int i = 0; i < N - 1; i++)
                    a = createRelayActor(a);

                a.send(new Message(1)); // start things off

                Message msg = null;
                for (int i = 0; i < M; i++) {
                    msg = receive();
                    a.send(new Message(msg.num + 1));
                }

                return msg.num;
            }
        });

        int totalCount = manager.get();
        //assert totalCount == M * N;
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount + " time (ms): " + time);
    }

    private Actor createRelayActor(final Actor<Message, ?> prev) {
        return spawnActor(new Actor<Message, Void>(mailboxSize) {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                for (;;) {
                    Message m = receive();
                    prev.send(new Message(m.num + 1));
                }
            }
        });
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }
}
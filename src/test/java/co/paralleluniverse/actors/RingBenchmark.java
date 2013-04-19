package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

public class RingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = 10;
    static ForkJoinPool fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 10; i++)
            new PrimitiveChannelRingBenchmark().run();
    }

    private static <Message, V> BasicActor<Message, V> spawnActor(BasicActor<Message, V> actor) {
        new Fiber(fjPool, actor).start();
        return actor;
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        BasicActor<Integer, Integer> manager = spawnActor(new BasicActor<Integer, Integer>(mailboxSize) {
            @Override
            protected Integer doRun() throws InterruptedException, SuspendExecution {
                BasicActor<Integer, ?> a = this;
                for (int i = 0; i < N - 1; i++)
                    a = createRelayActor(a);

                a.send(1); // start things off

                Integer msg = null;
                for (int i = 0; i < M; i++) {
                    msg = receive();
                    a.send(msg + 1);
                }

                return msg;
            }
        });

        int totalCount = manager.get();
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount + " time (ms): " + time);
    }

    private BasicActor<Integer,?> createRelayActor(final BasicActor<Integer, ?> prev) {
        return spawnActor(new BasicActor<Integer, Void>(mailboxSize) {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                for (;;) {
                    Integer m = receive();
                    prev.send(m + 1);
                }
            }
        });
    }
}
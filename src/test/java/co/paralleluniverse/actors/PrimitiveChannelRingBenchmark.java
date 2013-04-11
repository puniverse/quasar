package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.IntChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

public class PrimitiveChannelRingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = 10;
    static final ForkJoinPool fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 10; i++)
            new PrimitiveChannelRingBenchmark().run();
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        final IntChannel managerChannel = IntChannel.create(mailboxSize);
        IntChannel a = managerChannel;
        for (int i = 0; i < N - 1; i++)
            a = createRelayActor(a);
        final IntChannel lastChannel = a;
        
        Fiber<Integer> manager = new Fiber<Integer>(fjPool) {
            @Override
            protected Integer run() throws InterruptedException, SuspendExecution {
                lastChannel.send(1); // start things off

                int msg = 0;
                for (int i = 0; i < M; i++) {
                    msg = managerChannel.receiveInt();
                    lastChannel.send(msg + 1);
                }

                return msg;
            }
        };
        managerChannel.setStrand(manager);
        manager.start();

        int totalCount = manager.get();
        //assert totalCount == M * N;
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount + " time (ms): " + time);
    }

    private IntChannel createRelayActor(final IntChannel prev) {
        final IntChannel channel = IntChannel.create(mailboxSize);
        Fiber<Void> fiber = new Fiber<Void>(fjPool) {
            @Override
            protected Void run() throws InterruptedException, SuspendExecution {
                for (;;)
                    prev.send(channel.receiveInt() + 1);
            }
        };
        channel.setStrand(fiber);
        fiber.start();
        return channel;
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }
}
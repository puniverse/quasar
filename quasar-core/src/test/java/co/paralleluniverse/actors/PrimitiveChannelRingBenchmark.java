package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.QueueIntChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;

public class PrimitiveChannelRingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = 10;
    static final ForkJoinPool fjPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        for (int i = 0; i < 10; i++)
            new PrimitiveChannelRingBenchmark().run();
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        final QueueIntChannel managerChannel = QueueIntChannel.create(mailboxSize);
        QueueIntChannel a = managerChannel;
        for (int i = 0; i < N - 1; i++)
            a = createRelayActor(a);
        final QueueIntChannel lastChannel = a;
        
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
        //managerChannel.setStrand(manager);
        manager.start();

        int totalCount = manager.get();
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("Messages: " + totalCount + " Time (ms): " + time);
    }

    private QueueIntChannel createRelayActor(final QueueIntChannel prev) {
        final QueueIntChannel channel = QueueIntChannel.create(mailboxSize);
        Fiber<Void> fiber = new Fiber<Void>(fjPool) {
            @Override
            protected Void run() throws InterruptedException, SuspendExecution {
                for (;;)
                    prev.send(channel.receiveInt() + 1);
            }
        };
        //channel.setStrand(fiber);
        fiber.start();
        return channel;
    }
}
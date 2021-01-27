package co.paralleluniverse.actors;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import co.paralleluniverse.strands.channels.IntChannel;
import co.paralleluniverse.strands.channels.ReceivePort;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PrimitiveChannelRingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final int mailboxSize = 10;

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

        final IntChannel managerChannel = Channels.newIntChannel(mailboxSize);
        IntChannel a = managerChannel;
        for (int i = 0; i < N - 1; i++)
            a = createRelayActor(a);
        final IntChannel lastChannel = a;

        Fiber<Integer> manager = new Fiber<Integer>() {
            @Override
            protected Integer run() throws InterruptedException, SuspendExecution {
                lastChannel.send(1); // start things off

                int msg = 0;
                try {
                    for (int i = 0; i < M; i++) {
                        msg = managerChannel.receiveInt();
                        lastChannel.send(msg + 1);
                    }
                    return msg;
                } catch (ReceivePort.EOFException e) {
                    return null;
                }
            }
        };
        //managerChannel.setStrand(manager);
        manager.start();

        int totalCount = manager.get();
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("Messages: " + totalCount + " Time (ms): " + time);
    }

    private IntChannel createRelayActor(final IntChannel prev) {
        final IntChannel channel = Channels.newIntChannel(mailboxSize);
        Fiber<Void> fiber = new Fiber<Void>() {
            @Override
            protected Void run() throws InterruptedException, SuspendExecution {
                try {
                    for (;;)
                        prev.send(channel.receiveInt() + 1);
                } catch (ReceivePort.EOFException e) {
                    return null;
                }
            }
        };
        //channel.setStrand(fiber);
        fiber.start();
        return channel;
    }
}
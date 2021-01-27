package co.paralleluniverse.actors;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.suspend.SuspendExecution;
import co.paralleluniverse.strands.channels.Channels;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RingBenchmark {
    static final int N = 1000;
    static final int M = 1000;
    static final MailboxConfig mailboxConfig = new MailboxConfig(10, Channels.OverflowPolicy.THROW);
    //static ForkJoinPool fjPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    public static void main(String args[]) throws Exception {
        System.out.println("COMPILER: " + System.getProperty("java.vm.name"));
        System.out.println("VERSION: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("PROCESSORS: " + Runtime.getRuntime().availableProcessors());
        System.out.println();

        for (int i = 0; i < 10; i++)
            new RingBenchmark().run();
    }

    private static <Message, V> Actor<Message, V> spawnActor(Actor<Message, V> actor) {
        new Fiber(actor).start();
        return actor;
    }

    void run() throws ExecutionException, InterruptedException {
        final long start = System.nanoTime();

        Actor<Integer, Integer> manager = spawnActor(new BasicActor<Integer, Integer>(mailboxConfig) {
            @Override
            protected Integer doRun() throws InterruptedException, SuspendExecution {
                ActorRef<Integer> a = this.ref();
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

    private ActorRef<Integer> createRelayActor(final ActorRef<Integer> prev) {
        return spawnActor(new BasicActor<Integer, Void>(mailboxConfig) {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                for (;;) {
                    Integer m = receive();
                    prev.send(m + 1);
                }
            }
        }).ref();
    }
}
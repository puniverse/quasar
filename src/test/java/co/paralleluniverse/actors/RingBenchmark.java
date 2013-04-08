package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;


public class RingBenchmark {
    static final int N = 100;
    static final int M = 100;
    static final int mailboxSize = 10;

    public static void main(String args[]) throws Exception {
        new RingBenchmark().run();
    }
    ForkJoinPool fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

    void run() throws ExecutionException, InterruptedException {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

        System.out.println("Starting ");
        final long start = System.nanoTime();

        Actor<Message, Integer> manager = new Actor<Message, Integer>(fjPool, mailboxSize) {
            @Override
            protected Integer run() throws InterruptedException, SuspendExecution {
                Actor a = this;
                for (int i = 0; i < N; i++)
                    a = createRelayActor(a);

                a.send(new Message(1)); // start things off
                
                // final MessageProcessor<Message> relayMessage = relayMessage(a);
                Message msg = null;
                for (int i = 0; i < M; i++)
                    msg = receive(relayMessage(a));

                return msg.num;
            }
        }.start();

        int totalCount = manager.get();
        //assert totalCount == M * N;
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount + " time (ms): " + time);
    }

    private MessageProcessor<Message> relayMessage(final Actor to) {
        return new MessageProcessor<Message>() {
            @Override
            public boolean process(Message m) {
                to.send(new Message(m.num + 1));
                return true;
            }
        };
    }

    private Actor createRelayActor(final Actor<Message, ?> prev) {
        // final MessageProcessor<Message> relayMessage = relayMessage(prev);
        return new Actor<Message, Void>(fjPool, mailboxSize) {
            @Override
            protected Void run() throws InterruptedException, SuspendExecution {
                for (;;)
                    receive(relayMessage(prev));
            }
        }.start();
    }

    static class Message {
        final int num;

        public Message(int num) {
            this.num = num;
        }
    }
}
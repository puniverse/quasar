/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.lwthreads.SuspendExecution;
import java.util.concurrent.TimeUnit;
import jsr166e.ForkJoinPool;
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
public class RingBenchmark {
    static final int N = 100;
    static final int M = 100;
    static final int mailboxSize = 10;

    public static void main(String args[]) throws Exception {
        new RingBenchmark().run();
    }
    
    ForkJoinPool fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
    LongAdder totalCount = new LongAdder();
    
    void run() throws InterruptedException {
        fjPool = new ForkJoinPool(4, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);

        Actor<Message> manager = new Actor(fjPool, mailboxSize) {
            int counter;
            

            @Override
            protected void run() throws SuspendExecution {
                for (;;) {
                    receive(new MessageProcessor<Message>() {
                        @Override
                        public boolean process(Message m) {
                            counter++;
                            return true;
                        }
                    });
                    if(counter == M)
                        break;
                }
            }
        }.start();

        // create 
        Actor a = manager;
        for (int i = 0; i < N; i++)
            a = createRelayActor(a);

        System.out.println("Starting sending");
        final long start = System.nanoTime();
        for (int i = 0; i < M; i++)
            a.send(new Message(i+1));
        
        for(;;) {
            if(false)
                break;
            System.out.println("c: " + totalCount.sum());
            Thread.sleep(500);
        }
        manager.join();
        
        //assert totalCount.sum() == M * N;
        final long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("messages: " + totalCount.sum() + " time (ms): " + time);
    }

    private Actor createRelayActor(final Actor<Message> prev) {
        return new Actor<Message>(fjPool, mailboxSize) {
            @Override
            protected void run() throws SuspendExecution {
                for (;;) {
                    receive(new MessageProcessor<Message>() {
                        @Override
                        public boolean process(Message message) {
                            totalCount.increment();
                            prev.send(message);
                            return true;
                        }
                    });
                }
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
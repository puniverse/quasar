package co.paralleluniverse.strands.queues;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SingleConsumerLinkedArrayObjectQueue hangs test.
 */
public class SingleConsumerLinkedArrayObjectQueueTest {
    @Test
    public void uglyTest() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false); // Done flag for thread shutdown
        final AtomicInteger size = new AtomicInteger(); // Queue size
        final AtomicLong progress = new AtomicLong(); // Processed iterations count
        final SingleConsumerLinkedArrayObjectQueue<Object> queue = new SingleConsumerLinkedArrayObjectQueue<>(); // Tested queue
        final Thread consumer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!done.get()) {
                    if (queue.poll() != null) {
                        size.decrementAndGet();
                    }
                    progress.incrementAndGet();
                }
            }
        }, "consumer");
        final Thread producer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!done.get()) {
                    if (size.get() < 1) {
                        queue.add(new Object());
                        size.incrementAndGet();
                    }
                }
            }
        }, "producer");
        // Start threads
        consumer.setDaemon(true);
        producer.setDaemon(true);
        consumer.start();
        producer.start();
        // Run 10 seconds and print processed iterations
        for (int i = 0; i < 100; ++i) {
            Thread.sleep(100);
            System.out.println(progress.get());
        }
        // Shutdown threads
        done.set(true);
        consumer.join(TimeUnit.SECONDS.toMillis(5));
        producer.join(TimeUnit.SECONDS.toMillis(5));
        Assert.assertFalse(consumer.isAlive());
        Assert.assertFalse(producer.isAlive());
    }
}

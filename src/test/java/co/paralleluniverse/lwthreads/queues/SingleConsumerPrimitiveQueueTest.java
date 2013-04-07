/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.queues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Simple, single-threaded tests
 *
 * @author pron
 */
@RunWith(Parameterized.class)
public class SingleConsumerPrimitiveQueueTest {
    final SingleConsumerQueue<Integer, ?> wordQueue;
    final SingleConsumerQueue<Double, ?> dwordQueue;

//    public SingleConsumerPrimitiveQueueTest() {
//        this.wordQueue = new SingleConsumerLinkedArrayIntQueue();
//        this.dwordQueue = new SingleConsumerLinkedArrayDoubleQueue();
//    }

    public SingleConsumerPrimitiveQueueTest(int queueType) {
        switch (queueType) {
            case 1:
                this.wordQueue = new SingleConsumerArrayIntQueue(10);
                this.dwordQueue = new SingleConsumerArrayDoubleQueue(10);
                break;
            case 2:
                this.wordQueue = new SingleConsumerLinkedIntQueue();
                this.dwordQueue = new SingleConsumerLinkedDoubleQueue();
                break;
            case 3:
                this.wordQueue = new SingleConsumerLinkedArrayIntQueue();
                this.dwordQueue = new SingleConsumerLinkedArrayDoubleQueue();
                break;
            default:
                throw new AssertionError();
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{1}, {2}, {3}});
    }

    @Test
    public void testEmptyQueue() {
        testEmptyQueue(wordQueue);
        testEmptyQueue(dwordQueue);
    }

    private void testEmptyQueue(SingleConsumerQueue<?, ?> queue) {
        assertThat(queue.size(), is(0));
        assertTrue(queue.isEmpty());
        assertThat(queue.peek(), is(nullValue()));
        assertThat(queue.poll(), is(nullValue()));
        try {
            queue.element();
            fail();
        } catch (NoSuchElementException e) {
        }
        try {
            queue.remove();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testOffer() {
        wordQueue.offer(1);
        wordQueue.offer(2);
        wordQueue.offer(3);

        assertThat(wordQueue.isEmpty(), is(false));
        assertThat(wordQueue.size(), is(3));
        assertThat(list(wordQueue), is(equalTo(list(1, 2, 3))));

        dwordQueue.offer(1.2);
        dwordQueue.offer(2.3);
        dwordQueue.offer(3.4);

        assertThat(dwordQueue.isEmpty(), is(false));
        assertThat(dwordQueue.size(), is(3));
        assertThat(list(dwordQueue), is(equalTo(list(1.2, 2.3, 3.4))));
    }

    @Test
    public void testPoll() {
        int j = 1;
        int k = 1;
        for (int i = 0; i < 8; i++) {
            wordQueue.offer((j++));
            wordQueue.offer((j++));
            Integer s = wordQueue.poll();
            assertThat(s, equalTo((k++)));
        }
        assertThat(wordQueue.size(), is(8));
        assertThat(list(wordQueue), is(equalTo(list(9, 10, 11, 12, 13, 14, 15, 16))));

        for (int i = 0; i < 8; i++) {
            Integer s = wordQueue.poll();
            assertThat(s, equalTo((k++)));
        }
        testEmptyQueue(wordQueue);

        j = 1;
        k = 1;
        for (int i = 0; i < 8; i++) {
            dwordQueue.offer(0.1 + (j++));
            dwordQueue.offer(0.1 + (j++));
            Double s = dwordQueue.poll();
            assertThat(s, equalTo(0.1 + (k++)));
        }
        assertThat(dwordQueue.size(), is(8));
        assertThat(list(dwordQueue), is(equalTo(list(9.1, 10.1, 11.1, 12.1, 13.1, 14.1, 15.1, 16.1))));

        for (int i = 0; i < 8; i++) {
            Double s = dwordQueue.poll();
            assertThat(s, equalTo(0.1 + (k++)));
        }
        testEmptyQueue(dwordQueue);
    }

    @Test
    public void testIteratorRemove() {
        int j = 1;
        int k = 1;

        for (int i = 0; i < 9; i++)
            wordQueue.offer((j++));

        for (Iterator<Integer> it = wordQueue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 == 0)
                it.remove();
        }

        assertThat(list(wordQueue), is(equalTo(list(1, 3, 5, 7, 9))));

        for (int i = 0; i < 4; i++)
            wordQueue.offer((j++));

        k = 1;
        for (Iterator<Integer> it = wordQueue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 != 0)
                it.remove();
        }

        assertThat(list(wordQueue), is(equalTo(list(3, 7, 10, 12))));


        j = 1;
        k = 1;

        for (int i = 0; i < 9; i++)
            dwordQueue.offer(0.1 + (j++));

        for (Iterator<Double> it = dwordQueue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 == 0)
                it.remove();
        }

        assertThat(list(dwordQueue), is(equalTo(list(1.1, 3.1, 5.1, 7.1, 9.1))));

        for (int i = 0; i < 4; i++)
            dwordQueue.offer(0.1 + (j++));

        k = 1;
        for (Iterator<Double> it = dwordQueue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 != 0)
                it.remove();
        }

        assertThat(list(dwordQueue), is(equalTo(list(3.1, 7.1, 10.1, 12.1))));
    }

    @Test
    public void testIteratorRemoveFirst() {
        {
            wordQueue.offer(1);
            wordQueue.offer(2);
            wordQueue.offer(3);
            wordQueue.offer(4);

            Iterator<Integer> it = wordQueue.iterator();
            it.next();
            it.remove();
            it.next();
            it.remove();

            assertThat(wordQueue.size(), is(2));
            assertThat(list(wordQueue), is(equalTo(list(3, 4))));

            wordQueue.offer(5);
            wordQueue.offer(6);

            it.next();
            it.remove();
            it.next();
            it.remove();

            assertThat(wordQueue.size(), is(2));
            assertThat(list(wordQueue), is(equalTo(list(5, 6))));
        }

        {
            dwordQueue.offer(1.2);
            dwordQueue.offer(2.3);
            dwordQueue.offer(3.4);
            dwordQueue.offer(4.5);

            Iterator<Double> it = dwordQueue.iterator();
            it.next();
            it.remove();
            it.next();
            it.remove();

            assertThat(dwordQueue.size(), is(2));
            assertThat(list(dwordQueue), is(equalTo(list(3.4, 4.5))));

            dwordQueue.offer(5.6);
            dwordQueue.offer(6.7);

            it.next();
            it.remove();
            it.next();
            it.remove();

            assertThat(dwordQueue.size(), is(2));
            assertThat(list(dwordQueue), is(equalTo(list(5.6, 6.7))));
        }
    }

    @Test
    public void testIteratorRemoveLast() {
        {
            wordQueue.offer(1);
            wordQueue.offer(2);
            wordQueue.offer(3);
            wordQueue.offer(4);

            Iterator<Integer> it = wordQueue.iterator();
            while (it.hasNext())
                it.next();
            it.remove();

            wordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();


            assertThat(wordQueue.size(), is(2));
            assertThat(list(wordQueue), is(equalTo(list(1, 2))));

            wordQueue.offer(5);
            wordQueue.offer(6);

            wordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();

            wordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();

            assertThat(wordQueue.size(), is(2));
            assertThat(list(wordQueue), is(equalTo(list(1, 2))));
        }

        {
            dwordQueue.offer(1.2);
            dwordQueue.offer(2.3);
            dwordQueue.offer(3.4);
            dwordQueue.offer(4.5);

            Iterator<Double> it = dwordQueue.iterator();
            while (it.hasNext())
                it.next();
            it.remove();

            dwordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();


            assertThat(dwordQueue.size(), is(2));
            assertThat(list(dwordQueue), is(equalTo(list(1.2, 2.3))));

            dwordQueue.offer(5.6);
            dwordQueue.offer(6.7);

            dwordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();

            dwordQueue.resetIterator(it);
            while (it.hasNext())
                it.next();
            it.remove();

            assertThat(dwordQueue.size(), is(2));
            assertThat(list(dwordQueue), is(equalTo(list(1.2, 2.3))));
        }
    }

    @Test
    public void testIteratorRemoveOnly() {
        {
            wordQueue.offer(1);

            Iterator<Integer> it = wordQueue.iterator();
            it.next();
            it.remove();
            testEmptyQueue(wordQueue);

            wordQueue.offer(1);
            assertThat(wordQueue.size(), is(1));
            assertThat(list(wordQueue), is(equalTo(list(1))));

            it = wordQueue.iterator();
            it.next();
            it.remove();

            testEmptyQueue(wordQueue);

            wordQueue.offer(1);
            assertThat(wordQueue.size(), is(1));
            assertThat(list(wordQueue), is(equalTo(list(1))));
        }

        {
            dwordQueue.offer(1.2);

            Iterator<Double> it = dwordQueue.iterator();
            it.next();
            it.remove();
            testEmptyQueue(dwordQueue);

            dwordQueue.offer(1.2);
            assertThat(dwordQueue.size(), is(1));
            assertThat(list(dwordQueue), is(equalTo(list(1.2))));

            it = dwordQueue.iterator();
            it.next();
            it.remove();

            testEmptyQueue(dwordQueue);

            dwordQueue.offer(1.2);
            assertThat(dwordQueue.size(), is(1));
            assertThat(list(dwordQueue), is(equalTo(list(1.2))));
        }
    }

    private static <E> List<E> list(Queue<E> queue) {
        return new ArrayList<>(queue);
    }

    private static <E> List<E> list(E... vals) {
        return Arrays.asList(vals);
    }
}

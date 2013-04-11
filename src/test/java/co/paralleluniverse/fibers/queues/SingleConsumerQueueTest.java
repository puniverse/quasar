/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.queues;

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
public class SingleConsumerQueueTest {
    final SingleConsumerQueue<String, ?> queue;

//    public SingleConsumerQueueTest() {
//        this.queue = new SingleConsumerLinkedArrayObjectQueue<String>();
//    }
    
    public SingleConsumerQueueTest(int queueType) {
        switch (queueType) {
            case 1:
                this.queue = new SingleConsumerArrayObjectQueue<String>(10);
                break;
            case 2:
                this.queue = new SingleConsumerLinkedObjectQueue<String>();
                break;
            case 3:
                this.queue = new SingleConsumerLinkedArrayObjectQueue<String>();
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
        queue.offer("one");
        queue.offer("two");
        queue.offer("three");

        assertThat(queue.isEmpty(), is(false));
        assertThat(queue.size(), is(3));
        assertThat(list(queue), is(equalTo(list("one", "two", "three"))));
    }

    @Test
    public void testPoll() {
        int j = 1;
        int k = 1;
        for (int i = 0; i < 8; i++) {
            queue.offer("x" + (j++));
            queue.offer("x" + (j++));
            String s = queue.poll();
            assertThat(s, equalTo("x" + (k++)));
        }
        assertThat(queue.size(), is(8));
        assertThat(list(queue), is(equalTo(list("x9", "x10", "x11", "x12", "x13", "x14", "x15", "x16"))));

        for (int i = 0; i < 8; i++) {
            String s = queue.poll();
            assertThat(s, equalTo("x" + (k++)));
        }
        testEmptyQueue();
    }

    @Test
    public void testIteratorRemove() {
        int j = 1;
        int k = 1;

        for (int i = 0; i < 9; i++)
            queue.offer("x" + (j++));

        for (Iterator<String> it = queue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 == 0)
                it.remove();
        }

        assertThat(list(queue), is(equalTo(list("x1", "x3", "x5", "x7", "x9"))));

        for (int i = 0; i < 4; i++)
            queue.offer("x" + (j++));

        k = 1;
        for (Iterator<String> it = queue.iterator(); it.hasNext();) {
            it.next();
            if ((k++) % 2 != 0)
                it.remove();
        }

        assertThat(list(queue), is(equalTo(list("x3", "x7", "x10", "x12"))));
    }

    @Test
    public void testIteratorRemoveFirst() {
        queue.offer("one");
        queue.offer("two");
        queue.offer("three");
        queue.offer("four");

        Iterator<String> it = queue.iterator();
        it.next();
        it.remove();
        it.next();
        it.remove();

        assertThat(queue.size(), is(2));
        assertThat(list(queue), is(equalTo(list("three", "four"))));

        queue.offer("five");
        queue.offer("six");
        
        it.next();
        it.remove();
        it.next();
        it.remove();

        assertThat(queue.size(), is(2));
        assertThat(list(queue), is(equalTo(list("five", "six"))));
    }

    @Test
    public void testIteratorRemoveLast() {
        queue.offer("one");
        queue.offer("two");
        queue.offer("three");
        queue.offer("four");

        Iterator<String> it = queue.iterator();
        while (it.hasNext())
            it.next();
        it.remove();

        queue.resetIterator(it);
        while (it.hasNext())
            it.next();
        it.remove();


        assertThat(queue.size(), is(2));
        assertThat(list(queue), is(equalTo(list("one", "two"))));

        queue.offer("five");
        queue.offer("six");

        queue.resetIterator(it);
        while (it.hasNext())
            it.next();
        it.remove();

        queue.resetIterator(it);
        while (it.hasNext())
            it.next();
        it.remove();

        assertThat(queue.size(), is(2));
        assertThat(list(queue), is(equalTo(list("one", "two"))));
    }

    @Test
    public void testIteratorRemoveOnly() {
        queue.offer("one");

        Iterator<String> it = queue.iterator();
        it.next();
        it.remove();
        testEmptyQueue();

        queue.offer("one");
        assertThat(queue.size(), is(1));
        assertThat(list(queue), is(equalTo(list("one"))));

        it = queue.iterator();
        it.next();
        it.remove();

        testEmptyQueue();

        queue.offer("one");
        assertThat(queue.size(), is(1));
        assertThat(list(queue), is(equalTo(list("one"))));
    }

    private static <E> List<E> list(Queue<E> queue) {
        return new ArrayList<>(queue);
    }

    private static <E> List<E> list(E... vals) {
        return Arrays.asList(vals);
    }
}

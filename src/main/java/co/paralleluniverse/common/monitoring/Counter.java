/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.common.monitoring;

//import java.util.concurrent.atomic.AtomicLong;
import jsr166e.LongAdder;

/**
 *
 * @author pron
 */
public class Counter {
    private final LongAdder la = new LongAdder();
    //private final AtomicLong al = new AtomicLong();

    public void reset() {
        la.reset();
        //al.set(0);
    }

    public void inc() {
        la.increment();
        //al.incrementAndGet();
    }

    public void dec() {
        la.decrement();
        //al.decrementAndGet();
    }

    public void add(long val) {
        la.add(val);
        //al.addAndGet(val);
    }

    public long get() {
        return la.sum();
        //return al.get();
    }
}

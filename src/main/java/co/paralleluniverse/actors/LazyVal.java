/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author pron
 */
public abstract class LazyVal<V> {
    public final Class<V> type;
    private final AtomicBoolean compute = new AtomicBoolean(true);
    private volatile boolean doneComputing;
    private V value;

    public LazyVal(Class<V> type) {
        this.type = type;
    }

    public V get() {
        if (!doneComputing) {
            if (compute.compareAndSet(true, false)) {
                value = compute();
                doneComputing = true;
            } else {
                while(!doneComputing)
                    ; // wait
            }
        }
        return value;
    }

    /**
     * Must be a very short-running computation.
     * @return 
     */
    protected abstract V compute();
}

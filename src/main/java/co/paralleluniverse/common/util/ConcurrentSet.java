/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author pron
 */
public class ConcurrentSet<E> extends AbstractSet<E> implements Set<E> {
    public static <E> Set<E> make(ConcurrentMap<E, Object> map) {
        return new ConcurrentSet<E>(map);
    }
    
    private final ConcurrentMap<E, Object> map;
    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    public ConcurrentSet(ConcurrentMap<E, Object> map) {
        this.map = map;
    }

    public ConcurrentSet(ConcurrentMap<E, Object> map, Collection<E> elements) {
        for(E elem : elements)
            map.put(elem, PRESENT);
        this.map = map;
    }

    @Override
    public boolean add(E e) {
        return map.putIfAbsent(e, PRESENT) != PRESENT;
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }
}

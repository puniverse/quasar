/*
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.common.util;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 *
 * @author pron
 */
public abstract class Tuple<E> extends AbstractCollection<E> implements List<E>, RandomAccess {
    @Override
    public abstract int size();

    @Override
    public abstract E get(int index);

    @Override
    public final Iterator<E> iterator() {
        return new Iter();
    }

    @Override
    public final ListIterator<E> listIterator() {
        return new ListIter(0);
    }

    @Override
    public final ListIterator<E> listIterator(int index) {
        rangeCheck(index);
        return new ListIter(index);
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }
    
    @Override
    public final int indexOf(Object o) {
        final int size = size();
        if (o == null) {
            for (int i = 0; i < size; i++)
                if (get(i) == null)
                    return i;
        } else {
            for (int i = 0; i < size; i++)
                if (o.equals(get(i)))
                    return i;
        }
        return -1;
    }

    @Override
    public final int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size() - 1; i >= 0; i--)
                if (get(i) == null)
                    return i;
        } else {
            for (int i = size() - 1; i >= 0; i--)
                if (o.equals(get(i)))
                    return i;
        }
        return -1;
    }

    @Override
    public final List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void rangeCheck(int index) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException("" + index);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Tuple))
            return false;

        final Tuple ot = (Tuple) o;
        final int size = this.size();
        if (size != ot.size())
            return false;

        for (int i = 0; i < size; i++) {
            final Object o1 = this.get(i);
            final Object o2 = ot.get(i);
            if (!(o1 == null ? o2 == null : o1.equals(o2)))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            final Object e = get(i);
            hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
        }
        return hashCode;
    }

    private class Iter implements Iterator<E> {
        int cursor = 0;

        @Override
        public boolean hasNext() {
            return cursor != size();
        }

        @Override
        public E next() {
            if (cursor >= size())
                throw new NoSuchElementException();
            final int i = cursor;
            E next = get(i);
            cursor = i + 1;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class ListIter extends Iter implements ListIterator<E> {
        ListIter(int index) {
            cursor = index;
        }

        @Override
        public boolean hasPrevious() {
            return cursor != 0;
        }

        @Override
        public E previous() {
            if (cursor == 0)
                throw new NoSuchElementException();
            int i = cursor - 1;
            E previous = get(i);
            return previous;
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public final boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }
}

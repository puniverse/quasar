/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.common.util;


/**
 * @author pron
 */
public final class Pair<First, Second> extends Tuple<Object> {
    private final First first;
    private final Second second;

    public Pair(First first, Second second) {
        this.first = first;
        this.second = second;
    }

    public First getFirst() {
        return first;
    }

    public Second getSecond() {
        return second;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public Object get(int index) {
        switch(index) {
            case 0:
                return first;
            case 1:
                return second;
            default:
                throw new IndexOutOfBoundsException("" + index);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Pair<First, Second> other = (Pair<First, Second>) obj;
        if (this.first != other.first && (this.first == null || !this.first.equals(other.first)))
            return false;
        if (this.second != other.second && (this.second == null || !this.second.equals(other.second)))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 31 * hash + (this.second != null ? this.second.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}

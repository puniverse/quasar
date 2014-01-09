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

/**
 * An O(1) substring, necessitated by the JDK's new String representation.
 *
 * @author pron
 */
public final class Substring implements CharSequence, Comparable<Substring> {
    private final String s;
    private final int beginIndex;
    private final int endIndex;
    private int hash; // cache hash; benign races

    public Substring(String s, int beginIndex, int endIndex) {
        if (s == null)
            throw new NullPointerException();
        this.s = s;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public Substring(String s, int beginIndex) {
        this(s, beginIndex, s.length());
    }

    @Override
    public int length() {
        return endIndex - beginIndex;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length())
            throw new StringIndexOutOfBoundsException(index);
        return s.charAt(beginIndex + index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        int length = endIndex - beginIndex;
        if (length < 0)
            throw new StringIndexOutOfBoundsException("beginIndex > endIndex");
        if (beginIndex < 0)
            throw new StringIndexOutOfBoundsException(beginIndex);
        if (endIndex >= length)
            throw new StringIndexOutOfBoundsException(endIndex);
        return new Substring(s, beginIndex + start, beginIndex + end);
    }

    @Override
    public String toString() {
        return s.substring(beginIndex, endIndex);
    }

    @Override
    public int hashCode() {
        int h = hash;
        final int length = endIndex - beginIndex;
        if (h == 0 && length > 0) {
            for (int i = 0; i < length; i++)
                h = 31 * h + charAt(i);
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Substring) {
            final CharSequence other = (CharSequence) obj;
            int n = endIndex - beginIndex;
            if (n == other.length()) {
                int i = 0;
                while (n-- != 0) {
                    if (charAt(i) != other.charAt(i))
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }
    
    @Override
    public int compareTo(Substring other) {
        int len1 = length();
        int len2 = other.length();
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            char c1 = charAt(k);
            char c2 = other.charAt(k);
            if (c1 != c2)
                return c1 - c2;
            k++;
        }
        return len1 - len2;
    }
}

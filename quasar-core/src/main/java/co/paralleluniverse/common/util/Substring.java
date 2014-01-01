/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
 * @author pron
 */
public class Substring implements CharSequence {
    private final String s;
    private final int beginIndex;
    private final int endIndex;

    public Substring(String s, int beginIndex, int endIndex) {
        if(s == null)
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
}

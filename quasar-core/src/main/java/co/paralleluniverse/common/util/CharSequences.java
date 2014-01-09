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

import java.util.Comparator;

/**
 *
 * @author pron
 */
public final class CharSequences {
    public static int hashCode(CharSequence cs) {
        int h = 0;
        final int length = cs.length();
        if (length > 0) {
            for (int i = 0; i < length; i++)
                h = 31 * h + cs.charAt(i);
        }
        return h;
    }

    public static boolean equals(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2)
            return true;
        if (cs1 == null | cs2 == null)
            return false;
        int n = cs1.length();
        if (n == cs2.length()) {
            int i = 0;
            while (n-- != 0) {
                if (cs1.charAt(i) != cs2.charAt(i))
                    return false;
                i++;
            }
            return true;
        }
        return false;
    }

    public static int compare(CharSequence cs1, CharSequence cs2) {
        int len1 = cs1.length();
        int len2 = cs2.length();
        int lim = Math.min(len1, len2);

        int k = 0;
        while (k < lim) {
            char c1 = cs1.charAt(k);
            char c2 = cs2.charAt(k);
            if (c1 != c2)
                return c1 - c2;
            k++;
        }
        return len1 - len2;
    }

    public static Comparator<CharSequence> comparator() {
        return comparator;
    }
    
    private static Comparator<CharSequence> comparator = new Comparator<CharSequence>() {

        @Override
        public int compare(CharSequence o1, CharSequence o2) {
            return CharSequences.compare(o1, o2);
        }
    };
    
    private CharSequences() {
    }
}

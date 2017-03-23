/*
 * Copyright (c) 2013-2017, Parallel Universe Software Co. All rights reserved.
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

import java.util.regex.Pattern;

/**
 *
 * @author pron
 */
public final class Strings {
    @SuppressWarnings("fallthrough")
    public static String globToRegex(String pattern) {
        // Based on Neil Traft: http://stackoverflow.com/a/17369948/750563
        final String DOT = ".";
        
        final StringBuilder out = new StringBuilder(pattern.length());
        out.append('^');
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        out.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                out.append('\\');
                            default:
                                out.append('\\');
                        }
                        out.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        out.append(DOT + "*");
                    else
                        out.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        out.append(DOT);
                    else
                        out.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    out.append('[');
                    break;
                case ']':
                    inClass--;
                    out.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        out.append('\\');
                    out.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        out.append('^');
                    else
                        out.append('!');
                    break;
                case '{':
                    inGroup++;
                    out.append('(');
                    break;
                case '}':
                    inGroup--;
                    out.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        out.append('|');
                    else
                        out.append(',');
                    break;
                default:
                    out.append(ch);
            }
        }
        out.append('$');
        
        return out.toString();
    }
//    public static final String globToRegex(String glob) {
//        StringBuilder out = new StringBuilder(glob.length() + 5);
//        out.append('^');
//        for (int i = 0; i < glob.length(); ++i) {
//            final char c = glob.charAt(i);
//            switch (c) {
//                case '*':
//                    out.append(".*");
//                    break;
//                case '?':
//                    out.append('.');
//                    break;
//                case '.':
//                    out.append("\\.");
//                    break;
//                case '\\':
//                    out.append("\\\\");
//                    break;
//                default:
//                    out.append(c);
//            }
//        }
//        out.append('$');
//        
//        return out.toString();
//    }
    
    public static final Pattern globToPattern(String glob) {
        return Pattern.compile(globToRegex(glob));
    }
    
    private Strings() {
    }
}

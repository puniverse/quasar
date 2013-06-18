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
 *
 * @author pron
 */
public final class Objects {
    public static boolean equal(Object a, Object b) {
        if(a == b)
            return true;
        if(a == null || b == null) // but not both because of above test
            return false;
        return a.equals(b);
    }
    
    public static String systemToString(Object obj) {
        return obj == null ? "null" : obj.getClass().getName() + "@" + systemObjectId(obj);
    }
    
    public static String systemObjectId(Object obj) {
        return Integer.toHexString(System.identityHashCode(obj));
    }
    
    private Objects() {
    }
}

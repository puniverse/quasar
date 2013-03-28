/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

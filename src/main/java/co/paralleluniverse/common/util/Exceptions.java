/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.common.util;

/**
 *
 * @author pron
 */
public final class Exceptions {
    public static RuntimeException rethrow(Throwable t) {
        if(t instanceof RuntimeException)
            throw ((RuntimeException)t);
        if(t instanceof Error)
            throw ((Error)t);
        else
            throw new RuntimeException(t);
    }
    
    private Exceptions() {
    }
}

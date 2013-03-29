/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.concurrent.lwthreads;

/**
 *
 * @author pron
 */
public class TestsHelper {
    public static boolean exec(LightweightThread t) {
        t.resetState();
        return t.exec();
    }
    
    private TestsHelper() {
    }
}

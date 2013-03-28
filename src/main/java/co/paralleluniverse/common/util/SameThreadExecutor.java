/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.util;

import java.util.concurrent.Executor;

/**
 *
 * @author pron
 */
public final class SameThreadExecutor implements Executor {
    private static final SameThreadExecutor INSTANCE = new SameThreadExecutor();

    public static Executor getExecutor() {
        return INSTANCE;
    }
    
    public void execute(Runnable command) {
        command.run();
    }
     
    private SameThreadExecutor() {
    }
}

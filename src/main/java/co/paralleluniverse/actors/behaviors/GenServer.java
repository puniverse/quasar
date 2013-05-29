/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors.behaviors;

import co.paralleluniverse.fibers.SuspendExecution;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author pron
 */
public interface GenServer<Message, V> {
    V call(Message m) throws InterruptedException, SuspendExecution;
    V call(Message m, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException, SuspendExecution;
    void cast(Message m);
    void shutdown();
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

/**
 *
 * @author pron
 */
public interface ActorMXBean {
    void refresh();
    int getTotalReceivedMessages();
    int getQueueLength();
    int getTotalRestarts();
    String[] getLastDeathReasons();
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

/**
 *
 * @author pron
 */
public interface ActorMonitor {
    long nanoTime();
    void setActor(Actor actor);
    void addDeath(Object reason);
    void addRestart();
    void addMessage();
    void skippedMessage();
    void resetSkippedMessages();
}

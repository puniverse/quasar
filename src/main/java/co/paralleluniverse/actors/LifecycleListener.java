/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

/**
 *
 * @author pron
 */
interface LifecycleListener {
    void dead(Actor actor, Object reason);
}

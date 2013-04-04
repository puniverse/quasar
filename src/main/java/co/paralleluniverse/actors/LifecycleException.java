/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

/**
 *
 * @author pron
 */
public class LifecycleException extends RuntimeException {
    private final LifecycleMessage message;

    public LifecycleException(LifecycleMessage message) {
        this.message = message;
    }

    public LifecycleMessage message() {
        return message;
    }
    
    @Override    
    public String toString() {
        String s = getClass().getName();
        return (message != null) ? (s + ": " + message) : s;
    }
}

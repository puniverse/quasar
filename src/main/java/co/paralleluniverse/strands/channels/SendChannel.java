/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands.channels;

/**
 *
 * @author pron
 */
public interface SendChannel<Message> {
    void send(Message message);
}

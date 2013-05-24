/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import co.paralleluniverse.strands.channels.SendChannel;

/**
 *
 * @author pron
 */
public interface Actor<Message> extends SendChannel<Message> {
    String getName();

    boolean isDone();

    void send(Message message);

    void sendSync(Message message);

    Object monitor(Actor other);

    void demonitor(Actor other, Object listener);

    Actor link(Actor other);

    Actor unlink(Actor other);
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.pingpong;

import co.paralleluniverse.actors.Actor;
import java.io.Serializable;

/**
 *
 * @author eitan
 */
public class RepliableMessage<T> implements Serializable {
    T data;
    Actor sender;

    public RepliableMessage(T data, Actor sender) {
        this.data = data;
        this.sender = sender;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.simpleGenServer;

import java.io.Serializable;

/**
 *
 * @author eitan
 */
public class SumRequest implements Serializable {
    final int a;
    final int b;

    public SumRequest(int a, int b) {
        this.a = a;
        this.b = b;
    }
}
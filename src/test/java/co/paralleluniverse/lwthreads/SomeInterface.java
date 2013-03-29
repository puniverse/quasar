/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package co.paralleluniverse.lwthreads;

import co.paralleluniverse.lwthreads.SuspendExecution;

/**
 * A dummy interface used for the InterfaceTest
 * 
 * @author Elias Naur
 */
public interface SomeInterface {
    
    void doStuff() throws SuspendExecution;
    
}

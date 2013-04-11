/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.strands;

/**
 *
 * @author pron
 */
public interface Stranded {
    void setStrand(Strand strand);
    Strand getStrand();
}

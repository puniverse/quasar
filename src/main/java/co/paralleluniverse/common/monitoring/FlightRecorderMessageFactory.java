/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.common.monitoring;

/**
 *
 * @author pron
 */
public interface FlightRecorderMessageFactory {
    FlightRecorderMessage makeFlightRecorderMessage(String clazz, String method, String format, Object[] args);
}

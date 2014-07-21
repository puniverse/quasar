/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.common.io.Streamable;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.galaxy.TimeoutException;

/**
 *
 * @author pron
 */
public interface Messenger {
    /**
     * Adds a message listener on a {@code lonng} topic.
     *
     * @param topic    The topic.
     * @param listener The listener.
     */
    void addMessageListener(long topic, MessageListener listener);

    /**
     * Adds a message listener on a {@code String} topic.
     *
     * @param topic    The topic.
     * @param listener The listener.
     */
    void addMessageListener(String topic, MessageListener listener);

    /**
     * Removes a message listener from a {@code lonng} topic.
     *
     * @param topic    The topic.
     * @param listener The listener.
     */
    void removeMessageListener(long topic, MessageListener listener);

    /**
     * Removes a message listener from a {@code String} topic.
     *
     * @param topic    The topic.
     * @param listener The listener.
     */
    void removeMessageListener(String topic, MessageListener listener);

    /**
     * Sends a message to a known node, on a {@code String} topic.
     *
     * @param node  The node to which to send the message.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void send(short node, String topic, byte[] data);

    /**
     * Sends a message to a known node, on a {@code long} topic.
     *
     * @param node  The node to which to send the message.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void send(short node, long topic, Streamable data);

    /**
     * Sends a message to a known node, on a {@code String} topic.
     *
     * @param node  The node to which to send the message.
     * @param topic The message's topic.
     * @param data  The message.
     */
    void send(short node, String topic, Streamable data);

    /**
     * Sends a message to a known node, on a {@code long} topic.
     *
     * @param node  The node to which to send the message.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void send(short node, long topic, byte[] data);

    /**
     * Sends a message to a the owner of a known grid object node, on a {@code long} topic.
     *
     * @param ref   The grid ref to whose owner the message is to be sent.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void sendToOwnerOf(long ref, long topic, byte[] data) throws TimeoutException, SuspendExecution;

    /**
     * Sends a message to a the owner of a known grid object node, on a {@code String} topic.
     *
     * @param ref   The grid ref to whose owner the message is to be sent.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void sendToOwnerOf(long ref, String topic, byte[] data) throws TimeoutException, SuspendExecution;

    /**
     * Sends a message to a the owner of a known grid object node, on a {@code long} topic.
     *
     * @param ref   The grid ref to whose owner the message is to be sent.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void sendToOwnerOf(long ref, long topic, Streamable data) throws TimeoutException, SuspendExecution;

    /**
     * Sends a message to a the owner of a known grid object node, on a {@code String} topic.
     *
     * @param ref   The grid ref to whose owner the message is to be sent.
     * @param topic The message's topic.
     * @param data  The message.
     * @throws TimeoutException This exception is thrown if the operation has times-out.
     */
    void sendToOwnerOf(long ref, String topic, Streamable data) throws TimeoutException, SuspendExecution;
}

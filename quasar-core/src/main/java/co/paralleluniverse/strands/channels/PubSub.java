/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.strands.channels;

/**
 * Common interface for topics.
 * <p/>
 * @author circlespainter
 */
interface PubSub<Message> extends SendPort<Message> {
    /**
     * Subscribe a channel to receive messages sent to this topic.
     * <p>
     * @param sub the channel to subscribe
     */
    public <T extends SendPort<? super Message>> T subscribe(T sub);

    /**
     * Unsubscribe a channel from this topic.
     * <p>
     * @param sub the channel to subscribe
     */
    public void unsubscribe(SendPort<? super Message> sub);

    /**
     * Unsubscribe all channels from this topic.
     */
    public void unsubscribeAll();
}

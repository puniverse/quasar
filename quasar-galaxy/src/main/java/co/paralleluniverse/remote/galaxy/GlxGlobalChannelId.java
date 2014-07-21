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
package co.paralleluniverse.remote.galaxy;

/**
 *
 * @author pron
 */
public final class GlxGlobalChannelId implements java.io.Serializable {
    private final long topic;
    private final long address; // either my node or my ref

    public GlxGlobalChannelId(boolean global, long address, long topic) {
        this.address = address;
        this.topic = global ? -1L : topic;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + (int)(topic ^ (topic >>> 32));
        hash = 43 * hash + (int) (address ^ (address >>> 32));
        return hash;
    }

    public long getTopic() {
        return topic;
    }

    public long getAddress() {
        return address;
    }

    public boolean isGlobal() {
        return topic < 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GlxGlobalChannelId))
            return false;
        final GlxGlobalChannelId other = (GlxGlobalChannelId) obj;
        if (this.topic != other.topic)
            return false;
        if (this.address != other.address)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GlxGlobalChannelId{" + "global: " + isGlobal()  + " address: " + address + " topic: " + topic + '}';
    }
}

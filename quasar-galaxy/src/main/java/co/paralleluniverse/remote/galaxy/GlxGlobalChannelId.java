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

import java.util.Objects;

/**
 *
 * @author pron
 */
public final class GlxGlobalChannelId implements java.io.Serializable {
    final Object topic; // serializable (String or Long)
    final long address; // either my node or my ref
    final boolean global;

    public GlxGlobalChannelId(boolean global, long address, Object topic) {
        this.global = global;
        this.address = address;
        this.topic = topic;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + Objects.hashCode(this.topic);
        hash = 43 * hash + (int) (this.address ^ (this.address >>> 32));
        hash = 43 * hash + (this.global ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof GlxGlobalChannelId))
            return false;
        final GlxGlobalChannelId other = (GlxGlobalChannelId) obj;
        if (!Objects.equals(this.topic, other.topic))
            return false;
        if (this.address != other.address)
            return false;
        if (this.global != other.global)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GlxGlobalChannelId{" + "global: " + global  + " address: " + address + " topic: " + topic + '}';
    }
}

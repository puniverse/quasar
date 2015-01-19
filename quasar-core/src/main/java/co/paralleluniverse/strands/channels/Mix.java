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

import java.util.Map;
import java.util.Objects;

/**
 * Mix operations.
 *
 * @author circlespainter
 */
interface Mix<P extends Port<?>> {
    public static enum SoloEffect { PAUSE_OTHERS, MUTE_OTHERS };    

    public static enum Mode { NORMAL, PAUSE, MUTE };

    public static class State {
        public final Mode mode;
        public final Boolean solo;

        public State(final Mode mode, final Boolean solo) {
            this.mode = mode;
            this.solo = solo;            
        }

        // Null has meaning only on write operations and it means "don't set"
        public State(final Mode mode) {
            this(mode, null);
        }

        public State(final boolean solo) {
            this(null, solo);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.mode);
            hash = 79 * hash + Objects.hashCode(this.solo);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final State other = (State) obj;
            if (this.mode != other.mode)
                return false;
            if (!Objects.equals(this.solo, other.solo))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "State{" + "solo=" + solo + ", mode=" + mode + '}';
        }
    }

    public void add(final P... ports);

    /**
     * @param ports If {@code null} or empty, all ports will be removed.
     */
    public void remove(final P... ports);

    /**
     * @param ports If {@code null} or empty, all ports will be removed.
     */
    public Map<P, State> getState(final P... ports);

    /**
     * @param ports If {@code null} or empty, all ports' state will be set to {@code state}.
     */
    public void setState(final State state, final P... ports);
    public void setState(final Map<P, State> states);

    public SoloEffect getSoloEffect();
    public void setSoloEffect(final SoloEffect effect);   
 }

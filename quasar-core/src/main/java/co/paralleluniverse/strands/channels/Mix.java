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

import java.util.Collection;
import java.util.Map;

/**
 * Mix operations.
 *
 * @author circlespainter
 */
interface Mix<P extends Port<?>> {
    public Collection<P> get();
    public void add(final P... ports);
    public void remove(final P... ports);
    public void removeAll();

    public static enum State { NORMAL, PAUSE, MUTE };

    public Map<P, State> getState(final P... ports);
    public void setState(final State s, final P... ports);

    public Map<P, State> getStateAll();
    public void setStateAll(final State s);

    public static enum SoloEffect { PAUSE_OTHERS, MUTE_OTHERS };
    
    public SoloEffect getSoloEffect();
    public void setSoloEffect(final SoloEffect effect);
    
    public Map<P, Boolean> getSolo(final P... ports);
    public void setSolo(final boolean solo, final P... ports);

    public Map<P, Boolean> getSoloAll();
    public void setSoloAll(final boolean solo);
}

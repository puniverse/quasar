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
interface Mix<Message, Port extends ReceivePort<? extends Message>> {
    public Collection<Port> get();
    public void add(final Port... ports);
    public void remove(final Port... ports);
    public void removeAll();

    public static enum State { NORMAL, PAUSE, MUTE };

    public Map<Port, State> getState(final Port... ports);
    public void setState(final State s, final Port... ports);

    public Map<Port, State> getStateAll();
    public void setStateAll(final State s);

    public static enum SoloEffect { PAUSE_OTHERS, MUTE_OTHERS };
    
    public SoloEffect getSoloEffect();
    public void setSoloEffect(final SoloEffect effect);
    
    public Map<Port, Boolean> getSolo(final Port... ports);
    public void setSolo(final boolean solo, final Port... ports);

    public Map<Port, Boolean> getSoloAll();
    public void setSoloAll(final boolean solo);
}

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

import co.paralleluniverse.concurrent.util.SwapAtomicReference;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Atomic {@link ReceivePort} similar to {@link ReceiveGroup} but supporting atomic {@link Mix} operations.
 *
 * @author circlespainter
 */
public class ReceivePortMix<Message, Port extends ReceivePort<? extends Message>> implements ReceivePort<Message>, Mix<Message, Port> {
    private final static boolean soloDefault = false;
    private final static SoloEffect soloEffectDefault = SoloEffect.PAUSE_OTHERS;

    private SwapAtomicReference<Selector<Message>> selector;
    private SwapAtomicReference<HashMap<Port, ExtendedState>> states;
    private SwapAtomicReference<SoloEffect> soloEffect = new SwapAtomicReference<SoloEffect>();

    public ReceivePortMix(Collection<Port> ports) {
        soloEffect.set(soloEffectDefault);
        final Port[] snapshot = (Port[]) ports.toArray();
        final HashMap<Port, ExtendedState> newStates = new HashMap<>();
        ArrayList<SelectAction<Message>> actions = new ArrayList<>(ports.size());
        for (final Port port : ports) {
            actions.add(Selector.receive(port));
            newStates.put(port, new ExtendedState(State.NORMAL, soloDefault));
        }
        this.selector.set(new Selector(false, actions));
    }

    public ReceivePortMix(Port... ports) {
        this(Arrays.asList(ports));
    }

    @Override
    public Message tryReceive() {
        for (final ReceivePort<? extends Message> port : states.get().keySet()) {
            final Message m = port.tryReceive();
            if (m != null)
                return m;
        }
        return null;
    }

    @Override
    public Message receive() throws SuspendExecution, InterruptedException {
        setupSelector();
        return selector.get().select().message();
    }

    @Override
    public Message receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        setupSelector();
        final SelectAction<? extends Message> sa = selector.get().select(timeout, unit);
        if (sa != null)
            return sa.message();
        return null;
    }

    @Override
    public Message receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        setupSelector();
        final SelectAction<? extends Message> sa = selector.get().select(timeout);
        if (sa != null)
            return sa.message();
        return null;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public Collection<Port> get() {
        return states.get().keySet();
    }

    @Override
    public void add(final Port... ports) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>(currStates);
                for (final Port port : ports)
                    newStates.put(port, new ExtendedState(State.NORMAL, false));
                return newStates;
            }
        });
    }

    @Override
    public void remove(final Port... ports) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>(currStates);
                for (final Port port : ports)
                    newStates.remove(port);
                return newStates;
            }
        });
    }

    @Override
    public void removeAll() {
        states.set(new HashMap<Port, ExtendedState>());
    }

    @Override
    public Map<Port, State> getState(final Port... ports) {
        final HashMap<Port, ExtendedState> currStates = states.get();
        final Map<Port, State> ret = new HashMap<>(ports.length);
        for (final Port p : ports)
            ret.put(p, currStates.get(p).state);
        return ret;
    }

    @Override
    public void setState(final State state, final Port... ports) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>(currStates);
                for (final Port port : ports)
                    if (newStates.containsKey(port))
                        newStates.put(port, new ExtendedState(state, newStates.get(port).solo));
                return newStates;
            }
        });
    }

    @Override
    public Map<Port, State> getStateAll() {
        final HashMap<Port, ExtendedState> currStates = states.get();
        final Map<Port, State> ret = new HashMap<>(currStates.size());
        for (final Port p : currStates.keySet())
            ret.put(p, currStates.get(p).state);
        return ret;
    }

    @Override
    public void setStateAll(final State state) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>();
                for (final Port port : currStates.keySet())
                    newStates.put(port, new ExtendedState(state, soloDefault));
                return newStates;
            }
        });
    }

    @Override
    public SoloEffect getSoloEffect() {
        return soloEffect.get();
    }

    @Override
    public void setSoloEffect(final SoloEffect effect) {
        soloEffect.set(effect);
    }

    @Override
    public Map<Port, Boolean> getSolo(final Port... ports) {
        final HashMap<Port, ExtendedState> currStates = states.get();
        final Map<Port, Boolean> ret = new HashMap<>(ports.length);
        for (final Port p : ports)
            ret.put(p, currStates.get(p).solo);
        return ret;
    }

    @Override
    public void setSolo(final boolean solo, final Port... ports) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>();
                for (final Port port : ports)
                    if (newStates.containsKey(port))
                        newStates.put(port, new ExtendedState(newStates.get(port).state, solo));
                return newStates;
            }
        });
    }

    @Override
    public Map<Port, Boolean> getSoloAll() {
        final HashMap<Port, ExtendedState> currStates = states.get();
        final Map<Port, Boolean> ret = new HashMap<>(currStates.size());
        for (final Port p : currStates.keySet())
            ret.put(p, currStates.get(p).solo);
        return ret;
    }

    @Override
    public void setSoloAll(final boolean solo) {
        states.swap(new Function<HashMap<Port, ExtendedState>, HashMap<Port, ExtendedState>>() {
            @Override
            public HashMap<Port, ExtendedState> apply(HashMap<Port, ExtendedState> currStates) {
                final HashMap<Port, ExtendedState> newStates = new HashMap<>();
                for (final Port port : currStates.keySet())
                    newStates.put(port, new ExtendedState(newStates.get(port).state, solo));
                return newStates;
            }
        });
    }

    private void setupSelector() {
        if (selector.get() != null) {
            selector.get().reset();
        } else {
            final Set<Port> currStates = states.get().keySet();
            final ArrayList<SelectAction<Message>> actions = new ArrayList<>(currStates.size());
            for (final Port port : currStates)
                actions.add(Selector.receive(port));
            selector.set(new Selector<Message>(false, actions));
        }
    }

    private static class ExtendedState {
        public final boolean solo;
        public final State state;

        public ExtendedState(final State state, final boolean solo) {
            this.state = state;
            this.solo = solo;
        }
    }
}

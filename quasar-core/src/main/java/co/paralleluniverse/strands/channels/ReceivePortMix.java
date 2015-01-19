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
 * {@link ReceivePort} similar to {@link ReceiveGroup} but supporting atomic {@link Mix} operations.
 *
 * @author circlespainter
 */
public class ReceivePortMix<Message, Port extends ReceivePort<? extends Message>> implements ReceivePort<Message>, Mix<Port> {
    private final static Mode modeDefault = Mode.NORMAL;
    private final static boolean soloDefault = false;
    private final static SoloEffect soloEffectDefault = SoloEffect.PAUSE_OTHERS;
    private final SwapAtomicReference<SoloEffect> soloEffect = new SwapAtomicReference<>();

    private SwapAtomicReference<Selector<Message>> selector;
    private SwapAtomicReference<HashMap<Port, State>> states;

    public ReceivePortMix(Collection<Port> ports) {
        soloEffect.set(soloEffectDefault);
        final Port[] snapshot = (Port[]) ports.toArray();
        final HashMap<Port, State> newStates = new HashMap<>();
        ArrayList<SelectAction<Message>> actions = new ArrayList<>(snapshot.length);
        for (final Port port : snapshot) {
            actions.add(Selector.receive(port));
            newStates.put(port, new State(modeDefault, soloDefault));
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
    public void add(final Port... ports) {
        if (ports != null && ports.length > 0)
            states.swap(new Function<HashMap<Port, State>, HashMap<Port, State>>() {
                @Override
                public HashMap<Port, State> apply(HashMap<Port, State> currStates) {
                    final HashMap<Port, State> newStates = new HashMap<>(currStates);
                    for (final Port port : ports)
                        newStates.put(port, new State(Mode.NORMAL, false));
                    return newStates;
                }
            });
    }

    @Override
    public void remove(final Port... ports) {
        if (ports == null || ports.length == 0)
            states.set(new HashMap<Port, State>());
        else
            states.swap(new Function<HashMap<Port, State>, HashMap<Port, State>>() {
                @Override
                public HashMap<Port, State> apply(HashMap<Port, State> currStates) {
                    final HashMap<Port, State> newStates = new HashMap<>(currStates);
                    for (final Port port : ports)
                        newStates.remove(port);
                    return newStates;
                }
            });
    }

    @Override
    public Map<Port, State> getState(final Port... ports) {
        if (ports == null || ports.length == 0)
            return new HashMap<>(states.get());

        final HashMap<Port, State> currStates = states.get();
        final Map<Port, State> ret = new HashMap<>(ports.length);
        for (final Port p : ports)
            ret.put(p, currStates.get(p));
        return ret;
    }

    @Override
    public void setState(final State state, final Port... ports) {
        states.swap(new Function<HashMap<Port, State>, HashMap<Port, State>>() {
            @Override
            public HashMap<Port, State> apply(HashMap<Port, State> currStates) {
                final HashMap<Port, State> newStates = new HashMap<>(currStates);
                for (final Port port : (ports != null && ports.length > 0) ? Arrays.asList(ports) : new ArrayList<>(states.get().keySet()))
                    if (newStates.containsKey(port))
                        newStates.put (
                            port,
                            new State (
                                state.mode != null ? state.mode : currStates.get(port).mode,
                                state.solo != null ? state.solo : currStates.get(port).solo
                            )
                        );
                return newStates;
            }
        });
    }

    @Override
    public void setState(final Map<Port, State> newStates) {
        if (newStates != null)
            states.swap(new Function<HashMap<Port, State>, HashMap<Port, State>>() {
                @Override
                public HashMap<Port, State> apply(HashMap<Port, State> currStates) {
                    final HashMap<Port, State> newStates = new HashMap<>(currStates);
                    for (final Map.Entry<Port, State> e : newStates.entrySet()) {
                        final Port p = e.getKey();
                        final State newS = e.getValue();
                        if (newStates.containsKey(e.getKey()))
                            newStates.put (
                                e.getKey(),
                                newS != null ?
                                    new State (
                                        newS.mode != null ? newS.mode : currStates.get(p).mode,
                                        newS.solo != null ? newS.solo : currStates.get(p).solo
                                    ) :
                                    new State(modeDefault, soloDefault)
                            );
                    }
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

    private void setupSelector() {
        if (selector.get() != null) {
            selector.get().reset();
        } else {
            final Set<Port> currStates = states.get().keySet();
            final ArrayList<SelectAction<Message>> actions = new ArrayList<>(currStates.size());
            for (final Port port : currStates)
                actions.add(Selector.receive(port));
            selector.set(new Selector<>(false, actions));
        }
    }
}

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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 * @author circlespainter
 */
public class ReceivePortGroup<M> implements Mix<M> {
    private static final Predicate<Mix.State> soloP =
        new Predicate<Mix.State>() {
            @Override
            public boolean apply(Mix.State s) {
                return (s != null && s.solo);
            }
        };

    private final static Mode modeDefault = Mode.NORMAL;
    private final static boolean soloDefault = false;
    private final static SoloEffect soloEffectDefault = SoloEffect.PAUSE_OTHERS;

    private final SwapAtomicReference<SoloEffect> soloEffect = new SwapAtomicReference<>();
    private final SwapAtomicReference<Map<? extends ReceivePort<? extends M>, State>> states = new SwapAtomicReference<>();
    private final SwapAtomicReference<Selector<M>> selector = new SwapAtomicReference<>();

    private volatile Map<? extends ReceivePort<? extends M>, State> selectorStates = null;

    public ReceivePortGroup(final Collection<? extends ReceivePort<? extends M>> ports) {
        soloEffect.set(soloEffectDefault);
        final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>();
        for (final ReceivePort<? extends M> port : ImmutableList.copyOf(ports)) {
            newStates.put(port, new State(modeDefault, soloDefault));
        }
        states.set(newStates);
        setupSelector();
    }

    public ReceivePortGroup(final ReceivePort<? extends M>... ports) {
        this(ImmutableList.copyOf(ports));
    }

    @Override
    public M tryReceive() {
        for (final ReceivePort<? extends M> port : states.get().keySet()) {
            final Map<? extends ReceivePort<? extends M>, State> s = states.get();
            M m = null;
            while (m == null) {
                if (!isPaused(port, s))
                    m = port.tryReceive();

                if (m != null && !isMuted(port, s))
                    return m;
                else
                    m = null;
            }
        }
        return null;
    }

    @Override
    public M receive() throws SuspendExecution, InterruptedException {
        setupSelector();
        M m = null;
        while (m == null) {
            final SelectAction<? extends M> sa = selector.get().select();
            if (!isMuted(sa.port(), states.get())) {
                m = sa.message();
            }
        }
        return m;
    }

    @Override
    public M receive(final long timeout, final TimeUnit unit) throws SuspendExecution, InterruptedException {
        setupSelector();
        M m = null;
        while (m == null) {
            final SelectAction<? extends M> sa = selector.get().select(timeout, unit);
            if (sa != null) {
                if (!isMuted(sa.port(), states.get()))
                    m = sa.message();
            } else {
                return null;
            }
        }
        return m;
    }

    @Override
    public M receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        setupSelector();
        M m = null;
        while (m == null) {
            final SelectAction<? extends M> sa = selector.get().select(timeout);
            if (sa != null) {
                if (!isMuted(sa.port(), states.get()))
                    m = sa.message();
            } else {
                return null;
            }
        }
        return m;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    private void setupSelector() {
        if (selector.get() != null && selectorStates == states.get()) {
            selector.get().reset();
        } else {
            selectorStates = states.get();
            final Set<? extends ReceivePort<? extends M>> currStates = selectorStates.keySet();
            final ArrayList<SelectAction<M>> actions = new ArrayList<>(currStates.size());
            for (final ReceivePort<? extends M> port : currStates) {
                if (!isPaused(port, selectorStates))
                    actions.add(Selector.receive(port));
            }
            selector.set(new Selector<>(false, actions));
        }
    }

    @SuppressWarnings("element-type-mismatch")
    private boolean isMuted(final Port<? extends M> port, final Map<? extends ReceivePort<? extends M>, State> s) {
        return
            s.get(port).mode.equals(Mix.Mode.MUTE)
            || (soloEffect.get().equals(Mix.SoloEffect.MUTE_OTHERS)
                && exists(s.values().iterator(), soloP));
    }

    private boolean isPaused(ReceivePort<? extends M> port, final Map<? extends ReceivePort<? extends M>, State> s) {
        return
            s.get(port).mode.equals(Mix.Mode.PAUSE)
            || (soloEffect.get().equals(Mix.SoloEffect.PAUSE_OTHERS)
                && exists(s.values().iterator(), soloP));
    }

        @Override
    public <T extends ReceivePort<? extends M>> void add(final T... items) {
        if (items != null && items.length > 0)
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                    for (final ReceivePort<? extends M> port : ImmutableList.copyOf(items))
                        newStates.put(port, new State(Mode.NORMAL, false));
                    return newStates;
                }
            });
    }

    @Override
    public <T extends ReceivePort<? extends M>> void remove(final T... items) {
        if (items == null || items.length == 0)
            states.set(new HashMap<T, State>());
        else
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                    for (final ReceivePort<? extends M> port : ImmutableList.copyOf(items))
                        newStates.remove(port);
                    return newStates;
                }
            });
    }

    @Override
    public <T extends ReceivePort<? extends M>> Map<T, State> getState(final T... items) {
        if (items == null || items.length == 0)
            ImmutableMap.copyOf(states.get());

        final Map<? extends ReceivePort<? extends M>, State> currStates = states.get();
        final Map<T, State> ret = new HashMap<>(items.length);
        for (final T p : ImmutableList.copyOf(items))
            ret.put(p, currStates.get(p));
        return ret;
    }

    @Override
    public <T extends ReceivePort<? extends M>> void setState(final State state, final T... items) {
        states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
            @Override
            public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                for (final ReceivePort<? extends M> port : (items != null && items.length > 0) ? ImmutableList.copyOf(items) : ImmutableList.copyOf(states.get().keySet()))
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
    public <T extends ReceivePort<? extends M>> void setState(final Map<T, State> newStates) {
        if (newStates != null)
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> updatedStates = new HashMap<>(currStates);
                    final Map<T, State> newStatesSnapshot = ImmutableMap.copyOf(newStates);
                    for (final Map.Entry<T, State> e : newStatesSnapshot.entrySet()) {
                        final T p = e.getKey();
                        final State newS = e.getValue();
                        if (newStatesSnapshot.containsKey(e.getKey()))
                            updatedStates.put (
                                e.getKey(),
                                newS != null ?
                                    new State (
                                        newS.mode != null ? newS.mode : currStates.get(p).mode,
                                        newS.solo != null ? newS.solo : currStates.get(p).solo
                                    ) :
                                    new State(modeDefault, soloDefault)
                            );
                    }
                    return updatedStates;
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

    private static boolean exists(final Iterator<Mix.State> it, final Predicate<Mix.State> soloP) {
        boolean found = false;
        while (!found && it.hasNext())
            found = soloP.apply(it.next());
        return found;
    }
}
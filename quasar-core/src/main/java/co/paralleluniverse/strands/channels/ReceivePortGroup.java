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

import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.concurrent.util.EnhancedAtomicReference;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Timeout;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

    private final EnhancedAtomicReference<SoloEffect> soloEffect = new EnhancedAtomicReference<>();
    private final EnhancedAtomicReference<Map<? extends ReceivePort<? extends M>, State>> states = new EnhancedAtomicReference<>();
    private final EnhancedAtomicReference<Pair<Selector<M>, Map<? extends ReceivePort<? extends M>, State>>> selector = new EnhancedAtomicReference<>();

    public ReceivePortGroup(final Collection<? extends ReceivePort<? extends M>> ports) {
        soloEffect.set(soloEffectDefault);
        final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>();
        for (final ReceivePort<? extends M> port : ImmutableList.copyOf(ports)) {
            newStates.put(port, new State(modeDefault, soloDefault));
        }
        states.set(ImmutableMap.copyOf(newStates)); // RO
        setupSelector();
    }

    public ReceivePortGroup(final ReceivePort<? extends M>... ports) {
        this(ImmutableList.copyOf(ports));
    }

    @Override
    public M tryReceive() {
        try {
            return receive(0, TimeUnit.NANOSECONDS);
        } catch (Throwable t) {
            // This shouldn't happen
            throw new AssertionError(t);
        }
    }

    @Override
    public M receive() throws SuspendExecution, InterruptedException {
        return receive(-1, null);
    }

    @Override
    public M receive(final Timeout timeout) throws SuspendExecution, InterruptedException {
        return receive(timeout.nanosLeft(), TimeUnit.NANOSECONDS);
    }

    @Override
    public M receive(final long timeout, final TimeUnit unit) throws InterruptedException, SuspendExecution {
        // Freeze selector and states for this call
        final Pair<Selector<M>, Map<? extends ReceivePort<? extends M>, State>> curr = selector.get();
        final Selector currSelector = curr.getFirst();
        final Map<? extends ReceivePort<? extends M>, State> currStates = curr.getSecond();

        // Init time bookkeeping in case we have to wait when performing a timed receive
        final long start = timeout > 0 ? System.nanoTime() : 0;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        final long deadline = start + left;
        long now;

        setupSelector();
        SelectAction<M> sa;
        M ret = null;
        while (ret == null) {
            if (unit == null)
                // Untimed select
                sa = currSelector.select();
            else if (left > 0)
                // Timed select
                sa = currSelector.select(left, TimeUnit.NANOSECONDS);
            else
                // trySelect
                sa = currSelector.trySelect();

            if (sa != null) {
                // One port has been selected
                if (!isMuted(sa.port(), currStates))
                    // If it's not muted, return it's value no matter what it is (can be null if closed)
                    return sa.message();
                else {
                    // If it's muted throw away the value and retry
                    now = System.nanoTime();
                    final long prevLeft = left;
                    left = deadline - now;
                    if (prevLeft > 0 && left <= 0)
                        // It was a timed select but it has expired, stop trying
                        return null;
                }
            } else {
                // Timeout or none ready during trySelect
                return null;
            }
        }
        return ret;
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
        // Freeze for this call
        final Pair<Selector<M>, Map<? extends ReceivePort<? extends M>, State>> curr = selector.get();
        final Map<? extends ReceivePort<? extends M>, State> currStates =
            curr != null ? curr.getSecond() : states.get();

        if (currStates == states.get() && curr != null && curr.getFirst() != null) {
            // State has not changed, just reset the selector
            curr.getFirst().reset();
        } else {
            final Set<? extends ReceivePort<? extends M>> currPorts = currStates.keySet();
            // Build a new selector containing receive actions for all non-paused ports
            final ArrayList<SelectAction<M>> actions = new ArrayList<>(currPorts.size());
            for (final ReceivePort<? extends M> port : currPorts) {
                if (!isPaused(port, currStates))
                    actions.add(Selector.receive(port));
            }
            selector.set(new Pair(new Selector<>(false, actions), currStates));
        }
    }

    @SuppressWarnings("element-type-mismatch")
    private boolean isMuted(final Port<? extends M> port, final Map<? extends ReceivePort<? extends M>, State> s) {
        return
            s.get(port).mode.equals(Mix.Mode.MUTE)
            || (soloEffect.get().equals(Mix.SoloEffect.MUTE_OTHERS)
                && Iterables.any(s.values(), soloP));
    }

    private boolean isPaused(ReceivePort<? extends M> port, final Map<? extends ReceivePort<? extends M>, State> s) {
        return
            s.get(port).mode.equals(Mix.Mode.PAUSE)
            || (soloEffect.get().equals(Mix.SoloEffect.PAUSE_OTHERS)
                && Iterables.any(s.values(), soloP));
    }

        @Override
    public <T extends ReceivePort<? extends M>> void add(final T... items) {
        if (items != null && items.length > 0) {
            final List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                    for (final ReceivePort<? extends M> port : itemsCopy)
                        newStates.put(port, new State(Mode.NORMAL, false));
                    return ImmutableMap.copyOf(newStates); // RO
                }
            });
        }
    }

    @Override
    public <T extends ReceivePort<? extends M>> void remove(final T... items) {
        if (items == null || items.length == 0)
            states.set(ImmutableMap.<T, State>of()); // Reset
        else {
            final List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                    for (final ReceivePort<? extends M> port : itemsCopy)
                        newStates.remove(port);
                    return ImmutableMap.copyOf(newStates); // RO
                }
            });
        }
    }

    @Override
    public <T extends ReceivePort<? extends M>> Map<T, State> getState(final T... items) {
        if (items == null || items.length == 0)
            return (Map<T, State>) ImmutableMap.copyOf(states.get());

        List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
        final Map<? extends ReceivePort<? extends M>, State> currStates = states.get();
        final Map<T, State> ret = new HashMap<>(itemsCopy.size());
        for (final T p : itemsCopy)
            ret.put(p, currStates.get(p));
        return ret;
    }

    @Override
    public <T extends ReceivePort<? extends M>> void setState(final State state, final T... items) {
        final ImmutableList<T> itemsCopy = ImmutableList.copyOf(items);
        states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
            @Override
            public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                final Map<ReceivePort<? extends M>, State> newStates = new HashMap<>(currStates);
                for (final ReceivePort<? extends M> port : (items != null && items.length > 0) ? itemsCopy : ImmutableList.copyOf(currStates.keySet()))
                    if (newStates.containsKey(port))
                        newStates.put (
                            port,
                            new State (
                                state.mode != null ? state.mode : currStates.get(port).mode,
                                state.solo != null ? state.solo : currStates.get(port).solo
                            )
                        );
                return ImmutableMap.copyOf(newStates); // RO
            }
        });
    }

    @Override
    public <T extends ReceivePort<? extends M>> void setState(final Map<T, State> newStates) {
        if (newStates != null) {
            final Map<T, State> newStatesSnapshot = ImmutableMap.copyOf(newStates); // Freeze
            states.swap(new Function<Map<? extends ReceivePort<? extends M>, State>, Map<? extends ReceivePort<? extends M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? extends M>, State> apply(final Map<? extends ReceivePort<? extends M>, State> currStates) {
                    final Map<ReceivePort<? extends M>, State> updatedStates = new HashMap<>(currStates);
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
                    return ImmutableMap.copyOf(updatedStates); // RO
                }
            });
        }
    }

    @Override
    public SoloEffect getSoloEffect() {
        return soloEffect.get();
    }

    @Override
    public void setSoloEffect(final SoloEffect effect) {
        soloEffect.set(effect);
    }
}

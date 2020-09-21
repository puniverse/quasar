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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 * @author pron
 * @author circlespainter
 */
public class ReceivePortGroup<M> implements Mix<M> {
    private static final Object ping = new Object();

    private static final Predicate<State> soloP = s -> (s != null && s.solo);

    private final static Mode modeDefault = Mode.NORMAL;
    private final static boolean soloDefault = false;
    private final static SoloEffect soloEffectDefault = SoloEffect.PAUSE_OTHERS;
    private final static boolean alwaysOpenDefault = false;

    private final static Channel<? super Object> changedCh = Channels.newChannel(1, Channels.OverflowPolicy.DISPLACE, false, true);
    private final EnhancedAtomicReference<SoloEffect> soloEffect = new EnhancedAtomicReference<>();
    private final EnhancedAtomicReference<Map<? extends ReceivePort<? super M>, State>> states = new EnhancedAtomicReference<>();
    private final EnhancedAtomicReference<Pair<Selector<M>, Map<? extends ReceivePort<? super M>, State>>> selector = new EnhancedAtomicReference<>();
    private final boolean alwaysOpen;

    public ReceivePortGroup(final Collection<? extends ReceivePort<? super M>> ports, final boolean alwaysOpen) {
        this.alwaysOpen = alwaysOpen;
        soloEffect.set(soloEffectDefault);
        final Map<ReceivePort<? super M>, State> newStates = new HashMap<>();
        for (final ReceivePort<? super M> port : ImmutableList.copyOf(ports)) {
            newStates.put(port, new State(modeDefault, soloDefault));
        }
        states.set(ImmutableMap.copyOf(newStates)); // RO
    }

    public ReceivePortGroup(final Collection<? extends ReceivePort<? super M>> ports) {
        this(ImmutableList.copyOf(ports), alwaysOpenDefault);
    }

    public ReceivePortGroup(final ReceivePort<? super M>... ports) {
        this(ImmutableList.copyOf(ports), alwaysOpenDefault);
    }

    public ReceivePortGroup(final boolean alwaysOpen) {
        this(ImmutableList.of(), alwaysOpen);
    }

    public ReceivePortGroup() {
        this(ImmutableList.of(), alwaysOpenDefault);
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

        if (isClosed())
            return null;
        setupSelector();

        // Init time bookkeeping in case we have to wait when performing a timed receive
        final long start = timeout > 0 ? System.nanoTime() : 0;
        long left = unit != null ? unit.toNanos(timeout) : 0;
        final long deadline = start + left;
        long now;

        // Mutable in case of closed ports
        Pair<Selector<M>, Map<? extends ReceivePort<? super M>, State>> curr = selector.get();
        Selector<M> currSelector = curr.getFirst();
        Map<? extends ReceivePort<? super M>, State> currStates = curr.getSecond();
        SelectAction<M> sa;
        M ret = null;
        while (ret == null) {
            if (unit == null)
                // Untimed select
                sa = currSelector.select();
            else if (left > 0) {
                // Timed select
                sa = currSelector.select(left, TimeUnit.NANOSECONDS);
            }
            else
                // trySelect
                sa = currSelector.trySelect();

            if (sa != null) {
                if (sa.port() != null) {
                    // One port has been selected
                    if (sa.message() == null || changedCh.equals(sa.port())) {
                        // Configuration update or port closed, recalculate state
                        if (isClosed())
                            return null;
                        setupSelector();
                        curr = selector.get();
                        currSelector = curr.getFirst();
                        currStates = curr.getSecond();
                    } else if (!isMuted(sa.port(), currStates))
                        // It's not muted, it's not the special channel and it's not a null message => return it
                        return sa.message();
                    else {
                        // If it's muted throw away the value and retry
                        now = System.nanoTime();
                        final long prevLeft = left;
                        left = deadline - now;
                        if (prevLeft > 0 && left <= 0)
                            // It was a timed select but it has expired, stop trying
                            return null;
                        else
                            // Reset selector and retry
                            currSelector.reset();
                    }
                } else
                    throw new AssertionError(); // This should never happen
            } else
                // Either timeout for timed select, or none ready during trySelect
                return null;
        }
        throw new AssertionError(); // This should never happen
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() {
        removeClosed();
        return !alwaysOpen && states.get().isEmpty();
    }

    private void setupSelector() {
        // Freeze for this call
        final Pair<Selector<M>, Map<? extends ReceivePort<? super M>, State>> curr = selector.get();
        final Map<? extends ReceivePort<? super M>, State> currStates =
            curr != null ? curr.getSecond() : states.get();
        final Map<? extends ReceivePort<? super M>, State> newStates = states.get();

        if (currStates == newStates && curr != null && curr.getFirst() != null) {
            // State has not changed, just reset the selector
            curr.getFirst().reset();
        } else {
            // State has chnaged, update the selector
            final Set<? extends ReceivePort<? super M>> newPorts = newStates.keySet();
            // Build a new selector containing receive actions for all non-paused ports
            final List<SelectAction<M>> mutedActions = new ArrayList<>(newPorts.size());
            final List<SelectAction<M>> enabledActions = new ArrayList<>(newPorts.size());
            for (final ReceivePort<? super M> port : newPorts) {
                if (!isPaused(port, newStates)) {
                    if (isMuted(port, newStates))
                        mutedActions.add(Selector.receive(port));
                    else
                        enabledActions.add(Selector.receive(port));
                }
            }
            final List<SelectAction<M>> actions = new ArrayList<>(newPorts.size());
            actions.add(Selector.receive(changedCh)); // Always receive change pings
            actions.addAll(mutedActions);
            actions.addAll(enabledActions);
            // Priority to change signal, then to muted so they get elimintated first, then normal ones
            selector.set(new Pair<>(new Selector<>(true, actions), newStates));
        }
    }

    @SuppressWarnings("element-type-mismatch")
    private boolean isMuted(final Port<? super M> port, final Map<? extends ReceivePort<? super M>, State> s) {
        return
            !s.get(port).solo
             &&
                (s.get(port).mode.equals(Mix.Mode.MUTE)
                 || (soloEffect.get().equals(Mix.SoloEffect.MUTE_OTHERS)
                     && any(s.values(), soloP)));
    }

    private boolean isPaused(ReceivePort<? super M> port, final Map<? extends ReceivePort<? super M>, State> s) {
        return
            !s.get(port).solo
             &&
                (s.get(port).mode.equals(Mix.Mode.PAUSE)
                 || (soloEffect.get().equals(Mix.SoloEffect.PAUSE_OTHERS)
                     && any(s.values(), soloP)));
    }

    private static <T> boolean any(Iterable<T> values, Predicate<T> condition) {
        for (T value: values) {
            if (condition.test(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends ReceivePort<? super M>> void add(final T... items) throws SuspendExecution, InterruptedException {
        if (items != null && items.length > 0) {
            final List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
            states.swap(new Function<Map<? extends ReceivePort<? super M>, State>, Map<? extends ReceivePort<? super M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? super M>, State> apply(final Map<? extends ReceivePort<? super M>, State> currStates) {
                    final Map<ReceivePort<? super M>, State> newStates = new HashMap<>(currStates);
                    for (final ReceivePort<? super M> port : itemsCopy)
                        newStates.put(port, new State(Mode.NORMAL, false));
                    return ImmutableMap.copyOf(newStates); // RO
                }
            });
            changedCh.send(ping);
        }
    }

    @Override
    public <T extends ReceivePort<? super M>> void remove(final T... items) throws SuspendExecution, InterruptedException {
        if (items == null || items.length == 0)
            states.set(ImmutableMap.of()); // Reset
        else {
            final List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
            states.swap(new Function<Map<? extends ReceivePort<? super M>, State>, Map<? extends ReceivePort<? super M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? super M>, State> apply(final Map<? extends ReceivePort<? super M>, State> currStates) {
                    final Map<ReceivePort<? super M>, State> newStates = new HashMap<>(currStates);
                    for (final T port : itemsCopy)
                        newStates.remove(port);
                    return ImmutableMap.copyOf(newStates); // RO
                }
            });
            changedCh.send(ping);
        }
    }

    private void removeClosed() {
        states.swap(new Function<Map<? extends ReceivePort<? super M>, State>, Map<? extends ReceivePort<? super M>, State>>() {
            @Override
            public Map<? extends ReceivePort<? super M>, State> apply(final Map<? extends ReceivePort<? super M>, State> currStates) {
                final Map<ReceivePort<? super M>, State> newStates = new HashMap<>(currStates);
                for (final ReceivePort<? super M> port : currStates.keySet()) {
                    if (port.isClosed())
                        newStates.remove(port);
                }
                return ImmutableMap.copyOf(newStates); // RO
            }
        });
    }

    @Override
    public <T extends ReceivePort<? super M>> Map<T, State> getState(final T... items) {
        if (items == null || items.length == 0)
            return (Map<T, State>) ImmutableMap.copyOf(states.get());

        List<T> itemsCopy = ImmutableList.copyOf(items); // Freeze for this call
        final Map<? extends ReceivePort<? super M>, State> currStates = states.get();
        final Map<T, State> ret = new HashMap<>(itemsCopy.size());
        for (final T p : itemsCopy)
            ret.put(p, currStates.get(p));
        return ret;
    }

    @Override
    public <T extends ReceivePort<? super M>> void setState(final State state, final T... items) throws SuspendExecution, InterruptedException {
        final ImmutableList<T> itemsCopy = ImmutableList.copyOf(items);
        states.swap(new Function<Map<? extends ReceivePort<? super M>, State>, Map<? extends ReceivePort<? super M>, State>>() {
            @Override
            public Map<? extends ReceivePort<? super M>, State> apply(final Map<? extends ReceivePort<? super M>, State> currStates) {
                final Map<ReceivePort<? super M>, State> newStates = new HashMap<>(currStates);
                for (final ReceivePort<? super M> port : (items != null && items.length > 0) ? itemsCopy : ImmutableList.copyOf(currStates.keySet()))
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
        changedCh.send(ping);
    }

    @Override
    public <T extends ReceivePort<? super M>> void setState(final Map<T, State> newStates) throws SuspendExecution, InterruptedException {
        if (newStates != null) {
            final Map<T, State> newStatesSnapshot = ImmutableMap.copyOf(newStates); // Freeze
            states.swap(new Function<Map<? extends ReceivePort<? super M>, State>, Map<? extends ReceivePort<? super M>, State>>() {
                @Override
                public Map<? extends ReceivePort<? super M>, State> apply(final Map<? extends ReceivePort<? super M>, State> currStates) {
                    final Map<ReceivePort<? super M>, State> updatedStates = new HashMap<>(currStates);
                    for (final Map.Entry<T, State> e : newStatesSnapshot.entrySet()) {
                        final T p = e.getKey();
                        final State newS = e.getValue();
                        if (newStatesSnapshot.containsKey(e.getKey()))
                            updatedStates.put (
                                (ReceivePort<? super M>)e.getKey(),
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
            changedCh.send(ping);
        }
    }

    @Override
    public SoloEffect getSoloEffect() {
        return soloEffect.get();
    }

    @Override
    public void setSoloEffect(final SoloEffect effect) throws SuspendExecution, InterruptedException {
        soloEffect.set(effect);
        changedCh.send(ping);
    }
}

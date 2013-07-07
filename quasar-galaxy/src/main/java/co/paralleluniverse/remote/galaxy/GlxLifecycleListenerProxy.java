/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.LifecycleListener;
import co.paralleluniverse.actors.LifecycleListenerProxy;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.common.util.Pair;
import co.paralleluniverse.galaxy.cluster.NodeChangeListener;
import co.paralleluniverse.galaxy.quasar.Grid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author pron
 */
public class GlxLifecycleListenerProxy extends LifecycleListenerProxy {
    private final Grid grid;
    Map<Short, Set<Pair<Actor, LifecycleListener>>> map = new ConcurrentHashMap<>();

    public GlxLifecycleListenerProxy() {
        try {
            grid = new Grid(co.paralleluniverse.galaxy.Grid.getInstance());
            grid.cluster().addNodeChangeListener(new NodeChangeListener() {
                @Override
                public void nodeAdded(short id) {
                }

                @Override
                public void nodeSwitched(short id) {
                }

                @Override
                public void nodeRemoved(short id) {
                    Set<Pair<Actor, LifecycleListener>> set = map.get(id);
                    for (Pair<Actor, LifecycleListener> pair : set) {
                        pair.getSecond().dead(pair.getFirst(), null);
                    }
                    map.remove(id);
                }
            });
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        final short nodeId = ((GlxRemoteActor) actor).getOwnerNodeId();
        if (!grid.cluster().getNodes().contains(nodeId)) {
            listener.dead(actor, null);
            return;
        }
        super.addLifecycleListener(actor, listener);
        if (actor instanceof GlxRemoteActor) {
            Set<Pair<Actor, LifecycleListener>> set = map.get(nodeId);
            if (set == null) {
                set = Collections.newSetFromMap(new ConcurrentHashMap<Pair<Actor, LifecycleListener>, Boolean>());
                map.put(nodeId, set);
            }
            set.add(new Pair(actor, listener));
        }
    }

    @Override
    public void removeLifecycleListener(RemoteActor actor, LifecycleListener listener) {
        super.removeLifecycleListener(actor, listener);
        if (actor instanceof GlxRemoteActor) {
            final short nodeId = ((GlxRemoteActor) actor).getOwnerNodeId();
            Set<Pair<Actor, LifecycleListener>> set = map.get(nodeId);
            if (set != null) {
                set.remove(new Pair(actor, listener));
            }
        }

    }
}

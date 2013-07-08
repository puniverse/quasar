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

import co.paralleluniverse.actors.LifecycleListener;
import co.paralleluniverse.actors.LifecycleListenerProxy;
import co.paralleluniverse.actors.RemoteActor;
import co.paralleluniverse.galaxy.cluster.NodeChangeListener;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.remote.ServiceUtil;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class GlxLifecycleListenerProxy extends LifecycleListenerProxy {
    private static final Logger LOG = LoggerFactory.getLogger(GlxLifecycleListenerProxy.class);
    private static final ReferenceQueue<LifecycleListener> oldlistenerRefQueue = new ReferenceQueue<>();
    private final Grid grid;
    private static final Map<Short, Set<RegistryRecord>> map = new ConcurrentHashMap<>();

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
                    Set<RegistryRecord> set = map.get(id);
                    for (RegistryRecord rec : set) {
                        LifecycleListener listener = rec.getListenerRef().get();
                        if (listener != null)
                            listener.dead(rec.getActor(), null);
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
            Set<RegistryRecord> set = map.get(nodeId);
            if (set == null) {
                set = Collections.newSetFromMap(new ConcurrentHashMap<RegistryRecord, Boolean>());
                map.put(nodeId, set);
            }
            set.add(new RegistryRecord(new RemoteChannelReceiver.WellBehavedWeakRef<>(listener, oldlistenerRefQueue),actor));
        }
    }

    @Override
    public void removeLifecycleListener(RemoteActor actor, Object id) {
        super.removeLifecycleListener(actor, id);
        if (actor instanceof GlxRemoteActor) {
            final short nodeId = ((GlxRemoteActor) actor).getOwnerNodeId();
            Set<RegistryRecord> set = map.get(nodeId);
            for (Iterator<RegistryRecord> it = set.iterator(); it.hasNext();) {
                RegistryRecord rec = it.next();
                LifecycleListener listener = rec.getListenerRef().get();
                if (listener != null && listener.getId() == id) {
                    it.remove();
                    break;
                }
            }
            if (set.isEmpty())
                map.remove(nodeId);
        }
    }

    static {

        Thread collector = new Thread(new Runnable() {
            private final LifecycleListenerProxy lifecycleListenerProxy = ServiceUtil.loadSingletonService(LifecycleListenerProxy.class);

            @Override
            public void run() {
                try {
                    for (;;) {
                        RemoteChannelReceiver.WellBehavedWeakRef<LifecycleListener> ref = (RemoteChannelReceiver.WellBehavedWeakRef<LifecycleListener>) oldlistenerRefQueue.remove();
                        LOG.info("garbaging " + ref);
                        // we can't use map.get() b/c the map is organized by WellBehavedWeakRef's hashCode, and here we need identity
                        for (Iterator<Map.Entry<Short, Set<RegistryRecord>>> it = map.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<Short, Set<RegistryRecord>> entry = it.next();
                            for (Iterator<RegistryRecord> it1 = entry.getValue().iterator(); it1.hasNext();) {
                                RegistryRecord rec = it1.next();
                                if (ref==rec.getListenerRef()) {
                                    lifecycleListenerProxy.removeLifecycleListener((RemoteActor) rec.getActor(), rec.getId());
                                }
                            }
                        }
                    }

                } catch (InterruptedException e) {
                }
            }
        }, "remote-lifecycle-listeners-collector");
        collector.setDaemon(true);
        collector.start();
    }
    
    class RegistryRecord {
        WeakReference<LifecycleListener> listenerRef;
        RemoteActor actor;
        Object id;

        public RegistryRecord(WeakReference<LifecycleListener> listenerRef, RemoteActor actor) {
            this.listenerRef = listenerRef;
            this.actor = actor;
            LifecycleListener listener = this.listenerRef.get();
            assert listener!=null;
            this.id = listener.getId();
        }

        public WeakReference<LifecycleListener> getListenerRef() {
            return listenerRef;
        }

        public RemoteActor getActor() {
            return actor;
        }

        public Object getId() {
            return id;
        }
        
    }
}

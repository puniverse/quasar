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
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.quasar.Grid;
import co.paralleluniverse.galaxy.quasar.Store;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.remote.GlobalRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
public class GalaxyGlobalRegistry implements GlobalRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyGlobalRegistry.class);
    private final Grid grid;

    public GalaxyGlobalRegistry() {
        try {
            grid = new Grid(co.paralleluniverse.galaxy.Grid.getInstance());
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object register(LocalActor<?, ?> actor) throws SuspendExecution {
        final String rootName = actor.getName().toString();

        LOG.info("Registering actor {} at root {}", actor, rootName);

        final Store store = grid.store();
        StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                if (store.isRootCreated(root, txn)) {
                    store.set(root, Serialization.write(actor), txn);
                }
                store.commit(txn);

                return root; // root is the global id
            } catch (TimeoutException e) {
                LOG.error("Registering actor {} at root {} failed due to timeout", actor, rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor registration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unregister(Object name) throws SuspendExecution {
        final String rootName = name.toString();

        LOG.info("Uregistering {}", name);

        final Store store = grid.store();

        StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                store.set(root, (byte[]) null, txn);
                store.commit(txn);
            } catch (TimeoutException e) {
                LOG.error("Unregistering {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor unregistration failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <Message> Actor<Message> getActor(Object name) throws SuspendExecution {
        final String rootName = name.toString();

        final Store store = grid.store();
        StoreTransaction txn = store.beginTransaction();
        try {
            try {
                final long root = store.getRoot(rootName, txn);
                byte[] buf = store.get(root);
                if (buf == null)
                    return null;

                final Actor<Message> actor;
                try {
                    actor = (Actor<Message>) Serialization.read(buf);
                } catch (Exception e) {
                    LOG.info("Deserializing actor at root " + rootName + " has failed with exception", e);
                    return null;
                }

                return actor;
            } catch (TimeoutException e) {
                LOG.error("Getting actor {} failed due to timeout", rootName);
                store.rollback(txn);
                store.abort(txn);
                throw new RuntimeException("Actor discovery failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

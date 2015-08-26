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
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.remote.galaxy.RemoteInit;

/**
 *
 * @author pron
 */
public class Grid {
    public static Grid getInstance() throws InterruptedException {
        try {
            return LazyHolder.instance;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof InterruptedException)
                throw (InterruptedException) e.getCause();
            throw e;
        }
    }

    private static class LazyHolder {
        private static Grid instance;

        static {
            try {
                instance = new Grid(co.paralleluniverse.galaxy.Grid.getInstance());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final co.paralleluniverse.galaxy.Grid grid;
    private final Store store;
    private final Messenger messenger;

    static {
        RemoteInit.init();
    }

    private Grid(co.paralleluniverse.galaxy.Grid grid) {
        this.grid = grid;
        this.store = new StoreImpl(grid.store());
        this.messenger = new MessengerImpl(grid.messenger());
    }

    /**
     * Returns the grid's distributed data-store service.
     *
     * @return The grid's distributed data-store service.
     */
    public Store store() {
        return store;
    }

    /**
     * Returns the grid's messaging service.
     *
     * @return The grid's messaging service.
     */
    public Messenger messenger() {
        return messenger;
    }

    /**
     * Returns the grid's cluster management and node lifecycle service.
     *
     * @return The grid's cluster management and node lifecycle service.
     */
    public Cluster cluster() {
        return grid.cluster();
    }

    /**
     * Makes this node a full participant in the cluster (rather than just an observer).
     */
    public void goOnline() throws InterruptedException {
        grid.goOnline();
    }

    public co.paralleluniverse.galaxy.Grid getDelegate() {
        return grid;
    }
}

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
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.galaxy.Cluster;

/**
 *
 * @author pron
 */
public class Grid {
    private final co.paralleluniverse.galaxy.Grid grid;
    private final Store store;
    private final Messenger messenger;
    
    public Grid(co.paralleluniverse.galaxy.Grid grid) {
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
}

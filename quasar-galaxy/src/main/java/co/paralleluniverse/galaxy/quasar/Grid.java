/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.quasar;

import co.paralleluniverse.common.spring.Service;
import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.galaxy.Messenger;
import co.paralleluniverse.galaxy.Store;
import co.paralleluniverse.galaxy.core.AbstractCluster;

/**
 *
 * @author pron
 */
public class Grid {
    private final co.paralleluniverse.galaxy.Grid grid;

    public Grid(co.paralleluniverse.galaxy.Grid grid) {
        this.grid = grid;
    }

    /**
     * Returns the grid's distributed data-store service.
     *
     * @return The grid's distributed data-store service.
     */
    public Store store() {
        return null;
    }

    /**
     * Returns the grid's messaging service.
     *
     * @return The grid's messaging service.
     */
    public Messenger messenger() {
        return null;
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

/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
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
/*
 * Based on code:
 */
/* 
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package co.paralleluniverse.strands.concurrent;

import co.paralleluniverse.strands.Strand;

/**
 * A synchronizer that may be exclusively owned by a strand.  This
 * class provides a basis for creating locks and related synchronizers
 * that may entail a notion of ownership.  The
 * {@code AbstractOwnableSynchronizer} class itself does not manage or
 * use this information. However, subclasses and tools may use
 * appropriately maintained values to help control and monitor access
 * and provide diagnostics.
 *
 * @since 1.6
 * @author Doug Lea
 */
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /** Use serial ID even though all fields transient. */
    private static final long serialVersionUID = 3737899427754241961L;

    /**
     * Empty constructor for use by subclasses.
     */
    protected AbstractOwnableSynchronizer() { }

    /**
     * The current owner of exclusive mode synchronization.
     */
    private transient Strand exclusiveOwnerStrand;

    /**
     * Sets the strand that currently owns exclusive access.
     * A {@code null} argument indicates that no strand owns access.
     * This method does not otherwise impose any synchronization or
     * {@code volatile} field accesses.
     * @param strand the owner strand
     */
    protected final void setExclusiveOwnerStrand(Strand strand) {
        exclusiveOwnerStrand = strand;
    }

    /**
     * Returns the strand last set by {@code setExclusiveOwnerStrand},
     * or {@code null} if never set.  This method does not otherwise
     * impose any synchronization or {@code volatile} field accesses.
     * @return the owner strand
     */
    protected final Strand getExclusiveOwnerStrand() {
        return exclusiveOwnerStrand;
    }
}

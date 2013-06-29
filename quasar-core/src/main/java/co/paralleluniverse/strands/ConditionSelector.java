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
package co.paralleluniverse.strands;

import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author pron
 */
public class ConditionSelector extends ConditionSynchronizer implements Condition {
    private final Collection<Condition> conditions;

    public ConditionSelector(Collection<Condition> conditions) {
        this.conditions = conditions;
    }
    
    public ConditionSelector(Condition... conds) {
        this(Arrays.asList(conds));
    }

    @Override
    public void register() {
        for(Condition cond : conditions)
            cond.register();
    }

    @Override
    public void unregister() {
        for(Condition cond : conditions)
            cond.unregister();
    }

    @Override
    public void signal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void signalAll() {
        throw new UnsupportedOperationException();
    }
}

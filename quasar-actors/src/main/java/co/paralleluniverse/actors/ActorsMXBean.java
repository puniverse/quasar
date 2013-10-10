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
package co.paralleluniverse.actors;

import java.util.List;

/**
 *
 * @author pron
 */
public interface ActorsMXBean {
    void refresh();

    int getNumActiveActors();
    
    long[] getAllActorIds();
    
    String getStackTrace(long actorId);
    
    String[] getMailbox(long actorId);
            
    void addWatch(long actorId);
    
    void removeWatch(long actorId);
    
    List<ActorInfo> getWatchedActorsInfo();
}

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
package co.paralleluniverse.remote.galaxy;

import co.paralleluniverse.actors.ActorImpl;
import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.spi.ActorRefCanonicalizer;
import org.kohsuke.MetaInfServices;

/**
 *
 * @author pron
 */
@MetaInfServices
public class GlxActorRefCanonicalizer implements ActorRefCanonicalizer {
    private final Canonicalizer<GlxGlobalChannelId, ActorRef<?>> canonicalizer = new Canonicalizer<>();

    @Override
    public <Message> ActorRef<Message> getRef(final ActorImpl<Message> impl) {
        return (ActorRef<Message>)canonicalizer.get(((GlxRemoteActor) impl).getId(), impl.ref());
    }
}

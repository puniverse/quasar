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
package co.paralleluniverse.actors;

import co.paralleluniverse.actors.spi.ActorRefCanonicalizer;
import co.paralleluniverse.common.util.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
final class ActorRefCanonicalizerService {
    private static final Logger LOG = LoggerFactory.getLogger(ActorRefCanonicalizerService.class);
    private static final ActorRefCanonicalizer canonicalizer = ServiceUtil.loadSingletonService(ActorRefCanonicalizer.class);

    static {
        LOG.info("ActorRefCanonicalizer is {}", canonicalizer);
    }

    public static <Message> ActorRef<Message> getRef(ActorImpl<Message> impl, ActorRef<Message> ref) {
        return canonicalizer.getRef(impl, ref);
    }

    private ActorRefCanonicalizerService() {
    }
}

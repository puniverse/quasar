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

import co.paralleluniverse.actors.spi.MigrationRecord;
import co.paralleluniverse.actors.spi.Migrator;
import co.paralleluniverse.common.util.ServiceUtil;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.io.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pron
 */
class MigrationService {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);
    private static final Migrator migrator = ServiceUtil.loadSingletonService(Migrator.class);

    static {
        LOG.info("Migrator is {}", migrator);
    }

    private MigrationService() {
    }

    public static Object registerMigratingActor() throws SuspendExecution {
        Object res = migrator.registerMigratingActor();
        return res;
    }

    public static void migrate(Object id, Actor actor, byte[] serializedMigrationRecord) throws SuspendExecution {
        migrator.migrate(actor.getGlobalId(), actor, serializedMigrationRecord);
    }

    public static MigrationRecord hire(ActorRef<?> actorRef, ByteArraySerializer ser) throws SuspendExecution {
        return migrator.hire(actorRef, actorRef.getImpl(), ser);
    }
}

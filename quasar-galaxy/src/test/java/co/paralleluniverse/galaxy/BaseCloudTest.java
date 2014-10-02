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
package co.paralleluniverse.galaxy;

import org.gridkit.nanocloud.Cloud;
import org.gridkit.nanocloud.CloudFactory;

import org.gridkit.vicluster.ViProps;
import org.junit.After;

public abstract class BaseCloudTest {

    protected Cloud cloud;

    @After
    public void recycleCloud() {
        if (cloud != null)
            cloud.shutdown();
    }

    static Cloud createLocalCloud() {
        final Cloud cloud = CloudFactory.createCloud();
        ViProps.at(cloud.node("**")).setLocalType();
        return cloud;
    }

    static final String SERVER = "server";
    static final String PEER2 = "peer2";
    static final String PEER1 = "peer1";
    static final String PEER_NO_SERVER_CFG = "config/peerNoServer.xml";
    static final String PEER_WITH_ZK_SERVER_CFG = "config/peerWithZKServer.xml";
    static final String PEER_WITH_JG_SERVER_CFG = "config/peerWithJGServer.xml";
    static final String SERVER_PROPS = "config/server.properties";
    static final String SERVER_ZK_CFG = "config/serverZK.xml";
    static final String SERVER_JG_CFG = "config/serverJG.xml";
}

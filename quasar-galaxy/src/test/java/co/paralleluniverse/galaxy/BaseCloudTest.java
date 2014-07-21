package co.paralleluniverse.galaxy;

import org.gridkit.nanocloud.CloudFactory;

import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViProps;
import org.junit.After;

public abstract class BaseCloudTest {

    protected ViManager cloud;

    @After
    public void recycleCloud() {
        if (cloud != null) {
            cloud.shutdown();
        }
    }

    static ViManager createLocalCloud() {
        ViManager vim = CloudFactory.createCloud();
        ViProps.at(vim.node("**")).setLocalType();
        return vim;
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

package co.paralleluniverse.galaxy;

import co.paralleluniverse.galaxy.example.pingpong.Ping;
import co.paralleluniverse.galaxy.example.pingpong.Pong;
import static co.paralleluniverse.galaxy.testing.GalaxyTestingUtils.*;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class NanoCloudLocalTest extends BaseCloudTest {
    static final GlxConfig ZK_WITH_SERVER_CFG = new GlxConfig(PEER_WITH_ZK_SERVER_CFG, SERVER_ZK_CFG, true, true);
    static final GlxConfig JG_WITH_SERVER_CFG = new GlxConfig(PEER_WITH_JG_SERVER_CFG, SERVER_JG_CFG, false, true);
    static final GlxConfig JG_NO_SERVER_CFG = new GlxConfig(PEER_NO_SERVER_CFG, null, false, false);
    static final GlxConfig CFG = 
            ZK_WITH_SERVER_CFG;
//            JG_WITH_SERVER_CFG;
//            JG_NO_SERVER_CFG;
    private ServerCnxnFactory zk;

    @Before
    public void setUp() throws InterruptedException, IOException, QuorumPeerConfig.ConfigException {
        if (CFG.hasZK)
            zk = startZookeeper("config/zoo.cfg", "/tmp/zookeeper/");
        cloud = createLocalCloud();
    }

    @After
    public void tearDown() {
        if (zk != null)
            zk.shutdown();
    }

    @Test
    public void pingPongTest() throws InterruptedException, ExecutionException {
        cloud.nodes(SERVER, "ping", "pong");
        setJvmArgs(cloud);
        if (CFG.hasServer)
            cloud.node(SERVER).submit(startGlxServer(CFG.serverCfg, SERVER_PROPS));
        final Future<Integer> pingFuture = cloud.node("pong").submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                return Pong.runPong();
            }
        });
        Thread.sleep(500);
        Future<Void> ping = cloud.node("ping").submit(new Runnable() {
            @Override
            public void run() {
                Ping.runPing();
            }
        });
        int pings = pingFuture.get();
        assertEquals("Number of pings received by pong", 3, pings);
        ping.get();
    }

    private static void setJvmArgs(final ViManager cloud) {
        System.setProperty("co.paralleluniverse.galaxy.configFile", pathToResource(CFG.peerCfg));
        System.setProperty("galaxy.zkServers", "127.0.0.1:2181");
        String[] copyEnv = {
            "jgroups.bind_addr",
            "galaxy.zkServers",
            "galaxy.multicast.address",
            "galaxy.multicast.port",
            "co.paralleluniverse.galaxy.configFile",
            "log4j.configurationFile",
            "co.paralleluniverse.galaxy.autoGoOnline"};
        JvmProps props = JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + System.getProperty("co.paralleluniverse.quasarJar"));
        for (String string : copyEnv)
            props = props.addJvmArg("-D" + string + "=" + System.getProperty(string));
        // check why setEnv doesn't work
    }

    static class GlxConfig {
        String peerCfg;
        String serverCfg;
        boolean hasZK;
        boolean hasServer;

        GlxConfig(String peerCfg, String serverCfg, boolean hasZK, boolean hasServer) {
            this.peerCfg = peerCfg;
            this.serverCfg = serverCfg;
            this.hasZK = hasZK;
            this.hasServer = hasServer;
        }
    }
}

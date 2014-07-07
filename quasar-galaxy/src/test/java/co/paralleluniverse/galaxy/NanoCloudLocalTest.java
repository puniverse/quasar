package co.paralleluniverse.galaxy;

import co.paralleluniverse.galaxy.example.pingpong.Ping;
import co.paralleluniverse.galaxy.example.pingpong.Pong;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static co.paralleluniverse.galaxy.testing.GalaxyTestingUtils.*;


public class NanoCloudLocalTest extends BaseCloudTest {

    @Before
    public void setUp() throws InterruptedException {
        cloud = createLocalCloud();
        
    }

//    @Test
    public void pingPongTest() throws InterruptedException, ExecutionException {
        cloud.nodes("ping", "pong");
        setJvmArgs(cloud);
        cloud.node("ping").submit(new Runnable() {
            @Override
            public void run() {
                Ping.runPing();
            }
        });
        int pings = cloud.node("pong").submit(new Callable<Integer>() {
            @Override
            public Integer call() {
                return Pong.runPong();
            }
        }).get();
        assertEquals("Number of pings received by pong", 3, pings);
    }

    private static void setJvmArgs(final ViManager cloud) {
        String[] copyEnv = {
            "jgroups.bind_addr",
            "galaxy.multicast.address",
            "galaxy.multicast.port",
            "co.paralleluniverse.galaxy.configFile",
            "co.paralleluniverse.galaxy.autoGoOnline"};
        JvmProps props = JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + System.getProperty("co.paralleluniverse.quasarJar"));
        for (String string : copyEnv)
            props = props.addJvmArg("-D" + string + "=" + System.getProperty(string));
        // check why setEnv doesn't work
    }

}

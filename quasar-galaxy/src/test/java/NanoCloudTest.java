
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.example.pingpong.Ping;
import co.paralleluniverse.galaxy.example.pingpong.Pong;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.SuspendableCallable;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.ViManager;
import org.gridkit.vicluster.ViNode;
import org.gridkit.vicluster.ViProps;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.gridkit.vicluster.telecontrol.ssh.RemoteNodeProps;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author eitan
 */
public class NanoCloudTest extends BaseCloudTest {

    public static ViManager createLocalCloud() {
        ViManager vim = CloudFactory.createCloud();
        // this will configure "local" vi-node type by default
        ViProps.at(vim.node("**")).setLocalType();
        return vim;
    }

//    @Test
    public void test_distributed_hello_world__basic_example() throws InterruptedException {
        cloud = CloudFactory.createSimpleSshCloud();
        cloud.node("localhost");
        String cachePath = "/tmp/cache";
        RemoteNodeProps.at(cloud.node("**")).setRemoteJavaExec("java").setRemoteJarCachePath(cachePath);
        JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + cachePath + File.separatorChar + getRemotePathToJar("jatest"));

        cloud.node("**").touch();
        cloud.node("**").exec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello! file ");
                return null;
            }
        });
        Thread.sleep(300);
    }

    public static String getRemotePathToJar(final String partOfJarName) {
        for (Classpath.ClasspathEntry cpe : Classpath.getClasspath(ClassLoader.getSystemClassLoader()))
            if (cpe.getFileName().contains(partOfJarName))
                return File.separatorChar + cpe.getContentHash() + File.separatorChar + cpe.getFileName();
        throw new RuntimeException(partOfJarName + " not found in classpath");
    }

//    @Test
    public void test_hello_world__version1() throws InterruptedException, ExecutionException {
        cloud = createLocalCloud();

        String[] nodes = {"node1", "node2", "node3", "node4"};
        cloud.nodes(nodes);
        String jarPath = System.getProperty("co.paralleluniverse.quasarJar");
//        System.out.println("XXXXX local jar path: " + jarPath);
        JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + jarPath);
        warmUp(cloud);

        Thread.sleep(300);

        ArrayList<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++)
            futures.add(cloud.node(nodes[i]).submit(sleepAndReturnFromFiber(100, i)));
        int i = 0;
        for (Future<Integer> future : futures)
            assertEquals(i++, (int) future.get());
    }

    @Test
    public void pingPongTest() throws InterruptedException, ExecutionException {
        cloud = createLocalCloud();
        cloud.nodes("ping", "pong");
        String jarPath = System.getProperty("co.paralleluniverse.quasarJar");
        String[] copyEnv = {
            "jgroups.bind_addr",
            "galaxy.multicast.address",
            "galaxy.multicast.port",
            "co.paralleluniverse.galaxy.configFile",
            "co.paralleluniverse.galaxy.autoGoOnline"};
        JvmProps props = JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + jarPath);
        for (String string : copyEnv)
            props = props.addJvmArg("-D"+string+"="+System.getProperty(string));
        
        // check why setEnv doesn't work

        warmUp(cloud);

        Thread.sleep(300);
        Future<Void> ping = cloud.node("ping").submit(new Runnable() {
            @Override
            public void run() {
                try {
                Set<Map.Entry<Object, Object>> props = System.getProperties().entrySet();
                    for (Map.Entry<Object, Object> prop : props) 
                        System.out.println("prop "+prop.getKey()+": "+prop.getValue());
                    Ping.main(new String[]{});
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        Future<Void> pong = cloud.node("pong").submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Pong.main(new String[]{});
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        ping.get();
        pong.get();
    }

    private Callable<Integer> sleepAndReturnFromFiber(final int millis, final int value) {
        return new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                return new Fiber<>(new SuspendableCallable<Integer>() {

                    @Override
                    public Integer run() throws SuspendExecution, InterruptedException {
                        Strand.sleep(millis);
                        return value;
                    }
                }).start().get();
            }

        };
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LocalActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Grid;
import co.paralleluniverse.galaxy.Store;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.strands.Strand;
import com.google.common.primitives.Longs;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 *
 * @author eitan
 */
public class PeerMain {
    int i;
    Grid grid;

    public PeerMain(final int i) throws InterruptedException {
        System.setProperty("co.paralleluniverse.io.useJDKSerialization", "true");
        System.out.println("STARTING PEER " + i);
        final URL peerXml = PeerMain.class.getClassLoader().getResource("config/peer.xml");
        final URL serverProps = PeerMain.class.getClassLoader().getResource("config/server.properties");
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(serverProps.getPath()));
        } catch (IOException ex) {
            System.out.println("error: " + ex);
        }
        props.setProperty("galaxy.nodeId", Integer.toString(i));
        props.setProperty("galaxy.port", Integer.toString(7050 + i));
        props.setProperty("galaxy.slave_port", Integer.toString(8050 + i));
        props.setProperty("galaxy.multicast.address", "225.0.0.1");
        props.setProperty("galaxy.multicast.port", Integer.toString(7050));
        System.out.println(props);
        grid = co.paralleluniverse.galaxy.Grid.getInstance(peerXml.getPath(), props);
        grid.goOnline();
        this.i = i;
    }

    public void start() throws InterruptedException {
        if (i == 1) {
            Store store = grid.store();

            long root = -1;
            System.out.println("Getting root");
            
            while (root == -1) {
                StoreTransaction txn = store.beginTransaction();
                try {
                    root = store.getRoot("root1", txn);
                    if (store.isRootCreated(root, txn)) {
                        System.out.println("Created root!");
                        store.set(root, Longs.toByteArray(1000), txn);
                    }
                    store.commit(txn);
                } catch (TimeoutException e) {
                    System.out.println("OOPS. Timeout.");
                    store.rollback(txn);
                    store.abort(txn);
                }
            }
            System.out.println("Root is " + Long.toHexString(root));



            new Fiber<Void>(new BasicActor<String, Void>() {
                @Override
                protected Void doRun() throws InterruptedException, SuspendExecution {
                    System.out.println("registering");
                    register("master");
                    System.out.println("registered");
                    String msg = null;
                    while ((msg = receive()) != null) {
                        System.out.println("got msg: " + msg);
                    }
                    return null;
                }
            }).start();
        } else {
            new Fiber<Void>(new BasicActor<String, Void>() {
                @Override
                protected Void doRun() throws InterruptedException, SuspendExecution {
                    System.out.println("getting actor");
                    Actor<String> master = getActor("master");
                    System.out.println("actor is " + master);
                    while (true) {
                        System.out.println("sending msg to master");
                        master.send("message from actor " + i);
                        Strand.sleep(3000);
                    }
                }
            }).start();
        }
    }
}

/*
 * Galaxy
 * Copyright (C) 2012 Parallel Universe Software Co.
 * 
 * This file is part of Galaxy.
 *
 * Galaxy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * Galaxy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Galaxy. If not, see <http://www.gnu.org/licenses/>.
 */
package co.paralleluniverse.galaxy.example;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.Cluster;
import co.paralleluniverse.galaxy.Grid;
import co.paralleluniverse.galaxy.MessageListener;
import co.paralleluniverse.galaxy.Messenger;
import co.paralleluniverse.galaxy.Server;
import co.paralleluniverse.galaxy.Store;
import co.paralleluniverse.galaxy.StoreTransaction;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.cluster.LifecycleListener;
import co.paralleluniverse.galaxy.cluster.NodeChangeListener;
import co.paralleluniverse.galaxy.core.Comm;
import co.paralleluniverse.io.serialization.Serialization;
import co.paralleluniverse.strands.channels.QueueChannel;
import co.paralleluniverse.strands.channels.QueueObjectChannel;
import co.paralleluniverse.strands.channels.SendPort;
import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pron
 */
public class PeerTKB implements Runnable {
    private final Grid grid;
    private final Cluster cluster;
    private final Store store;
    private final Messenger messenger;
    private final short myNodeId;
    private final Random random = new Random();
    private final boolean hasServer;
    private volatile Thread myThread;

    public PeerTKB(String name, int i) throws InterruptedException, IOException {
        System.setProperty("co.paralleluniverse.io.useJDKSerialization", "true");
        System.out.println("STARTING PEER " + i);
        final URL peerXml = PeerTKB.class.getClassLoader().getResource("config/peer.xml");
        final URL serverProps = PeerTKB.class.getClassLoader().getResource("config/server.properties");
        Properties props = new Properties();
        props.load(new FileInputStream(serverProps.getPath()));
        props.setProperty("galaxy.nodeId", Integer.toString(i));
        props.setProperty("galaxy.port", Integer.toString(7050 + i));
        props.setProperty("galaxy.slave_port", Integer.toString(8050 + i));
        props.setProperty("galaxy.multicast.address", "225.0.0.1");
        props.setProperty("galaxy.multicast.port", Integer.toString(7050));
        System.out.println(props);
        grid = co.paralleluniverse.galaxy.Grid.getInstance(peerXml.getPath(), props);
        grid.goOnline();

        this.cluster = grid.cluster();
        this.store = grid.store();
        this.messenger = grid.messenger();
        this.myNodeId = cluster.getMyNodeId();
        this.hasServer = cluster.hasServer();
    }

    public void start() throws InterruptedException {
        cluster.addLifecycleListener(new LifecycleListener() {
            @Override
            public void joinedCluster() {
            }

            @Override
            public void online(boolean master) {
                if (master) {
                    myThread = new Thread(PeerTKB.this);
                    myThread.start();
                }
            }

            @Override
            public void switchToMaster() {
                myThread = new Thread(PeerTKB.this);
                myThread.start();
            }

            @Override
            public void offline() {
                myThread.interrupt();
            }
        });

        if (cluster.isMaster()) {
            myThread = Thread.currentThread();
            run();
        } else
            System.out.println("SLAVE. Waiting...");
    }

    @Override
    public void run() {
        try {
            System.out.println("MASTER: start running... " + myNodeId);
            messenger.addMessageListener("ringping", new MessageListener() {
                @Override
                public void messageReceived(short fromNode, byte[] msg) {
                    System.out.println("Got actor. writing. "+msg.length);
                    
                    Actor<String> act = (Actor<String>) Serialization.read(msg);
                    try {
                        act.send("Message from " + myNodeId);
                    } catch (SuspendExecution ex) {
                    }

                    try {
                        if (!isSmallest())
                            send(nextNode(), "ringping", msg);
                    } catch (TimeoutException e) {
                        System.out.println("OOPS. Timeout.");
                    }
                }
            });

//            long root = -1;
//            System.out.println("Getting root");
//
//            while (root == -1) {
//                StoreTransaction txn = store.beginTransaction();
//                try {
//                    root = store.getRoot("root1", txn);
//                    if (store.isRootCreated(root, txn)) {
//                        System.out.println("Created root!");
//                        store.set(root, Longs.toByteArray(1000), txn);
//                    }
//                    store.commit(txn);
//                } catch (TimeoutException e) {
//                    System.out.println("OOPS. Timeout.");
//                    store.rollback(txn);
//                    store.abort(txn);
//                }
//            }
//            System.out.println("Root is " + Long.toHexString(root));
            long id = 0;
            int c = 340;
            final BasicActor<String, Void> actor = new BasicActor<String, Void>() {
                @Override
                protected Void doRun() throws InterruptedException, SuspendExecution {
                    String msg = null;
                    while ((msg = receive()) != null) {
                        System.out.println("got msg: " + msg);
                    }
                    return null;
                }
            };
            new Fiber(actor).start();
            final byte[] serActor = Serialization.write(actor);
            System.out.println("length is "+serActor.length);

            while (!Thread.interrupted()) {
                QueueChannel<String> channel = QueueObjectChannel.create(100);
                boolean sent = false;
                System.out.println("=========================================");
                System.out.println("ident: " + myNodeId + " " + System.currentTimeMillis());
                System.out.println("nodes: " + cluster.getNodes());
                final int numOtherNodes = getNumPeerNodes() - 1;
                System.out.println("numOtherNodes = " + numOtherNodes);
                if (numOtherNodes > 0) {
                    if (isSmallest()) {
                        try {
                            sent = true;
                            final short nextNode = nextNode();
                            byte[] buf = new byte[c];
                            System.out.println("Starting ringping! size "+buf.length);
                            c+=1;
                            send(nextNode, "ringping", serActor);
                        } catch (TimeoutException e) {
                            System.out.println("OOPS. Timeout.");
                        }
                    }
                }
                System.out.println("sleeping");
                Thread.sleep(1000);
//                String backMsg = null;
//                do {
//                    try {
//                        backMsg = channel.receive(10, TimeUnit.MILLISECONDS);
//                        System.out.println("Got msg in channel: " + backMsg);
//                    } catch (SuspendExecution ex) {
//                    }
//                } while (backMsg != null);
            }
        } catch (InterruptedException e) {
        }
        System.out.println("DONE!");
//        System.exit(0);
    }

    boolean isSmallest() {
        for (short node : cluster.getNodes())
            if (node > 0 && node < myNodeId)
                return false;
        return true;
    }

    short nextNode() {
        final int numNodes = getNumPeerNodes();
        if (numNodes > 1) {
            int mod = (myNodeId + 1) % numNodes;
            return (short) (mod != 0 ? mod : numNodes);
        } else
            return -1;
    }

    void send(short node, String topic, byte[] msg) throws TimeoutException {
        if (node > 0)
            messenger.send(node, topic, msg);
    }

    private int getNumPeerNodes() {
        return cluster.getNodes().size() - (cluster.getNodes().contains(Comm.SERVER) ? 1 : 0) + 1;
    }
}

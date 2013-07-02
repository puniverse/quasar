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
import co.paralleluniverse.galaxy.Messenger;
import co.paralleluniverse.galaxy.Store;
import co.paralleluniverse.galaxy.TimeoutException;
import co.paralleluniverse.galaxy.cluster.LifecycleListener;
import co.paralleluniverse.galaxy.core.Comm;
import co.paralleluniverse.strands.Strand;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Random;

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
    private final int i;

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
        this.i = i;
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
            System.out.println("MASTER: start running... " + myNodeId);
            if (i == 1) {
                new Fiber<Void>(new BasicActor<String, Void>() {
                    @Override
                    protected Void doRun() throws InterruptedException, SuspendExecution {
                        System.out.println("registering");
                        register("master");
                        System.out.println("registered");
                        Strand.sleep(2000);
                        Actor<String> master = getActor("master");
                        System.out.println("actor is " + master);
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

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
import co.paralleluniverse.actors.ActorRegistry;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.LifecycleException;
import co.paralleluniverse.actors.LifecycleMessage;
import co.paralleluniverse.common.util.Debug;
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
import co.paralleluniverse.strands.SuspendableRunnable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final int i;

    public PeerTKB(String name, int i) throws InterruptedException, IOException {
        System.setProperty("co.paralleluniverse.io.useJDKSerialization", "true");
//        System.setProperty("co.paralleluniverse.debugMode", "true");
//        System.setProperty("co.paralleluniverse.globalFlightRecorder", "true");
//        System.setProperty("co.paralleluniverse.flightRecorderDumpFile", "~/quasar.log" + i);
//        System.setProperty("co.paralleluniverse.io.useJDKSerialization", "true");
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
        try {
            if (i == 1) {
                new Fiber<Void>(new BasicActor<String, Void>() {
                    @Override
                    protected Void doRun() throws InterruptedException, SuspendExecution {
                        System.out.println("registering");
                        register("master");
                        System.out.println("registered");
//                        Strand.sleep(2000);
//                        Actor<String> master = getActor("master");
//                        System.out.println("actor is " + master);
                        String msg = null;
                        int count = 5;
                        while (--count > 0 && (msg = receive()) != null) {
                            System.out.println("got msg: " + msg);
                        }
                        System.out.println("I'm here1");
                        System.exit(0);
                        return null;
                    }
                }).start().join();
                System.out.println("I'm here2");


            } else {
//                final Actor actor = ActorRegistry.getActor("master");
//                final AtomicInteger ai = new AtomicInteger();
//                for (int j = 0; j < 10; j++) {
//                    new Fiber<>(new BasicActor<Void, Void>() {
//                        @Override
//                        protected Void doRun() throws InterruptedException, SuspendExecution {
//                            ai.incrementAndGet();
//                            Object watch = watch(actor);
////                            unwatch(actor, watch);
//                            return null;
//                        }
//                    }).start().join();
////                    int val = ai.get();
////                    if (val % 1000 == 0)
////                        System.out.println("Till now actors " + val);
//                    try {
//                        Strand.sleep(5);
//                    } catch (SuspendExecution | InterruptedException ex) {
//                        Logger.getLogger(PeerTKB.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                System.gc();
//                Thread.sleep(50000);
//                System.out.println("ai is" + ai.get());
                new Fiber<Void>(new BasicActor<String, Void>() {
                    private boolean masterIsAlive;

                    @Override
                    protected Void doRun() throws InterruptedException, SuspendExecution {
                        System.out.println("getting actor");
                        Actor<String> master = getActor("master");
                        masterIsAlive = true;
                        System.out.println("actor is " + master);
                        watch(master);
                        int c = 15;
                        while (masterIsAlive) {
                            System.out.println("sending msg to master");
                            master.send("message from actor " + i);
                            Strand.sleep(3000);
                            String tryReceive = tryReceive();
                            System.out.println("tryReceive " + tryReceive);
                        }
                        System.out.println("master is Alive = " + masterIsAlive);
                        return null;
                    }

                    @Override
                    protected void handleLifecycleMessage(LifecycleMessage m) {
                        masterIsAlive = false;
                    }
                }).start().join();
                System.out.println("starting second actor");
                new Fiber<Void>(new BasicActor<String, Void>() {
                    private boolean masterIsAlive;

                    @Override
                    protected Void doRun() throws InterruptedException, SuspendExecution {
                        System.out.println("getting actor");
                        Actor<String> master = getActor("master");
                        masterIsAlive = true;
                        watch(master);
                        tryReceive();
                        while (masterIsAlive) {
                            System.out.println("waiting for dead meassage");
                            Strand.sleep(3000);
                            tryReceive();
                        }
                        System.out.println("master is Alive = " + masterIsAlive);
                        return null;
                    }

                    @Override
                    protected void handleLifecycleMessage(LifecycleMessage m) {
                        masterIsAlive = false;
                    }
                }).start().join();
            }
        } catch (ExecutionException | InterruptedException ex) {
        }
        Debug.dumpRecorder();

        System.exit(0);
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

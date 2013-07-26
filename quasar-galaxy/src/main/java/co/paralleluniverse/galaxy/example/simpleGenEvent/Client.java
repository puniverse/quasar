/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.simpleGenEvent;

import co.paralleluniverse.galaxy.example.simpleGenServer.*;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.MailboxConfig;
import co.paralleluniverse.actors.behaviors.GenEvent;
import co.paralleluniverse.actors.behaviors.GenServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.example.testing.PeerTKB;
import co.paralleluniverse.galaxy.example.pingpong.*;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.channels.Channels;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class Client {
    private static final int nodeId = 1;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));
        new Fiber<>(new BasicActor<String, Void>() {
            @Override
            protected Void doRun() throws SuspendExecution, InterruptedException {
                GenEvent<String> ge;
                while ((ge = (GenEvent<String>) getActor("myEventServer")) == null) {
                    System.out.println("waiting for myEventServer");
                    Strand.sleep(3000);
                }
                ge.notify("hello world");
                return null;
            }

        }).start().join();
        System.out.println("client finished. ");
        Thread.sleep(500);
        System.exit(0);
    }
}

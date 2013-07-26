/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.simpleGenServer;

import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.MailboxConfig;
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

        Integer res = new Fiber<>(new BasicActor<SumRequest, Integer>(new MailboxConfig(10, Channels.OverflowPolicy.THROW)) {
            protected Integer doRun() throws SuspendExecution, InterruptedException {
                GenServer<SumRequest, Integer, SumRequest> gs;
                while ((gs = (GenServer) getActor("myServer")) == null) {
                    System.out.println("waiting for myServer");
                    Strand.sleep(3000);                    
                }
                final Integer call = gs.call(new SumRequest(3, 4));
                gs.call(new SumRequest(0, 0)); // cause the server shutdown
                return call;
            }
        }).start().get();

        System.out.println("client finished. result is: " + res);
        System.exit(0);
    }
}

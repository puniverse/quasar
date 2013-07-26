/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.simpleGenServer;

import co.paralleluniverse.galaxy.example.pingpong.*;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.AbstractServer;
import co.paralleluniverse.actors.behaviors.LocalGenServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.example.testing.PeerTKB;
import com.sleepycat.je.rep.elections.Protocol;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class Server {
    private static final int nodeId = 2;
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.setProperty("galaxy.nodeId", Integer.toString(nodeId));
        System.setProperty("galaxy.port", Integer.toString(7050 + nodeId));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + nodeId));

        new Fiber(new LocalGenServer(new AbstractServer<SumRequest, Integer, SumRequest>() {
            @Override
            public void init() throws SuspendExecution {
                super.init();
                LocalGenServer.currentGenServer().register("myServer");
                System.out.println(this.toString()+" is ready");
            }

            @Override
            public Integer handleCall(Actor<Integer> from, Object id, SumRequest m) {
                System.out.println(this.toString()+" is handling "+m);
                if (m.a == 0 && m.b == 0)
                    LocalGenServer.currentGenServer().shutdown();
                return m.a + m.b;
            }
        })).start().join();        
        System.exit(0);
    }
}
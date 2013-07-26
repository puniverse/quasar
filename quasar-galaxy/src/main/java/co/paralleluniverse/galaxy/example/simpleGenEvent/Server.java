/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.simpleGenEvent;

import co.paralleluniverse.galaxy.example.simpleGenServer.*;
import co.paralleluniverse.galaxy.example.pingpong.*;
import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.actors.behaviors.AbstractServer;
import co.paralleluniverse.actors.behaviors.EventHandler;
import co.paralleluniverse.actors.behaviors.Initializer;
import co.paralleluniverse.actors.behaviors.LocalGenEvent;
import co.paralleluniverse.actors.behaviors.LocalGenServer;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.example.testing.PeerTKB;
import co.paralleluniverse.strands.channels.DelayedVal;
import com.sleepycat.je.rep.elections.Protocol;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        final DelayedVal<String> dv = new DelayedVal<>();
        new Fiber(new LocalGenEvent<>(new Initializer() {
            @Override
            public void init() throws SuspendExecution {
                LocalGenEvent.currentGenEvent().register("myEventServer");
                LocalGenEvent<String> ge = LocalGenEvent.currentGenEvent();
                try {
                    ge.addHandler(new EventHandler<String>() {
                        @Override
                        public void handleEvent(String event) {
                            System.out.println("Handling event: "+event);
                            LocalGenEvent.currentGenEvent().shutdown();                            
                        }
                    });
                } catch (InterruptedException ex) {
                    System.out.println("error "+ex);
                }
            }

            @Override
            public void terminate(Throwable cause) throws SuspendExecution {
                System.out.println("server terminated");
            }
        })).start().join();

        System.exit(0);
    }
}
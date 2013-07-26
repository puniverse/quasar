/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.pingpong;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class Ping {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final int i = 1;
        System.setProperty("galaxy.nodeId", Integer.toString(i));
        System.setProperty("galaxy.port", Integer.toString(7050 + i));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + i));

        new Fiber(new BasicActor<RepliableMessage<String>, Void>() {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                Actor pong;
                while ((pong = getActor("pong")) == null) {
                    System.out.println("waiting for pong");
                    Strand.sleep(3000);
                }
                System.out.println("pong is " + pong);
                for (int i=0; i<3; i++) {
                    pong.send(new RepliableMessage("ping", this));
                    RepliableMessage<String> msg = receive();
                    System.out.println("ping received " + msg.data);
                }
                pong.send(new RepliableMessage("finished", null));
                return null;
            }
        }).start().join();
        System.out.println("finished ping");
        System.exit(0);
    }
}

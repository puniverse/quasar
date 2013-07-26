/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.galaxy.example.pingpong;

import co.paralleluniverse.actors.Actor;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.galaxy.example.testing.PeerTKB;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author eitan
 */
public class Pong {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final int i = 2;
        System.setProperty("galaxy.nodeId", Integer.toString(i));
        System.setProperty("galaxy.port", Integer.toString(7050 + i));
        System.setProperty("galaxy.slave_port", Integer.toString(8050 + i));

        new Fiber(new BasicActor<RepliableMessage<String>, Void>() {
            @Override
            protected Void doRun() throws InterruptedException, SuspendExecution {
                register("pong");
                while (true) {
                    RepliableMessage<String> msg = receive();
                    if (msg.data.equals("finished"))
                        break;
                    if (msg.data.equals("ping"))
                        msg.sender.send(new RepliableMessage("pong", this));
                }
                return null;
            }
        }).start().join();
        System.out.println("finished pong");
        System.exit(0);
    }
}

package co.paralleluniverse.galaxy.example;

import java.io.IOException;

/**
 *
 * @author pron
 */
public class Peer {
    public static void main(String[] args) {
        if (args.length == 1)
            try {
                new PeerTKB("grid1", Integer.parseInt(args[0])).start();
            } catch (InterruptedException | IOException ex) {
                System.out.println("usage: Peer nodeID. " + ex);
            }
        else
            System.out.println("usage: Peer nodeID");
    }
}

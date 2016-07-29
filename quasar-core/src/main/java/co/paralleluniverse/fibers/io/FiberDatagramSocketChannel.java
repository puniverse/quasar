package com.proxy4o.fiber.udp;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.proxy4o.utils.FiberExecutors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * Created by linkerlin on 6/5/16.
 */
public class FiberDatagramSocketChannel implements Closeable {
    public static Logger log = LogManager.getLogger(FiberUDP.class.getName());

    private DatagramSocket ds;

    public FiberDatagramSocketChannel() {
    }

    @Suspendable
    public void bind(SocketAddress addr) throws SocketException {
        ds=new DatagramSocket(addr);
        if(!ds.isBound())
            ds.bind(addr);
    }
    @Suspendable
    public void bind() throws SocketException {
        this.bind(null);
    }

    @Suspendable
    public DatagramPacket send(DatagramPacket dp) throws InterruptedException, SuspendExecution, IOException {
        return FiberExecutors.fiberSubmit(() -> {
            synchronized (ds) {
                ds.send(dp);
            }
            return dp;
        }, IOException.class);
    }


    @Suspendable
    public DatagramPacket receive() throws InterruptedException, SuspendExecution, IOException {
        return FiberExecutors.fiberSubmit(() -> {
            synchronized (ds) {
                byte[] b = new byte[1500];// for most erthnets
                DatagramPacket dp = new DatagramPacket(b, b.length);
                ds.receive(dp);
                return dp;
            }
        }, IOException.class);
    }

    @Override
    public void close() throws IOException {
        if (ds != null) {
            ds.close();
        }
    }
}

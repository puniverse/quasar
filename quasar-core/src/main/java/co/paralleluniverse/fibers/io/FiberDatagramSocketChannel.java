package com.proxy4o.fiber.udp;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.proxy4o.utils.FiberExecutors;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.*;

/**
 * Created by linkerlin on 6/5/16.
 */
public class FiberDatagramSocketChannel implements Closeable {
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
    public DatagramPacket send(final DatagramPacket dp) throws InterruptedException, SuspendExecution, IOException {
        return FiberExecutors.fiberSubmit(new Callable<DatagramPacket>() {
            @Override
            public DatagramPacket call() throws IOException {
                synchronized (ds) {
                    ds.send(dp);
                }
                return dp;
            }
        }, IOException.class);
    }


    @Suspendable
    public DatagramPacket receive() throws InterruptedException, SuspendExecution, IOException {
        return FiberExecutors.fiberSubmit(new Callable<DatagramPacket>() {
            @Override
            public DatagramPacket call() throws IOException {
                synchronized (ds) {
                    byte[] b = new byte[1500];
                    DatagramPacket dp = new DatagramPacket(b, b.length);
                    ds.receive(dp);
                    return dp;
                }
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

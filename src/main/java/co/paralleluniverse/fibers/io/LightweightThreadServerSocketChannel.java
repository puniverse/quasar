/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.fibers.io;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.util.Set;

/**
 *
 * @author pron
 */
public class LightweightThreadServerSocketChannel implements NetworkChannel {
    private final AsynchronousServerSocketChannel ac;

    private LightweightThreadServerSocketChannel(AsynchronousServerSocketChannel assc) {
        this.ac = assc;
    }

    public static LightweightThreadServerSocketChannel open() throws IOException {
        return new LightweightThreadServerSocketChannel(AsynchronousServerSocketChannel.open());
    }

    public LightweightThreadSocketChannel accept() throws IOException, SuspendExecution {
        return new LightweightThreadSocketChannel(new LightweightThreadAsyncIO<AsynchronousSocketChannel>() {
            @Override
            protected void requestAsync(Fiber current, CompletionHandler<AsynchronousSocketChannel, Fiber> completionHandler) {
                ac.accept(current, completionHandler);
            }
        }.run());
    }

    @Override
    public boolean isOpen() {
        return ac.isOpen();
    }

    @Override
    public void close() throws IOException {
        ac.close();
    }

    @Override
    public LightweightThreadServerSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    public LightweightThreadServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        ac.bind(local, backlog);
        return this;
    }

    @Override
    public <T> LightweightThreadServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        ac.setOption(name, value);
        return this;
    }

    @Override
    public SocketAddress getLocalAddress() throws IOException {
        return ac.getLocalAddress();
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return ac.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return ac.supportedOptions();
    }
}

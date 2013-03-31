/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NetworkChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author pron
 */
public class LightweightThreadSocketChannel implements LightweightThreadByteChannel, NetworkChannel {
    private final AsynchronousSocketChannel ac;

    public LightweightThreadSocketChannel(AsynchronousSocketChannel asc) {
        this.ac = asc;
    }

    public static LightweightThreadSocketChannel open() throws IOException {
        return new LightweightThreadSocketChannel(AsynchronousSocketChannel.open());
    }

    public static LightweightThreadSocketChannel open(SocketAddress remote) throws IOException {
        return new LightweightThreadSocketChannel(AsynchronousSocketChannel.open());
    }

    public void connect(final SocketAddress remote) throws IOException, SuspendExecution {
        new LightweightThreadAsyncIO<Void>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Void, LightweightThread> completionHandler) {
                ac.connect(remote, current, completionHandler);
            }
        }.run();
    }

    public long read(final ByteBuffer[] dsts, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Long>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Long, LightweightThread> completionHandler) {
                ac.read(dsts, offset, length, timeout, unit, current, completionHandler);
            }
        }.run();
    }

    public int read(final ByteBuffer dst, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                ac.read(dst, timeout, unit, current, completionHandler);
            }
        }.run();
    }

    public long write(final ByteBuffer[] srcs, final int offset, final int length, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Long>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Long, LightweightThread> completionHandler) {
                ac.write(srcs, offset, length, timeout, unit, current, completionHandler);
            }
        }.run();
    }

    public int write(final ByteBuffer src, final long timeout, final TimeUnit unit) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {
            @Override
            protected void requestAsync(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                ac.write(src, timeout, unit, current, completionHandler);
            }
        }.run();
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException, SuspendExecution {
        return read(dsts, offset, length, 0L, TimeUnit.MILLISECONDS);
    }

    public long read(ByteBuffer[] dsts) throws IOException, SuspendExecution {
        return read(dsts, 0, dsts.length);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException, SuspendExecution {
        return read(dst, 0L, TimeUnit.MILLISECONDS);
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException, SuspendExecution {
        return write(srcs, offset, length, 0L, TimeUnit.MILLISECONDS);
    }

    public long write(ByteBuffer[] srcs) throws IOException, SuspendExecution {
        return write(srcs, 0, srcs.length);
    }

    @Override
    public int write(final ByteBuffer src) throws IOException, SuspendExecution {
        return write(src, 0L, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isOpen() {
        return ac.isOpen();
    }

    //@Override
    @Override
    public void close() throws IOException {
        ac.close();
    }

    public LightweightThreadSocketChannel shutdownInput() throws IOException {
        ac.shutdownInput();
        return this;
    }

    public LightweightThreadSocketChannel shutdownOutput() throws IOException {
        ac.shutdownOutput();
        return this;
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return ac.getRemoteAddress();
    }

    public final AsynchronousChannelProvider provider() {
        return ac.provider();
    }

    @Override
    public LightweightThreadSocketChannel bind(SocketAddress local) throws IOException {
        ac.bind(local);
        return this;
    }

    @Override
    public <T> LightweightThreadSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
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

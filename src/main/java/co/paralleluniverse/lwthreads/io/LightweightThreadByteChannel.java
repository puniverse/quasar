/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.LightweightThread;
import co.paralleluniverse.lwthreads.SuspendExecution;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ByteChannel;
import java.nio.channels.CompletionHandler;

/**
 *
 * @author pron
 */
public class LightweightThreadByteChannel /*implements ByteChannel*/ {
    private final AsynchronousByteChannel abc;

    public LightweightThreadByteChannel(AsynchronousByteChannel abc) {
        this.abc = abc;
    }

    //@Override
    public boolean isOpen() {
        return abc.isOpen();
    }

    //@Override
    public void close() throws IOException {
        abc.close();
    }

    //@Override
    public int read(final ByteBuffer dst) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {

            @Override
            void callIO(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                abc.read(dst, current, completionHandler);
            }
            
        }.run();
    }

    //@Override
    public int write(final ByteBuffer src) throws IOException, SuspendExecution {
        return new LightweightThreadAsyncIO<Integer>() {

            @Override
            void callIO(LightweightThread current, CompletionHandler<Integer, LightweightThread> completionHandler) {
                abc.write(src, current, completionHandler);
            }
            
        }.run();
    }
}

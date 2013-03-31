/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.lwthreads.io;

import co.paralleluniverse.lwthreads.SuspendExecution;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;

/**
 *
 * @author pron
 */
public interface LightweightThreadByteChannel extends Channel /*ByteChannel*/ {

    int read(final ByteBuffer dst) throws IOException, SuspendExecution;
    int write(final ByteBuffer src) throws IOException, SuspendExecution;
}

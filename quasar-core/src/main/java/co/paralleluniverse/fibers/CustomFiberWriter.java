package co.paralleluniverse.fibers;

/**
 * A callback used by {@link Fiber#parkAndCustomSerialize(CustomFiberWriter)}.
 *
 * @author Christian Sailer (christian.sailer@r3.com)
 */
public interface CustomFiberWriter {
    void write(Fiber fiber);
}

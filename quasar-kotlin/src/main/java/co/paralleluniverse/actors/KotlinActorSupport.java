package co.paralleluniverse.actors;

/**
 * @author circlespainter
 */
public abstract class KotlinActorSupport<Message, V> extends BasicActor<Message, V> {
    // Needed to get access to this package-level facility in Kotlin's inlines
    protected void checkThrownIn1() {
        checkThrownIn0();
    }
}

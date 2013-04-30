/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.actors;

import java.util.concurrent.ConcurrentMap;
import jsr166e.ConcurrentHashMapV8;

/**
 *
 * @author pron
 */
public class ActorRegistry {
    // TODO: logs
    private static final ConcurrentMap<Object, Actor> registeredActors = new ConcurrentHashMapV8<Object, Actor>();
    private static final ConcurrentMap<Object, ActorMonitor> registeredActorMonitors = new ConcurrentHashMapV8<Object, ActorMonitor>();

    public static ActorMonitor register(Object name, Actor actor) {
        if (name == null)
            throw new IllegalArgumentException("name is null");

        // atomically register
        final Actor old = registeredActors.get(name);
        if (old != null && !old.isDone())
            throw new RuntimeException("Actor " + old + " is not dead and is already registered under " + name);
        if (!registeredActors.remove(name, old))
            throw new RuntimeException("Concurrent registration under the name " + name);
        if (registeredActors.putIfAbsent(name, actor) != null)
            throw new RuntimeException("Concurrent registration under the name " + name);

        ActorMonitor monitor = registeredActorMonitors.get(name);
        if (monitor == null) {
            monitor = Actor.newActorMonitor(name.toString());
            registeredActorMonitors.put(name, monitor);
        }
        
        monitor.setActor(actor);
        return monitor;
    }

    public static void unregister(Object name) {
        registeredActorMonitors.get(name).setActor(null);
        registeredActors.remove(name);
    }

    public static Actor getActor(Object name) {
        return registeredActors.get(name);
    }
}

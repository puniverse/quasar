---
layout: default
title: Quasar's Actor System
weight: 2
---

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

To use the terms we've learned so far, an *actor* is a strand that owns a single channel with some added lifecyce management and error handling. But this reductionist view of actors does them little justice. Actors are fundamental building blocks that are combined to build a fault-tolerant application. If you are familiar with Erlang, Quasar actors are just like Erlang processes.

An actor is a self-contained execution unit with well-defined inputs and outputs. Actors communicate with other actors (as well as regular program threads and fibers) by passing messages.

{:.alert .alert-info}
**Note**: Actors may write to and read from channels other than their own mailbox. In fact, actors can do whatever regular fibers can.

### Creating Actors

All actors extends the [`Actor`]({{javadoc}}/actors/Actor.html) class. The constructor takes the actor's name (which does not have to be unique, and may even be `null`), and its mailbox settings (of type [`MailboxConfig`]({{javadoc}}/actors/MailboxConfig.html)).

`MailboxConfig` defines the mailbox size (the number of messages that can wait in the mailbox channel), with `-1` specifying an unbounded mailbox, and an *overflow policy*. The overflow policy works the same as for plain channels, except that the `THROW` policy doesn't cause an exception to be thrown in the sender if the mailbox capacity is exceeded, but rather throws an exception into the receiving actor (the exception will be thrown when the actor next blocks on a `recieve`).

An actor is required to implement the [`doRun`]({{javadoc}}/actors/Actor.html#doRun()) method. This method is the actor body, and is run when the actor is spawned.

It is prefereable to subclass [`BasicActor`]({{javadoc}}/actors/BasicActor.html) rather than `Actor`; `BasicActor` provides the ability to perform selective receives (more on that later).

### Spawning Actors

Actors can run in any strand – fiber or thread, although you'd usually want to run them in fibers. `Actor` implements `SuspendableCallable` so you may run it by setting it as the target of a fiber or a thread (via `Strand.toRunnable(SuspendableCallable))`). A simpler way to start an actor is by calling

~~~ java
actor.spawn()
~~~

which assigns the actor to a newly created fiber and starts it, or

~~~ java
actor.spawnThread()
~~~

which assigns the actor to a newly created thread and starts.

An actor can be `join`ed, just like a fiber. Calling `get` on an actor will join it and return the value returned by `doRun`.

{:.alert .alert-info}
**Note**: Just like fibers, spawning an actor is a very cheap operation in both computation and memory. Do not fear creating many (thousands, tens-of-thousands or even hundereds-of-thousands) actors.

## Sending and Receiving Messages, `ActorRef`

The `spawn` method returns an instance of [`ActorRef`]({{javadoc}}/actors/ActorRef.html). All (almost) interactions with an actor take place through its `ActorRef`, which can also be obtained by calling `ref()` on the actor. The `ActorRef` is used as a level of indirection that provides additional isolation for the actor (and actors are all about isolation). It enables things like hot code swapping and more.

[`ActorRef.self()`]({{javadoc}}/actors/ActorRef.html#self()) is a static function that returns the currently executing actor's ref, and [`Actor.self()`]({{javadoc}}/actors/Actor.html#self()) is a protected member function that returns an actor's ref. Use them to obtain and share an actor's ref with other actors.

{:.alert .alert-warn}
**Note**: An actor must *never* pass a direct reference to itself to other actors or to be used on other strands. However, it may share its `ActorRef` freely.

The `ActorRef` allows sending messages to the actor's mailbox. In fact, `ActorRef` implements `SendPort` so it can be used just like a channel.

An actor receives a message by calling the [`recieve`]({{javadoc}}/actors//Actor.html#receive()) method. The method blocks until a message is available in the mailbox, and then returns it. [Another version]({{javadoc}}/actors//Actor.html#receive(long, java.util.concurrent.TimeUnit)) of `receive` blocks up to a given duration, and returns `null` if no message is received by that time.

Normally, an actor is implements a loop similar to this one:

~~~ java
@Override
protected Void doRun() {
    for(;;) {
        Object msg = receive();
        // process message
        if (thatsIt())
            break;
    }
    return null;
}
~~~

### Actors vs. Channels

One of the reasons of providing a different `receive` function for actors is because programming with actors is conceptually different from just using fibers and channels. I think of channels as hoses pumping data into a function, or as sort of like asynchronous parameters. A fiber may pull many different kinds of data from many different channels, and combine the data in some way. 

Actors are a different abstraction. They are more like objects in object-oriented languages, assigned to a single thread. The mailbox serves as the object's dispatch mechanism; it's not a hose but a switchboard. It's for this reason that actors often need to pattern-match their mailbox messages, while regular channels – each usually serving as a conduit for a single kind of data – don't.

### Selective Receive

An actor is a state machine. It usually encompasses some *state* and the messages it receives trigger *state transitions*. But because the actor has no control over which messages it receives and when (which can be a result of either other actors' behavior, or even the way the OS schedules threads), an actor would be required to process any message and any state, and build a full *state transition matrix*, namely how to transition whenever *any* messages is received at *any* state.

This can not only lead to code explosion; it can lead to bugs. The key to managing a complex state machine is by not handling messages in the order they arrive, but in the order we wish to process them. If your actor extends [`BasicActor`]({{javadoc}}/actors/BasicActor.html), there's [another form]({{javadoc}}/actors//BasicActor.html#receive(co.paralleluniverse.actors.MessageProcessor)) of the `receive` method that allows for *selective receive*. This method takes an instance of [`MessageProcessor`]({{javadoc}}/actors/MessageProcessor.html), which *selects* messages out of the mailbox (a message is selected iff `MessageProcessor.process` returns a non-null value when it is passed the message). 

Let's look at an example. Suppose we have this message class:

~~~ java
class ComplexMessage {
    enum Type { FOO, BAR, BAZ, WAT }
    final Type type;
    final int num;
    public ComplexMessage(Type type, int num) {
        this.type = type;
        this.num = num;
    }
}
~~~

Then, this call:

~~~ java
ComplexMessage m = receive(new MessageProcessor<ComplexMessage, ComplexMessage>() {
        public ComplexMessage process(ComplexMessage m) throws SuspendExecution, InterruptedException {
            switch (m.type) {
            case FOO:
            case BAR:
                return m;
            default:
                return null;
            }
        }
    });
~~~

will only return a message whose `type` value is `FOO` or `BAR`, but not `BAZ`. If a message of type `BAZ` is found in the mailbox, it
will remain there and be skipped, until it is selcted by a subsequent call to `receive` (selective or plain).

`MessageProcessor.process` can also process the message inline (rather than have it processed by the caller to `receive`), and even call a nested `receive:

~~~ java
protected List<Integer> doRun() throws SuspendExecution, InterruptedException {
    final List<Integer> list = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
        receive(new MessageProcessor<ComplexMessage, ComplexMessage>() {
            public ComplexMessage process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                switch (m.type) {
                case FOO:
                    list.add(m.num);
                    receive(new MessageProcessor<ComplexMessage, ComplexMessage>() {
                        public ComplexMessage process(ComplexMessage m) throws SuspendExecution, InterruptedException {
                            switch (m.type) {
                            case BAZ:
                                list.add(m.num);
                                return m;
                            default:
                                return null;
                            }
                        }
                    });
                    return m;
                case BAR:
                    list.add(m.num);
                    return m;
                default:
                    return null;
                }
            }
        });
    }
    return list;
}
~~~

If a `FOO` is received first, then the next `BAZ` will be added to the list following the `FOO`, even if a `BAR` is found in the mailbox after the `FOO`, because the nested receive in the `case FOO:` clause selects only a `BAZ` message.

{:.alert .alert-info}
**Note**: `MessageProcessor` will become much less cumbersome in Java 8 with the introduction of lambda expressions.

There are several actor systems that do not support selective receive, but Erlang does, and so does Quasar. [The talk *Death by Accidental Complexity*](http://www.infoq.com/presentations/Death-by-Accidental-Complexity), by Ulf Wiger, shows how using selective receive avoids implementing a full, complicated and error-prone transition matrix. [In a different talk](http://www.infoq.com/presentations/1000-Year-old-Design-Patterns), Wiger compared non-selective (FIFO) receive to a tetris game where you must fit each piece into the puzzle as it comes, while selective receive turns the problem into a jigsaw puzzle, where you can look for a piece that you know will fit.

{:.alert .alert-warn}
**A word of caution**: Using selective receive in your code may lead to deadlocks (because you're essentially saying, I'm going to wait here until a specific message arrives). This can be easily avoided by always specifying a timeout (with the `:after millis` clause) when doing a selective receive. Selective receive is a powerful tool that can greatly help writing readable, maintainable message-handling code, but don't over-use it.

## Error Handling

The actor model does not only make concurrency easy; it also helps build fault-tolerant systems by compartmentalizing failure. Each actor is it's own execution context - if it encounters an exception, only the actor is directly affected (like a thread, only actors are lightweight). Unlike regular functions/objects, where an exception has to be caught and handled immediately on the callstack, with actors we can completely separate code execution from error handling.

In fact, when using actors, it is often best to to follow the [philosophy laid out by Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf), Erlang's chief designer, of "let it crash". The idea is not to try and catch exceptions inside an actor, because attempting to catch and handle all exceptions is futile. Instead, we just let the actor crash, monitor its death elsewhere, and then take some action.

The principle of actor error handling is that an actor can be asked to be notified of another actor's death. This is done through *linking* and *watching*. 

### Linking actors


{% comment %}




You link two actors with the `link!` function like this:

~~~ clojure
(link! actor1 actor2)
~~~

Better yet, is to call the function from within one of the actors, say `actor1`, in which case it will be called like so:

~~~ clojure
(link! actor2)
~~~

A link is symmetrical. When two actors are linked, when one of them dies, the other throws an exception which, unless caught, kills it as well.

Here's an example from the tests:

~~~ clojure
(let [actor1 (spawn #(Fiber/sleep 100))
      actor2 (spawn
               (fn []
                 (link! actor1)
                 (try
                   (loop [] (receive [m] :foo :bar) (recur))
                   (catch co.paralleluniverse.actors.LifecycleException e
                     true))))]
  
  (join actor1)
  (join actor2)) ; => true
~~~

Remember, linking is symmetrical, so if `actor2` were to die, `actor1` would get the exception.

What if `actor2` wants to be notified when `actor1` dies, but doesn't want to die itself? The `:trap` flag for the `spawn` macro, tells is to trap lifecycle exceptions and turn them into messages:

~~~ clojure
(let [actor1 (spawn #(Strand/sleep 100))
      actor2 (spawn :trap true
                    (fn []
                      (link! actor1)
                      (receive [m]
                               [:exit _ actor reason] actor)))]
  (join actor1)
  (join actor2)) ; => actor1
~~~

Now, when `actor1` dies, `actor2` receives an `:exit` message, telling it which actor has died and how. We'll look into the `:exit` message in a second.

We can undo the link by calling

~~~ clojure
(unlink! actor1 actor2)
~~~

or 

~~~ clojure
(unlink! actor2)
~~~

from within `actor1`.

### Watching actors

A more robust way than linking of being notified of actor death is with a *watch* (called *monitor* in Erlang; this is one of the very few occasions we have abandoned the Erlang function names):

~~~ clojure
(let [actor1 (spawn #(Fiber/sleep 200))
      actor2 (spawn
               #(let [w (watch! actor1)]
                  (receive
                    [:exit w actor reason] actor)))]
  (join actor1)
  (join actor2)) ; => actor1
~~~

Watches are asymmetrical. Here, `actor2` watches for `actor1`'s death, but not vice-versa. When `actor1` dies, `actor2` gets an `:exit` message, of the exact same structure of the message sent when we used a link and a `:trap` flag.

The `watch!` function returns a watch object. Because an actor can potentially set many watches on another actor (say, it calls a library function which calls `watch!`), we could potentially get several copies of the exit message, each for a different watch.

The message is a vector of 4 elements:

1. `:exit`
2. The watch interested in the message (or `nil` when linking). Note how in the example we pattern-match on the second element (with the `w` value, which contains the watch object), to ensure that we only process the message belonging to our watch.
3. The actor that just died.
4. The dead actor's death cause: `nil` for a natural death (no exception thrown, just like in our example), or the throwable responsible for the actor's death.

We can remove a watch by calling

~~~ clojure
(unwatch! actor1 actor2)
~~~

or 

~~~ clojure
(unwatch! actor2)
~~~

from within `actor1`.

## Actor registration

*Registering* an actor gives it a public name that can be used to locate the actor. You register an actor like so:

~~~ clojure
(register! actor name)
~~~

or:

~~~ clojure
(register! actor)
~~~

in which case the name will be the one given to the actor when it was `spawn`ed. `name` can be a string, or any object with a nice string representation (like a keyword).

You obtain a reference to a registered actor with:

~~~ clojure
(whois name)
~~~

but most actor-related functions can work directly with the registered name. For example, instead of this:

~~~ clojure
(register! actor :foo)
(! (whois :foo) "hi foo!")
~~~

you can write:

~~~ clojure
(register !actor :foo)
(! :foo "hi foo!")
~~~

You unregister an actor like so:

~~~ clojure
(unregister! actor)
~~~

### Registration and Monitoring

When you register an actor, Pulsar automatically creates a JMX MBean to monitor it. Look for it using JConsole or VisualVM.

Details TBD.

### Registration and Clustering

If you're running in a Galaxy cluster, registering an actor will make it globally available on the cluster (so the name must be unique to the entire cluster).

Details TBD.

## Behaviors

Erlang's designers have realized that many actors follow some common patterns - like an actor that receives requests for work and then sends back a result to the requester. They've turned those patterns into actor templates, called behaviors, in order to save poeple work and avoid some common errors. Some of these behaviors have been ported to Pulsar. 

Behaviors have two sides. One is the provider side, and is modeled in Pulsar as a protocols. You implement the protocol, and Pulsar provides the full actor implementation that uses your protocol. The other is the consumer side -- functions used by other actors to access the functionality provided by the behavior.

All behaviors (gen-server, gen-event and supervisors) support the `shutdown!` function, which requests an orderly shutdown of the actor:

~~~ clojure
(shutdown! behavior-actor)
~~~

### gen-server

`gen-server` is a template for a server actor that receives requests and replies with responses. The consumer side for gen-server consists of the following functions:

~~~ clojure
(call! actor request)
~~~

This would send the `request` message to the gen-server actor, and block until a response is received. It will then return the response. If the request triggers an exception in the actor, that exception will be thrown by `call!`.

There's also a timed version of `call!`, which gives up and returns `nil` if the timeout expires. For example, :

~~~ clojure
(call-timed! actor 100 :ms request)
~~~

would wait up to 100ms for a response.

You can also send a gen-server messages that do not require a response with the `cast!` function:

~~~ clojure
(cast! actor message)
~~~

Finally, you can shutdown a gen-server with the shutdown function:

~~~ clojure
(shutdown! actor)
~~~

In order to create a gen-server actor(the provider side), you need to implement the following protocol:

~~~ clojure
(defprotocol Server
  (init [this])
  (handle-call [this ^Actor from id message])
  (handle-cast [this ^Actor from id message])
  (handle-info [this message])
  (handle-timeout [this])
  (terminate [this ^Throwable cause]))
~~~

* `init` -- will be called alled when the actor starts
* `terminate` -- will be called when the actor terminates.
* `handle-call` -- called when the `call` function has been called on the actor :). This is where the gen-server's functionality usually lies. The value returned from `handle-call` will be sent back to the actor making the request, unless `nil` is returned, in which case the response has to be sent manually as we'll see later.
* `handle-cast` -- called to handle messages sent with `cast!`.
* `handle-info` -- called whenever a message has been sent to the actor directly (i.e., with `!`) rather than through `call!` or `cast!`.
* `handle-timeout` -- called whenever the gen-server has not received any messages for a configurable duration of time. The timeout can be configured using either the `:timeout` option to the `gen-server` function, or by calling the `set-timeout!` function, as we'll immediately see.

You spawn a gen-server actor like so:

~~~ clojure
(spawn (gen-server <options?> server))
~~~

where `options` can now only be `:timeout millis`. Here's an example from the tests:

~~~ clojure
(let [gs (spawn
           (gen-server (reify Server
                         (init [_])
                         (terminate [_ cause])
                         (handle-call [_ from id [a b]]
                                      (Strand/sleep 50)
                                      (+ a b)))))]
  (call! gs 3 4); => 7
~~~

And here's one with server timeouts:

~~~ clojure
(let [times (atom 0)
            gs (spawn
                 (gen-server :timeout 20
                             (reify Server
                               (init [_])
                               (handle-timeout [_]
                                               (if (< @times 5)
                                                 (swap! times inc)
                                                 (shutdown!)))
                               (terminate [_ cause]))))]
        (join 200 :ms gs)
        @times) ; => 5
~~~

You can set (and reset) the timeout from anywhere within the protocol's methods by calling, say 

~~~ clojure
(set-timeout! 100 :ms)
~~~

A timeout value of 0 or less means no timeout.

If the `handle-call` function returns `nil`, then no response is sent to the caller. The `call!` function remains blocked until a response is sent manually. This is done with the `reply!` function, which takes, along with the response message, the identitiy of the caller and the request ID, both passed to `handle-call`. Here's an example:

~~~ clojure
(let [gs (spawn
           (gen-server :timeout 50
                       (reify Server
                         (init [_]
                               (set-state! {}))
                         (terminate [_ cause])
                         (handle-call [_ from id [a b]]
                                      (set-state! (assoc @state :a a :b b :from from :id id))
                                      nil)
                         (handle-timeout [_]
                                         (let [{:keys [a b from id]} @state]
                                           (when id
                                             (reply! from id (+ a b))))))))]
  (call-timed! gs 100 :ms 5 6)) ; => 11
~~~

In the example, `handle-call` saves the request in the actor's state, and later, in `handle-timeout` sends the response using `reply!`. The response is returned by `call-timed!`.

If an error is encountered during the generation of the delayed repsonse, an exception can be returned to the caller (and will be thrown by `call!`), using `reply-error!`:

~~~ clojure
(reply-error! to id (Exception. "does not compute"))
~~~

where `to` is the identity of the caller passed as `from` to `handle-call`.

### gen-event

gen-event is an actor behavior that receives messages (*events*) and forwards them to registered *event handlers*.

You spawn a gen-event like this:

~~~ clojure
(spawn (gen-event init))
~~~

`init` is an initializer function called from within the gen-event actor.

You can then add event handlers:

~~~~ clojure
(add-handler! ge handler)
~~~~

with `ge` being the gen-event actor (returned by the call to `spawn`), and `handler` being a function of a single argument that will be called whenever an event is generated.

You generate an event with the `notify!` function:

~~~ clojure
(notify! ge event)
~~~

with `ge` being the gen-event actor, and `event` is the event object (which can be any object). The event object is then passed to all registered event handlers.

An event handler can be removed like so:

~~~ clojure
(remove-handler! ge handler)
~~~

Here's a complete example, taken from the tests:

~~~ clojure
(let [ge (spawn (gen-event
                  #(add-handler! @self handler1)))]
  (add-handler! ge handler2)
  (notify! ge "hello"))
~~~

In this example, `handler1` is added in the `init` function (note how `@self` refers to the gen-event actor itself, as the init function is called from within the actor), and `handler2` is added later.

When `notify!` is called, both handlers will be called and passed the event object (in this case, the `"hello"` string).

## Supervisors

A supervisor is an actor behavior designed to standardize error handling. Internally it uses watches and links, but it offers a more structured, standard, and simple way to react to errors.

The general idea is that actors performing business logic, "worker actors", are supervised by a supervisor actor that detects when they die and takes one of several pre-configured actions. Supervisors may, in turn, be supervised by other supervisors, thus forming a supervision hierarchy that compartmentalizes failure and recovery. 

A supervisors work as follows: it has a number of *children*, worker actors or other supervisors that are registered to be supervised wither at the supervisor's construction time or at a later time. Each child has a mode, `:permanent`, `:transient` or `:temporary` that determines whether its death will trigger the supervisor's *recovery event*. When the recovery event is triggered, the supervisor takes action specified by its *restart strategy*, or it will give up and fail, depending on predefined failure modes. 

When a child actor in the `:permanent` mode dies, it will always trigger its supervisor's recovery event. When a child in the `:transient` mode dies, it will trigger a recovery event only if it has died as a result of an exception, but not if it has simply finished its operation. A `:temporary` child never triggers it supervisor's recovery event.

A supervisor's *restart strategy* determines what it does during a *recovery event*: A strategy of `:escalate` measns that the supervisor will shut down ("kill") all its surviving children and then die; a `:one-for-one` strategy will restart the dead child; an `:all-for-one` strategy will shut down all children and then restart them all; a `:rest-for-one` strategy will shut down and restart all those children added to the suervisor after the dead child.

A supervisor is spawned so:

~~~ clojure
(spawn (supervisor restart-strategy init))
~~~

where `restart-strategy` is one of: `:escalate`, `:one-for-one`, `:all-for-one`, or `:rest-for-one`, and `init` is a function that returns a sequence of *child specs* that will be used to add children to the supervisor when it's constructed.

A *child spec* is a vector of the following form:

~~~ clojure
[id mode max-restarts duration unit shutdown-deadline-millis actor-fn & actor-args]
~~~

where:

* `id` is an optional identifier (usually a string) for the child actor. May be `nil`.
* `mode` is one of `:permanent`, `:transient` or `:temporary`.
* `max-restarts`, `duration` and `unit` are a triplet specifying how many times is the child allowed to restart in a given period of time before the supervisor should give up, kill all its children and die. For example `20 5 :sec` means at most 20 restarts in 5 seconds.
* `shutdown-deadline-millis` is the maximal amount of time, in milliseconds that the child is allowed to spend from the time it's requested to shut down until the time it is terminated. Whenever a the supervisor shuts down a child, it does so by sending it the message `[:shutdown sup]`, with `sup` being the supervisor. If the shutdown deadline elapses, the supervisor will forcefully shut it down by interrupting the child's strand.
* `actor-fn & actor-args` are the (suspendable) function (with optional arguments) that's to serve as the child actor's body.

It is often useful to pass the supervisor to a child (so it could later dynamically add other children to the supervisor, for example). This is easily done because the `init` function is called inside the supervisor; therefore, any reference to `@self` insode the init function returns the supervisor. If you pass `@self`, then, as an argument to a child actor, it will receive the supervisor.

Other than returning a sequence of child specs from the `init` function, you can also dynamically add a child to a supervisor by simply calling

~~~ clojure
(add-child! sup id mode max-restarts duration unit shutdown-deadline-millis actor-fn & actor-args)
~~~

with `sup` being the supervisor, and the rest of the arguments comprising the child spec for the actor, with the difference that if `actor-fn`, instead of an actor function, is a spawned actor (the value returned from `spawn`), then supervisor will supervise an already-spawned actor. Otherwise, (if it is a function), a new actor will be spawned.

A supervised actor may be removed from the supervisor by calling

~~~ clojure
(remove-child! sup id)
~~~

with `id` being the one given to the actor in the child spec or the arguments to `add-child`.

{% endcomment %}


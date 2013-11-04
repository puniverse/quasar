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

An actor is required to implement the `doRun` method. This method is the actor body, and is run when the actor is spawned.

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

{:.alert .alert-warn}
**Note**: An actor must *never* pass a direct reference to itself to other actors or to be used on other strands. However, it may share its `ActorRef` freely.

The `ActorRef` allows sending messages to the actor's mailbox. In fact, `ActorRef` implements `SendPort` so it can be used just like a channel.

An actor's mailbox is a channel, that can be obtained with the `mailbox-of` function. You can therefore send a message to an actor like so:

~~~ clojure
(snd (mailbox-of actor) msg)
~~~

But there's an easier way. Actors implement the `SendPort` interface, and so, are treated like a channel by the `snd` function. So we can simple call:

~~~ clojure
(snd actor msg)
~~~

While the above is a perfectly valid way of sending a message to an actor, this is not how it's normally done. Instead of `snd` we normally use the `!` (bang) function to send a message to an actor, like so:

~~~ clojure
(! actor msg)
~~~

The bang operator has a slightly different semantic than `snd`. While `snd` will always place the message in the mailbox, `!` will only do it if the actor is alive. It will not place a message in the mailbox if there is no one to receive it on the other end (and never will be, as mailboxes, like all channels, cannot change ownership).

In many circumstances, an actor sends a message to another actor, and expects a reply. In those circumstances, using `!!` instead of `!` might offer reduced latency (but with the same semantics; both `!` and `!!` always return `nil`)

The value `@self`, when evaluated in an actor, returns the actor. So, as you may guess, an actor can receive messages with:

~~~ clojure
(rcv (mailbox-of @self))
~~~

`@mailbox` returns the actor's own mailbox channel, so the above may be written as:

~~~ clojure
(rcv @mailbox)
~~~

... and because actors also implement the `ReceivePort` interface required by `rcv`, the following will also work:

~~~ clojure
(rcv @self)
~~~

But, again, while an actor can be treated as a fiber with a channel, it has some extra features that give it a super-extra punch. Actors normally receive messages with the `receive` function, like so:

~~~ clojure
(receive)
~~~

`receive` has some features that make it very suitable for handling messages in actors. Its most visible feature is pattern matching. When an actor receives a message, it usually takes different action based on the type and content of the message. Making the decision with pattern matching is easy and elegant:

~~~ clojure
(let [actor (spawn
               #(receive
                   :abc "yes!"
                   [:why? answer] answer
                   :else "oy"))]
     (! actor [:why? "because!"])
     (join actor)) ; => "because!"
~~~

As we can see in the example, `receive` not only picks the action based on the message, but also destructures the message and binds free variable, in our example – the `answer` variable. `receive` uses the [core.match](https://github.com/clojure/core.match) library for pattern matching, and you can consult [its documentation](https://github.com/clojure/core.match/wiki/Overview) to learn exactly how matching works.

Sometimes, we would like to assign the whole message to a variable. We do it by creating a binding clause in `receive`:

~~~ clojure
(receive [m]
   [:foo val] (println "got foo:" val)
   :else      (println "got" m))
~~~

We can also match not on the raw message as its been received, but transform it first, and then match on the transformed value, like so, assuming `transform` is a function that takes a single argument (the message):

~~~ clojure
(receive [m transform]
   [:foo val] (println "got foo:" val)
   :else      (println "got" m))
~~~

Now `m` – and the value we're matching – is the the transformed value.

`receive` also deals with timeouts. Say we want to do something if a message has not been received within 30 milliseconds (all `receive` timeouts are specified in milliseconds):

~~~ clojure
(receive [m transform]
   [:foo val] (println "got foo:" val)
   :else      (println "got" m)
   :after 30  (println "nothing..."))
~~~

{:.alert .alert-warn}
**Note**: The `:after` clause in `receive` *must* be last.

Before we move on, it's time for a short example. In this example, we will define an actor, `adder`, that receives an `:add` message with two numbers, and reply to the sender with the sum of those two numbers. In order to reply to the sender, we need to know who the sender is. So the sender will add a reference to itself in the message. In this request-reply pattern, it is also good practice to attach a random unique tag to the request, because messages are asynchronous, and it is possible that the adder will not respond to the requests in the order they were received, and the requester might want to send two requests before waiting for a response, so a tag is a good way to match replies with their respective requests. We can generate a random tag with the `maketag` function.

Here's the adder actor:

~~~ clojure
(defsfn adder []
  (loop []
    (receive
     [from tag [:add a b]] (! from tag [:sum (+ a b)]))
    (recur)))
~~~

And this is how we'll use it from within another actor:

~~~ clojure
...
(let [tag (maketag)
      a ...
      b ...]
   (! adder-actor @self tag [:add a b])
   (->>
      (receive 
         [tag [:sum sum]] sum
         :after 10        nil)
      (println "sum:"))
...
~~~

## Actors vs. Channels

One of the reasons of providing a different `receive` function for actors is because programming with actors is conceptually different from just using fibers and channels. I think of channels as hoses  pumping data into a function, or as sort of like asynchronous parameters. A fiber may pull many different kinds of data from many different channels, and combine the data in some way. 

Actors are a different abstraction. They are more like objects in object-oriented languages, assigned to a single thread. The mailbox serves as the object's dispatch mechanism; it's not a hose but a switchboard. It's for this reason that actors often need to pattern-match their mailbox messages, while regular channels – each usually serving as a conduit for a single kind of data – don't.

But while the `receive` syntax is nice and all (it mirrors Erlang's syntax), we could have achieved the same with `rcv` almost as easily:

~~~ clojure
(let [m1 (rcv 30 :ms)]
   (if m1
      (let [m (transform m1)]
         (match (transform (rcv 30 :ms))
             [:foo val]  (println "got foo:" val)
   		     :else      (println "got" m)))
   	   (println "nothing...")))
~~~

Pretty syntax is not the main goal of the `receive` function. The reason `receive` is much more powerful than `rcv`, is mostly because of a feature we will now introduce.

## Selective Receive

An actor is a state machine. It usually encompasses some *state* and the messages it receives trigger *state transitions*. But because the actor has no control over which messages it receives and when (which can be a result of either other actors' behavior, or even the way the OS schedules threads), an actor would be required to process any message and any state, and build a full *state transition matrix*, namely how to transition whenever *any* messages is received at *any* state.

This can not only lead to code explosion; it can lead to bugs. The key to managing a complex state machine is by not handling messages in the order they arrive, but in the order we wish to process them. If a message does not match any of the clauses in `receive`, it will remain in the mailbox. `receive` will return only when it finds a message that does. When another `receive` statement is called, it will again search the messages that are in the mailbox, and may match a message that has been skipped by a previous `receive`. 

In this code snippet, we specifically wait for the `:baz` message after receiving `:foo`, and so process the messages in this order -- `:foo`, `:baz`, `:bar` -- even though `:bar` is sent before `:baz`:

~~~ clojure
(let [res (atom [])
      actor (spawn
              #(dotimes [i 2]
                 (receive
                   [:foo x] (do
                              (swap! res conj x)
                              (receive
                                [:baz z] (swap! res conj z)))
                   [:bar y] (swap! res conj y)
                   [:baz z] (swap! res conj z))))]
  (! actor [:foo 1])
  (! actor [:bar 2])
  (! actor [:baz 3])
  (join actor)
  @res) ; => [1 3 2]
~~~

[Another example]({{examples}}/priority.clj) demonstrates receiving messages in order of priority.

Selective receive is also very useful when communicating with other actors. Here's an excerpt from [this example]({{examples}}/selective.clj):

~~~ clojure
(defsfn adder []
  (loop []
    (receive
      [from tag [:add a b]] (! from tag [:sum (+ a b)]))
    (recur)))

(defsfn computer [adder]
  (loop []
    (receive [m]
             [from tag [:compute a b c d]] (let [tag1 (maketag)]
                                             (! adder [@self tag1 [:add (* a b) (* c d)]])
                                             (receive
                                               [tag1 [:sum sum]]  (! from tag [:result sum])
                                               :after 10          (! from tag [:error "timeout!"])))
             :else (println "Unknown message: " m))
    (recur)))

(defsfn curious [nums computer]
  (when (seq nums)
    (let [[a b c d] (take 4 nums)
          tag       (maketag)]
      (! computer @self tag [:compute a b c d])
      (receive [m]
               [tag [:result res]]  (println a b c d "->" res)
               [tag [:error error]] (println "ERROR: " a b c d "->" error)
               :else (println "Unexpected message" m))
      (recur (drop 4 nums) computer))))

(defn -main []
  (let [ad (spawn adder)
        cp (spawn computer ad)
        cr (spawn curious (take 20 (repeatedly #(rand-int 10))) cp)]
    (join cr)
    :ok))
~~~

In the example, we have three actors: `curious`, `computer` and `adder`. `curious` asks `computer` to perform a computation, and `computer` relies on `adder` to perform addition. Note the nested `receive` in `computer`: the actor waits for a reply from `adder` before accepting other requests (from `curious`) in the outer receive (actually, because this pattern of sending a message to an actor and waiting for a reply is so common, it's encapsulated by a construct call `gen-server` - yet another blatant theft from Erlang - which we'll introduce later; if you want to see how this example looks using `gen-server`, take a look [here]({{examples}}/selective_gen_server.clj). 

There are several actor systems that do not support selective receive, but Erlang does, and so does Pulsar. [The talk *Death by Accidental Complexity*](http://www.infoq.com/presentations/Death-by-Accidental-Complexity), by Ulf Wiger, shows how using selective receive avoids implementing a full, complicated and error-prone transition matrix. [In a different talk](http://www.infoq.com/presentations/1000-Year-old-Design-Patterns), Wiger compared non-selective (FIFO) receive to a tetris game where you must fit each piece into the puzzle as it comes, while selective receive turns the problem into a jigsaw puzzle, where you can look for a piece that you know will fit.

{:.alert .alert-warn}
**A word of caution**: Using selective receive in your code may lead to deadlocks (because you're essentially saying, I'm going to wait here until a specific message arrives). This can be easily avoided by always specifying a timeout (with the `:after millis` clause) when doing a selective receive. Selective receive is a powerful tool that can greatly help writing readable, maintainable message-handling code, but don't over-use it.

## Actor State

In Erlang, actor state is set by recursively calling the actor function with the new state as an argument. In Pulsar, we can do the same. Here’s an example:

~~~ clojure
(let [actor
      (spawn #(loop [i (int 2)
                     state (int 0)]
                (if (== i 0)
                  state
                  (recur (dec i) (+ state (int (receive)))))))]
  (! actor 13)
  (! actor 12)
  (join actor)) ; => 25
~~~

Clojure is all about managing state. It ensures that every computation has access to consistent data. Because actors communicate with other computation only by exchanging immutable messages, and because each actor runs in a single strand, it's absolutely ok for an actor to have mutable state - only the actor has access to it. 

Every Pulsar actor has a `state` field that can be read like this `@state` and written with `set-state!`. Here’s an example:

~~~ clojure
(let [actor
      (spawn #(do
                (set-state! 0)
                (set-state! (+ @state (receive)))
                (set-state! (+ @state (receive)))
                @state))]
  (! actor 13)
  (! actor 12)
  (join actor)) ; => 25
~~~

Finally, what if we want several state fields? What if we want some or all of them to be of a primitive type? This, too, poses no risk of race conditions because all state fields are written and read only by the actor, and there is no danger of them appearing inconsistent to an observer.
Pulsar supports this as an experimental feature (implemented internally with `deftype`), like so:

~~~ clojure
(let [actor (spawn (actor [^int sum 0]
                          (set! sum (int (+ sum (receive))))
                          (set! sum (int (+ sum (receive))))
                          sum))]
  (! actor 13)
  (! actor 12)
  (join actor)) ; => 25
~~~

These are three different ways of managing actor state. Eventually, we’ll settle on just one or two (and are open to discussion about which is preferred).

## State Machines with strampoline

As we've seen, the `receive` form defines which messages the actor is willing to accept and process. You can nest `receive` statements, or place them in other functions that the actor calls (in which case the must be defined with `defsfn`). It is often useful to treat the actor as a state machine, going from one state to another, executing a different `receive` at each state (to define the acceptable transitions from the state). To change state, all we would have to do is call a different function, each with its own receive, but here we face a technical limitation of Clojure. As Clojure (due to JVM limitations) does not perform true tail-call optimization, every state transition (i.e. every function call), would add a frame to the stack, eventually throwing a stack overflow. Clojure solves it with the `clojure.core/trampoline` function. It takes a function and calls it. When the function returns, if the returned value is a function, `trampoline` calls it.

Pulsar comes with a version of `trampoline` for suspendable functions called `strampoline` (with the exact same API as `trampoline`).

Consider this example:

~~~ clojure
(let [state2 (sfn []
                    (receive
                      :bar :foobar))
      state1 (sfn []
                    (receive
                      :foo state2))
      actor (spawn (fn []
                     (strampoline state1)))]
  (! actor :foo)
  (Thread/sleep 50) ; or (Strand/sleep 50)
  (! actor :bar)
  (join actor)) ; => :foobar
~~~

The actor starts at `state1` (represented by the function with the same name), by calling `(strampoline state1)`. In `state1` we expect to receive the message `:foo`. When it arrives, we transition to `state2` by returning the `state2` function (which will immediately be called by `strampoline`). In `state1` we await the `:bar` message, and then terminate.

What happens if the messages `:foo` and `:bar` arrive in reverse order? Thanks to selective receive the result will be exactly the same! `state1` will skip the `:bar` message, and transition to `state2` when `:foo` arrives; the `receive` statement in `state2` will then find the `:bar` message waiting in the mailbox:

~~~ clojure
(let [state2 (sfn []
                    (receive
                      :bar :foobar))
      state1 (sfn []
                    (receive
                      :foo state2))
      actor (spawn (fn []
                     (strampoline state1)))]
  (! actor :bar)
  (Thread/sleep 50) ; or (Strand/sleep 50)
  (! actor :foo)
  (join actor)) ; => :foobar
~~~

## Error Handling

The actor model does not only make concurrency easy; it also helps build fault-tolerant systems by compartmentalizing failure. Each actor is it's own execution context - if it encounters an exception, only the actor is directly affected (like a thread, only actors are lightweight). Unlike regular functions/objects, where an exception has to be caught and handled immediately on the callstack, with actors we can completely separate code execution from error handling.

In fact, when using actors, it is often best to to follow the [philosophy laid out by Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf), Erlang's chief designer, of "let it crash". The idea is not to try and catch exceptions inside an actor, because attempting to catch and handle all exceptions is futile. Instead, we just let the actor crash, monitor its death elsewhere, and then take some action.

The principle of actor error handling is that an actor can be asked to be notified of another actor's death. This is done through *linking* and *watching*. 

### Linking actors

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




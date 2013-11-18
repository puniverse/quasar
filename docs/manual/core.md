---
layout: default
title: Quasar Core
weight: 1
---

{% capture code %}https://github.com/{{site.github}}/tree/master/quasar-core/src/main/java/co/paralleluniverse{% endcapture %}
{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

## Fibers {#fibers}

Quasar's chief contribution is that of the lightweight thread, called *fiber* in Quasar.  
Fibers provide functionality similar to threads, and a similar API, but they're not managed by the OS. They are lightweight in terms of RAM (an idle fiber occupies ~400 bytes of RAM) and put a far lesser burden on the CPU when task-switching. You can have millions of fibers in an application. If you are familiar with Go, fibers are like goroutines. Fibers in Quasar are scheduled by one or more [ForkJoinPool](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html)s. 

Fibers are not meant to replace threads in all circumstances. A fiber should be used when its body (the code it executes) blocks very often waiting on other fibers (e.g. waiting for messages sent by other fibers on a channel, or waiting for the value of a dataflow-variable). For long-running computations that rarely block, traditional threads are preferable. Fortunately, as we shall see, fibers and threads interoperate very well.

Fibers are especially useful for replacing callback-ridden asynchronous code. They allow you to enjoy the scalability and performance benefits of asynchronous code while keeping the simple to use and understand threaded modedl.

### Using Fibers

A fiber is represented by the [`Fiber`]({{javadoc}}/fibers/Fiber.html) class. Similarly to a thread, you spawn a fiber like so:

~~~ java
new Fiber<V>() {
	@Override
	protected V run() throws SuspendExecution, InterruptedException {
        // your code
    }
}.start();
~~~

There are several differences between this and starting a thread. First, a fiber can have a return value of the generic type `V` (we'll shortly see how to use it). If your fiber does not need to return a value, use `Void` as the type for `V`, and return `null` from the `run` method. Second, the `run` method is allowed to throw an `InterruptedException`, (this is mostly a matter of convenience), as well as `SuspendExecution` which we'll come back to.

You can also start a fiber by passing an instance of [`SuspendableRunnable`]({{javadoc}}/strands/SuspendableRunnable.html) or [`SuspendableCallable`]({{javadoc}}/strands/SuspendableCallable.html) to `Fiber`'s constructor:

~~~ java
new Fiber<Void>(new SuspendableRunnable() {
	public void run() throws SuspendExecution, InterruptedException {
		// your code
	}
}).start();
~~~

You can join a fiber much as you'd do a thread with the `join` method. To obtain the value returned by the fiber (if any), you call the `get` method, which joins the fiber and returns its result.

Other than `Fiber`'s constructor and `start` method, and possibly the `join` and `get` methods, you will not access the `Fiber` class directly much. To perform operations you would normally want to do on a thread, it is better to use the `Strand` class (discussed later), which is a generalizations of both threads and fibers.

### Thread Locals

Using `ThreadLocal`s in a fiber works as you'd expect – the values are local to the fiber. An `InheritableThreadLocal` inherits its value from the fiber's parent, i.e. the thread or the fiber that spawned it.

### Suspendable This, Suspendable That

The `run` method in `Fiber`, `SuspendableRunnable` and `SuspendableCallable` declares that it may throw a `SuspendExecution` exception. This is not a real exception, but part of the inner working of fibers. Any method that may run in a fiber and may block, declares to throw this exception or is annotated with the `@Suspendable` annotation. Such a method is called a *suspendable method*. When a method you write calls a suspendable method, it, too, is a suspendable method, and must therefore declare to throw `SuspendExecution` (if you cannot add this exception to your method's `throws` clause, say, because you're implementing an interface that does not throw it, you can annotate your method with the `@Suspendable` annotation, but this requires extra consideration; please see the [Advanced Fiber Usage](#advanced-fibers) section). Adding `SuspendExecution` to the `throws` clause is convenient because it makes the compiler force you to add the exception to any method that calls your method, which you should.

{:.alert .alert-warning}
**Note**: Other than a few methods in the `Fiber` class that are usually only used internally, whenever you encounter a method that declares to throw `SuspendExecution`, it is safe to call to use by fibers as well as regular threads. If used in a thread, it will never actually throw a `SuspendExecution` exception, so it is best to declare a `catch(SuspendExecution e)` block when called on a regular thread, and just throw an `AssertionError`, as it should never happen.

## Strands

A *strand* (represented by the [`Strand`]({{javadoc}}/strands/Strand.html) class) is an abstraction for both fibers and threads; in short – a strand is either a fiber or a thread. The `Strand` class provides many useful methods. `Strand.currentStrand()` returns the current running strand (be it a fiber or a thread); `Strand.sleep()` suspends the current strand for the given number of milliseconds; `getStackTrace` returns the current stack trace of the strand. To learn more about what operations you can perform on strands, please consult the [Javadoc]({{javadoc}}/strands/Strand.html).

### `park` and `unpark`

Most importantly (though relevant only for power-users who would like to implement their own concurrency primitives, such as locks), the `Strand` classcontains the methods `park` and `unpark`, that delegate to `Fiber.park` and `Fiber.unpark` methods if the strand is a fiber, or to [`LockSupport`](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/LockSupport.html)'s `park` and `unpark` methods if the strand is a thread (`LockSupport` lies at the core of all `java.util.concurrent` classes). This allows to create synchronization mechanisms that work well for both fibers and threads. 

Just as you almost never use `LockSupport` directly, so, too, you will never need to call `Strand.park` or `Strand.unpark`, unless you're writing your own concurrency constructs (like a new kind of lock).

## Transforming any Asynchronous Callback to A Fiber-Blocking Operation

As we said above, fibers are great as a replacement for callbacks. The [FiberAsync]({{javadoc}}/fibers/FiberAsync.html) class helps us easily turn any callback-based asynchronous operation to as simple fiber-blocking call.

 Assume that operation `Foo.asyncOp(FooCompletion callback)` is an asynchronous operation, where `Completion` is defined as:
 
~~~ java
interface FooCompletion {
	void success(String result);
	void failure(FooException exception);
}
~~~

We then define the following subclass of `FiberAsync`:
 
~~~ java
class FooAsync extends FiberAsync<String, Void, FooException> implements FooCompletion {
	@Override
	public void success(String result) {
		asyncCompleted(result);
	}
 
 	@Override
 	public void failure(FooException exception) {
 		asyncFailed(exception);
 	}
}
~~~
 
Then, to transform the operation to a fiber-blocking one, we can define:
 
~~~ java
String op() {
	new FooAsync() {
		protected Void requestAsync() {
        	Foo.asyncOp(this);
     	}
	}.run();
}
~~~

The call to `run` will block the fiber until the operation completes.

Transforming asynchronous code to fiber-blocking calls has a negligible overhead both in terms of memory and performance, while making the code shorter and far simpler to understand.

## Advanced Fiber Usage {#advanced-fibers}

### Fiber Internals 

We will now cover in some depth the inner workings of Quasar fibers. You should read this section if you'd like to annotate suspendable methods with the `@Suspendable` annotation rather than by declaring `throws SuspendExecution`, or if you're just curious.

Internally, a fiber is a *continuation* which is then scheduled in a scheduler. A continuation captures the instantaneous state of a computation, and allows it to be suspended and then resumed at a later time from the point where it was suspended. Quasar creates continuations by instrumenting (at the bytecode level) suspendable methods. For scheduling, Quasar uses `ForkJoinPool`, which is a very efficient, work-stealing, multi-threaded scheduler.

Whenever a class is loaded, Quasar's instrumentation module (usually run as a Java agent) scans it for suspendable methods. Every suspendable method `f` is then instrumented in the following way: It is scanned for calls to other suspendable methods. For every call to a suspendable method `g`, some code is inserted before (and after) the call to `g` that saves (and restores) the state of a local variables to the fiber's stack (a fiber manages its own stack), and records the fact that this (i.e. the call to `g`) is a possible suspension point. At the end of this "suspendable function chain", we'll find a call to `Fiber.park`. `park` suspends the fiber by throwing a `SuspendExecution` exception (which the instrumentation prevents you from catching, even if your method contains a `catch(Throwable t)` block).

If `g` indeed blocks, the `SuspendExecution` exception will be caught by the `Fiber` class. When the fiber is awakened (with `unpark`), method `f` will be called, and then the execution record will show that we're blocked at the call to `g`, so we'll immediately jump to the line in `f` where `g` is called, and call it. Finally, we'll reach the actual suspension point (the call to `park`), where we'll resume execution immediately following the call. When `g` returns, the code inserted in `f` will restore `f`'s local variables from the fiber stack.

This process sounds complicated, but its incurs a performance overhead of no more than 3%-5%.

### @Suspendable

So far, our way to specify a suspendable method is by declaring it throws `SusepndExecution`. This is convenient because `SuspendExecution` is a checked exception, so if `f` calls `g` and `g` is suspendable, the Java compiler will force us to declare that `f` is suspendable (and it must be because it calls `g` and `g` might be suspended).

Sometimes, however, we cannot declare `f` to throw `SuspendExecution`. One example is that `f` is an implementation of an interface method, and we cannot (or don't want to) change the interface so that it throws `SuspendExecution`. It is also possible that we want `f` to be run in regular threads as well as fibers. 

An example for that are the synchronization primitives in the `co.paralleluniverse.strands.concurrent` package, which implement interfaces declared in `java.util.concurrent`, and we want to maintain compatibility. Also, no harm will come if we use these classes in regular threads. They will work just as well for threads as for fibers, because internally they call `Strand.park` which is fiber-blocking (suspends) if run in a fiber, but simply blocks the thread if not.

So, suppose method `f` is declared in interface `I`, and we'd like to make its implementation in class `C` suspendable. The compiler will not let us declare that we throw `SuspendExecution` because that will conflict with `f`'s declaration in `I`.

What we do, then, is annotate `C.f` with the `co.paralleluniverse.fibers.@Suspendable` annotation. Assuming `C.f` calls `park` or some other suspendable method `g` – which does declare `throws SuspendExecution`, we need to surround `f`'s body with `try {} catch(SuspendExecution)` just so the method will compile, like so:

~~~ java
class C implements I {
	@Suspendable
	public int f() {
		try {
			// do some stuff
			return g() * 2;
		} catch(SuspendExecution s) {
			throw new AssertionError(s); 
		}
	}
}
~~~

The `catch` block will never be executed; the intstrumentation will take care of that.

But now let's consider method `h`:

~~~ java
@Suspendable
public void h(I x) {
	x.f();
}
~~~

First, if we want to run `h` in a fiber, then it must be suspendable because it calls `f` which is suspendable. We could designate `h` as suspendable either by annotating it with `@Suspendable` or by declaring `throws SuspendExecution` (even though `f` is not declared to throw `SuspendExecution`).

When `h` is encountered by the instrumentation module, it will be instrumented because it's marked suspendable, but in order for the instrumentation to work, it needs to know of `h`'s calls to other instrumented methods. `h` calls `f`, which is suspendable, but through its interface `I`, while we've only annotated `f`'s *implementation* in class C. The instrumenter does not know that `I.f` has an implementation that might suspend.

Therefore, if you'd like to use the `@Suspendable` annotation, there's a step you need to add to your build step, after compilation and before creating the jar file: running the class `co.paralleluniverse.fibers.instrument.SuspendablesScanner`. In Gradle it looks like this:

~~~ groovy
task scanSuspendables(type: JavaExec, dependsOn: classes) { // runs SuspendableScanner
    main = "co.paralleluniverse.fibers.instrument.SuspendablesScanner"
    classpath = sourceSets.main.runtimeClasspath
    args = ["package1", "package2"]
}
~~~ 

Where `"package1"` and `"package2"` are packages that contain methods annotated with `@Suspendable`. 

`SuspendablesScanner` scans your code after it's been compiled for methods annotated with `@Suspendable`. In our example it will find `C.f`. It will then see that `C.f` is an implementation of `I.f`, and so it will list `I.f` in a text file (`META-INF/suspendable-supers`), that contains all methods that have overriding suspendable implementations. 

When the instrumentation module instruments `h`, it will find `I.f` in the file, and, knowing it might suspend, inject the appropriate code.

Note that this has no effect on other calls to `I.f`. The instrumentation module only cares that `I.f` has suspendable implementations when it finds it called in suspendable methods (in our case: `h`).

## Channels {#channels}

Channels are queues used to pass messages between strands (remember, strands are a general name for threads and fibers). If you are familiar with Go, Quasar channels are like Go channels. 

A [channel]({{javadoc}}/strands/channels/Channel.html) is an interface that extends two other interfaces: [`SendPort`]({{javadoc}}/strands/channels/SendPort.html), which defines the methods used to send messages to a channel, and [`ReceivePort`]({{javadoc}}/strands/channels/ReceivePort.html), which defines the methods used to receive messages from a channel.

Channels are normally created by calling any of the `newChannel` static methods of the [`Channels`]({{javadoc}}/strands/channels/Channels.html) class. The `newChannel` methods create a channel with a specified set of properties. Those properties are:

* `bufferSize` – if positive, the number of messages that the channel can hold in an internal buffer; `0` for a *transfer* channel, i.e. a channel with no internal buffer. or `-1` for a channel with an unbounded (infinite) buffer.
* `policy`         – the [`OverflowPolicy`]({{javadoc}}/strands/channels/OverflowPolicy.html) specifying how the channel (if bounded) will behave if its internal buffer overflows.
* `singleProducer` – whether the channel will be used by a single producer strand.
* `singleConsumer` – whether the channel will be used by a single consumer strand.

Note that not all property combinations are supported. Consult the [`Javadoc`]({{javadoc}}/strands/channels/Channels.html) for details.
 
### Sending and Receiving Messages

Messages are sent to a channel using the [`SendPort.send`]({{javadoc}}/strands/channels/SendPort.html#send(Message)) method. The `send` method blocks if the channel's buffer is full and the channel has been configured with the `BLOCK` overflow policy. There are versions of `send` that block indefinitely or up to a given timeout, and the `trySend` method sends a message if the channel's buffer has room, or returns immediately, without blocking, if not. Consult the [Javadoc]({{javadoc}}/strands/channels/SendPort.html) for details.

Messages are received from a channel using the [`ReceivePort.receive`]({{javadoc}}/strands/channels/ReceivePort.html#receive()) method. There are versions of `receive` that block indefinitely or up to a given timeout, and the `tryReceive` method receives a message if one is available, or returns immediately, without blocking, if not. Consult the [Javadoc]({{javadoc}}/strands/channels/ReceivePort.html) for details.

Close XXXXX

{:.alert .alert-info}
**Note**: As usual, while the blocking channel methods declare to throw `SuspendExecution`, this exception will never actually be thrown. If using channels in a plain thread, you should `catch(SuspendExecution e) { throw AssertionError(); }`. Alternatively, you can use the convenience wrappers [`ThreadReceivePort`]({{javadoc}}/strands/channels/ThreadReceivePort.html) and [`ThreadSendPort`]({{javadoc}}/strands/channels/ThreadSendPort.html).

### Primitive Channels

Quasar provides 4 types of channels for primitive data types: `int`, `long`, `float` and `double`. Consult the Javadoc of, for example, [`IntSendPort`]({{javadoc}}/strands/channels/IntSendPort.html) [`IntReceivePort`]({{javadoc}}/strands/channels/IntReceivePort.html) and for details.

All primitive channels do not support multiple consumers.


### Ticker Channels

A channel created with the `DISPLACE` overflow policy is called a *ticker channel* because it provides guarantees similar to that of a digital stock-ticker: you can start watching at any time, the messages you read are always read in order, but because of the limited screen size, if you look away or read to slowly you may miss some messages.

The ticker channel is useful when a program component continually broadcasts some information. The size channel's circular buffer, its "screen" if you like, gives the subscribers some leeway if they occasionally fall behind reading.

A ticker channel is single-consumer, i.e. only one strand is allowed to consume messages from the channel. On the other hand, it is possible, and useful, to create several views of the channel, each used by a different consumer strand. A view (which is of type [`TickerChannelConsumer`]({{javadoc}}/strands/channels/TickerChannelConsumer.html) is created with the [`Channels.newTickerConsumerFor`]({{javadoc}}/strands/channels/Channels.html#newTickerConsumerFor(Channel)) method.

The method returns a `ReceivePort` that can be used to receive messages from `channel`. Each ticker-consumer will yield monotonic messages, namely no message will be received more than once, and the messages will be received in the order they're sent, but if the consumer is too slow, messages could be lost. 

Each consumer strand will use its own `ticker-consumer`, and each can consume messages at its own pace, and each `TickerChannelConsumer` port will return the same messages (messages consumed from one will not be removed from the other views), subject possibly to different messages being missed by different consumers depending on their pace.

### Transofrming Channels

The [`Channels`]({{javadoc}}/strands/channels/Channels.html) class has several static methods that can be used to manipulate and compose values sent to or received off channels:

* map - returns a channel that transforms messages by applying a given mapping function. There are two versions of `map`: [one that operates]({{javadoc}}/strands/channels/Channels.html#map(co.paralleluniverse.strands.channels.ReceivePort, com.google.common.base.Function)) on `ReceivePort` and [one that operates]({{javadoc}}/strands/channels/Channels.html#map(co.paralleluniverse.strands.channels.SendPort, com.google.common.base.Function)) on `SendPort`.
* `filter` - returns a channel that only lets messages that satisfy a predicate through. There are two versions of `filter`: [one that operates]({{javadoc}}/strands/channels/Channels.html#filter(co.paralleluniverse.strands.channels.ReceivePort, com.google.common.base.Predicate)) on `ReceivePort` and [one that operates]({{javadoc}}/strands/channels/Channels.html#filter(co.paralleluniverse.strands.channels.SendPort, com.google.common.base.Predicate)) on `SendPort`.
* [`zip`]({{javadoc}}/strands/channels/Channels.html#zip(com.google.common.base.Function, co.paralleluniverse.strands.channels.ReceivePort...)) - returns a channel that combines each vector of messages from a vector of channels into a single combined message.
* [`group`]({{javadoc}}/strands/channels/Channels.html#group(co.paralleluniverse.strands.channels.ReceivePort...)) - returns a channel that funnels messages from a set of given channels.


### Channel Selection

## Delay Variables






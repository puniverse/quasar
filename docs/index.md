---
layout: default
---

# Overview

Quasar is a Java library that provides high-performance lightweight threads, Go-like channels, Erlang-like actors, and other asynchronous programming tools.

A good introduction to Quasar can be found in the blog post [Erlang (and Go) in Clojure (and Java), Lightweight Threads, Channels and Actors for the JVM](http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar).

Quasar is developed by [Parallel Universe] and released as free software, dual-licensed under the Eclipse Public License and the GNU Lesser General Public License.

[Parallel Universe]: http://paralleluniverse.co


### Dependencies

* [JSR166e](http://g.oswego.edu/dl/concurrency-interest/) (embedded in quasar-core) -- java.util.concurrent, by Doug Lea and contributors
* [ASM](http://asm.ow2.org/) --- Java bytecode manipulation and analysis framework, by the ASM team
* [Metrics](http://metrics.codahale.com/) --- A measurement and monitoring library, by Coda Hale
* [Guava](https://code.google.com/p/guava-libraries/) --- Java utility classes, by Google
* [SLF4J](http://www.slf4j.org/) --- Simple Logging Facade for Java (SLF4J)

Quasar's clustering makes use of [Galaxy](http://puniverse.github.io/galaxy/), by Parallel Universe

### Acknowledgments

A core component of Quasar, bytecode instrumentation, is a fork of the wonderful [Continuations Library](http://www.matthiasmann.de/content/view/24/26/) by Matthias Mann.

## News

### January 21, 2014

Quasar 0.4.0 has been released.

### October 15, 2013

Quasar 0.3.0 has been released.

A [new spaceships demo](https://github.com/puniverse/spaceships-demo) showcases Quasar's (and SpaceBase's) abilities.

### July 19, 2013

Quasar/Pulsar 0.2.0 [has been released](http://blog.paralleluniverse.co/post/55876031297/quasar-pulsar-0-2-0-distributed-actors-supervisors).

### May 2, 2013

Introductory blog post: [Erlang (and Go) in Clojure (and Java), Lightweight Threads, Channels and Actors for the JVM](<http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar>). 

# Getting Started

### System Requirements

Java 7 and is required to run Quasar.

### Using Maven {#maven}

Add the following dependency to Maven:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>{{site.version}}</version>
</dependency>
~~~

To use actors, add:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-actors</artifactId>
    <version>{{site.version}}</version>
</dependency>
~~~

For clustering support, add:

~~~ xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-galaxy</artifactId>
    <version>{{site.version}}</version>
</dependency>
~~~

### Running the Instrumentation Java Agent

Quasar's lightweight thread implementation relies on bytecode instrumentation. Instrumentation can be performed at compilation time (detailed below) or at runtime using a Java agent. To run the Java agent, the following must be added to the java command line (or use your favorite build tool to add this as a JVM argument):

~~~ sh
-javaagent:path-to-quasar-jar.jar
~~~

#### Specifying the Java Agent with Maven:

The best way to do this with Maven, as explained [here](http://stackoverflow.com/questions/14777909/specify-javaagent-argument-with-maven-exec-plugin), is:

First, setup the [maven-dependency-plugin](http://maven.apache.org/plugins/maven-dependency-plugin/) to always run the "properties" goal.

~~~ xml
<plugin>
    <artifactId>maven-dependency-plugin</artifactId>
    <version>2.5.1</version>
    <executions>
        <execution>
            <id>getClasspathFilenames</id>
            <goals>
                <goal>properties</goal>
            </goals>
        </execution>
     </executions>
</plugin>
~~~

Later on, use the property it sets [as documented here](http://maven.apache.org/plugins/maven-dependency-plugin/properties-mojo.html) with the form:

~~~
groupId:artifactId:type:[classifier]
~~~

like so

~~~ xml
<argument>-javaagent:${co.paralleluniverse:quasar-core:jar}</argument>
~~~

#### Specifying the Java Agent with Gradle

The way to do this with Gradle is as follows. Add a `quasar` configuration to your `build.gradle` file:

~~~ groovy
configurations {
    // ...
    quasar
}
~~~

In your dependencies block, add:

~~~ groovy
dependencies {
    // ....
    quasar  "co.paralleluniverse:quasar-core:{{site.version}}"
}
~~~

Finally, in your `run` task (or any task of type `JavaExec` or `Test`), add the system property:

~~~ groovy
jvmArgs "-javaagent:${configurations.quasar.iterator().next()}"
~~~

#### Ahead-of-Time (AOT) Instrumentation {#aot}

The easy and preferable way to instrument programs using Quasar is with the Java agent, which instruments code at runtime. Sometimes, however, running a Java agent is not an option. 

Quasar supports AOT instrumentation with an Ant task. The task is `co.paralleluniverse.fibers.instrument.InstrumentationTask` found in `quasar-core.jar`, and it accepts a fileset of classes to instrument. Not all classes will actually be instrumented – only those with suspendable methods (see below) – so simply give the task all of the class files in your program. In fact, Quasar itself is instrumented ahead-of-time.

### Building Quasar {#build}

Clone the repository:

    git clone git://github.com/puniverse/quasar.git quasar

and run:

    ./gradlew

Or, if you have gradle installed, run:

    gradle


{% comment %}
**Note**: Blah blah blah 
{:.centered .alert .alert-info}

**Note**: Blah blah blah 
{:.alert}

**Note**: Blah blah blah 
{:.alert .alert-error}
{% endcomment %}

# User Manual

## Quasar Core

{% capture code %}https://github.com/{{site.github}}/tree/master/quasar-core/src/main/java/co/paralleluniverse{% endcapture %}
{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

### Fibers {#fibers}

Quasar's chief contribution is that of the lightweight thread, called *fiber* in Quasar.  
Fibers provide functionality similar to threads, and a similar API, but they're not managed by the OS. They are lightweight in terms of RAM (an idle fiber occupies ~400 bytes of RAM) and put a far lesser burden on the CPU when task-switching. You can have millions of fibers in an application. If you are familiar with Go, fibers are like goroutines. Fibers in Quasar are scheduled by one or more [ForkJoinPool](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html)s. 

Fibers are not meant to replace threads in all circumstances. A fiber should be used when its body (the code it executes) blocks very often waiting on other fibers (e.g. waiting for messages sent by other fibers on a channel, or waiting for the value of a dataflow-variable). For long-running computations that rarely block, traditional threads are preferable. Fortunately, as we shall see, fibers and threads interoperate very well.

Fibers are especially useful for replacing callback-ridden asynchronous code. They allow you to enjoy the scalability and performance benefits of asynchronous code while keeping the simple to use and understand threaded modedl.

#### Using Fibers

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

#### The Fiber Scheduler and Runtime Fiber Monitoring {#runtime-monitoring}

Fibers are scheduled by a [`FiberScheduler`]({{javadoc}}/fibers/FiberScheduler.html) in a thread pool (actually a `ForkJoinPool`). When constructing a fiber, you can specify which scheduler should schedule it. If you don't a [default scheduler]({{javadoc}}/fibers/DefaultFiberScheduler.html) is used. You can set the default scheduler's properties by [setting some system properties]({{javadoc}}/fibers/DefaultFiberScheduler.html).

Every scheduler creates a [MXBean]({{javadoc}}/fibers/FibersMXBean.html) that monitors the fibers scheduled by that scheduler. The MXBean's name is `"co.paralleluniverse:type=Fibers,name=SCHEDULER_NAME"`, and you can find more details in the [Javadoc]({{javadoc}}/fibers/FibersMXBean.html).

#### Runaway Fibers

A fiber that is stuck in a loop without blocking, or is blocking the thread its running on (by directly or indirectly performing a thread-blocking operation) is called a *runaway fiber*. It is *perfectly OK* for fibers to do that spradically (as the work stealing scheduler will deal with that), but doing so frequently may severely impact system performance (as most of the scheduler's threads might be tied up by runaway fibers). Quasar detects runaway fibers, and notifies you about which fibers are problematic, whether they're blocking the thread or hogging the CPU, and gives you their stack trace, by printing this information to the console as well as reporting it to the runtime fiber monitor (exposed through a JMX MBean; see [the previous section](#runtime-monitoring)).

Note that this condition might happen when classes are encountered for the first time and need to be loaded from disk. This is alright because this happens only sporadically, but you may notice reports about problematic fibers during startup, as this when most class loading usually occurs.

#### Thread Locals

Using `ThreadLocal`s in a fiber works as you'd expect – the values are local to the fiber. An `InheritableThreadLocal` inherits its value from the fiber's parent, i.e. the thread or the fiber that spawned it.

#### Suspendable This, Suspendable That

The `run` method in `Fiber`, `SuspendableRunnable` and `SuspendableCallable` declares that it may throw a `SuspendExecution` exception. This is not a real exception, but part of the inner working of fibers. Any method that may run in a fiber and may block, declares to throw this exception or is annotated with the `@Suspendable` annotation. Such a method is called a *suspendable method*. When a method you write calls a suspendable method, it, too, is a suspendable method, and must therefore declare to throw `SuspendExecution` (if you cannot add this exception to your method's `throws` clause, say, because you're implementing an interface that does not throw it, you can annotate your method with the `@Suspendable` annotation, but this requires extra consideration; please see the [Advanced Fiber Usage](#advanced-fibers) section). Adding `SuspendExecution` to the `throws` clause is convenient because it makes the compiler force you to add the exception to any method that calls your method, which you should.

{:.alert .alert-warning}
**Note**: Other than a few methods in the `Fiber` class that are usually only used internally, whenever you encounter a method that declares to throw `SuspendExecution`, it is safe to call to use by fibers as well as regular threads. If used in a thread, it will never actually throw a `SuspendExecution` exception, so it is best to declare a `catch(SuspendExecution e)` block when called on a regular thread, and just throw an `AssertionError`, as it should never happen.

### Strands

A *strand* (represented by the [`Strand`]({{javadoc}}/strands/Strand.html) class) is an abstraction for both fibers and threads; in short – a strand is either a fiber or a thread. The `Strand` class provides many useful methods. `Strand.currentStrand()` returns the current running strand (be it a fiber or a thread); `Strand.sleep()` suspends the current strand for the given number of milliseconds; `getStackTrace` returns the current stack trace of the strand. To learn more about what operations you can perform on strands, please consult the [Javadoc]({{javadoc}}/strands/Strand.html).

#### `park` and `unpark`

Most importantly (though relevant only for power-users who would like to implement their own concurrency primitives, such as locks), the `Strand` classcontains the methods `park` and `unpark`, that delegate to `Fiber.park` and `Fiber.unpark` methods if the strand is a fiber, or to [`LockSupport`](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/locks/LockSupport.html)'s `park` and `unpark` methods if the strand is a thread (`LockSupport` lies at the core of all `java.util.concurrent` classes). This allows to create synchronization mechanisms that work well for both fibers and threads. 

Just as you almost never use `LockSupport` directly, so, too, you will never need to call `Strand.park` or `Strand.unpark`, unless you're writing your own concurrency constructs (like a new kind of lock).

### Transforming any Asynchronous Callback to A Fiber-Blocking Operation

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

### Advanced Fiber Usage {#advanced-fibers}

#### Fiber Internals 

We will now cover in some depth the inner workings of Quasar fibers. You should read this section if you'd like to annotate suspendable methods with the `@Suspendable` annotation rather than by declaring `throws SuspendExecution`, or if you're just curious.

Internally, a fiber is a *continuation* which is then scheduled in a scheduler. A continuation captures the instantaneous state of a computation, and allows it to be suspended and then resumed at a later time from the point where it was suspended. Quasar creates continuations by instrumenting (at the bytecode level) suspendable methods. For scheduling, Quasar uses `ForkJoinPool`, which is a very efficient, work-stealing, multi-threaded scheduler.

Whenever a class is loaded, Quasar's instrumentation module (usually run as a Java agent) scans it for suspendable methods. Every suspendable method `f` is then instrumented in the following way: It is scanned for calls to other suspendable methods. For every call to a suspendable method `g`, some code is inserted before (and after) the call to `g` that saves (and restores) the state of a local variables to the fiber's stack (a fiber manages its own stack), and records the fact that this (i.e. the call to `g`) is a possible suspension point. At the end of this "suspendable function chain", we'll find a call to `Fiber.park`. `park` suspends the fiber by throwing a `SuspendExecution` exception (which the instrumentation prevents you from catching, even if your method contains a `catch(Throwable t)` block).

If `g` indeed blocks, the `SuspendExecution` exception will be caught by the `Fiber` class. When the fiber is awakened (with `unpark`), method `f` will be called, and then the execution record will show that we're blocked at the call to `g`, so we'll immediately jump to the line in `f` where `g` is called, and call it. Finally, we'll reach the actual suspension point (the call to `park`), where we'll resume execution immediately following the call. When `g` returns, the code inserted in `f` will restore `f`'s local variables from the fiber stack.

This process sounds complicated, but its incurs a performance overhead of no more than 3%-5%.

#### @Suspendable

So far, our way to specify a suspendable method is by declaring it throws `SusepndExecution`. This is convenient because `SuspendExecution` is a checked exception, so if `f` calls `g` and `g` is suspendable, the Java compiler will force us to declare that `f` is suspendable (and it must be because it calls `g` and `g` might be suspended).

Sometimes, however, we cannot declare `f` to throw `SuspendExecution`. One example is that `f` is an implementation of an interface method, and we cannot (or don't want to) change the interface so that it throws `SuspendExecution`. It is also possible that we want `f` to be run in regular threads as well as fibers. 

An example for that are the synchronization primitives in the `co.paralleluniverse.strands.concurrent` package, which implement interfaces declared in `java.util.concurrent`, and we want to maintain compatibility. Also, no harm will come if we use these classes in regular threads. They will work just as well for threads as for fibers, because internally they call `Strand.park` which is fiber-blocking (suspends) if run in a fiber, but simply blocks the thread if not.

So, suppose method `f` is declared in interface `I`, and we'd like to make its implementation in class `C` suspendable. The compiler will not let us declare that we throw `SuspendExecution` because that will conflict with `f`'s declaration in `I`.

What we do, then, is annotate `C.f` with the `@Suspendable` annotation (in the `co.paralleluniverse.fibers` package). Assuming `C.f` calls `park` or some other suspendable method `g` – which does declare `throws SuspendExecution`, we need to surround `f`'s body with `try {} catch(SuspendExecution)` just so the method will compile, like so:

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

Therefore, if you'd like to use the `@Suspendable` annotation, there's a step you need to add to your build step, after compilation and before creating the jar file: running the `co.paralleluniverse.fibers.instrument.SuspendablesScanner` Ant task. In Gradle it looks like this:

~~~ groovy
ant.taskdef(name:'scanSuspendables', classname:'co.paralleluniverse.fibers.instrument.SuspendablesScanner', 
    classpath: "build/classes/main:" + configurations.compile.asPath)
ant.scanSuspendables(outputFile:"$sourceSets.main.output.resourcesDir/META-INF/suspendable-supers") {
    fileset(dir: sourceSets.main.output.classesDir)
}
~~~

`SuspendablesScanner` scans your code after it's been compiled for methods annotated with `@Suspendable`. In our example it will find `C.f`. It will then see that `C.f` is an implementation of `I.f`, and so it will list `I.f` in a text file (`META-INF/suspendable-supers`), that contains all methods that have overriding suspendable implementations. 

When the instrumentation module instruments `h`, it will find `I.f` in the file, and, knowing it might suspend, inject the appropriate code.

Note that this has no effect on other calls to `I.f`. The instrumentation module only cares that `I.f` has suspendable implementations when it finds it called in suspendable methods (in our case: `h`).

When using [AOT instrumentation](#aot), `InstrumentationTask` must be able to find the `suspendable-supers` in its classpath. 

### Channels {#channels}

Channels are queues used to pass messages between strands (remember, strands are a general name for threads and fibers). If you are familiar with Go, Quasar channels are like Go channels. 

A [channel]({{javadoc}}/strands/channels/Channel.html) is an interface that extends two other interfaces: [`SendPort`]({{javadoc}}/strands/channels/SendPort.html), which defines the methods used to send messages to a channel, and [`ReceivePort`]({{javadoc}}/strands/channels/ReceivePort.html), which defines the methods used to receive messages from a channel.

Channels are normally created by calling any of the `newChannel` static methods of the [`Channels`]({{javadoc}}/strands/channels/Channels.html) class. The `newChannel` methods create a channel with a specified set of properties. Those properties are:

* `bufferSize` – if positive, the number of messages that the channel can hold in an internal buffer; `0` for a *transfer* channel, i.e. a channel with no internal buffer. or `-1` for a channel with an unbounded (infinite) buffer.
* `policy`         – the [`OverflowPolicy`]({{javadoc}}/strands/channels/OverflowPolicy.html) specifying how the channel (if bounded) will behave if its internal buffer overflows.
* `singleProducer` – whether the channel will be used by a single producer strand.
* `singleConsumer` – whether the channel will be used by a single consumer strand.

Note that not all property combinations are supported. Consult the [`Javadoc`]({{javadoc}}/strands/channels/Channels.html) for details.
 
#### Sending and Receiving Messages

Messages are sent to a channel using the [`SendPort.send`]({{javadoc}}/strands/channels/SendPort.html#send(Message)) method. The `send` method blocks if the channel's buffer is full and the channel has been configured with the `BLOCK` overflow policy. There are versions of `send` that block indefinitely or up to a given timeout, and the `trySend` method sends a message if the channel's buffer has room, or returns immediately, without blocking, if not. Consult the [Javadoc]({{javadoc}}/strands/channels/SendPort.html) for details.

Messages are received from a channel using the [`ReceivePort.receive`]({{javadoc}}/strands/channels/ReceivePort.html#receive()) method. There are versions of `receive` that block indefinitely or up to a given timeout, and the `tryReceive` method receives a message if one is available, or returns immediately, without blocking, if not. Consult the [Javadoc]({{javadoc}}/strands/channels/ReceivePort.html) for details.

A channel can be closed with the `close` method, found in both `RecivePort` and `SendPort`. All messages sent to the channel after the `close` method has been called will be silently ignored, but all those sent before will still be available (when calling `receive`). After all messages sent before the channel closed are consumed, the `receive` function will return `null`, and [`ReceivePort.isClosed()`]({{javadoc}}/strands/channels/ReceivePort.html#isClosed()) will return `true`.

{:.alert .alert-info}
**Note**: As usual, while the blocking channel methods declare to throw `SuspendExecution`, this exception will never actually be thrown. If using channels in a plain thread, you should `catch(SuspendExecution e) { throw AssertionError(); }`. Alternatively, you can use the convenience wrappers [`ThreadReceivePort`]({{javadoc}}/strands/channels/ThreadReceivePort.html) and [`ThreadSendPort`]({{javadoc}}/strands/channels/ThreadSendPort.html).

#### Primitive Channels

Quasar provides 4 types of channels for primitive data types: `int`, `long`, `float` and `double`. Consult the Javadoc of, for example, [`IntSendPort`]({{javadoc}}/strands/channels/IntSendPort.html) [`IntReceivePort`]({{javadoc}}/strands/channels/IntReceivePort.html) and for details.

All primitive channels do not support multiple consumers.


#### Ticker Channels

A channel created with the `DISPLACE` overflow policy is called a *ticker channel* because it provides guarantees similar to that of a digital stock-ticker: you can start watching at any time, the messages you read are always read in order, but because of the limited screen size, if you look away or read to slowly you may miss some messages.

The ticker channel is useful when a program component continually broadcasts some information. The size channel's circular buffer, its "screen" if you like, gives the subscribers some leeway if they occasionally fall behind reading.

A ticker channel is single-consumer, i.e. only one strand is allowed to consume messages from the channel. On the other hand, it is possible, and useful, to create several views of the channel, each used by a different consumer strand. A view (which is of type [`TickerChannelConsumer`]({{javadoc}}/strands/channels/TickerChannelConsumer.html) is created with the [`Channels.newTickerConsumerFor`]({{javadoc}}/strands/channels/Channels.html#newTickerConsumerFor(Channel)) method.

The method returns a `ReceivePort` that can be used to receive messages from `channel`. Each ticker-consumer will yield monotonic messages, namely no message will be received more than once, and the messages will be received in the order they're sent, but if the consumer is too slow, messages could be lost. 

Each consumer strand will use its own `ticker-consumer`, and each can consume messages at its own pace, and each `TickerChannelConsumer` port will return the same messages (messages consumed from one will not be removed from the other views), subject possibly to different messages being missed by different consumers depending on their pace.

#### Transofrming Channels (AKA Reactive Extensions)

The [`Channels`]({{javadoc}}/strands/channels/Channels.html) class has several static methods that can be used to manipulate and compose values sent to or received off channels:

* map - returns a channel that transforms messages by applying a given mapping function. There are two versions of `map`: [one that operates]({{javadoc}}/strands/channels/Channels.html#map(co.paralleluniverse.strands.channels.ReceivePort, com.google.common.base.Function)) on `ReceivePort` and [one that operates]({{javadoc}}/strands/channels/Channels.html#map(co.paralleluniverse.strands.channels.SendPort, com.google.common.base.Function)) on `SendPort`.
* `filter` - returns a channel that only lets messages that satisfy a predicate through. There are two versions of `filter`: [one that operates]({{javadoc}}/strands/channels/Channels.html#filter(co.paralleluniverse.strands.channels.ReceivePort, com.google.common.base.Predicate)) on `ReceivePort` and [one that operates]({{javadoc}}/strands/channels/Channels.html#filter(co.paralleluniverse.strands.channels.SendPort, com.google.common.base.Predicate)) on `SendPort`.
* [`zip`]({{javadoc}}/strands/channels/Channels.html#zip(com.google.common.base.Function, co.paralleluniverse.strands.channels.ReceivePort...)) - returns a channel that combines each vector of messages from a vector of channels into a single combined message.
* [`group`]({{javadoc}}/strands/channels/Channels.html#group(co.paralleluniverse.strands.channels.ReceivePort...)) - returns a channel that funnels messages from a set of given channels.


#### Channel Selection

A powerful tool when working with channels is the ability to wait on several channel operations at once. If you are familiar with the Go programming language, this capability is provided by the `select` statement.

The [`Selector`]({{javadoc}}/strands/channels/Selector.html) class exposes several static methods that allow *channel selection*. The basic idea is this: you declare several channel operations (sends and receives), each possibly operating on a different channel, and then use `Selector` to perform at most one.

Here is an example of using `Selector`. For details, please consult the [Javadoc]({{javadoc}}/strands/channels/Selector.html):

~~~ java
SelectAction sa = Selector.select(Selector.receive(ch1), Selector.send(ch2, msg));
~~~

The example will do exactly one of the following operations: send `msg` to `ch1` or receive a message from `ch2`.

### Delay Variables

Delay variables, delayed values, or dataflow variables (represented by the [`DelayedVal`]({{javadoc}}/strands/channels/DelayedVal.html) class) are a simple and effective strand coordination mechanism. A `DelayedVal` can have its value set once, and when read, blocks until the value is set. `DelayVal` implements `j.u.c.Future`.

## Quasar's Actor System

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

To use the terms we've learned so far, an *actor* is a strand that owns a single channel with some added lifecyce management and error handling. But this reductionist view of actors does them little justice. Actors are fundamental building blocks that are combined to build a fault-tolerant application. If you are familiar with Erlang, Quasar actors are just like Erlang processes.

An actor is a self-contained execution unit with well-defined inputs and outputs. Actors communicate with other actors (as well as regular program threads and fibers) by passing messages.

{:.alert .alert-info}
**Note**: Actors may write to and read from channels other than their own mailbox. In fact, actors can do whatever regular fibers can.

#### Creating Actors

All actors extends the [`Actor`]({{javadoc}}/actors/Actor.html) class. The constructor takes the actor's name (which does not have to be unique, and may even be `null`), and its mailbox settings (of type [`MailboxConfig`]({{javadoc}}/actors/MailboxConfig.html)).

`MailboxConfig` defines the mailbox size (the number of messages that can wait in the mailbox channel), with `-1` specifying an unbounded mailbox, and an *overflow policy*. The overflow policy works the same as for plain channels, except that the `THROW` policy doesn't cause an exception to be thrown in the sender if the mailbox capacity is exceeded, but rather throws an exception into the receiving actor (the exception will be thrown when the actor next blocks on a `recieve`).

An actor is required to implement the [`doRun`]({{javadoc}}/actors/Actor.html#doRun()) method. This method is the actor body, and is run when the actor is spawned.

It is prefereable to subclass [`BasicActor`]({{javadoc}}/actors/BasicActor.html) rather than `Actor`; `BasicActor` provides the ability to perform selective receives (more on that later).

#### Spawning Actors

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

### Sending and Receiving Messages, `ActorRef`

The `spawn` method returns an instance of [`ActorRef`]({{javadoc}}/actors/ActorRef.html). All (almost) interactions with an actor take place through its `ActorRef`, which can also be obtained by calling `ref()` on the actor. The `ActorRef` is used as a level of indirection that provides additional isolation for the actor (and actors are all about isolation). It enables things like hot code swapping and more.

[`ActorRef.self()`]({{javadoc}}/actors/ActorRef.html#self()) is a static function that returns the currently executing actor's ref, and [`Actor.self()`]({{javadoc}}/actors/Actor.html#self()) is a protected member function that returns an actor's ref. Use them to obtain and share an actor's ref with other actors.

{:.alert .alert-warn}
**Note**: An actor must *never* pass a direct reference to itself to other actors or to be used on other strands. However, it may share its `ActorRef` freely. In fact, no class should hold a direct Java reference to an actor object other than classes that are part of the actor.

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

{:.alert .alert-warn}
**Note**: Because messages can be read by the actor at any time, you must take great care to only send messages that are immutable, or, at the very least, ensure that the sender does not retain a reference to the message after it is sent. Failing to do so may result in nasty race-condition bugs.

#### Actors vs. Channels

One of the reasons of providing a different `receive` function for actors is because programming with actors is conceptually different from just using fibers and channels. I think of channels as hoses pumping data into a function, or as sort of like asynchronous parameters. A fiber may pull many different kinds of data from many different channels, and combine the data in some way. 

Actors are a different abstraction. They are more like objects in object-oriented languages, assigned to a single thread. The mailbox serves as the object's dispatch mechanism; it's not a hose but a switchboard. It's for this reason that actors often need to pattern-match their mailbox messages, while regular channels – each usually serving as a conduit for a single kind of data – don't.

#### Selective Receive

An actor is a state machine. It usually encompasses some *state* and the messages it receives trigger *state transitions*. But because the actor has no control over which messages it receives and when (which can be a result of either other actors' behavior, or even the way the OS schedules threads), an actor would be required to process any message and any state, and build a full *state transition matrix*, namely how to transition whenever *any* messages is received at *any* state.

This can not only lead to code explosion; it can lead to bugs. The key to managing a complex state machine is by not handling messages in the order they arrive, but in the order we wish to process them. If your actor extends [`BasicActor`]({{javadoc}}/actors/BasicActor.html), there's [another form]({{javadoc}}/actors/BasicActor.html#receive(co.paralleluniverse.actors.MessageProcessor)) of the `receive` method that allows for *selective receive*. This method takes an instance of [`MessageProcessor`]({{javadoc}}/actors/MessageProcessor.html), which *selects* messages out of the mailbox (a message is selected iff `MessageProcessor.process` returns a non-null value when it is passed the message). Alternatively (to extending `BasicActor`, you can make use of the [`SelectiveReceiveHelper`]({{javadoc}}/actors/SelectiveReceiveHelper.html) class.

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

### Error Handling

The actor model does not only make concurrency easy; it also helps build fault-tolerant systems by compartmentalizing failure. Each actor is it's own execution context - if it encounters an exception, only the actor is directly affected (like a thread, only actors are lightweight). Unlike regular functions/objects, where an exception has to be caught and handled immediately on the callstack, with actors we can completely separate code execution from error handling.

In fact, when using actors, it is often best to to follow the [philosophy laid out by Joe Armstrong](http://www.erlang.org/download/armstrong_thesis_2003.pdf), Erlang's chief designer, of "let it crash". The idea is not to try and catch exceptions inside an actor, because attempting to catch and handle all exceptions is futile. Instead, we just let the actor crash, monitor its death elsewhere, and then take some action.

The principle of actor error handling is that an actor can be asked to be notified of another actor's death and its cause. This is done through *linking* or *watching*. 

#### Linking and Watching Actors

*Linking* two actors causes the death of one to throw an exception in the other. Two actors are linked with the [`link`]({{javadoc}}/actors/Actor.html#link(co.paralleluniverse.actors.ActorRef)) method of the `Actor` class, and can be unlinked with the [`unlink`]({{javadoc}}/actors/Actor.html#unlink(co.paralleluniverse.actors.ActorRef)) method. A link is symmetric: `a.link(b)` has the exact same effect of `b.link(a)`. The next section explains in detail how the linking mechanism works.

A more robust way of being notified of actor death than linking is with a *watch* (called *monitor* in Erlang; this is one of the few occasions we have abandoned the Erlang function names). To make an actor watch another you use the [`watch`]({{javadoc}}/actors/Actor.html#watch(co.paralleluniverse.actors.ActorRef)) method. When a watched actor, its watcher actor (or many watching actors) receives an `ExitMessage`, explained in the next section. Unlike links, watches are asymmetric (if A watches B, B does not necessarily watch A), and they are also composable: the `watch` method returns a *watch-id* object that identifies the particular watch; every `ExitMessage` contains that *watch-id* object that uniquely identifies the watch that caused the message to be received. If an actor calls the `watch` method several times with the same argument (i.e. it watches the same actor more than once), a message will be received for each of these different watches. A watch can be undone with the [`unwatch`]({{javadoc}}/actors/Actor.html#unwatch(co.paralleluniverse.actors.ActorRef, java.lang.Object)) method.

#### Lifecycle Messages and Lifecycle Exceptions

When actor B that is linked to or watched by actor A dies, it automatically sends an [`ExitMessage`]({{javadoc}}/actors/ExitMessage.html) to A. The message is put in A's mailbox and retrieved when A calls `receive` or `tryReceive`, but it isn't actually returned by those methods. 

When `receive` (or `tryRecive`) is called, it takes the next message in the mailbox, and passes it to a protected method called [`filterMessage`]({{javadoc}}/actors/Actor.html#filterMessage(java.lang.Object)). Whatever `filterMessage` returns, that's the message actually returned by `receive` (or `tryReceive`), but it `filterMessage` returns `null`, `receive` will not return and wait for the next message (and `tryReceive` will check if another message is already available, or otherwise return `null`). The default implementation of `filterMessage` always returns the message it received unless it is of type [`LifecycleMessage`]({{javadoc}}/actors/LifecycleMessage.html), in which case it passes it to the protected [`handleLifecycleMessage`]({{javadoc}}/actors/Actor.html#handleLifecycleMessage(co.paralleluniverse.actors.LifecycleMessage)) method.

[`handleLifecycleMessage`]({{javadoc}}/actors/Actor.html#handleLifecycleMessage(co.paralleluniverse.actors.LifecycleMessage)) examines the message. If it is an [`ExitMessage`]({{javadoc}}/actors/ExitMessage.html) (which extends `LifecycleMessage`), it checks to see if it's been sent as a result of a *watch* (by testing whether its [`getWatch`]({{javadoc}}/actors/ExitMessage.html#getWatch()) method returns a non-null value). If it is, it's silently ignored. But if it's a result of a *linked* actor dying (`getWatch()` returns `null`), the method throws a [`LifecycleException`]({{javadoc}}/actors/LifecycleException.html). This exception is thrown, in turn, by actor A's call to `receive` (or `tryReceive`). You can override `handleLifecycleMessage` to change this behavior.

If you do not want actor A to die if linked actor B does, you should surround the call to `receive` or `tryReceive` with a `try {} catch(LifecycleException) {}` block.

While you *can* override the `filterMessage` or the `handleLifecycleMessage` method, but will seldom have reason to override the latter, and almost never should override the former.

### Registering Actors

*Registering* an actor gives it a public name that can be used to locate the actor. You register an actor with the [`register`]({{javadoc}}/actors/Actor.html#register()) method of the `Actor` class, and unregister with the [`unregister`]({{javadoc}}/actors/Actor.html#unregister()) method. To find an actor by its name, use the [`ActorRegistry.getActor`]({{javadoc}}/actors/ActorRegistry.html#getActor(java.lang.String)) static method.

If you're running Quasar in a cluster configuration (see [Clustering](cluster.html)), registering an actor makes it globally available in the cluster. Calling `ActorRegistry.getActor` on any remote node would return a remote reference to the actor.

In addition, registering an actor automatically sets up monitoring for the actor, as explained in the next section.

### Monitoring Actors

All actors running in a JVM instance are monitored by a [MXBean]({{javadoc}}/actors/ActorsMXBean.html) registered with the name `"co.paralleluniverse:type=Actors"`. For details, please consult the [Javadoc]({{javadoc}}/actors/ActorsMXBean.html).

In addition, you can create a an [MXBean]({{javadoc}}/actors/ActorMXBean.html) that monitors a specific actor by calling the actor's [`monitor`]({{javadoc}}/actors/Actor.html#monitor()) method. That MBean will be registered as `"co.paralleluniverse:type=quasar,monitor=actor,name=ACTOR_NAME"`.This happens automatically when an actor is registered.

A monitored actor (either as a result of it being registered or of having called the `monitor` method) can have its MBean removed by calling the [`stopMonitor`]({{javadoc}}/actors/Actor.html#stopMonitor()) method.

### Behaviors

Erlang's designers have realized that many actors follow some common patterns - like an actor that receives requests for work and then sends back a result to the requester. They've turned those patterns into actor templates, called behaviors, in order to save poeple work and avoid some common errors. Erlang serves as the main inspiration to Quasar Actors, so some of these behaviors have been ported to Quasar. 

#### RequestReplyHelper

A very common pattern that emerges when working with patterns is request-response, whereby a *request* message is sent to an actor, and a *response* is sent back to the sender of the request. While simple, some care must be taken to ensure that the response is matched with the correct request. 

This behavior is implemented for you in the [`RequestReplyHelper`]({{javadoc}}/actors/behaviors/RequestReplyHelper.html) class (in the `co.paralleluniverse.actors.behaviors` package). 

To use it, the request message must extend [`co.paralleluniverse.actors.behaviors.RequestMessage`]({{javadoc}}/actors/behaviors/RequestMessage.html). Suppose we have a `IsDivisibleBy` message class that extends `RequestMessage`. We can interact with a divisor-checking actor like so:

~~~ java
boolean result = RequestReplyHelper.call(actor, new IsDivisibleBy(100, 50));
~~~

And define the actor thus:

~~~ java
ActorRef<IsDivisibleBy> actor = new Actor<IsDivisibleBy, Void>(null, null) {
    protected Void doRun() {
        for(;;) {
            IsDivisibleBy msg = receive();
            try {
                boolean result = (msg.getNumber() % msg.getDivisor() == 0);
                RequestReplyHelper.reply(msg, result);
            } catch (ArithmeticException e) {
                RequestReplyHelper.replyError(msg, e);
            }
        }
    }
}.spawn();
~~~

In the case of an `ArithmeticException` (if the divisor is 0), the exception will be thrown by `RequestReplyHelper.call`. 

One of the nicest things about the `RequestReplyHelper` class, is that the code calling `call` does not have to be an actor. It can be called by a regular thread (or fiber). But if you examine the code of the `reply` method, you'll see that it simply sends a response message to the request's sender, which is an actor. This is achieved by the `call` method creating a temporary virtual actor, that will receive the reply message.

#### Behavior Actors

Similarly to Erlang, Quasar includes "actor templates" for some common actor behaviors, called *behavior actors*. Their functionality is separated in two: the implementation, which extends [`BehaviorActor`]({{javadoc}}/actors/behaviors/BehaviorActor.html) and standardize handling of standard messages, and the interface, which extends [`Behavior`]({{javadoc}}/actors/behaviors/Behavior.html) (which, in turn, extends `ActorRef`), and includes additional methods to those of `ActorRef`. It's important to note that those interface methods do nothing more than assist in the creation and sending of said standard messages to the actor implementation. They employ no new construct.

By itself, [`BehaviorActor`]({{javadoc}}/actors/behaviors/BehaviorActor.html) provides handling for [`ShutdownMessage`]({{javadoc}}/actors/ShutdownMessage.html), which, as its name suggests, requests an actor to shut itself down, along with the accompanying [`shutdown`]({{javadoc}}/actors/behaviors/Behavior.html#shutdown()) method in the [`Behavior`]({{javadoc}}/actors/behaviors/Behavior.html) class (the "interface" side). In addition, `BehaviorActor` defines standard initialization and termination methods which may be overriden. You should consult the [Javadoc]({{javadoc}}/actors/behaviors/BehaviorActor.html) for more detail.

When a behavior actor is spawned, its `spawn` (or `spawnThread`) method returns its "interface" (which is also an `ActorRef`).

{:.alert .alert-info}
**Note:** Behavior actors usually have different constructors for convenience. Those that do not take an explicit `MailboxConfig` parameter, use the default configuration of *an unbounded mailbox*. 

#### Server

The *server* behavior is an actor that implements a request-reply model. The behavior implementation is found in [`ServerActor`]({{javadoc}}/actors/behaviors/ServerActor.html), and the interface is [`Server`]({{javadoc}}/actors/behaviors/Server.html).

You can implement a server actor by subclassing `ServerActor` and overriding the some or all of the methods:
 
* init
* terminate 
* handleCall
* handleCast
* handleInfo
* handleTimeout

or by providing an instance of [`ServerHandler`]({{javadoc}}/actors/behaviors/ServerHandler.html) which implements these methods to the `ServerActor` constructor. Please consult the `ServerActor` [JavaDoc]({{javadoc}}/actors/behaviors/ServerActor.html) for details.

The interface, [`Server`]({{javadoc}}/actors/behaviors/Server.html), adds additional methods to `ActorRef`, such as `call` and `cast`, that allow sending synchronous (a request that waits for a response) or asynchronous (a request that does not wait for a response) requests to the server actor.

#### EventSource

The event-source behavior is an actor that can be notified of *event* messages, which are delivered to *event handlers* which may be registered with the actor.

To create an event source actor, simply construct an instance of the [`EventSourceActor`]({{javadoc}}/actors/behaviors/EventSourceActor.html) class. Event handlers are instances of [`EventHandler`]({{javadoc}}/actors/behaviors/EventHandler.html). Event handlers can be registered or unregistered with the actor, and events sent to the actor, through the behavior's interface, the [`EventSource`]({{javadoc}}/actors/behaviors/EventSource.html) class.

Event handlers are called synchronously on the same strand as the actor's, so they may delay processing by other handlers if they block the strand.

### Supervisors

The last behavior actor, the *supervisor* deserves a chapter of its own, as it's at the core of the actor model's error handling philosophy.

Actors provide fault isolation. When an exception occurs in an actor it can only (directly) take down that actor. Actors also provide fault detection and identification. As we've seen, other actors can be notified of an actor's deat, as well as its cause, via watches and links.

Like other behaviors, the *supervisor* is a behavior that codifies and standardizes good actor practices; in this case: fault handling. As its name implies, a supervisor is an actor that supervises one or more other actors and watches them to detect their death. When a supervised (or *child*) actor dies, the supervisor can take several pre-configured actions such as restarting the dead actor or killing and restarting all children. The supervisor might also choose to kill itself and *escalate* the problem, possibly to its own supervisor.

Actors performing business logic, "worker actors", are supervised by a supervisor actor that detects when they die and takes one of several pre-configured actions. Supervisors may, in turn, be supervised by other supervisors, thus forming a *supervision hierarchy* that compartmentalizes failure and recovery. 

The basic philosophy behind supervisor-based fault handling was named "let it crash" by Erlang's designer, Joe Armstrong. The idea is that instead of trying to fix the program state after every expected exception, we simply let an actor crash when it encounters an unexcpected condition and "reboot" it.

A supervisors works as follows: it has a number of *children*, worker actors or other supervisors that are registered to be supervised wither at the supervisor's construction time or at a later time. Each child has a mode (represented by the [`Supervisor.ChildMode`]({{javadoc}}/actors/behaviors/Supervisor.ChildMode.html) class): 
`PERMANENT`, `TRANSIENT` or `TEMPORARY` that determines whether its death will trigger the supervisor's *recovery event*. When the recovery event is triggered, the supervisor takes action specified by its *restart strategy* - represented by the [`SupervisorActor.RestartStrategy`]({{javadoc}}/actors/behaviors/SupervisorActor.RestartStrategy.html) class - or it will give up and fail, depending on predefined failure modes. 

When a child actor in the `PERMANENT` mode dies, it will always trigger its supervisor's recovery event. When a child in the `TRANSIENT` mode dies, it will trigger a recovery event only if it has died as a result of an exception, but not if it has simply finished its operation. A `TEMPORARY` child never triggers it supervisor's recovery event.

A supervisor's *restart strategy* determines what it does during a *recovery event*: A strategy of `ESCALATE` measns that the supervisor will shut down ("kill") all its surviving children and then die; a `ONE_FOR_ONE` strategy will restart the dead child; an `ALL_FOR_ONE` strategy will shut down all children and then restart them all; a `REST_FOR_ONE` strategy will shut down and restart all those children added to the suervisor after the dead child.

Children can be added to the supervisor actor either at construction time or later, with [`Supervisor`]({{javadoc}}/actors/behaviors/Supervisor.html)'s `addChild` method. A child is added by passing a [`ChildSpec`]({{javadoc}}/actors/behaviors/Supervisor.ChildSpec.html) to the supervisor. The `ChildSpec` contains the means of how to start the actor, usually in the form of an [`ActorSpec`]({{javadoc}}/actors/actors/ActorSpec.html) (see the [next section](#actor-restarts)), or as an already constructed actor; the childs mode; and how many times an actor is allowed to be restarted in a given amount of time. If the actor is restarted too many times within the specified duration, the supervisor gives up and terminates (along with all its children) causing an escalation.

If an actor needs to know the identity of its siblings, it should add them to the supervisor manually (with `Supervisor`'s `addChild` method). For that, it needs to know the identity of its supervisor. To do that, you can construct the `ActorSpec` in the `SupervisorActor`'s `Initializer` or in `SupervisorActor.init()` method (subclass `SupervisorActor`), pass `Actor.self()` to the actor's constructor, and add it to the supervisor with `addChild`. Alternatively, simply call `Actor.self()` in the child's constructor. This works because the children are constructed from specs (provided they have not been constructed by the caller) during the supervisor's run, so calling `Actor.self()` anywhere in the construction process would return the supervisor.

#### Actor Restarts {#actor-restarts}

Restarting an actor means construction a new actor and spawning it. That is why the supervisor's `ChildSpec` takes an instance of `ActorBuilder`. Usually, you'll use [`ActorSpec`]({{javadoc}}/actors/actors/ActorSpec.html) as the builder instance. Sometimes, however, you'd like to add a running actor to the supervisor, and that is why `ChildSpec` has a constructor that takes an `ActorRef`. To restart such actors, the supervisor relies on the fact that `ActorRef`s to local actors implement `ActorBuilder`. When requested to build a new actor, they call the old actor's [`reinstantiate`]({{javadoc}}/actors/Actor.html#reinstantiate()) method to create a clone of the old actor.

When an actor is restarted, the supervisor takes care to run it on the same type of strand (thread or fiber) as the old actor.

## Records

{% capture javadoc %}{{site.baseurl}}/javadoc/co/paralleluniverse{% endcapture %}

Just as actors expose their operations through a simple, standard interface that allows (pretty-much) only to send it messages, so should state be exposed through a simple, standard, interface. A record, then, is such a data-access interface akin to a map. It basically has get(field) and set(field, value) methods, only it preserves the type information of its field to provide type safety.

Records provide similar functionality to plain Java objects with public fields, but, unlike plain objects, they allow us to inject cross-cutting concerns on field-get and field-set operations, like restricting access to specific strands, or making sure that state is mutated (or read) only inside transactions.

Just as objects are instances of a classe, so too are records instances of a *record type*, represented by the [`RecordType`]({{javadoc}}/data/record/RecordType.html) class. A new record type must be declared as a static member of a class. The class must only include the definition of a single record type, and this class is called the type's *identifier class*, because it is used only to uniquely identify the record type (only its name is used internally).

Here's an example record type definition:

~~~ java
class A {
  public static final RecordType<A> aType = RecordType.newType(A.class);
  public static final IntField<A> $id = stateType.intField("id");
  public static final DoubleField<A> $foo = stateType.doubleField("id", Field.TRANSIENT);
  public static final ObjectField<A, String> $name = stateType.objectField("name", String.class);
  public static final ObjectField<A, List<String>> $emails = stateType.objectField("emails", new TypeToken<List<String>() {});
}
~~~

`A` is the type's identifier class. The fields are instances of [`Field`]({{javadoc}}/data/record/Field.html) and are, by convention, given identifiers that begin with a `$` to make it clear that they identify fields rather than values.

Record types, like classes, can extend a parent record type by providing the super-type to `RecordType.newType` or to `RecordType`'s constructor.

A new record is instantiated by calling one of `RecordType`'s `newInstance` methods. Please consult the [Javadoc]({{javadoc}}/data/record/RecordType.html) for details.

So, instead of writing `obj.getX()`, or `obj.x` we write `obj.get($x)`. What does this get us other than re-inventing what is a basic Java functionality, minus the some type safety? Like actors, records give up some type safety (we preserve the type of the `x` field, but the compiler can’t tell us whether obj even has an `x` field; similarly, if we send message `m` to actor `a`, the compiler can’t know whether `a` supports an `m` operation), they do so at well-defined interface points between separate software components. What we gain is loose coupling. For example, among other things, we gain the ability to swap the implementation of the record or actor at runtime for maintenance (hot code-swapping).

We gain other things by limiting component interaction to the narrow interfaces of actors and records, and that is the ability to insert cross-cutting concerns. For example, what happens if a method that consumes a resource is called too often? We need to explicitely insert load-handling code into the method. But if we communicate with the component through an actor interface, we can implement a general policy of handling too many messages that are thrown at any actor. Similarly with records. Parallel Universe's database, SpaceBase, uses records to restrict read and writes of shared state to well-defined transactions. Attempts to read or write state outside a transaction will throw a runtime exception.

Because records are intended to control mutability, an `ObjectField` should never reference a mutable object. `RecordType` will perform a very simple test on an `ObjectField` type and output a warning to the console if the class appears mutable. Conclusively determining whether a class is mutable or not is extremely difficult, so the test is a very simple one: it will warn if the class has public non-static, non-final fields, or if it has public methods whose name begins with "set".

## Clustering

{% capture examples %}https://github.com/{{site.github}}/tree/master/src/test/java/co/paralleluniverse/pulsar/examples{% endcapture %}

Quasar is able to run on a cluster, thereby letting actors and channels communicate across machines. The Quasar cluster runs on top of [Galaxy](http://puniverse.github.io/galaxy/), Parallel Universe's in-memory data grid. 

In this version, clustering is pretty rudimentary, but essential features should work: actors can be made discoverable on the network, messages can be passed among actors on different nodes, and an actor on a failing node will behave as expected of a dying actor with respect to exit messages sent to other, remote, *watching* it or *linked* to it.

### Enabling Clustering

First, you will need to add the `co.paralleluniverse:quasar-galaxy` artifact as a dependency to your project, and set some Galaxy cluster properties. At the very least you will need to set `"galaxy.nodeId"`, which will have to be a different `short` value for each master node. If you're running several nodes on the same machine, you will also need to set `"galaxy.port"` and `"galaxy.slave_port"`. These properties can be set in several ways. The simplest is to define them as JVM system properties (as `-D` command line arguments).However, you can also set them in the Galaxy configuration XML files or in a properties file. Please refer to the [Galaxy documentation](http://puniverse.github.io/galaxy/) for more detail

Then, to make an actor discoverable cluster-wide, all you need to do is register it with the [`register`]({{javadoc}}/actors/Actor.html#register()) method of the `Actor` class.

That's it. The actor is now known throughout the cluster, and can be accessed by calling [`ActorRegistry.getActor`]({{javadoc}}/actors/ActorRegistry.html#getActor(java.lang.String)) on any node. 

An actor doesn't have to be registered in order to be reachable on the network. Registering it simply makes it *discoverable*. If we pass an `ActorRef` of local actor in a message to a remote actor, the remote actor will be able to send messages to the local actor as well.

### Examples

There are a few examples of distributed actors in the [example package](https://github.com/puniverse/quasar/tree/master/quasar-galaxy/src/main/java/co/paralleluniverse/galaxy/example).
You can run them after cloning the repository. 

In order to run the ping pong example, start the Pong actor by:

~~~ sh
./gradlew :quasar-galaxy:run -PmainClass=co.paralleluniverse.galaxy.example.pingpong.Pong
~~~

Start the Ping actor in a different terminal by:

~~~
./gradlew :quasar-galaxy:run -PmainClass=co.paralleluniverse.galaxy.example.pingpong.Ping
~~~

To run the actors on different computers, change the following lines in the build.gradle file to the apropriate network configuration:

~~~ groovy
systemProperty "jgroups.bind_addr", "127.0.0.1"
systemProperty "galaxy.multicast.address", "225.0.0.1"
~~~

### Cluster Configuration

For instructions on how to configure the Galaxy cluster, please refere to Galaxy's [getting started guide](http://puniverse.github.io/galaxy/start/getting-started.html).

## Examples

{% capture examples %}https://github.com/{{site.github}}/tree/master/src/test/java/co/paralleluniverse/examples{% endcapture %}






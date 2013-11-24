---
layout: default
title: Clustering
weight: 4
---

{% capture examples %}https://github.com/{{site.github}}/tree/master/src/test/java/co/paralleluniverse/pulsar/examples{% endcapture %}

Quasar is able to run on a cluster, thereby letting actors and channels communicate across machines. The Quasar cluster runs on top of [Galaxy](http://puniverse.github.io/galaxy/), Parallel Universe's in-memory data grid. 

In this version, clustering is pretty rudimentary, but essential features should work: actors can be made discoverable on the network, messages can be passed among actors on different nodes, and an actor on a failing node will behave as expected of a dying actor with respect to exit messages sent to other, remote, *watching* it or *linked* to it.

## Enabling Clustering

First, you will need to add the `co.paralleluniverse:quasar-galaxy` artifact as a dependency to your project, and set some Galaxy cluster properties. At the very least you will need to set `"galaxy.nodeId"`, which will have to be a different `short` value for each master node. If you're running several nodes on the same machine, you will also need to set `"galaxy.port"` and `"galaxy.slave_port"`. These properties can be set in several ways. The simplest is to define them as JVM system properties (as `-D` command line arguments).However, you can also set them in the Galaxy configuration XML files or in a properties file. Please refer to the [Galaxy documentation](http://puniverse.github.io/galaxy/) for more detail

Then, to make an actor discoverable cluster-wide, all you need to do is register it with the [`register`]({{javadoc}}/actors/Actor.html#register()) method of the `Actor` class.

That's it. The actor is now known throughout the cluster, and can be accessed by calling [`ActorRegistry.getActor`]({{javadoc}}/actors/ActorRegistry.html#getActor(java.lang.String)) on any node. 

An actor doesn't have to be registered in order to be reachable on the network. Registering it simply makes it *discoverable*. If we pass an `ActorRef` of local actor in a message to a remote actor, the remote actor will be able to send messages to the local actor as well.

## Examples

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

## Cluster Configuration

For instructions on how to configure the Galaxy cluster, please refere to Galaxy's [getting started guide](http://puniverse.github.io/galaxy/start/getting-started.html).




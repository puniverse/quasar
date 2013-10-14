# Quasar [![Build Status](https://travis-ci.org/puniverse/quasar.png?branch=master)](https://travis-ci.org/puniverse/quasar) [![Dependency Status](http://www.versioneye.com/user/projects/525be659632bac578f002552/badge.png)](http://www.versioneye.com/user/projects/525be659632bac578f002552)<br/>Lightweight threads and actors for the JVM


## Getting started

In Maven:

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>0.3.0</version>
</dependency>
```

For clustering support add:

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-galaxy</artifactId>
    <version>0.3.0</version>
</dependency>
```

Or, build from sources by running:

```
./gradlew
```

## Usage

Currently, there isn’t much in the way of documentation (coming soon!).
In the meantime, you can study the examples [here](https://github.com/puniverse/quasar/tree/master/quasar-actors/src/test/java/co/paralleluniverse/actors).

You can also read the introductory [blog post](http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar).

When running code that uses Quasar, the instrumentation agent must be run by adding this to the `java` command line:

```
-javaagent:path-to-quasar-jar.jar
```

## Running the Distributed Examples

There are a few examples of distributed actors in the [example package](https://github.com/puniverse/quasar/tree/master/quasar-galaxy/src/main/java/co/paralleluniverse/galaxy/example).
You can run them after cloning the repository. 

In order to run the ping pong example, start the Pong actor by:
```
./gradlew :quasar-galaxy:run -PmainClass=co.paralleluniverse.galaxy.example.pingpong.Pong
```
Start the Ping actor in a different terminal by:
```
./gradlew :quasar-galaxy:run -PmainClass=co.paralleluniverse.galaxy.example.pingpong.Ping
```

To run the actors on different computers, change the following lines in the build.gradle file to the apropriate network configuration:
```
systemProperty "jgroups.bind_addr", "127.0.0.1"
systemProperty "galaxy.multicast.address", "225.0.0.1"
```

You can similarly run the other examples in the [co.paralleluniverse.galaxy.example.simplegenevent](https://github.com/puniverse/quasar/tree/master/quasar-galaxy/src/main/java/co/paralleluniverse/galaxy/example/simplegenevent) and [co.paralleluniverse.galaxy.example.simplegenserver](https://github.com/puniverse/quasar/tree/master/quasar-galaxy/src/main/java/co/paralleluniverse/galaxy/example/simplegenserver) packages.

## Getting help

Questions and suggestions are welcome at this [forum/mailing list](https://groups.google.com/forum/?fromgroups#!forum/quasar-pulsar-user).

## License 

Quasar is free software published under the following license:

```
Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation
 
  or (per the licensee's choosing)
 
under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```

[![githalytics.com alpha](https://cruel-carlota.gopagoda.com/d376531837c3513ea73279fdbee7d48b "githalytics.com")](http://githalytics.com/puniverse/quasar)

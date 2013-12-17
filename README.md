# Quasar [![Build Status](https://travis-ci.org/puniverse/quasar.png?branch=master)](https://travis-ci.org/puniverse/quasar)[![Dependency Status](https://www.versioneye.com/user/projects/52b019baec1375ace70000de/badge.png)](https://www.versioneye.com/user/projects/52b019baec1375ace70000de)<br/>Lightweight threads and actors for the JVM


## Getting started

In Maven:

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-core</artifactId>
    <version>0.3.0</version>
</dependency>
```
To use actors, add

```xml
<dependency>
    <groupId>co.paralleluniverse</groupId>
    <artifactId>quasar-actors</artifactId>
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

* [Documentation](http://puniverse.github.io/quasar/)
* [Javadoc](http://puniverse.github.io/quasar/javadoc)

You can also study the examples [here](https://github.com/puniverse/quasar/tree/master/quasar-actors/src/test/java/co/paralleluniverse/actors).

You can also read the introductory [blog post](http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar).

When running code that uses Quasar, the instrumentation agent must be run by adding this to the `java` command line:

```
-javaagent:path-to-quasar-jar.jar
```

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

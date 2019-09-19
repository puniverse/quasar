# *Quasar*<br/>Fibers, Channels and Actors for the JVM
[![Build Status](https://travis-ci.org/puniverse/quasar.svg?branch=master)](https://travis-ci.org/puniverse/quasar) [![Version](http://img.shields.io/badge/version-0.8.0-blue.svg?style=flat)](https://github.com/puniverse/quasar/releases) [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html) [![License](http://img.shields.io/badge/license-LGPL-blue.svg?style=flat)](https://www.gnu.org/licenses/lgpl.html)


## Getting started

Add the following Maven/Gradle dependencies:

| Feature          | Artifact
|------------------|------------------
| Core (required)  | `co.paralleluniverse:quasar-core:0.8.0`
| Actors           | `co.paralleluniverse:quasar-actors:0.8.0`
| Reactive Streams | `co.paralleluniverse:quasar-reactive-streams:0.8.0`
| Disruptor Channels| `co.paralleluniverse:quasar-disruptor:0.8.0`
| Kotlin (JDK8+)   | `co.paralleluniverse:quasar-kotlin:0.8.0`

Or, build from sources by running:

```
./gradlew install
```

## Usage

* [Documentation](http://docs.paralleluniverse.co/quasar/)
* [Javadoc](http://docs.paralleluniverse.co/quasar/javadoc)

You can also study the examples [here](https://github.com/puniverse/quasar/tree/master/quasar-actors/src/test/java/co/paralleluniverse/actors).

You can also read the introductory [blog post](http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar).

When running code that uses Quasar, the instrumentation agent must be run by adding this to the `java` command line:

```
-javaagent:path-to-quasar-jar.jar
```

## Related Projects

* [Pulsar](https://github.com/puniverse/pulsar) is Quasar's extra-cool Clojure API
* [Comsat](https://github.com/puniverse/comsat) integrates Quasar with the JVM's web APIs

## Getting help

Please make sure to double-check the [System Requirements](http://docs.paralleluniverse.co/quasar/#system-requirements) and [Troubleshooting](http://docs.paralleluniverse.co/quasar/#troubleshooting) sections of the docs, as well as at [currently open issues](https://github.com/puniverse/quasar/issues).

Questions and suggestions are welcome at this [forum/mailing list](https://groups.google.com/forum/?fromgroups#!forum/quasar-pulsar-user).

You can also open a [new GitHub issue](https://github.com/puniverse/quasar/issues/new) especially for bug reports and feature requests but if you're not sure please first get in touch with the community through the forum/mailing list.

## Contributions (including Pull Requests)

Please have a look at some brief [information for contributors](https://github.com/puniverse/quasar/blob/master/CONTRIBUTING.md).

## License

Quasar is free software published under the following license:

```
Copyright (c) 2013-2018, Parallel Universe Software Co. All rights reserved.

This program and the accompanying materials are dual-licensed under
either the terms of the Eclipse Public License v1.0 as published by
the Eclipse Foundation

  or (per the licensee's choosing)

under the terms of the GNU Lesser General Public License version 3.0
as published by the Free Software Foundation.
```

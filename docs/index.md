---
layout: default
title: Welcome
---

Quasar is a Java library that provides high-performance lightweight threads, Go-like channels, Erlang-like actors, and other asynchronous programming tools.

A good introduction to Pulsar (and Quasar) can be found in the blog post [Erlang (and Go) in Clojure (and Java), Lightweight Threads, Channels and Actors for the JVM](http://blog.paralleluniverse.co/post/49445260575/quasar-pulsar).

Quasar is developed by [Parallel Universe] and released as free software, dual-licensed under the Eclipse Public License and the GNU Lesser General Public License.

[Pulsar]: https://github.com/puniverse/pulsar
[Parallel Universe]: http://paralleluniverse.co

## License

    Quasar
    Copyright © 2013 Parallel Universe
    
    This program and the accompanying materials are dual-licensed under
    either the terms of the Eclipse Public License v1.0 as published by
    the Eclipse Foundation
    
      or (per the licensee's choosing)  
    
    under the terms of the GNU Lesser General Public License version 3.0
    as published by the Free Software Foundation. 


## Dependencies

* [JSR166e](http://g.oswego.edu/dl/concurrency-interest/) -- java.util.concurrent, by Doug Lea and contributors
* [ASM](http://asm.ow2.org/) --- Java bytecode manipulation and analysis framework, by the ASM team
* [Metrics](http://metrics.codahale.com/) --- A measurement and monitoring library, by Coda Hale
* [Guava](https://code.google.com/p/guava-libraries/) --- Java utility classes, by Google
* [SLF4J](http://www.slf4j.org/) --- Simple Logging Facade for Java (SLF4J)

Quasar's clustering makes use of [Galaxy](http://puniverse.github.io/galaxy/), by Parallel Universe

## Acknowledgments

A core component of Quasar, bytecode instrumentation, is a fork of the wonderful [Continuations Library](http://www.matthiasmann.de/content/view/24/26/) by Matthias Mann.

Parts of the documentation layout, icons and styles were taken from Google's [Polymer Project](http://www.polymer-project.org/).

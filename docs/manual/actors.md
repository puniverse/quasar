---
layout: default
title: Quasar's Actor System
weight: 2
---


To use the terms we've learned so far, an *actor* is a strand that owns a single channel with some added lifecyce management and error handling. But this reductionist view of actors does them little justice. Actors are fundamental building blocks that are combined to build a fault-tolerant application. If you are familiar with Erlang, Pulsar actors are just like Erlang processes.

An actor is a self-contained execution unit with well-defined inputs and outputs. Actors communicate with other actors (as well as regular program threads and fibers) by passing messages.

{:.alert .alert-info}
**Note**: Actors may write to and read from channels other than their own mailbox. In fact, actors can do whatever regular fibers can.

### Spawning actors

Actors can run in any strand – fiber or thread.

T B D

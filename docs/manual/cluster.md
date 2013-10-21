---
layout: default
title: Clustering
weight: 4
---

{% capture examples %}https://github.com/{{site.github}}/tree/master/src/test/java/co/paralleluniverse/pulsar/examples{% endcapture %}

Quasar is able to run on a cluster, thereby letting actors and channels communicate across machines. The Quasar cluster runs on top of [Galaxy](http://puniverse.github.io/galaxy/), Parallel Universe's in-memory data grid. 

In this version, clustering is pretty rudimentary, but essential features should work: actors can be made discoverable on the network, messages can be passed among actors on different nodes, and an actor on a failing node will behave as expected of a dying actor with respect to exit messages sent to other, remote, *watching* it or *linked* to it.

## Enabling Clustering

First, you will need to add `quasar-galaxy` as a dependency to your project:


T B D


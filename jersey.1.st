=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] 
java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@78b1de2d, 
java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, 
java.lang.ThreadLocal@7454f875: Instance{id=8f05dad7-e3b6-47e7-8b3c-1583f71308dc, referenceCounter=2, store size=13}, 
sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, 
java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, 
java.lang.ThreadLocal@3e727a27: null, 
java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1b44ecb9, 
java.lang.ThreadLocal@4db1b678: null, 
java.lang.ThreadLocal@4e42a821: null, 
java.lang.ThreadLocal@6a2ffb0c: null, 

=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] 
java.lang.ThreadLocal@197f676a: null, 
java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, 
java.lang.ThreadLocal@7454f875: null, 
sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, 
java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, 
java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4ecf57a2, 
java.lang.ThreadLocal@3e727a27: null, 
java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@16d60567, 
java.lang.ThreadLocal@4db1b678: null, 
java.lang.ThreadLocal@4e42a821: null, 
java.lang.ThreadLocal@6a2ffb0c: null, 

[15:49:24.446 ForkJoinPool-default-fiber-pool-worker-7 (230)]	Fiber.installFiberLocals Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] -> Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
[15:49:24.446 ForkJoinPool-default-fiber-pool-worker-7 (231)]	Fiber.switchFiberAndThreadLocals threadLocals: 
java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b5e6d39, 
java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, 
java.lang.ThreadLocal@7454f875: Instance{id=d0f6ecf6-05f9-40b5-9708-9f700df2c504, referenceCounter=2, store size=13}, 
sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, 
java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, 
java.lang.ThreadLocal@3e727a27: null, 
java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1b44ecb9, 
java.lang.ThreadLocal@4db1b678: null, 
java.lang.ThreadLocal@4e42a821: null, 
java.lang.ThreadLocal@6a2ffb0c: null, 

[15:49:25.259 ForkJoinPool-default-fiber-pool-worker-3 (836)]	Fiber.exec1 running WAITING Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b]
[15:49:25.259 ForkJoinPool-default-fiber-pool-worker-3 (837)]	Fiber.installFiberLocals Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] -> Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main]
[15:49:25.259 ForkJoinPool-default-fiber-pool-worker-3 (838)]	Fiber.switchFiberAndThreadLocals threadLocals: 
java.lang.ThreadLocal@197f676a: null, 
java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, 
java.lang.ThreadLocal@7454f875: null, 
sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, 
java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, 
java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4ecf57a2, 
java.lang.ThreadLocal@3e727a27: null, 
java.lang.ThreadLocal@4db1b678: null, 
java.lang.ThreadLocal@4e42a821: null, 
java.lang.ThreadLocal@6a2ffb0c: null, 


=====================

Executing: gradle :run
Arguments: [-PmainClass=com.example.JettyMain]

:compileJava
:processResources UP-TO-DATE
:classes
QUASAR WARNING: Debug mode enabled. This may harm performance.
QUASAR WARNING: Assertions enabled. This may harm performance.
QUASAR WARNING: Fibers are set to verify instrumentation. This may *severely* harm performance.
Aug 08, 2013 6:49:15 PM org.glassfish.jersey.server.ApplicationHandler initialize
INFO: Initiating Jersey application, version Jersey: 2.1 2013-07-18 18:14:16...
:run
Jersey app started. Hit enter to stop it...
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-19,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-42,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-41,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-36,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-32,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-33,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-43,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-38,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-18,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-27,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-30,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-35,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-24,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-37,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-40,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-34,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-28,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-39,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-44,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-29,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-26,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-25,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-31,5,main] OF SIZE 200000
STARTING FLIGHT RECORDER FOR THREAD Thread[qtp89039675-45,5,main] OF SIZE 200000
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@4[task: ParkableForkJoinTask@6d92df62, target: VoidSuspendableCallable@4587d321] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@58363f30, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: Instance{id=2db8db78-941c-41fa-8b19-03738c29e42f, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4d579aa5, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@3[task: ParkableForkJoinTask@1850890a, target: VoidSuspendableCallable@273f018f] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b5e6d39, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: Instance{id=d0f6ecf6-05f9-40b5-9708-9f700df2c504, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@50893e4c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@6[task: ParkableForkJoinTask@73aa4afc, target: VoidSuspendableCallable@312121b7] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@762c8e93, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5283973, java.lang.ThreadLocal@7454f875: Instance{id=6e5b19fe-03cf-460b-9f01-a5d3a435a94b, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@13d3afaa, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@625918e2, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4770cfdf, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@1[task: ParkableForkJoinTask@17a0a648, target: VoidSuspendableCallable@3404cd16] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@27347a08, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: Instance{id=dbd58491-ae0d-4c19-beef-92750505c341, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@53e4de92, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@b[task: ParkableForkJoinTask@131dfe52, target: VoidSuspendableCallable@1623c34c] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5eabdef, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=a72ce13c-f99a-48fd-baf0-311029f9a7ca, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@765b98f1, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@7[task: ParkableForkJoinTask@779632eb, target: VoidSuspendableCallable@25e91fa3] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6c1c19c6, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4bd9c06c, java.lang.ThreadLocal@7454f875: Instance{id=83baa6ca-b9d9-40a5-a654-08a0df88e727, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@44e8195b, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@61d8c8b4, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@28e04d9d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@8[task: ParkableForkJoinTask@1c4bdc7c, target: VoidSuspendableCallable@38785f67] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@66bf8027, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=a8ddebbc-6768-4188-afc0-e52b107b8df6, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2676b524, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@12[task: ParkableForkJoinTask@4bf1e5b5, target: VoidSuspendableCallable@761ff506] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@2c0f9980, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: Instance{id=ed4fdbea-ab10-44bd-a222-27a0295913aa, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3426b919, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@15[task: ParkableForkJoinTask@7f9fd9af, target: VoidSuspendableCallable@578cb19e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@55e128f9, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: Instance{id=e5a2fa8a-dc3e-43dd-ab92-76991af6e447, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@31491f7b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2[task: ParkableForkJoinTask@19bf8a6, target: VoidSuspendableCallable@26e15b1d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1524e0dc, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=a0772ec1-a024-4440-a1f8-64c39187324b, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2826965c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@c[task: ParkableForkJoinTask@61a572fc, target: VoidSuspendableCallable@35203079] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@544e732e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: Instance{id=514ced12-1d6d-4793-a509-04ebba343123, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6a6bfc3f, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@18[task: ParkableForkJoinTask@475d8af, target: VoidSuspendableCallable@54e4cb72] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@78e8d4e8, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@37b1dd84, java.lang.ThreadLocal@7454f875: Instance{id=8fff9e7e-15f3-49b2-843b-20b1128e71f0, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7df2095b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7e4621ac, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@e[task: ParkableForkJoinTask@faa5344, target: VoidSuspendableCallable@77409d64] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@331d4d66, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5a42330e, java.lang.ThreadLocal@7454f875: Instance{id=f7154879-ed81-4919-8314-ec19d1a1c433, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@30260a6d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@117692cd, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@48c47dc1, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@17[task: ParkableForkJoinTask@6662795b, target: VoidSuspendableCallable@4b0d79e0] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@61bc879e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=45f339c2-85db-4405-8343-2ebedcd5dd01, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@206bc833, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@6a2ffb0c: null, 
STARTING FLIGHT RECORDER FOR THREAD Thread[fiber-timeout-0,5,main] OF SIZE 200000
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@d[task: ParkableForkJoinTask@68fb261a, target: VoidSuspendableCallable@4082aace] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@429bdb78, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: Instance{id=f65f06c6-bcb4-4f7e-bbee-b2d7f6101d5a, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@665c572, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@3[task: ParkableForkJoinTask@1850890a, target: VoidSuspendableCallable@273f018f] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b5e6d39, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: Instance{id=d0f6ecf6-05f9-40b5-9708-9f700df2c504, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1b44ecb9, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@16[task: ParkableForkJoinTask@11389231, target: VoidSuspendableCallable@1905d7cc] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@4e9a9ee9, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=4ce4afef-8db7-473b-9b99-3e656649e26e, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@936fb50, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@11[task: ParkableForkJoinTask@19195b2e, target: VoidSuspendableCallable@4fb135e1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b486fa6, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4249ce6, java.lang.ThreadLocal@7454f875: Instance{id=12dc74a4-bc9f-46ab-8db1-662b6897ab94, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@183b677d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7fb8c229, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@5[task: ParkableForkJoinTask@3d019bf1, target: VoidSuspendableCallable@34a50a81] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3f54b627, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: Instance{id=ca6ba3d0-c5cd-4c5d-930e-d3ae11825435, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5282daec, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@a[task: ParkableForkJoinTask@5740ad63, target: VoidSuspendableCallable@57376b55] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7767c62e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=1f1f5240-ef97-413f-a787-8422dfe87c73, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3e043340, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@6[task: ParkableForkJoinTask@73aa4afc, target: VoidSuspendableCallable@312121b7] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@762c8e93, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5283973, java.lang.ThreadLocal@7454f875: Instance{id=6e5b19fe-03cf-460b-9f01-a5d3a435a94b, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@13d3afaa, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@625918e2, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5c0ce8f, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@f[task: ParkableForkJoinTask@3539113c, target: VoidSuspendableCallable@359a9570] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@37c9078e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: Instance{id=c77db156-6f8a-4ab7-9108-cb80ca874a3c, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6f0f183e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@9[task: ParkableForkJoinTask@4800c768, target: VoidSuspendableCallable@2b23a1c6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3051e44a, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f81b21f, java.lang.ThreadLocal@7454f875: Instance{id=86a105a5-cee5-46fc-8bcf-26aebe45e1f5, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5062cf0e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3bd54235, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@78b1de2d, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: Instance{id=8f05dad7-e3b6-47e7-8b3c-1583f71308dc, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1b44ecb9, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@1[task: ParkableForkJoinTask@17a0a648, target: VoidSuspendableCallable@3404cd16] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7e63aeeb, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: Instance{id=dbd58491-ae0d-4c19-beef-92750505c341, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@65719a42, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@1b[task: ParkableForkJoinTask@22007d90, target: VoidSuspendableCallable@4e338a60] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7e63aeeb, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: Instance{id=84f4d839-de62-4fa3-b490-d958a328aaca, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@65719a42, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@10[task: ParkableForkJoinTask@5dd23619, target: VoidSuspendableCallable@13ddd36e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@4863e4f2, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: Instance{id=68729248-0f02-4831-8f51-f2843f97d0db, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@164bf0ff, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@b[task: ParkableForkJoinTask@131dfe52, target: VoidSuspendableCallable@1623c34c] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5eabdef, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=a72ce13c-f99a-48fd-baf0-311029f9a7ca, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@215d55cb, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@23[task: ParkableForkJoinTask@796c1a02, target: VoidSuspendableCallable@281fb795] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3a4dabc7, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: Instance{id=58f9563d-46ff-467e-867f-5e8973f929b3, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4148b3b8, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@27[task: ParkableForkJoinTask@863c657, target: VoidSuspendableCallable@4e727e0e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3eb1f15e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: Instance{id=d128f2e0-9cf4-4075-92f8-104ab0958d16, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@145d727a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@24[task: ParkableForkJoinTask@306b208, target: VoidSuspendableCallable@2c922cc5] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@30b9505f, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: Instance{id=6ca62dfa-15b1-4b8b-9153-b7e50cf0ef67, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@564ba5f6, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@8[task: ParkableForkJoinTask@1c4bdc7c, target: VoidSuspendableCallable@38785f67] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@66bf8027, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=a8ddebbc-6768-4188-afc0-e52b107b8df6, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1daf86ae, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@12[task: ParkableForkJoinTask@4bf1e5b5, target: VoidSuspendableCallable@761ff506] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3eb1f15e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: Instance{id=d128f2e0-9cf4-4075-92f8-104ab0958d16, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1f99ab1c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@7[task: ParkableForkJoinTask@779632eb, target: VoidSuspendableCallable@25e91fa3] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6c1c19c6, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4bd9c06c, java.lang.ThreadLocal@7454f875: Instance{id=83baa6ca-b9d9-40a5-a654-08a0df88e727, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@44e8195b, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@61d8c8b4, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@21b0cb79, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@28[task: ParkableForkJoinTask@679a7ae9, target: VoidSuspendableCallable@4efb8c22] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@64ad34b0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: Instance{id=45a99cbf-9c1d-4089-ae2e-f1363d8b9d97, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2556d485, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@14[task: ParkableForkJoinTask@275e4273, target: VoidSuspendableCallable@1d5ca036] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@2c1a4849, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7ce5bd, java.lang.ThreadLocal@7454f875: Instance{id=304e0e34-58d6-48b6-8117-15cc5b0ef7d8, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@717fc73d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7cedb8b, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@15[task: ParkableForkJoinTask@7f9fd9af, target: VoidSuspendableCallable@578cb19e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@64ad34b0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: Instance{id=45a99cbf-9c1d-4089-ae2e-f1363d8b9d97, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2556d485, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@13[task: ParkableForkJoinTask@6bc5a139, target: VoidSuspendableCallable@7bdbdc75] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1f66c4da, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7bf1b001, java.lang.ThreadLocal@7454f875: Instance{id=a5ed24aa-668b-4388-b0f0-6c9c99804dae, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1405d451, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@385a5139, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@1a[task: ParkableForkJoinTask@ab61c1c, target: VoidSuspendableCallable@39ef77c2] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@189f7f45, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: Instance{id=1405a856-6264-4c0c-9da3-0b7a918eed03, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@f5c97a4, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@2[task: ParkableForkJoinTask@19bf8a6, target: VoidSuspendableCallable@26e15b1d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1524e0dc, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=a0772ec1-a024-4440-a1f8-64c39187324b, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2d3636de, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@4[task: ParkableForkJoinTask@6d92df62, target: VoidSuspendableCallable@4587d321] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@189f7f45, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: Instance{id=1405a856-6264-4c0c-9da3-0b7a918eed03, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5aad33e4, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@c[task: ParkableForkJoinTask@61a572fc, target: VoidSuspendableCallable@35203079] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3a4dabc7, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: Instance{id=58f9563d-46ff-467e-867f-5e8973f929b3, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2129ec3e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@1c[task: ParkableForkJoinTask@10b37095, target: VoidSuspendableCallable@74a44ec3] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@4f2f2008, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=0a95df29-44f7-4632-afee-491ea6d04642, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@318a9216, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@d74bf75, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1ee015d5, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@e[task: ParkableForkJoinTask@faa5344, target: VoidSuspendableCallable@77409d64] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@331d4d66, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5a42330e, java.lang.ThreadLocal@7454f875: Instance{id=f7154879-ed81-4919-8314-ec19d1a1c433, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@30260a6d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@117692cd, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2f9e62cf, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@1e[task: ParkableForkJoinTask@47691c75, target: VoidSuspendableCallable@45cb69f3] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@2d98f451, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=5a5526ad-8aa1-4a7b-a921-397fe057148d, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@14d0093a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@18[task: ParkableForkJoinTask@475d8af, target: VoidSuspendableCallable@54e4cb72] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@78e8d4e8, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@37b1dd84, java.lang.ThreadLocal@7454f875: Instance{id=8fff9e7e-15f3-49b2-843b-20b1128e71f0, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@41bf358f, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7e4621ac, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@17[task: ParkableForkJoinTask@6662795b, target: VoidSuspendableCallable@4b0d79e0] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@61bc879e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=45f339c2-85db-4405-8343-2ebedcd5dd01, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7553c244, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@1d[task: ParkableForkJoinTask@4df73a89, target: VoidSuspendableCallable@2224205c] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@71c88986, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: Instance{id=ace1ca4c-8988-486b-acbd-63c241344915, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@24bcf6c6, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2b[task: ParkableForkJoinTask@14264816, target: VoidSuspendableCallable@5d46a6af] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@49695b00, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f81b21f, java.lang.ThreadLocal@7454f875: Instance{id=bf1ff0d9-c175-4991-86b0-dbf61c87fe59, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@23d582a7, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3bd54235, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@d[task: ParkableForkJoinTask@68fb261a, target: VoidSuspendableCallable@4082aace] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@30b9505f, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: Instance{id=6ca62dfa-15b1-4b8b-9153-b7e50cf0ef67, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@21261342, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@16[task: ParkableForkJoinTask@11389231, target: VoidSuspendableCallable@1905d7cc] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@4e9a9ee9, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=4ce4afef-8db7-473b-9b99-3e656649e26e, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4aba630e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2c[task: ParkableForkJoinTask@12bd5276, target: VoidSuspendableCallable@4b70ca96] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5a04648b, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4249ce6, java.lang.ThreadLocal@7454f875: Instance{id=7dfcef73-b7ac-46a5-b607-f60257850390, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@47ea7f02, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7fb8c229, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@74388130, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@56650fd8, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@1f[task: ParkableForkJoinTask@4f35a38, target: VoidSuspendableCallable@4a81512] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@36ff5321, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=7d88bed4-f158-4151-9f1f-ffc667e185cb, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@34aab7bc, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@11[task: ParkableForkJoinTask@19195b2e, target: VoidSuspendableCallable@4fb135e1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5a04648b, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4249ce6, java.lang.ThreadLocal@7454f875: Instance{id=7dfcef73-b7ac-46a5-b607-f60257850390, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@47ea7f02, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7fb8c229, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@74388130, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@231b98ae, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@21[task: ParkableForkJoinTask@53a45d31, target: VoidSuspendableCallable@4be266dd] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7ccf5e16, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=8fdfe00f-21e0-45a7-b845-a3bf5e6781f3, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@206d0c9f, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@122a365e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@20[task: ParkableForkJoinTask@11575ea3, target: VoidSuspendableCallable@7f062839] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6d3af755, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=a18464da-7d5a-49e3-9ee1-05f43358739b, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3a442579, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@5[task: ParkableForkJoinTask@3d019bf1, target: VoidSuspendableCallable@34a50a81] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@71c88986, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: Instance{id=ace1ca4c-8988-486b-acbd-63c241344915, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4ff848d1, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@25[task: ParkableForkJoinTask@36be5783, target: VoidSuspendableCallable@a637589] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b97ab3, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: Instance{id=7f3da718-f9f5-4ceb-9655-c44010388647, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@222681bb, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@a[task: ParkableForkJoinTask@5740ad63, target: VoidSuspendableCallable@57376b55] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6d3af755, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=a18464da-7d5a-49e3-9ee1-05f43358739b, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@f46cd29, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@26[task: ParkableForkJoinTask@7e6ad8a5, target: VoidSuspendableCallable@18b5797a] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@323a4542, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: Instance{id=09d66e45-6891-4c25-a2a3-15f729b313f9, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@f9909d1, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@f[task: ParkableForkJoinTask@3539113c, target: VoidSuspendableCallable@359a9570] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7b97ab3, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: Instance{id=7f3da718-f9f5-4ceb-9655-c44010388647, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@d7dd0a0, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2f[task: ParkableForkJoinTask@3f0bbf4b, target: VoidSuspendableCallable@1c3bf543] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@24631030, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5a42330e, java.lang.ThreadLocal@7454f875: Instance{id=e7f2b985-55e5-4ce8-9ae5-8bae8938260e, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@220ed406, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@30260a6d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@117692cd, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@7b81e761, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@63bbd1f9, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@22[task: ParkableForkJoinTask@7fa342bf, target: VoidSuspendableCallable@37734b10] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@724523e0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=1dee070b-cde1-4c11-b2e1-0f2c9f3e35c2, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@14a77b98, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@193681bc, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4ecf57a2, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@16d60567, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2d[task: ParkableForkJoinTask@5972c734, target: VoidSuspendableCallable@7e7454cc] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@76b5df0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7ce5bd, java.lang.ThreadLocal@7454f875: Instance{id=54884e49-c44c-4edc-b5f6-169439f398b9, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@3fd66596, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7cedb8b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@19e43b5e, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@2e[task: ParkableForkJoinTask@5f18d1f9, target: VoidSuspendableCallable@56816335] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@489858f1, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7bf1b001, java.lang.ThreadLocal@7454f875: Instance{id=ab52b892-d121-4f5d-9d79-e9551dc32e95, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@73cf64f8, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@385a5139, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@9f75b73, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@1b[task: ParkableForkJoinTask@22007d90, target: VoidSuspendableCallable@4e338a60] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@3949cc16, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4e94e021, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@9[task: ParkableForkJoinTask@4800c768, target: VoidSuspendableCallable@2b23a1c6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@49695b00, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f81b21f, java.lang.ThreadLocal@7454f875: Instance{id=bf1ff0d9-c175-4991-86b0-dbf61c87fe59, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@e82751a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3bd54235, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@30[task: ParkableForkJoinTask@744decf5, target: VoidSuspendableCallable@2800047a] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1925fa1a, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@37b1dd84, java.lang.ThreadLocal@7454f875: Instance{id=44626a78-64c6-4735-872a-5716d41dc887, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1006bed5, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7e4621ac, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@2ada9a2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@75bf9307, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@c46cddc, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@10[task: ParkableForkJoinTask@5dd23619, target: VoidSuspendableCallable@13ddd36e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@323a4542, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: Instance{id=09d66e45-6891-4c25-a2a3-15f729b313f9, referenceCounter=2, store size=13}, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3f7188a9, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@6a2ffb0c: null, 
Aug 08, 2013 6:49:25 PM org.glassfish.jersey.server.ServerRuntime$Responder release
WARNING: Attempt to release single request processing resources has failed.
MultiException stack 1 of 1
java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.ServerRuntime$Responder.release(ServerRuntime.java:590)
	at org.glassfish.jersey.server.ServerRuntime$Responder.process(ServerRuntime.java:406)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:243)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main]
Fiber@19[task: ParkableForkJoinTask@69e5ffa, target: VoidSuspendableCallable@7af197b]
SHUTTING DOWN THE VM.
Aug 08, 2013 6:49:25 PM org.glassfish.jersey.server.ServerRuntime$Responder release
WARNING: Attempt to release single request processing resources has failed.
MultiException stack 1 of 1
java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@2a[task: ParkableForkJoinTask@1cfc547, target: VoidSuspendableCallable@6fa0b93e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@20e1ceb3, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=76990772-a723-47b1-a272-33273342c1cf, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@e7e29d7, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@7cece9af, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@25e2203c, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@48c95aa2, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.ServerRuntime$Responder.release(ServerRuntime.java:590)
	at org.glassfish.jersey.server.ServerRuntime$Responder.process(ServerRuntime.java:406)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:243)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
Fiber@1b[task: ParkableForkJoinTask@22007d90, target: VoidSuspendableCallable@4e338a60]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@23[task: ParkableForkJoinTask@796c1a02, target: VoidSuspendableCallable@281fb795] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@111603fe, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3c602175, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@29[task: ParkableForkJoinTask@6dd311d8, target: VoidSuspendableCallable@3c38b43c] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@2ec0ee07, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=257e26b8-ee6c-4499-942f-552073186e4a, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@7c752ccf, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@70d109de, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bdf07af, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7c4b258b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@27[task: ParkableForkJoinTask@863c657, target: VoidSuspendableCallable@4e727e0e] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@298e086d, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bcb416b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@2d33ee43, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@24[task: ParkableForkJoinTask@306b208, target: VoidSuspendableCallable@2c922cc5] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@2c5f87e4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@33d2a07e, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4926cac6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@11cab2af, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Aug 08, 2013 6:49:25 PM org.glassfish.jersey.server.ServerRuntime$Responder release
WARNING: Attempt to release single request processing resources has failed.
MultiException stack 1 of 1
java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.ServerRuntime$Responder.release(ServerRuntime.java:590)
	at org.glassfish.jersey.server.ServerRuntime$Responder.process(ServerRuntime.java:406)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:243)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

DUMPING FLIGHT LOG TO /Users/eitan/jersey.log.gz...
AVAILABLE RECORDERS
====================
THREAD Thread[qtp89039675-39,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-27,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-18,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-30,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-28,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-36,5,main] TOTAL RECORDED: 2 AVAILABLE: 2
THREAD Thread[qtp89039675-26,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-40,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-32,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-33,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-41,5,main] TOTAL RECORDED: 2 AVAILABLE: 2
THREAD Thread[qtp89039675-29,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-38,5,main] TOTAL RECORDED: 8 AVAILABLE: 8
THREAD Thread[qtp89039675-45,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-35,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-34,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] TOTAL RECORDED: 1093 AVAILABLE: 1093
THREAD Thread[qtp89039675-37,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[fiber-timeout-0,5,main] TOTAL RECORDED: 43 AVAILABLE: 43
THREAD Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] TOTAL RECORDED: 983 AVAILABLE: 983
THREAD Thread[ForkJoinPool-default-fiber-pool-worker-3,5,main] TOTAL RECORDED: 881 AVAILABLE: 881
THREAD Thread[qtp89039675-42,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-44,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-25,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-31,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-43,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[qtp89039675-19,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
THREAD Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] TOTAL RECORDED: 979 AVAILABLE: 979
THREAD Thread[qtp89039675-24,5,main] TOTAL RECORDED: 4 AVAILABLE: 4
Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main]
Fiber@27[task: ParkableForkJoinTask@863c657, target: VoidSuspendableCallable@4e727e0e]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@28[task: ParkableForkJoinTask@679a7ae9, target: VoidSuspendableCallable@4efb8c22] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@2154cecb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46d48285, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@29c50e17, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
Aug 08, 2013 6:49:25 PM org.glassfish.jersey.server.ServerRuntime$Responder release
WARNING: Attempt to release single request processing resources has failed.
MultiException stack 1 of 1
java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.ServerRuntime$Responder.release(ServerRuntime.java:590)
	at org.glassfish.jersey.server.ServerRuntime$Responder.process(ServerRuntime.java:406)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:243)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)

javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@14[task: ParkableForkJoinTask@275e4273, target: VoidSuspendableCallable@1d5ca036] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@76b5df0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7ce5bd, java.lang.ThreadLocal@7454f875: Instance{id=54884e49-c44c-4edc-b5f6-169439f398b9, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@3fd66596, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7cedb8b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@51a1efae, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main]
Fiber@24[task: ParkableForkJoinTask@306b208, target: VoidSuspendableCallable@2c922cc5]
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@13[task: ParkableForkJoinTask@6bc5a139, target: VoidSuspendableCallable@7bdbdc75] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@489858f1, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7bf1b001, java.lang.ThreadLocal@7454f875: Instance{id=ab52b892-d121-4f5d-9d79-e9551dc32e95, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@73cf64f8, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@385a5139, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@525daa01, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
Fiber@23[task: ParkableForkJoinTask@796c1a02, target: VoidSuspendableCallable@281fb795]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@1a[task: ParkableForkJoinTask@ab61c1c, target: VoidSuspendableCallable@39ef77c2] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: null, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@34451e6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7a5cd4b6, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@1c[task: ParkableForkJoinTask@10b37095, target: VoidSuspendableCallable@74a44ec3] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@4f2f2008, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=0a95df29-44f7-4632-afee-491ea6d04642, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@318a9216, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@d74bf75, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6491377c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main]
Fiber@28[task: ParkableForkJoinTask@679a7ae9, target: VoidSuspendableCallable@4efb8c22]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@2b[task: ParkableForkJoinTask@14264816, target: VoidSuspendableCallable@5d46a6af] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f81b21f, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@2da017b6, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3bd54235, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6974c520, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@7139edf6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1235eb8d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@1e[task: ParkableForkJoinTask@47691c75, target: VoidSuspendableCallable@45cb69f3] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@79d1e178, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@4229cbc3, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46009124, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7984f8dc, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main]
Fiber@2b[task: ParkableForkJoinTask@14264816, target: VoidSuspendableCallable@5d46a6af]
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@1d[task: ParkableForkJoinTask@4df73a89, target: VoidSuspendableCallable@2224205c] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@5e74207c, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1a00dd56, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2ec34670, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@15f96ede, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
Fiber@1e[task: ParkableForkJoinTask@47691c75, target: VoidSuspendableCallable@45cb69f3]
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2c[task: ParkableForkJoinTask@12bd5276, target: VoidSuspendableCallable@4b70ca96] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4249ce6, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@47ea7f02, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7fb8c229, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@74388130, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2addbc17, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4107e577, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@32[task: ParkableForkJoinTask@5b03c8d8, target: VoidSuspendableCallable@55770b96] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7a99d0af, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: Instance{id=ccfcd499-2f8f-4070-abda-0c9f1dea4972, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1c693c93, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@34451e6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@853d00b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main]
Fiber@1d[task: ParkableForkJoinTask@4df73a89, target: VoidSuspendableCallable@2224205c]
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main]
Fiber@1a[task: ParkableForkJoinTask@ab61c1c, target: VoidSuspendableCallable@39ef77c2]
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@21[task: ParkableForkJoinTask@53a45d31, target: VoidSuspendableCallable@4be266dd] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7ccf5e16, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=8fdfe00f-21e0-45a7-b845-a3bf5e6781f3, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@206d0c9f, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@367f8672, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
Fiber@2c[task: ParkableForkJoinTask@12bd5276, target: VoidSuspendableCallable@4b70ca96]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@1f[task: ParkableForkJoinTask@4f35a38, target: VoidSuspendableCallable@4a81512] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=1f1f5240-ef97-413f-a787-8422dfe87c73, referenceCounter=0, store size=0}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@79d1e178, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@4229cbc3, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46009124, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3cfa8c6d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@20[task: ParkableForkJoinTask@11575ea3, target: VoidSuspendableCallable@7f062839] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=5a5526ad-8aa1-4a7b-a921-397fe057148d, referenceCounter=0, store size=0}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@79d1e178, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@4229cbc3, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46009124, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@65196761, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@25[task: ParkableForkJoinTask@36be5783, target: VoidSuspendableCallable@a637589] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@66d24521, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@76b8ae43, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@56b8ed99, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6f7044a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main]
Fiber@25[task: ParkableForkJoinTask@36be5783, target: VoidSuspendableCallable@a637589]
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@34[task: ParkableForkJoinTask@433a99d8, target: VoidSuspendableCallable@3c4a5663] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@346d9c81, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=7903011b-d97b-40fe-a677-f66983a7dc37, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@6e53c251, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@318a9216, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@d74bf75, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@83df40b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@26[task: ParkableForkJoinTask@7e6ad8a5, target: VoidSuspendableCallable@18b5797a] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1f68e7c4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@204804f8, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@583e4656, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@607be55c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@31[task: ParkableForkJoinTask@6e32bbd8, target: VoidSuspendableCallable@44ca9a6d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@51608d63, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: Instance{id=68166253-0114-485b-bb44-246e91a8cf1c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@4f2948c6, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4ecf57a2, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@19e28852, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@2f[task: ParkableForkJoinTask@3f0bbf4b, target: VoidSuspendableCallable@1c3bf543] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@24631030, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5a42330e, java.lang.ThreadLocal@7454f875: Instance{id=e7f2b985-55e5-4ce8-9ae5-8bae8938260e, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@220ed406, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@30260a6d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@117692cd, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@7b81e761, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6763ccc4, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@22[task: ParkableForkJoinTask@7fa342bf, target: VoidSuspendableCallable@37734b10] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@724523e0, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=1dee070b-cde1-4c11-b2e1-0f2c9f3e35c2, referenceCounter=2, store size=13}, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@14a77b98, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@49188ce3, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2d[task: ParkableForkJoinTask@5972c734, target: VoidSuspendableCallable@7e7454cc] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7ce5bd, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@3fd66596, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7cedb8b, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6ac9b092, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@5f677803, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1f83de4, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@2e[task: ParkableForkJoinTask@5f18d1f9, target: VoidSuspendableCallable@56816335] java.lang.ThreadLocal@197f676a: null, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7bf1b001, java.lang.ThreadLocal@7454f875: null, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@73cf64f8, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@385a5139, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@15923f50, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@347aa840, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@60bc1caa, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main]
Fiber@26[task: ParkableForkJoinTask@7e6ad8a5, target: VoidSuspendableCallable@18b5797a]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@30[task: ParkableForkJoinTask@744decf5, target: VoidSuspendableCallable@2800047a] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1925fa1a, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@37b1dd84, java.lang.ThreadLocal@7454f875: Instance{id=44626a78-64c6-4735-872a-5716d41dc887, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1006bed5, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@7e4621ac, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@2ada9a2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@75bf9307, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@24c4bc13, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@29[task: ParkableForkJoinTask@6dd311d8, target: VoidSuspendableCallable@3c38b43c] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@2ec0ee07, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=257e26b8-ee6c-4499-942f-552073186e4a, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@7c752ccf, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@70d109de, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bdf07af, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3f07bf66, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@33[task: ParkableForkJoinTask@15a6e1de, target: VoidSuspendableCallable@7f36c0ec] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@63b40733, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: Instance{id=f5335ee8-bc5b-4dfa-9a8d-89040965b9af, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@465a1e0d, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@3949cc16, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4044d868, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@2a[task: ParkableForkJoinTask@1cfc547, target: VoidSuspendableCallable@6fa0b93e] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@20e1ceb3, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=76990772-a723-47b1-a272-33273342c1cf, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@e7e29d7, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@7cece9af, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@25e2203c, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@67dbb4ce, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@34[task: ParkableForkJoinTask@433a99d8, target: VoidSuspendableCallable@3c4a5663] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@346d9c81, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1e079362, java.lang.ThreadLocal@7454f875: Instance{id=7903011b-d97b-40fe-a677-f66983a7dc37, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@6e53c251, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@35d1655, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@318a9216, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@d74bf75, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@93b75a7, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@32[task: ParkableForkJoinTask@5b03c8d8, target: VoidSuspendableCallable@55770b96] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7a99d0af, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@11b7de87, java.lang.ThreadLocal@7454f875: Instance{id=ccfcd499-2f8f-4070-abda-0c9f1dea4972, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1c693c93, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6cb9118f, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@bc6cb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@34451e6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@7cdc646, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@31[task: ParkableForkJoinTask@6e32bbd8, target: VoidSuspendableCallable@44ca9a6d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@51608d63, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@69f75644, java.lang.ThreadLocal@7454f875: Instance{id=68166253-0114-485b-bb44-246e91a8cf1c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@4f2948c6, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@6d40c0b5, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@53b5fb5b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4ecf57a2, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@569421aa, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@35[task: ParkableForkJoinTask@22f3f10b, target: VoidSuspendableCallable@3d561b1a] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@600d676b, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: Instance{id=4a505e1a-07a7-4b1e-bc16-92c1c8f0604a, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@5e74207c, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1a00dd56, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2ec34670, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1000d311, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@36[task: ParkableForkJoinTask@2621094c, target: VoidSuspendableCallable@42e97022] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6bc0ea7f, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=b9a20b81-f2a8-4d55-a8dc-b85126743d3d, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@79d1e178, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@4229cbc3, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46009124, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@46636f1d, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@33[task: ParkableForkJoinTask@15a6e1de, target: VoidSuspendableCallable@7f36c0ec] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@63b40733, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2eb1cc11, java.lang.ThreadLocal@7454f875: Instance{id=f5335ee8-bc5b-4dfa-9a8d-89040965b9af, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@465a1e0d, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1acc880a, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@6637b753, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@3949cc16, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@64868d8, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@37[task: ParkableForkJoinTask@68688e1c, target: VoidSuspendableCallable@284a05d1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@13c3f101, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5283973, java.lang.ThreadLocal@7454f875: Instance{id=04e8d846-561c-4db9-8143-06b2b5291025, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@70f951bb, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@13d3afaa, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@625918e2, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@6505ef, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@250382ca, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@36[task: ParkableForkJoinTask@2621094c, target: VoidSuspendableCallable@42e97022] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@6bc0ea7f, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@78ad45e9, java.lang.ThreadLocal@7454f875: Instance{id=b9a20b81-f2a8-4d55-a8dc-b85126743d3d, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@79d1e178, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@37c901e4, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@4229cbc3, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46009124, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@16375452, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@35[task: ParkableForkJoinTask@22f3f10b, target: VoidSuspendableCallable@3d561b1a] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@600d676b, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@7d59869b, java.lang.ThreadLocal@7454f875: Instance{id=4a505e1a-07a7-4b1e-bc16-92c1c8f0604a, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@5e74207c, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@946a68, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1a00dd56, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2ec34670, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@18f4f1da, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@37[task: ParkableForkJoinTask@68688e1c, target: VoidSuspendableCallable@284a05d1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@13c3f101, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5283973, java.lang.ThreadLocal@7454f875: Instance{id=04e8d846-561c-4db9-8143-06b2b5291025, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@70f951bb, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@13d3afaa, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@625918e2, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@6505ef, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@43418cb1, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@38[task: ParkableForkJoinTask@47bdaf6, target: VoidSuspendableCallable@5f644ec6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@326fda17, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4bd9c06c, java.lang.ThreadLocal@7454f875: Instance{id=7c408151-5f5d-493e-a925-ab5f99fbc508, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@54b78bc, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@44e8195b, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@61d8c8b4, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@1de3815b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4d5735df, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@39[task: ParkableForkJoinTask@682b3448, target: VoidSuspendableCallable@6c759865] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1c684684, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=3798e2eb-5e24-44c8-bf34-102cc298e84c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@52af2320, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@206d0c9f, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1ed92ae, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@38[task: ParkableForkJoinTask@47bdaf6, target: VoidSuspendableCallable@5f644ec6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@326fda17, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4bd9c06c, java.lang.ThreadLocal@7454f875: Instance{id=7c408151-5f5d-493e-a925-ab5f99fbc508, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@54b78bc, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@44e8195b, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@61d8c8b4, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@1de3815b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1438de7f, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main]
Fiber@2e[task: ParkableForkJoinTask@5f18d1f9, target: VoidSuspendableCallable@56816335]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@39[task: ParkableForkJoinTask@682b3448, target: VoidSuspendableCallable@6c759865] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@1c684684, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5f60d39b, java.lang.ThreadLocal@7454f875: Instance{id=3798e2eb-5e24-44c8-bf34-102cc298e84c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@52af2320, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@4de49608, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@5a360f2b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@206d0c9f, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4902d674, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
javax.servlet.ServletException: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.

=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3a[task: ParkableForkJoinTask@3b8ada76, target: VoidSuspendableCallable@190c9784] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@9af67f5, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=5c2605d1-becc-4ea4-a937-037d5890887c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1dd7b098, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@14a77b98, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5ad20aca, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:353)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:372)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:335)
	at org.glassfish.jersey.servlet.ServletContainer.service(ServletContainer.java:218)
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3b[task: ParkableForkJoinTask@2d6cbcec, target: VoidSuspendableCallable@3de74140] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@196cf080, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: Instance{id=2b781fa8-b689-439a-9fb3-2e9701e56db9, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@5650b841, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@111603fe, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3f21708a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3c[task: ParkableForkJoinTask@2c639467, target: VoidSuspendableCallable@3da40cf5] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5d4ec870, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: Instance{id=ad73bdd9-1623-45ce-98fb-9ab2456bf4ed, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@2c5f87e4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@33d2a07e, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4926cac6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5f4960eb, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
	at co.paralleluniverse.fibers.jersey.ServletContainer.suspendableService(ServletContainer.java:67)
	at co.paralleluniverse.fibers.servlet.FiberGenericServlet$1.run(FiberGenericServlet.java:61)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:38)
	at co.paralleluniverse.strands.SuspendableUtils$VoidSuspendableCallable.run(SuspendableUtils.java:29)
	at co.paralleluniverse.fibers.Fiber.run(Fiber.java:715)
	at co.paralleluniverse.fibers.Fiber.run1(Fiber.java:710)
	at co.paralleluniverse.fibers.Fiber.exec1(Fiber.java:577)
	at co.paralleluniverse.fibers.Fiber.access$300(Fiber.java:69)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec1(Fiber.java:1011)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.doExec(ParkableForkJoinTask.java:76)
	at co.paralleluniverse.concurrent.forkjoin.ParkableForkJoinTask.exec(ParkableForkJoinTask.java:62)
	at co.paralleluniverse.fibers.Fiber$FiberForkJoinTask.exec(Fiber.java:1090)
	at jsr166e.ForkJoinTask.doExec(ForkJoinTask.java:261)
	at jsr166e.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:988)
	at jsr166e.ForkJoinPool.runWorker(ForkJoinPool.java:1628)
	at jsr166e.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
Caused by: A MultiException has 1 exceptions.  They are:
1. java.lang.IllegalStateException: Not inside a request scope.
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3a[task: ParkableForkJoinTask@3b8ada76, target: VoidSuspendableCallable@190c9784] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@9af67f5, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@2e4ce3f8, java.lang.ThreadLocal@7454f875: Instance{id=5c2605d1-becc-4ea4-a937-037d5890887c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1dd7b098, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3a0ac687, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@196fe6d6, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@14a77b98, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@4a7a152c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3d[task: ParkableForkJoinTask@7c3afd19, target: VoidSuspendableCallable@4beb407] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@41d3a00, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: Instance{id=f5c1b074-a4e6-4dc2-ac25-7bc68729ba3c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@66d24521, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@76b8ae43, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@56b8ed99, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1acad81c, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3e[task: ParkableForkJoinTask@f669400, target: VoidSuspendableCallable@177e93e6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@771fed16, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: Instance{id=3de7cec8-f705-4320-8014-522b10d2ce3f, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1f68e7c4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@204804f8, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@583e4656, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@51067c73, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 

	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2302)
	at org.jvnet.hk2.internal.ServiceLocatorImpl.getService(ServiceLocatorImpl.java:586)
	at org.jvnet.hk2.internal.IterableProviderImpl.get(IterableProviderImpl.java:87)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.invoke(ResourceMethodInvoker.java:362)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:343)
	at org.glassfish.jersey.server.model.ResourceMethodInvoker.apply(ResourceMethodInvoker.java:102)
	at org.glassfish.jersey.server.ServerRuntime$1.run(ServerRuntime.java:237)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:271)
	at org.glassfish.jersey.internal.Errors$1.call(Errors.java:267)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
	at org.glassfish.jersey.internal.Errors.process(Errors.java:267)
	at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:322)
	at org.glassfish.jersey.server.ServerRuntime.process(ServerRuntime.java:211)
	at org.glassfish.jersey.server.ApplicationHandler.handle(ApplicationHandler.java:979)
	at org.glassfish.jersey.servlet.WebComponent.service(WebComponent.java:344)
	... 19 more
Caused by: java.lang.IllegalStateException: Not inside a request scope.
	at com.google.common.base.Preconditions.checkState(Preconditions.java:149)
	at org.glassfish.jersey.process.internal.RequestScope.current(RequestScope.java:230)
	at org.glassfish.jersey.process.internal.RequestScope.findOrCreate(RequestScope.java:158)
	at org.jvnet.hk2.internal.Utilities.createService(Utilities.java:2296)
	... 34 more
Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main]
Fiber@2d[task: ParkableForkJoinTask@5972c734, target: VoidSuspendableCallable@7e7454cc]
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@3b[task: ParkableForkJoinTask@2d6cbcec, target: VoidSuspendableCallable@3de74140] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@196cf080, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3b6e6236, java.lang.ThreadLocal@7454f875: Instance{id=2b781fa8-b689-439a-9fb3-2e9701e56db9, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@5650b841, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@fd5d224, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@1ed7419b, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@111603fe, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@d48b94, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@3c[task: ParkableForkJoinTask@2c639467, target: VoidSuspendableCallable@3da40cf5] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@5d4ec870, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5c1c2f39, java.lang.ThreadLocal@7454f875: Instance{id=ad73bdd9-1623-45ce-98fb-9ab2456bf4ed, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@2c5f87e4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3e60766d, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@33d2a07e, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@4926cac6, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@1a8f9d8a, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@3f[task: ParkableForkJoinTask@4aafdca5, target: VoidSuspendableCallable@5fbefaf1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@734d1552, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: Instance{id=adb1f613-dde4-40b8-b7b5-c6966994137f, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@637ae9dc, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@298e086d, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bcb416b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@5b676e67, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@3d[task: ParkableForkJoinTask@7c3afd19, target: VoidSuspendableCallable@4beb407] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@41d3a00, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@4b9ac4c9, java.lang.ThreadLocal@7454f875: Instance{id=f5c1b074-a4e6-4dc2-ac25-7bc68729ba3c, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@66d24521, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1ded8ee6, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@76b8ae43, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@56b8ed99, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@66e417de, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@3e[task: ParkableForkJoinTask@f669400, target: VoidSuspendableCallable@177e93e6] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@771fed16, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@741d171e, java.lang.ThreadLocal@7454f875: Instance{id=3de7cec8-f705-4320-8014-522b10d2ce3f, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@1f68e7c4, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@21d8ee20, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@204804f8, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@583e4656, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@6b3f7bb2, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@40[task: ParkableForkJoinTask@14d21a20, target: VoidSuspendableCallable@6db1358d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@d32f43e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: Instance{id=6fecfa53-d8bd-4f35-a8c1-c63b198603e6, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@498b856a, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@2154cecb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46d48285, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@c411937, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@3f[task: ParkableForkJoinTask@4aafdca5, target: VoidSuspendableCallable@5fbefaf1] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@734d1552, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@5465282c, java.lang.ThreadLocal@7454f875: Instance{id=adb1f613-dde4-40b8-b7b5-c6966994137f, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@637ae9dc, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@3d308bfd, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@298e086d, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bcb416b, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3b905dce, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@42[task: ParkableForkJoinTask@77a9aacd, target: VoidSuspendableCallable@240cb61b] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3e87b91a, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=2a87ddc6-eaae-4986-b038-cf895cbbc7b3, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@7c752ccf, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@70d109de, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bdf07af, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@40897a08, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== bef: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@41[task: ParkableForkJoinTask@1aac452, target: VoidSuspendableCallable@723ee458] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7f018800, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=21881cfe-729b-40a3-bb25-98ef2f5651dc, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@e7e29d7, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@7cece9af, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@25e2203c, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@3d87f924, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-1,5,main] Fiber@40[task: ParkableForkJoinTask@14d21a20, target: VoidSuspendableCallable@6db1358d] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@d32f43e, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@3e36ae6d, java.lang.ThreadLocal@7454f875: Instance{id=6fecfa53-d8bd-4f35-a8c1-c63b198603e6, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@498b856a, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@77f59603, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@2154cecb, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@46d48285, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@29f5ca0b, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-7,5,main] Fiber@41[task: ParkableForkJoinTask@1aac452, target: VoidSuspendableCallable@723ee458] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@7f018800, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@1d4475f6, java.lang.ThreadLocal@7454f875: Instance{id=21881cfe-729b-40a3-bb25-98ef2f5651dc, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@e7e29d7, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@799c787c, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@7cece9af, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@25e2203c, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@62a934eb, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
=== aft: Thread[ForkJoinPool-default-fiber-pool-worker-5,5,main] Fiber@42[task: ParkableForkJoinTask@77a9aacd, target: VoidSuspendableCallable@240cb61b] java.lang.ThreadLocal@197f676a: org.eclipse.jetty.server.AsyncContextState@3e87b91a, java.lang.ThreadLocal@793cf7e8: jsr166e.ForkJoinPool$Submitter@577dc2ae, java.lang.ThreadLocal@7454f875: Instance{id=2a87ddc6-eaae-4986-b038-cf895cbbc7b3, referenceCounter=2, store size=13}, jsr166e.Striped64$ThreadHashCode@1674b452: jsr166e.Striped64$HashCode@7c752ccf, sun.nio.ch.Util$1@7a2c36aa: sun.nio.ch.Util$BufferCache@1785a62, java.lang.ThreadLocal@740b074a: java.lang.ref.SoftReference@70d109de, java.lang.ThreadLocal@7cfbc3: sun.nio.ch.IOVecWrapper@2bdf07af, java.lang.ThreadLocal@3e727a27: null, java.lang.ThreadLocal@169fd6: org.glassfish.jersey.internal.Errors@52d162e7, java.lang.ThreadLocal@4db1b678: null, java.lang.ThreadLocal@4e42a821: null, java.lang.ThreadLocal@6a2ffb0c: null, 
DUMPED FLIGHT LOG TO /Users/eitan/jersey.log.gz

BUILD SUCCESSFUL

Total time: 17.797 secs



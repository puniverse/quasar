/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
package co.paralleluniverse.galaxy;

import co.paralleluniverse.common.test.TestUtil;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Callable;
import org.gridkit.nanocloud.CloudFactory;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.jvm.JvmProps;
import org.gridkit.vicluster.telecontrol.ssh.RemoteNodeProps;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

/**
 *
 * @author eitan
 */
public class NanoCloudRemoteTest extends BaseCloudTest {
    @Rule
    public TestName name = new TestName();
    @Rule
    public TestRule watchman = TestUtil.WATCHMAN;

//    @Test
    public void test_distributed_hello_world__basic_example() throws InterruptedException {
        cloud = CloudFactory.createSimpleSshCloud();
        //cloud.node("**").x(RemoteNode.REMOTE).useSimpleRemoting();

        cloud.node("localhost");
        String cachePath = "/tmp/cache";
        RemoteNodeProps.at(cloud.node("**")).setRemoteJavaExec("java").setRemoteJarCachePath(cachePath);
        JvmProps.at(cloud.node("**")).addJvmArg("-javaagent:" + cachePath + File.separatorChar + getRemotePathToJar("jatest"));

        cloud.node("**").touch();
        cloud.node("**").exec(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                String jvmName = ManagementFactory.getRuntimeMXBean().getName();
                System.out.println("My name is '" + jvmName + "'. Hello! file ");
                return null;
            }
        });
        Thread.sleep(300);
    }

    private static String getRemotePathToJar(final String partOfJarName) {
        for (Classpath.ClasspathEntry cpe : Classpath.getClasspath(ClassLoader.getSystemClassLoader()))
            if (cpe.getFileName().contains(partOfJarName))
                return File.separatorChar + cpe.getContentHash() + File.separatorChar + cpe.getFileName();
        throw new RuntimeException(partOfJarName + " not found in classpath");
    }
}

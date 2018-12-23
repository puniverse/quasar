package com.proxy4o.fiber.dns;

import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import com.proxy4o.utils.FiberExecutors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by linkerlin on 6/1/16.
 */
public class FiberDNS  {

    @Suspendable
    public static InetAddress doResolve(final String domain) throws SuspendExecution, InterruptedException, UnknownHostException {
        return FiberExecutors.fiberSubmit(new Callable<InetAddress>() {
            @Override
            public InetAddress call() throws UnknownHostException {

                InetAddress ret = InetAddress.getByName(domain);
                return ret;
            }
        }, UnknownHostException.class);
    }



}

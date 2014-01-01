/*
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.common.util;

import java.util.ArrayList;
import java.util.ServiceLoader;

/**
 * A utility class for loading services with {@link ServiceLoader}.
 *
 * @author pron
 */
public final class ServiceUtil {
    /**
     * Loads a service of the given type and ensures it has either one implementation or none.
     *
     * @param type The service's type.
     * @return The service implementation if found, or {@code null} if not.
     * @throws Error if more than one implementation of the service have been found.
     */
    public static <T> T loadSingletonServiceOrNull(Class<T> type) {
        final ServiceLoader<T> loader = ServiceLoader.load(type);

        ArrayList<T> services = new ArrayList<>();
        for (T service : loader)
            services.add(service);

        if (services.size() == 1)
            return services.iterator().next();
        else {
            if (services.isEmpty())
                return null;
            else
                throw new Error("Several implementations of " + type.getName() + " found: " + services);
        }
    }

    /**
     * Loads a service of the given type and ensures it has exactly one implementation.
     *
     * @param type The service's type.
     * @return The service implementation.
     * @throws Error if no implementation has been found or if more than one implementation of the service have been found.
     */
    public static <T> T loadSingletonService(Class<T> type) {
        final T service = loadSingletonServiceOrNull(type);
        if (service == null)
            throw new Error("No implementation of " + type.getName() + " found!");
        return service;
    }

    private ServiceUtil() {
    }
}

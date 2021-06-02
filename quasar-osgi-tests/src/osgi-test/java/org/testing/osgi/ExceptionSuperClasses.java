package org.testing.osgi;

import org.testing.osgi.exception.second.SecondException;

final class ExceptionSuperClasses {
    @SuppressWarnings("SameParameterValue")
    static Exception throwException(String message) {
        try {
            throw new SecondException(message);
        } catch (SecondException | RuntimeException e) {
            System.err.println("Caught: " + e.getMessage());
            return e;
        }
    }
}

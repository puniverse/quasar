package org.testing.osgi.exception.second;

import org.testing.osgi.exception.first.FirstException;

public class SecondException extends FirstException {
    public SecondException(String message) {
        super(message);
    }
}

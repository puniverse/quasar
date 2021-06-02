package org.testing.osgi;

import org.junit.jupiter.api.Test;
import org.testing.osgi.exception.second.SecondException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class QuasarInstrumentationTest {
    private static final String MESSAGE = "BOOM!";

    @Test
    void testSuperClasses() {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        PrintStream oldStderr = System.err;
        try (PrintStream stderr = new PrintStream(errors)) {
            System.setErr(stderr);
            assertThat(ExceptionSuperClasses.throwException(MESSAGE))
                .isInstanceOf(SecondException.class)
                .hasMessage(MESSAGE);
        } finally {
            System.setErr(oldStderr);
        }

        String[] lines = errors.toString(UTF_8).split(System.lineSeparator());
        assertThat(lines)
            .contains("Caught: " + MESSAGE)
            .noneMatch(line -> line.contains("Can't determine super class of "))
            .noneMatch(line -> line.contains("[quasar]"));
    }
}

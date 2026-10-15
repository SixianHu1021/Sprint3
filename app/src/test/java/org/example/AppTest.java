package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the VSAS application entry point.
 */
class AppTest {

    @TempDir
    Path directory;

    /**
     * Verifies that the application creates an initial administrator,
     * persists account data, and supports login after restart.
     *
     * @throws Exception if reading persisted data fails
     */
    @Test
    void startsAndRestartsWithPersistentAdmin() throws Exception {
        var originalIn = System.in;
        var originalOut = System.out;

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));

            String initialInput =
                    "root\n"
                            + "admin\n"
                            + "secret123\n"
                            + "Administrator\n"
                            + "admin@example.com\n"
                            + "123\n"
                            + "quit\n";

            System.setIn(new ByteArrayInputStream(initialInput.getBytes(StandardCharsets.UTF_8)));

            App.main(new String[]{directory.toString()});

            assertTrue(output.toString(StandardCharsets.UTF_8).contains("Account created"));

            output.reset();

            String restartInput = "login\nroot\nsecret123\nquit\n";

            System.setIn(new ByteArrayInputStream(restartInput.getBytes(StandardCharsets.UTF_8)));

            App.main(new String[]{directory.toString()});

            String transcript = output.toString(StandardCharsets.UTF_8);

            assertTrue(transcript.contains("[ADMIN]"));
            assertFalse(transcript.contains("First run"));
            assertTrue(transcript.contains("ID key: "));

            String storedUsers = Files.readString(
                    directory.resolve("users.properties"));

            assertFalse(storedUsers.contains("secret123"));

        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
    }
}
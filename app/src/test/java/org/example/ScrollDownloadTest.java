package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests authenticated scroll downloads, file content,
 * download counters and error handling.
 */
class ScrollDownloadTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-13T09:30:00Z"),
            ZoneOffset.UTC);

    private static final PasswordHasher HASHER = new PasswordHasher() {
        @Override
        public String hash(String password) {
            return "test-only:" + password;
        }

        @Override
        public boolean verify(String password, String encoded) {
            return password != null && hash(password).equals(encoded);
        }
    };

    @TempDir
    Path directory;

    private Path snapshot;
    private AccountService accounts;
    private ScrollService scrolls;

    @BeforeEach
    void setUp() {
        snapshot = directory.resolve("scrolls.properties");

        accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")),
                HASHER);

        scrolls = new ScrollService(
                new FileScrollRepository(snapshot),
                accounts,
                CLOCK);

        accounts.register(
                "m", "member", "password123",
                "Member", "member@example.com", "456");
    }

    private void login() {
        accounts.login("m", "password123");
    }

    private void uploadSample(byte[] content) throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), content);
        login();
        scrolls.upload("s1", "Sample Scroll", source);
    }

    private DigitalScroll storedScroll() {
        return new FileScrollRepository(snapshot)
                .findById("s1")
                .orElseThrow();
    }

    @Test
    void downloadWritesCorrectBinaryContent() throws IOException {
        byte[] content = {0, 1, -1, 127, -128};
        uploadSample(content);

        Path destination = directory.resolve("download.bin");
        scrolls.download("s1", destination);

        assertTrue(Files.exists(destination));
        assertArrayEquals(content, Files.readAllBytes(destination));
    }

    @Test
    void successfulDownloadIncrementsCounter() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        assertEquals(0, storedScroll().getDownloadCount());

        scrolls.download("s1", directory.resolve("download.bin"));

        assertEquals(1, storedScroll().getDownloadCount());
    }

    @Test
    void multipleDownloadsIncrementCounter() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        scrolls.download("s1", directory.resolve("first.bin"));
        scrolls.download("s1", directory.resolve("second.bin"));

        assertEquals(2, storedScroll().getDownloadCount());
    }

    @Test
    void guestsCannotDownload() throws IOException {
        uploadSample(new byte[]{1, 2, 3});
        accounts.logout();

        Path destination = directory.resolve("guest.bin");

        assertThrows(IllegalStateException.class,
                () -> scrolls.download("s1", destination));

        assertFalse(Files.exists(destination));
        assertEquals(0, storedScroll().getDownloadCount());
    }

    @Test
    void invalidScrollIdsAreRejected() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> scrolls.download(
                        "missing", directory.resolve("missing.bin")));

        assertThrows(IllegalArgumentException.class,
                () -> scrolls.download(
                        null, directory.resolve("null.bin")));

        assertThrows(IllegalArgumentException.class,
                () -> scrolls.download(
                        " ", directory.resolve("blank.bin")));

        assertEquals(0, storedScroll().getDownloadCount());
    }

    @Test
    void nullDestinationIsRejected() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> scrolls.download("s1", null));

        assertEquals(0, storedScroll().getDownloadCount());
    }

    @Test
    void failedWriteDoesNotIncrementCounter() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        // A directory cannot be written as a regular output file.
        assertThrows(IOException.class,
                () -> scrolls.download("s1", directory));

        assertEquals(0, storedScroll().getDownloadCount());
    }

    @Test
    void downloadPreservesOriginalMetadata() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        DigitalScroll before = storedScroll();

        scrolls.download("s1", directory.resolve("download.bin"));

        DigitalScroll after = storedScroll();

        assertEquals(before.getScrollId(), after.getScrollId());
        assertEquals(before.getName(), after.getName());
        assertEquals(before.getUploaderId(), after.getUploaderId());
        assertEquals(before.getUploadDate(), after.getUploadDate());
        assertEquals(before.getUploadCount(), after.getUploadCount());
        assertArrayEquals(before.getContent(), after.getContent());
        assertEquals(1, after.getDownloadCount());
    }

    @Test
    void downloadCanOverwriteExistingFile() throws IOException {
        byte[] content = {10, 20, 30};
        uploadSample(content);

        Path destination = Files.write(
                directory.resolve("existing.bin"),
                new byte[]{99, 99});

        scrolls.download("s1", destination);

        assertArrayEquals(content, Files.readAllBytes(destination));
        assertEquals(1, storedScroll().getDownloadCount());
    }

    /**
     * Download counts persist after restarting the services.
     */
    @Test
    void downloadCountPersistsAfterRestart() throws IOException {
        uploadSample(new byte[]{1, 2, 3});

        // First download before restart.
        scrolls.download("s1", directory.resolve("first.bin"));
        assertEquals(1, storedScroll().getDownloadCount());

        // Simulate an application restart using fresh service instances.
        AccountService restartedAccounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")),
                HASHER);

        ScrollService restartedScrolls = new ScrollService(
                new FileScrollRepository(snapshot),
                restartedAccounts,
                CLOCK);

        // Log in again after restarting.
        restartedAccounts.login("m", "password123");

        // The previous download count must still be available.
        assertEquals(1, restartedScrolls.preview("s1").getDownloadCount());

        // Download again after restart.
        restartedScrolls.download("s1", directory.resolve("second.bin"));

        // The updated count must also persist to the repository.
        assertEquals(2, new FileScrollRepository(snapshot)
                .findById("s1")
                .orElseThrow()
                .getDownloadCount());
    }

}

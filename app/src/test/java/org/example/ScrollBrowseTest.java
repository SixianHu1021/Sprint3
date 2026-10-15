
package org.example;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.Pbkdf2PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ScrollBrowseTest {

    @TempDir
    Path directory;

    private FileScrollRepository repository;
    private ScrollService service;

    private static final LocalDateTime DATE =
            LocalDateTime.of(2026, 10, 10, 12, 0);

    @BeforeEach
    void setUp() {
        repository = new FileScrollRepository(
                directory.resolve("scrolls.properties"));
        // Browsing needs no login; the account service only satisfies the constructor.
        AccountService accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")),
                new Pbkdf2PasswordHasher());
        service = new ScrollService(repository, accounts, Clock.systemDefaultZone());
    }

    private DigitalScroll createScroll(
            String id, String name, String uploader) {
        return new DigitalScroll(
                id, name, new byte[]{1, 2, 3},
                uploader, DATE);
    }

    @Test
    void browseEmptyRepository() {
        assertTrue(service.browse().isEmpty());
    }

    @Test
    void browseMultipleScrolls() {
        repository.save(createScroll("s1", "Java Notes", "u1"));
        repository.save(createScroll("s2", "SQL Notes", "u2"));

        List<DigitalScroll> result = service.browse();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(
                s -> s.getScrollId().equals("s1")));
        assertTrue(result.stream().anyMatch(
                s -> s.getScrollId().equals("s2")));
    }

    @Test
    void previewReturnsCorrectMetadata() {
        repository.save(createScroll("s1", "Java Notes", "u1"));

        DigitalScroll preview = service.preview("s1");

        assertEquals("s1", preview.getScrollId());
        assertEquals("Java Notes", preview.getName());
        assertEquals("u1", preview.getUploaderId());
        assertEquals(DATE, preview.getUploadDate());
    }

    @Test
    void previewRejectsMissingScroll() {
        assertThrows(IllegalArgumentException.class,
                () -> service.preview("missing"));
    }

    @Test
    void previewRejectsInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.preview(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.preview(" "));
    }

    @Test
    void browsingAndPreviewDoNotChangeCounters() {
        DigitalScroll scroll = new DigitalScroll(
                "s1", "Java Notes",
                new byte[]{1, 2, 3}, "u1", DATE, 2, 5);

        repository.save(scroll);

        service.browse();
        service.preview("s1");

        DigitalScroll restored =
                new FileScrollRepository(
                        directory.resolve("scrolls.properties"))
                        .findById("s1")
                        .orElseThrow();

        assertEquals(2, restored.getUploadCount());
        assertEquals(5, restored.getDownloadCount());
    }

    @Test
    void browsingWorksWithoutLogin() {
        repository.save(createScroll("s1", "Public Scroll", "u1"));

        // No login session is required.
        assertEquals(1, service.browse().size());
        assertNotNull(service.preview("s1"));
    }
}

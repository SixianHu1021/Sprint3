package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
 *
 * Tests authenticated scroll upload: stored content and metadata, duplicate
 * and invalid input rejection, guest rejection and visibility in browsing.
 */
class ScrollUploadTest {

    /** Fixed clock so the recorded upload date is predictable. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-13T09:30:00Z"), ZoneOffset.UTC);

    /** Fast, deterministic hashing for service-level tests. */
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

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Creates isolated repositories and services, with a registered member, for each test.
     */
    @BeforeEach
    void setUp() {
        snapshot = directory.resolve("scrolls.properties");
        accounts = new AccountService(new FileUserRepository(directory.resolve("users.properties")), HASHER);
        scrolls = new ScrollService(new FileScrollRepository(snapshot), accounts, CLOCK);
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Logs in as the registered member.
     */
    private void login() {
        accounts.login("m", "password123");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Writes a source file containing the given bytes.
     */
    private Path file(String fileName, byte[] content) throws IOException {
        return Files.write(directory.resolve(fileName), content);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Content, uploader, date and counters are stored and survive a restart.
     */
    @Test
    void uploadStoresContentAndMetadata() throws IOException {
        login();
        byte[] content = {0, 1, -1, 127, -128};
        DigitalScroll uploaded = scrolls.upload("s1", "Binary", file("source.bin", content));

        assertEquals("s1", uploaded.getScrollId());
        assertEquals("Binary", uploaded.getName());
        assertEquals("m", uploaded.getUploaderId());
        assertEquals(LocalDateTime.of(2026, 10, 13, 9, 30), uploaded.getUploadDate());
        assertEquals(1, uploaded.getUploadCount());
        assertEquals(0, uploaded.getDownloadCount());

        DigitalScroll restored = new FileScrollRepository(snapshot).findById("s1").orElseThrow();
        assertArrayEquals(content, restored.getContent());
        assertEquals("m", restored.getUploaderId());
        assertEquals(1, restored.getUploadCount());
        assertEquals(0, restored.getDownloadCount());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * A new upload is immediately visible through browse and preview.
     */
    @Test
    void uploadedScrollAppearsInBrowse() throws IOException {
        login();
        assertTrue(scrolls.browse().isEmpty());
        scrolls.upload("s1", "Listed", file("source.bin", new byte[]{1}));

        assertEquals(1, scrolls.browse().size());
        assertEquals("Listed", scrolls.browse().get(0).getName());
        assertEquals("m", scrolls.preview("s1").getUploaderId());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Guests and logged-out users cannot upload, and nothing is saved.
     */
    @Test
    void guestsCannotUpload() throws IOException {
        Path source = file("source.bin", new byte[]{1});
        assertThrows(IllegalStateException.class, () -> scrolls.upload("s1", "Guest", source));

        login();
        accounts.logout();
        assertThrows(IllegalStateException.class, () -> scrolls.upload("s1", "Guest", source));
        assertTrue(scrolls.browse().isEmpty());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * A used scroll ID or name is rejected and the original scroll is kept.
     */
    @Test
    void duplicateIdOrNameIsRejected() throws IOException {
        login();
        Path source = file("source.bin", new byte[]{1});
        scrolls.upload("s1", "Original", source);

        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("s1", "Different", source));
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("s2", "Original", source));
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload(" s1 ", "Other", source));
        assertEquals(1, scrolls.browse().size());
        assertEquals("Original", scrolls.preview("s1").getName());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Missing paths, directories and null paths are rejected.
     */
    @Test
    void invalidSourceFilesAreRejected() {
        login();
        assertThrows(IllegalArgumentException.class,
                () -> scrolls.upload("s1", "Missing", directory.resolve("missing.bin")));
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("s1", "Folder", directory));
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("s1", "Null", null));
        assertTrue(scrolls.browse().isEmpty());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Blank IDs or names are rejected; surrounding whitespace is removed from valid ones.
     */
    @Test
    void scrollIdAndNameAreValidated() throws IOException {
        login();
        Path source = file("source.bin", new byte[]{1});
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload(" ", "Name", source));
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("s1", null, source));

        DigitalScroll uploaded = scrolls.upload("  s1  ", "  Trimmed  ", source);
        assertEquals("s1", uploaded.getScrollId());
        assertEquals("Trimmed", uploaded.getName());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * An empty file is a valid scroll; a file over the size limit is rejected.
     */
    @Test
    void fileSizeLimits() throws IOException {
        login();
        assertEquals(0, scrolls.upload("empty", "Empty", file("empty.bin", new byte[0])).getContent().length);

        Path large = file("large.bin", new byte[ScrollService.MAX_BYTES + 1]);
        assertThrows(IllegalArgumentException.class, () -> scrolls.upload("big", "Big", large));
        assertFalse(scrolls.browse().stream().anyMatch(s -> s.getScrollId().equals("big")));
    }
}

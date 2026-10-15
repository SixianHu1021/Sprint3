
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

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
 *
 */
class ScrollBrowseTest {

    @TempDir
    Path directory;

    private FileScrollRepository repository;
    private ScrollService service;

    private static final LocalDateTime DATE =
            LocalDateTime.of(2026, 10, 10, 12, 0);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
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

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    private DigitalScroll createScroll(
            String id, String name, String uploader) {
        return new DigitalScroll(
                id, name, new byte[]{1, 2, 3},
                uploader, DATE);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void browseEmptyRepository() {
        assertTrue(service.browse().isEmpty());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
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

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void previewReturnsCorrectMetadata() {
        repository.save(createScroll("s1", "Java Notes", "u1"));

        DigitalScroll preview = service.preview("s1");

        assertEquals("s1", preview.getScrollId());
        assertEquals("Java Notes", preview.getName());
        assertEquals("u1", preview.getUploaderId());
        assertEquals(DATE, preview.getUploadDate());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void previewRejectsMissingScroll() {
        assertThrows(IllegalArgumentException.class,
                () -> service.preview("missing"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void previewRejectsInvalidId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.preview(null));

        assertThrows(IllegalArgumentException.class,
                () -> service.preview(" "));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
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

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void browsingWorksWithoutLogin() {
        repository.save(createScroll("s1", "Public Scroll", "u1"));

        // No login session is required.
        assertEquals(1, service.browse().size());
        assertNotNull(service.preview("s1"));
    }
}

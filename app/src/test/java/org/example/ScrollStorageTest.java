package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.example.service.ScrollService;
import org.example.service.AccountService;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
 *
 * 【Sprint 2 · R1 · US10 #11 / US17 #52】主负责人：Stella。
 * 测什么：模型的 fileFormat 默认值与小写化；withName / withContent / withRecordedDownload 只改该改的字段；
 * 仓库保存并恢复 format；旧快照没有 format 时读为 unknown；deleteById 持久化；更新后列表位置不变。
 * 只依赖模型和仓库，因此可以在 R1 独立合并。
 */
class ScrollStorageTest {

    @TempDir
    Path directory;

    private FileScrollRepository repository;
    private final LocalDateTime date = LocalDateTime.parse("2026-10-16T10:00");

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @BeforeEach
    void setUp() {
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * Creates a scroll with the given ID and format.
     */
    private DigitalScroll scroll(String id, String format) {
        return new DigitalScroll(id, "Name " + id, new byte[]{1, 2}, "m", date, 1, 0, format);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void formatDefaultsToUnknownAndIsLowercased() {
        assertEquals(DigitalScroll.UNKNOWN_FORMAT,
                new DigitalScroll("x", "X", new byte[]{1}, "m", date).getFileFormat());
        assertEquals(DigitalScroll.UNKNOWN_FORMAT,
                new DigitalScroll("x", "X", new byte[]{1}, "m", date, 1, 0).getFileFormat());
        assertEquals(DigitalScroll.UNKNOWN_FORMAT, scroll("x", " ").getFileFormat());
        assertEquals("pdf", scroll("x", " PDF ").getFileFormat());
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void withHelpersChangeOnlyTheirFields() {
        DigitalScroll original = new DigitalScroll("x", "X", new byte[]{1}, "m", date, 1, 4, "txt");
        DigitalScroll renamed = original.withName("Y");
        assertEquals("Y", renamed.getName());
        assertEquals(4, renamed.getDownloadCount());
        assertEquals("txt", renamed.getFileFormat());

        DigitalScroll replaced = original.withContent(new byte[]{9, 9}, "png");
        assertArrayEquals(new byte[]{9, 9}, replaced.getContent());
        assertEquals("png", replaced.getFileFormat());
        assertEquals(2, replaced.getUploadCount());
        assertEquals(4, replaced.getDownloadCount());

        DigitalScroll downloaded = original.withRecordedDownload();
        assertEquals(5, downloaded.getDownloadCount());
        assertEquals("txt", downloaded.getFileFormat());
        assertEquals("X", downloaded.getName());
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void counterOverflowIsRejected() {
        DigitalScroll max = new DigitalScroll("x", "X", new byte[]{1}, "m", date, Long.MAX_VALUE, Long.MAX_VALUE, "txt");
        assertThrows(ArithmeticException.class, max::withRecordedDownload);
        assertThrows(ArithmeticException.class, () -> max.withContent(new byte[0], "txt"));
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void formatIsSavedAndRestored() {
        repository.save(scroll("a", "pdf"));
        assertEquals("pdf", new FileScrollRepository(directory.resolve("scrolls.properties"))
                .findById("a").orElseThrow().getFileFormat());
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void oldSnapshotsWithoutFormatLoadAsUnknown() throws IOException {
        Path file = directory.resolve("legacy.properties");
        Properties data = new Properties();
        data.setProperty("count", "1");
        data.setProperty("0.id", "old");
        data.setProperty("0.name", "Old");
        data.setProperty("0.content", "AQI=");
        data.setProperty("0.uploader", "m");
        data.setProperty("0.date", "2026-10-01T10:00");
        try (var writer = Files.newBufferedWriter(file)) {
            data.store(writer, null);
        }
        assertEquals(DigitalScroll.UNKNOWN_FORMAT,
                new FileScrollRepository(file).findById("old").orElseThrow().getFileFormat());
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void deleteByIdPersistsAndUnknownIdIsHarmless() {
        repository.save(scroll("a", "txt"));
        repository.save(scroll("b", "txt"));
        repository.deleteById("a");
        repository.deleteById("ghost");
        FileScrollRepository restored = new FileScrollRepository(directory.resolve("scrolls.properties"));
        assertFalse(restored.existsById("a"));
        assertEquals(1, restored.findAll().size());
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void updatesKeepListPosition() {
        repository.save(scroll("a", "txt"));
        repository.save(scroll("b", "txt"));
        repository.save(scroll("a", "txt").withRecordedDownload());
        List<String> ids = repository.findAll().stream().map(DigitalScroll::getScrollId).toList();
        assertEquals(List.of("a", "b"), ids);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void uploadFieldChecksRejectTakenIdsNamesAndBadFiles() throws IOException {
        PasswordHasher hasher = new PasswordHasher() {
            @Override
            public String hash(String password) {
                return "test-only:" + password;
            }

            @Override
            public boolean verify(String password, String encoded) {
                return password != null && hash(password).equals(encoded);
            }
        };
        AccountService accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")), hasher);
        ScrollService scrolls = new ScrollService(repository, accounts, java.time.Clock.systemUTC());
        repository.save(scroll("a", "txt"));
        assertEquals("b", scrolls.checkNewScrollId(" b "));
        assertEquals("Scroll ID 'a' is already taken",
                assertThrows(IllegalArgumentException.class, () -> scrolls.checkNewScrollId("a")).getMessage());
        assertEquals("Scroll name 'Name a' is already taken",
                assertThrows(IllegalArgumentException.class, () -> scrolls.checkNewScrollName("Name a")).getMessage());
        Path file = Files.writeString(directory.resolve("ok.txt"), "x");
        assertEquals(file, ScrollService.checkSource(file.toString()));
        assertThrows(IllegalArgumentException.class, () -> ScrollService.checkSource(" "));
        assertThrows(IllegalArgumentException.class, () -> ScrollService.checkSource("bad\0path"));
        assertThrows(IllegalArgumentException.class, () -> ScrollService.checkSource(directory.toString()));
        Path big = directory.resolve("big.bin");
        try (var channel = java.nio.channels.FileChannel.open(big,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE)) {
            channel.write(java.nio.ByteBuffer.wrap(new byte[]{1}), ScrollService.MAX_BYTES);
        }
        assertEquals("Scroll exceeds the 10 MiB limit",
                assertThrows(IllegalArgumentException.class,
                        () -> ScrollService.checkSource(big.toString())).getMessage());
    }
}

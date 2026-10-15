package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollEditService;
import org.example.service.ScrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Sprint 2 · US10 #11】主负责人：Stella。审核人：Sixian。
 * 测什么：只列出自己的卷轴；改名、替换内容（格式更新、上传次数 +1、下载次数保留）、删除；
 * 不能编辑／删除别人的卷轴；删除后浏览不再出现；重启后修改仍在；修改后在列表中的位置不变。
 * （模型和仓库本身的测试在 Stella R1 的 ScrollStorageTest。）
 */
class ScrollEditServiceTest {

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

    private AccountService accounts;
    private FileScrollRepository repository;
    private ScrollService scrolls;
    private ScrollEditService edits;

    @BeforeEach
    void setUp() throws IOException {
        accounts = new AccountService(new FileUserRepository(directory.resolve("users.properties")), HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        edits = new ScrollEditService(repository, accounts);
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("m", "password123");
        scrolls.upload("s1", "Mine", Files.writeString(directory.resolve("mine.txt"), "one"));
        accounts.login("a", "secret123");
        scrolls.upload("s2", "Theirs", Files.writeString(directory.resolve("theirs.txt"), "two"));
        accounts.login("m", "password123");
    }

    @Test
    void listsOnlyOwnScrolls() {
        List<DigitalScroll> mine = edits.myScrolls();
        assertEquals(1, mine.size());
        assertEquals("s1", mine.get(0).getScrollId());
    }

    @Test
    void renameKeepsEverythingElseAndPersists() {
        edits.rename("s1", "  Renamed  ");
        DigitalScroll restored = new FileScrollRepository(directory.resolve("scrolls.properties"))
                .findById("s1").orElseThrow();
        assertEquals("Renamed", restored.getName());
        assertEquals("m", restored.getUploaderId());
        assertEquals("txt", restored.getFileFormat());
        assertEquals(1, restored.getUploadCount());
    }

    @Test
    void renameRejectsDuplicateOrBlankNames() {
        assertEquals("Scroll name already exists",
                assertThrows(IllegalArgumentException.class, () -> edits.rename("s1", "Theirs")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> edits.rename("s1", " "));
        edits.rename("s1", "Mine");
        assertEquals("Mine", repository.findById("s1").orElseThrow().getName());
    }

    @Test
    void replaceContentUpdatesFormatAndUploadCountButKeepsDownloads() throws IOException {
        scrolls.download("s1", directory.resolve("copy.txt"));
        byte[] pdf = "%PDF-1.4 test".getBytes();
        edits.replaceContent("s1", Files.write(directory.resolve("new.pdf"), pdf));
        DigitalScroll updated = repository.findById("s1").orElseThrow();
        assertArrayEquals(pdf, updated.getContent());
        assertEquals("pdf", updated.getFileFormat());
        assertEquals(2, updated.getUploadCount());
        assertEquals(1, updated.getDownloadCount());
    }

    @Test
    void replaceContentRejectsMissingFileWithoutChanges() {
        assertThrows(IllegalArgumentException.class,
                () -> edits.replaceContent("s1", directory.resolve("missing.bin")));
        assertThrows(IllegalArgumentException.class, () -> edits.replaceContent("s1", directory));
        assertEquals(1, repository.findById("s1").orElseThrow().getUploadCount());
    }

    @Test
    void cannotEditOrDeleteOtherUsersScrolls() {
        assertEquals("You can only edit scrolls you uploaded",
                assertThrows(IllegalStateException.class, () -> edits.rename("s2", "Hijack")).getMessage());
        assertThrows(IllegalStateException.class,
                () -> edits.replaceContent("s2", directory.resolve("mine.txt")));
        assertEquals("You can only delete scrolls you uploaded",
                assertThrows(IllegalStateException.class, () -> edits.delete("s2")).getMessage());
        assertEquals("Theirs", repository.findById("s2").orElseThrow().getName());
    }

    @Test
    void deleteRemovesScrollFromBrowseAndAfterRestart() {
        edits.delete("s1");
        assertTrue(scrolls.browse().stream().noneMatch(s -> s.getScrollId().equals("s1")));
        assertFalse(new FileScrollRepository(directory.resolve("scrolls.properties")).existsById("s1"));
        assertEquals(1, scrolls.browse().size());
    }

    @Test
    void missingScrollsAndGuestsAreRejected() {
        assertEquals("Scroll not found: nope",
                assertThrows(IllegalArgumentException.class, () -> edits.delete("nope")).getMessage());
        accounts.logout();
        assertThrows(IllegalStateException.class, () -> edits.myScrolls());
        assertThrows(IllegalStateException.class, () -> edits.delete("s1"));
    }

    @Test
    void updatingAScrollKeepsItsPositionInTheList() {
        edits.rename("s1", "Still first");
        assertEquals("s1", scrolls.browse().get(0).getScrollId());
    }
}

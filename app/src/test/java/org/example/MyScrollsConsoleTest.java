package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollEditService;
import org.example.service.ScrollService;
import org.example.ui.ConsoleIO;
import org.example.ui.MyScrollsScreen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
 *
 * 【Sprint 2 · US10 #11】主负责人：Stella。
 * 测什么：“我的卷轴”页面：列表只含自己的卷轴、编号改名、替换文件、删除需要确认（n 取消、y 删除）、
 * 别名带参数、删除别人的卷轴报错、没有卷轴时的空状态。
 */
class MyScrollsConsoleTest {

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
    private ScrollEditService edits;
    private StringWriter out;

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @BeforeEach
    void setUp() throws IOException {
        accounts = new AccountService(new FileUserRepository(directory.resolve("users.properties")), HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        ScrollService scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        edits = new ScrollEditService(repository, accounts);
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("a", "secret123");
        scrolls.upload("s2", "Admin Scroll", Files.writeString(directory.resolve("admin.txt"), "a"));
        accounts.login("m", "password123");
        scrolls.upload("s1", "Mine", Files.writeString(directory.resolve("mine.txt"), "m"));
        out = new StringWriter();
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     * Runs the screen with scripted input.
     */
    private String run(String... lines) {
        new MyScrollsScreen(edits,
                new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true))).show();
        return out.toString();
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void listsOnlyOwnScrollsWithNumberedActions() {
        String text = run("0");
        assertTrue(text.contains("Your scrolls:"));
        assertTrue(text.contains("s1 | Mine | txt | uploads: 1 | downloads: 0"));
        assertFalse(text.contains("Admin Scroll"));
        assertTrue(text.contains("  1. Rename a scroll"));
        assertTrue(text.contains("  2. Replace a scroll's file"));
        assertTrue(text.contains("  3. Delete a scroll"));
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void renameAndReplaceThroughNumbers() throws IOException {
        Path png = Files.write(directory.resolve("picture.png"), new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        String text = run("1", "s1", "Better Name", "2", "s1", png.toString(), "0");
        assertTrue(text.contains("Scroll renamed."));
        assertTrue(text.contains("Scroll file replaced (format: png)."));
        assertTrue(text.contains("s1 | Better Name | png | uploads: 2 | downloads: 0"));
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void deleteAsksForConfirmation() {
        String text = run("3", "s1", "n", "delete s1", "y", "0");
        assertTrue(text.contains("Deletion cancelled."));
        assertTrue(text.contains("Scroll deleted."));
        assertTrue(text.contains("You have not uploaded any scrolls yet."));
        assertFalse(repository.existsById("s1"));
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void otherUsersScrollsCannotBeChanged() {
        String text = run("3", "s2", "1", "s2", "cancel", "0");
        assertTrue(text.contains("Error: You can only delete scrolls you uploaded"));
        assertTrue(text.contains("Error: You can only edit scrolls you uploaded"));
        assertTrue(text.contains("Cancelled."));
        assertFalse(text.contains("New name: "));
        assertTrue(repository.existsById("s2"));
        assertEquals("Admin Scroll", repository.findById("s2").orElseThrow().getName());
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Sixian
     */
    @Test
    void guestsCannotOpenMyScrolls() {
        accounts.logout();
        assertThrows(IllegalStateException.class, () -> run("0"));
    }
}

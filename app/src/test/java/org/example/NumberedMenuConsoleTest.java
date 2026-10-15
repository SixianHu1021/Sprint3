package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;
import org.example.model.UserRole;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollEditService;
import org.example.service.ScrollService;
import org.example.ui.ConsoleIO;
import org.example.ui.ConsoleUi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US13 #48 / US14 #49 / US15 #50】主负责人：Yuhan。
 * 测什么：完整 ConsoleUi 只用数字完成访客 → 登录 → 上传 → 浏览 → 退出；三种角色共用编号一致；
 * 无效输入不崩溃且停留在同一菜单；登录时 ID 为空立即重问；有隐藏读取器时密码不出现在输出；
 * 管理员用第 7 项创建 MEMBER/ADMIN、第 9 项打开 Stella 的统计页（US12 端到端，Stella 审核）。
 */
class NumberedMenuConsoleTest {

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

    private FileUserRepository users;
    private AccountService accounts;
    private ScrollService scrolls;
    private FileScrollRepository repository;

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     * Runs the full console with scripted input.
     */
    private String cli(String... lines) {
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
        new ConsoleUi(accounts, scrolls, new ScrollEditService(repository, accounts), io).run();
        return out.toString();
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void wholeWorkflowUsesNumbersOnly() throws Exception {
        Path source = Files.writeString(directory.resolve("notes.txt"), "hello");
        String transcript = cli(
                "3", "m", "password123",          // guest 3 = Log in
                "2", "s1", "Notes", source.toString(), // member 2 = Upload
                "1", "0",                          // 1 = Browse, 0 = Back
                "5",                               // 5 = Log out
                "0");                              // 0 = Quit
        assertTrue(transcript.contains("Logged in."));
        assertTrue(transcript.contains("Scroll uploaded successfully."));
        assertTrue(transcript.contains("Detected format: txt"));
        assertTrue(transcript.contains("s1 | Notes | m |"));
        assertTrue(transcript.contains("Logged out."));
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void sharedOptionsHaveTheSameNumberForEveryRole() {
        String guest = cli("0");
        accounts.login("m", "password123");
        String member = cli("0");
        accounts.logout();
        accounts.login("a", "secret123");
        String admin = cli("0");
        for (String transcript : new String[]{guest, member, admin}) {
            assertTrue(transcript.contains("  1. Browse scrolls"));
            assertTrue(transcript.contains("  0. Quit"));
        }
        for (String transcript : new String[]{member, admin}) {
            assertTrue(transcript.contains("  2. Upload a scroll"));
            assertTrue(transcript.contains("  3. My scrolls (edit / delete)"));
            assertTrue(transcript.contains("  4. My profile"));
            assertTrue(transcript.contains("  5. Log out"));
        }
        assertFalse(member.contains("  6. "));
        assertTrue(admin.contains("  9. Scroll statistics"));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void invalidMenuInputShowsErrorAndRedisplaysSameMenu() {
        String transcript = cli("9", "hello", "", "0");
        assertTrue(transcript.contains("Error: '9' is out of range. Please enter a number between 0 and 3."));
        assertTrue(transcript.contains("Error: 'hello' is not a valid option."));
        assertTrue(transcript.contains("Error: No option entered."));
        assertEquals(4, transcript.split("Guest menu:", -1).length - 1);
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void blankLoginIdIsRejectedImmediately() {
        String transcript = cli("3", "", "m", "password123", "0");
        assertTrue(transcript.contains("Error: ID key cannot be empty."));
        assertTrue(transcript.contains("Logged in."));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void maskedReaderKeepsPasswordOffScreen() {
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner("3\nm\n0\n"), new PrintWriter(out, true),
                prompt -> "password123".toCharArray(), true);
        new ConsoleUi(accounts, scrolls, null, io).run();
        assertTrue(out.toString().contains("Member (member) [MEMBER]"));
        assertFalse(out.toString().contains("password123"));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void missingEditServiceGivesClearError() {
        accounts.login("m", "password123");
        StringWriter out = new StringWriter();
        new ConsoleUi(accounts, scrolls, new Scanner("3\n0\n"), new PrintWriter(out, true)).run();
        assertTrue(out.toString().contains("Error: Scroll editing is not available in this session"));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void guestTypingMemberCommandsGetsLoginError() {
        String transcript = cli("profile", "my-scrolls", "logout", "stats", "0");
        assertEquals(4, transcript.split("Error: Please log in first", -1).length - 1);
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void adminCreatesBothRolesAndOpensStatisticsByNumber() {
        accounts.login("a", "secret123");
        String transcript = cli(
                "7", "g", "general", "password123", "General", "g@example.com", "777", "1",
                "7", "b", "boss", "password123", "Boss", "b@example.com", "888", "2",
                "9", "0");
        assertEquals(UserRole.MEMBER, users.findByIdKey("g").orElseThrow().getRole());
        assertEquals(UserRole.ADMIN, users.findByIdKey("b").orElseThrow().getRole());
        assertTrue(transcript.contains("Scroll statistics:"));
    }
}

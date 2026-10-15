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
 * 【Sprint 2 · US14 #49 逐项检查】主负责人：Yuhan（ConsoleIO.askValid）；
 * 字段检查：Henry（账户）、Stella（上传与我的卷轴）。
 * 测什么：表单中任何一项出错都在输入当时提示，并且只重问这一项，后面的问题不会提前出现；
 * cancel 放弃表单回到菜单；首次管理员设置不能被 cancel 跳过。
 * （各 check 方法的单元测试在各负责人自己的测试文件：ProfileServiceTest、ScrollStorageTest、
 *  ScrollEditServiceTest、ConsoleIOTest。）
 */
class ImmediateFieldCheckTest {

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
    private FileScrollRepository repository;
    private ScrollService scrolls;

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     * Creates the admin and one member.
     */
    private void people() {
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "0400000000");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "0411111111");
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
     * Returns the part of the transcript between two markers.
     */
    private static String between(String text, String from, String to) {
        int start = text.indexOf(from);
        return text.substring(start, text.indexOf(to, start));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void registrationReportsEachProblemImmediately() {
        people();
        String transcript = cli("2",
                "m", "n",                                    // duplicate ID, then a free one
                "member", "newbie",                          // duplicate username
                "short", "goodpass123",                      // password too short
                "Newbie",
                "not-an-email", "MEMBER@example.com", "newbie@example.com", // bad format, duplicate (any case)
                "abc", "0411 111 111", "0499999999",         // bad format, duplicate phone
                "0");
        assertTrue(transcript.contains("Error: ID key 'm' is already taken"));
        assertTrue(transcript.contains("Error: Username 'member' is already taken"));
        assertTrue(transcript.contains("Error: Password must contain 8-128 characters"));
        assertTrue(transcript.contains("Error: Email must include a valid domain"));
        assertTrue(transcript.contains("Error: Email address is already registered to another account"));
        assertTrue(transcript.contains("Error: Phone may contain only digits"));
        assertTrue(transcript.contains("Error: Phone number is already registered to another account"));
        assertTrue(transcript.contains("Account created."));
        assertEquals("newbie@example.com", users.findByIdKey("n").orElseThrow().getEmail());
        // The duplicate-ID error appears before the form moves on to the username.
        assertFalse(between(transcript, "ID key: ", "Error: ID key 'm'").contains("Username: "));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void cancellingRegistrationSavesNothing() {
        people();
        String transcript = cli("2", "n", "newbie", "cancel", "0");
        assertTrue(transcript.contains("Cancelled."));
        assertFalse(transcript.contains("Full name: "));
        assertFalse(users.existsByIdKey("n"));
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void uploadReportsEachProblemImmediately() throws IOException {
        people();
        accounts.login("m", "password123");
        scrolls.upload("s1", "Taken Name", Files.writeString(directory.resolve("first.txt"), "x"));
        Path good = Files.writeString(directory.resolve("good.txt"), "hello");
        String transcript = cli("2",
                "s1", "s2",
                "Taken Name", "Fresh Name",
                directory.resolve("missing.txt").toString(), directory.toString(), good.toString(),
                "0");
        assertTrue(transcript.contains("Error: Scroll ID 's1' is already taken"));
        assertTrue(transcript.contains("Error: Scroll name 'Taken Name' is already taken"));
        assertTrue(transcript.contains("Error: No readable file found at: " + directory.resolve("missing.txt")));
        assertTrue(transcript.contains("Error: No readable file found at: " + directory));
        assertTrue(transcript.contains("Scroll uploaded successfully."));
        assertEquals("Fresh Name", repository.findById("s2").orElseThrow().getName());
        assertFalse(between(transcript, "Scroll ID: ", "Error: Scroll ID 's1'").contains("Scroll name: "));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void cancellingUploadSavesNothing() {
        people();
        accounts.login("m", "password123");
        String transcript = cli("2", "s9", "cancel", "0");
        assertTrue(transcript.contains("Cancelled."));
        assertTrue(repository.findAll().isEmpty());
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void initialAdministratorCannotBeSkippedWithCancel() {
        String transcript = cli("cancel",
                "a", "admin", "secret123", "Admin", "admin@example.com", "0400000000", "0");
        assertTrue(transcript.contains("The initial administrator is required"));
        assertFalse(accounts.needsBootstrap());
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void myScrollsChecksOwnershipAndNameImmediately() throws IOException {
        people();
        accounts.login("a", "secret123");
        scrolls.upload("theirs", "Admin Scroll", Files.writeString(directory.resolve("a.txt"), "a"));
        accounts.login("m", "password123");
        scrolls.upload("mine", "My Scroll", Files.writeString(directory.resolve("m.txt"), "m"));
        String transcript = cli("3", "1",
                "theirs", "mine",
                "Admin Scroll", "Renamed",
                "2", "mine", directory.resolve("nope.bin").toString(), "cancel",
                "0", "0");
        assertTrue(transcript.contains("Error: You can only edit scrolls you uploaded"));
        assertTrue(transcript.contains("Error: Scroll name 'Admin Scroll' is already taken"));
        assertTrue(transcript.contains("Scroll renamed."));
        assertTrue(transcript.contains("Error: No readable file found at:"));
        assertTrue(transcript.contains("Cancelled."));
        assertEquals("Renamed", repository.findById("mine").orElseThrow().getName());
        assertEquals("Admin Scroll", repository.findById("theirs").orElseThrow().getName());
    }
}

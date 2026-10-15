package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Scanner;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.ui.ConsoleUi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
 *   - Sprint 1 原作者：Yuhan（未标 PR 的方法本轮不改）
 *
 * Tests role-aware console menus and account interactions.
 */
class ConsoleUiTest {

    @TempDir
    Path directory;

    private AccountService accounts;

    /**
     * Provides deterministic password hashing for console tests.
     */
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

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Creates an isolated account service for each test.
     */
    @BeforeEach
    void setUp() {
        FileUserRepository users = new FileUserRepository(
                directory.resolve("users.properties"));

        accounts = new AccountService(users, HASHER);

        accounts.bootstrapAdmin(
                "a", "admin", "secret123",
                "Admin", "admin@example.com", "123");

        accounts.register(
                "m", "member", "password123",
                "Member", "member@example.com", "456");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Runs the console with scripted input and captures its output.
     *
     * @param script simulated console input
     * @return captured console output
     */
    private String runConsole(String script) {
        StringWriter result = new StringWriter();

        ConsoleUi ui = new ConsoleUi(
                accounts,
                null,
                new Scanner(script),
                new PrintWriter(result, true));

        ui.run();

        return result.toString();
    }

    /**
     * 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
     * Verifies that guests see only guest menu options.
     */
    @Test
    void guestMenuShowsAvailableCommands() {
        String transcript = runConsole("quit\n");

        // Sprint 2 · US13（Yuhan）：菜单改为编号格式。
        assertTrue(transcript.contains("Guest [GUEST]"));
        assertTrue(transcript.contains("Guest menu:"));
        assertTrue(transcript.contains("  1. Browse scrolls"));
        assertTrue(transcript.contains("  2. Register"));
        assertTrue(transcript.contains("  3. Log in"));
        assertTrue(transcript.contains("  0. Quit"));
        assertFalse(transcript.contains("Admin menu:"));
    }

    /**
     * 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
     * Verifies that members do not see administrator menu options.
     */
    @Test
    void memberMenuHidesAdminCommands() {
        accounts.login("m", "password123");

        String transcript = runConsole("quit\n");

        assertTrue(transcript.contains("Member menu:"));
        assertTrue(transcript.contains("Member (member) [MEMBER]"));
        assertFalse(transcript.contains("Admin menu:"));
        assertFalse(transcript.contains("View all users"));
        assertFalse(transcript.contains("Delete a user"));
    }

    /**
     * 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
     * Verifies that administrators see account management options.
     */
    @Test
    void adminMenuShowsManagementCommands() {
        accounts.login("a", "secret123");

        String transcript = runConsole("quit\n");

        // Sprint 2 · US13（Yuhan）：管理员额外选项编号 6–9。
        assertTrue(transcript.contains("Admin menu:"));
        assertTrue(transcript.contains("  6. View all users"));
        assertTrue(transcript.contains("  7. Add a user"));
        assertTrue(transcript.contains("  8. Delete a user"));
        assertTrue(transcript.contains("  9. Scroll statistics"));
        assertTrue(transcript.contains("Admin (admin) [ADMIN]"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies successful ID-key authentication.
     */
    @Test
    void loginDisplaysCorrectIdentity() {
        String transcript = runConsole("login\nm\npassword123\nquit\n");

        assertTrue(transcript.contains("Logged in."));
        assertTrue(transcript.contains("Member (member) [MEMBER]"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that invalid credentials produce a consistent error.
     */
    @Test
    void failedLoginShowsError() {
        String transcript = runConsole(
                "login\nm\nwrong\n"
                        + "login\nunknown\npassword123\n"
                        + "quit\n");

        String message = "Invalid ID key or password";

        assertTrue(transcript.contains(message));
        assertTrue(transcript.contains("Guest [GUEST]"));
        assertFalse(transcript.contains("Logged in."));
    }

    /**
     * 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
     * Verifies that logout restores the guest session.
     */
    @Test
    void logoutReturnsToGuestMenu() {
        String transcript = runConsole(
                "login\nm\npassword123\nlogout\nquit\n");

        assertTrue(transcript.contains("Logged in."));
        assertTrue(transcript.contains("Logged out."));
        assertTrue(transcript.contains("Guest menu:"));
        assertTrue(accounts.currentUser().isEmpty());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that members cannot invoke administrator commands
     * even when entering them manually.
     */
    @Test
    void memberCannotAccessAdminOperations() {
        accounts.login("m", "password123");

        String transcript = runConsole("users\ndelete-user\nm\nquit\n");

        assertTrue(transcript.contains("Administrator access required"));
        assertTrue(transcript.contains("Member menu:"));

        assertTrue(accounts.currentUser().isPresent());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that administrator user listings do not expose
     * password hashes.
     */
    @Test
    void userListDoesNotExposePasswordHash() {
        accounts.login("a", "secret123");

        String transcript = runConsole("users\nquit\n");

        assertTrue(transcript.contains("admin@example.com"));
        assertTrue(transcript.contains("member@example.com"));

        assertFalse(transcript.contains("test-only:"));
        assertFalse(transcript.contains("secret123"));
        assertFalse(transcript.contains("password123"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that administrators can delete another user.
     */
    @Test
    void adminCanDeleteUserFromConsole() {
        accounts.login("a", "secret123");

        String transcript = runConsole("delete-user\nm\nusers\nquit\n");

        assertTrue(transcript.contains("User deleted."));
        assertFalse(transcript.contains("member@example.com"));
        assertTrue(transcript.contains("admin@example.com"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that users cannot delete their own administrator account.
     */
    @Test
    void adminCannotDeleteOwnAccount() {
        accounts.login("a", "secret123");

        String transcript = runConsole("delete-user\na\nquit\n");

        assertTrue(transcript.contains(
                "Cannot delete your own administrator account"));

        assertTrue(accounts.currentUser().isPresent());
        assertTrue(accounts.currentUser().orElseThrow().getRole() == UserRole.ADMIN);
    }
}

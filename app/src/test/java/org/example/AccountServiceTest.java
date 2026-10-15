package org.example;

import java.nio.file.Path;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
 *
 * Tests authentication, session management, and administrator operations.
 */
class AccountServiceTest {

    @TempDir
    Path directory;

    private FileUserRepository users;
    private AccountService accounts;

    /**
     * Provides deterministic password hashing for service-level tests.
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
     * Creates an isolated repository and account service for each test.
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Creates and authenticates the initial administrator.
     */
    private void admin() {
        accounts.bootstrapAdmin(
                "a", "admin", "secret123",
                "Admin", "admin@example.com", "123");
        accounts.login("a", "secret123");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Registers a member account for authentication tests.
     *
     * @return the registered member
     */
    private User member() {
        return accounts.register(
                "m", "member", "password123",
                "Member", "member@example.com", "456");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies successful authentication, failed login, logout,
     * and invalidation of deleted accounts.
     */
    @Test
    void loginAndLogoutEnforceSessionState() {
        member();

        User loggedIn = accounts.login(" m ", "password123");
        assertEquals("m", loggedIn.getIdKey());
        assertEquals("m", accounts.requireUser().getIdKey());

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("m", "wrong"));

        assertTrue(accounts.currentUser().isEmpty());

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("missing", "password123"));

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login(null, "password123"));

        accounts.login("m", "password123");
        accounts.logout();

        assertTrue(accounts.currentUser().isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.requireUser());

        accounts.login("m", "password123");
        users.deleteByIdKey("m");

        assertTrue(accounts.currentUser().isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.requireUser());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies initial administrator setup and account management.
     */
    @Test
    void administratorLifecycle() {
        assertTrue(accounts.needsBootstrap());

        admin();

        assertFalse(accounts.needsBootstrap());
        assertEquals(UserRole.ADMIN, accounts.requireUser().getRole());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.bootstrapAdmin(
                        "x", "other", "password123",
                        "Other", "other@example.com", "123"));

        User secondAdmin = accounts.addUser(
                "b", "boss", "password123",
                "Boss", "boss@example.com", "789",
                UserRole.ADMIN);

        User newMember = accounts.addUser(
                "m", "member", "password123",
                "Member", "member@example.com", "456",
                UserRole.MEMBER);

        assertEquals(UserRole.ADMIN, secondAdmin.getRole());
        assertEquals(UserRole.MEMBER, newMember.getRole());
        assertEquals(3, accounts.listUsers().size());

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.addUser(
                        "x", "other", "password123",
                        "Other", "other@example.com", "123",
                        null));

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.deleteUser("a"));

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.deleteUser("missing"));

        accounts.deleteUser("m");
        assertFalse(users.existsByIdKey("m"));

        accounts.deleteUser("b");
        assertEquals(1, accounts.listUsers().size());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that guests and members cannot perform
     * administrator operations.
     */
    @Test
    void unauthorizedAdministrativeCallsAreRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> accounts.listUsers());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.addUser(
                        "x", "other", "password123",
                        "Other", "other@example.com", "123",
                        UserRole.ADMIN));

        assertThrows(
                IllegalStateException.class,
                () -> accounts.deleteUser("x"));

        member();
        accounts.login("m", "password123");

        assertEquals(UserRole.MEMBER, accounts.requireUser().getRole());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.listUsers());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.addUser(
                        "x", "other", "password123",
                        "Other", "other@example.com", "123",
                        UserRole.ADMIN));

        assertThrows(
                IllegalStateException.class,
                () -> accounts.deleteUser("m"));

        assertFalse(users.existsByIdKey("x"));
        assertTrue(users.existsByIdKey("m"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies ID-key authentication, uniform error messages,
     * and session persistence until logout.
     */
    @Test
    void idKeyLoginHasUniformFailuresAndPersistsSessionUntilLogout() {
        member();

        String unknownIdMessage = assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("unknown", "password123")
        ).getMessage();

        String incorrectPasswordMessage = assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("m", "incorrect")
        ).getMessage();

        assertEquals(unknownIdMessage, incorrectPasswordMessage);
        assertEquals("Invalid ID key or password", unknownIdMessage);

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("member", "password123"));

        accounts.login("m", "password123");

        assertEquals(
                "m",
                accounts.currentUser().orElseThrow().getIdKey());

        assertEquals(
                "m",
                accounts.requireUser().getIdKey());

        accounts.logout();

        assertTrue(accounts.currentUser().isEmpty());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that deleted accounts cannot authenticate,
     * including after the repository is reopened.
     */
    @Test
    void deletedAccountCannotAuthenticateAfterRestart() {
        admin();
        member();

        accounts.deleteUser("m");

        assertFalse(users.existsByIdKey("m"));

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("m", "password123"));

        FileUserRepository reopened = new FileUserRepository(
                directory.resolve("users.properties"));

        AccountService restarted = new AccountService(reopened, HASHER);

        assertFalse(reopened.existsByIdKey("m"));

        assertThrows(
                IllegalArgumentException.class,
                () -> restarted.login("m", "password123"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that protected operations return the authenticated
     * user and reject unauthenticated access.
     */
    @Test
    void requireUserReturnsAuthenticatedUser() {
        assertTrue(accounts.currentUser().isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.requireUser());

        member();
        accounts.login("m", "password123");

        User authenticated = accounts.requireUser();

        assertEquals("m", authenticated.getIdKey());
        assertEquals("member", authenticated.getUsername());
        assertEquals(UserRole.MEMBER, authenticated.getRole());

        accounts.logout();

        assertThrows(
                IllegalStateException.class,
                () -> accounts.requireUser());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that a successful login replaces the previous session
     * and a failed login clears the active session.
     */
    @Test
    void loginReplacesExistingSession() {
        admin();

        assertEquals("a", accounts.requireUser().getIdKey());

        member();
        accounts.login("m", "password123");

        assertEquals("m", accounts.requireUser().getIdKey());
        assertEquals(UserRole.MEMBER, accounts.requireUser().getRole());

        assertThrows(
                IllegalArgumentException.class,
                () -> accounts.login("a", "wrong"));

        assertTrue(accounts.currentUser().isEmpty());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.requireUser());

        accounts.login("a", "secret123");

        assertEquals("a", accounts.requireUser().getIdKey());
        assertEquals(UserRole.ADMIN, accounts.requireUser().getRole());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that administrators can create accounts with
     * different roles and that each account can authenticate.
     */
    @Test
    void adminCanCreateBothRoles() {
        admin();

        User createdMember = accounts.addUser(
                "m", "member", "password123",
                "Member", "member@example.com", "456",
                UserRole.MEMBER);

        User createdAdmin = accounts.addUser(
                "b", "boss", "password123",
                "Boss", "boss@example.com", "789",
                UserRole.ADMIN);

        assertEquals(UserRole.MEMBER, createdMember.getRole());
        assertEquals(UserRole.ADMIN, createdAdmin.getRole());

        assertEquals(3, accounts.listUsers().size());

        assertNotEquals(
                "password123",
                users.findByIdKey("m").orElseThrow().getPasswordHash());

        accounts.logout();

        User loggedInMember = accounts.login("m", "password123");
        assertEquals(UserRole.MEMBER, loggedInMember.getRole());

        assertThrows(
                IllegalStateException.class,
                () -> accounts.listUsers());

        accounts.logout();

        User loggedInAdmin = accounts.login("b", "password123");
        assertEquals(UserRole.ADMIN, loggedInAdmin.getRole());

        assertEquals(3, accounts.listUsers().size());
    }
}
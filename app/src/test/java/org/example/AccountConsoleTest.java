package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.Pbkdf2PasswordHasher;
import org.example.service.AccountService;
import org.example.ui.ConsoleUi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Tests account forms through scripted console input and persistent storage. */
class AccountConsoleTest {
    @TempDir
    Path directory;
    private FileUserRepository users;
    private AccountService accounts;
    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    /** Creates isolated storage without a preconfigured administrator. */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, hasher);
    }

    /** Runs the public console workflow and captures its output. */
    private String cli(String... lines) {
        StringWriter output = new StringWriter();
        new ConsoleUi(accounts, null, new Scanner(String.join("\n", lines) + "\n"),
                new PrintWriter(output, true)).run();
        return output.toString();
    }

    private void admin() {
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
    }

    /** Verifies first-run setup persists an administrator without logging in. */
    @Test
    void bootstrapPersistsAdministrator() {
        String transcript = cli("a", "admin", "secret123", "Admin", "admin@example.com", "123", "quit");
        var restored = new FileUserRepository(directory.resolve("users.properties"));
        assertEquals(UserRole.ADMIN, restored.findByIdKey("a").orElseThrow().getRole());
        assertTrue(accounts.currentUser().isEmpty());
        assertTrue(transcript.contains("First run:"));
        assertTrue(transcript.contains("Account created."));
    }

    /** Verifies profile fields, password hashing, persistence, and subsequent login. */
    @Test
    void registrationPersistsProfileAndAllowsLogin() throws Exception {
        admin();
        String transcript = cli("register", "m", "member", "password123", "Member",
                "member@example.com", "456", "login", "m", "password123", "quit");
        var member = new FileUserRepository(directory.resolve("users.properties"))
                .findByIdKey("m").orElseThrow();
        assertEquals("member", member.getUsername());
        assertEquals("Member", member.getFullName());
        assertEquals("member@example.com", member.getEmail());
        assertEquals("456", member.getPhone());
        assertEquals(UserRole.MEMBER, member.getRole());
        assertTrue(hasher.verify("password123", member.getPasswordHash()));
        assertFalse(Files.readString(directory.resolve("users.properties")).contains("password123"));
        assertTrue(transcript.contains("Member (member) [MEMBER]"));
        assertFalse(transcript.contains(member.getPasswordHash()));
    }

    /** Verifies administrators can create both supported roles through the form. */
    @Test
    void administratorCreatesMemberAndAdministrator() {
        admin();
        cli("login", "a", "secret123",
                "add-user", "m", "member", "password123", "Member", "m@example.com", "456", "member",
                "add-user", "b", "boss", "password123", "Boss", "b@example.com", "789", "ADMIN", "quit");
        var restored = new FileUserRepository(directory.resolve("users.properties"));
        assertEquals(UserRole.MEMBER, restored.findByIdKey("m").orElseThrow().getRole());
        assertEquals(UserRole.ADMIN, restored.findByIdKey("b").orElseThrow().getRole());
    }

    /** Verifies unauthorized account creation is rejected before reading profile fields. */
    @Test
    void guestAndMemberCannotCreateAdministrativeAccounts() {
        admin();
        accounts.register("m", "member", "password123", "Member", "m@example.com", "456");
        String transcript = cli("add-user", "login", "m", "password123", "add-user", "quit");
        assertTrue(transcript.contains("Please log in first"));
        assertTrue(transcript.contains("Administrator access required"));
        assertEquals(2, users.findAll().size());
        assertFalse(transcript.contains("Full name:"));
    }

    /** Verifies invalid setup can be retried without creating a partial account. */
    @Test
    void bootstrapRetriesInvalidProfile() {
        String transcript = cli("a", "admin", "short", "Admin", "admin@example.com", "123",
                "a", "admin", "secret123", "Admin", "admin@example.com", "123", "quit");
        assertTrue(transcript.contains("Password must contain"));
        assertEquals(1, users.findAll().size());
        assertFalse(accounts.needsBootstrap());
    }

    /** Verifies duplicate registration is rejected and the menu remains usable. */
    @Test
    void duplicateRegistrationDoesNotOverwriteAccount() {
        admin();
        String transcript = cli("register", "a", "different", "password123", "Other",
                "other@example.com", "456", "quit");
        assertTrue(transcript.contains("ID key or username already exists"));
        assertEquals(1, users.findAll().size());
        assertEquals("admin", users.findByIdKey("a").orElseThrow().getUsername());
        assertTrue(transcript.contains("Goodbye."));
    }

    /** Verifies an unsupported role does not save an account. */
    @Test
    void invalidAdministratorSelectedRoleDoesNotSave() {
        admin();
        String transcript = cli("login", "a", "secret123", "add-user", "b", "boss", "password123",
                "Boss", "b@example.com", "789", "GUEST", "quit");
        assertTrue(transcript.contains("Error:"));
        assertFalse(users.existsByIdKey("b"));
    }

    /** Verifies incomplete setup exits gracefully without saving partial input. */
    @Test
    void incompleteBootstrapDoesNotSave() {
        String transcript = cli("a", "admin");
        assertTrue(transcript.contains("Input closed."));
        assertTrue(transcript.contains("Goodbye."));
        assertTrue(accounts.needsBootstrap());
    }
}

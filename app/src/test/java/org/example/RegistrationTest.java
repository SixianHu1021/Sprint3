package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.Pbkdf2PasswordHasher;
import org.example.service.AccountService;
import org.example.service.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies registration with the production password hasher and isolated file storage. */
class RegistrationTest {
    @TempDir Path directory;
    private FileUserRepository users;
    private AccountService accounts;
    private Pbkdf2PasswordHasher hasher;

    /** Creates independent dependencies for each scenario. */
    @BeforeEach
    void setup() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        hasher = new Pbkdf2PasswordHasher();
        accounts = new AccountService(users, hasher);
    }

    /** Creates a valid profile for persistence and duplicate scenarios. */
    private User member() {
        return accounts.register("m", "member", "Password123", "Member", "member@example.com", "456");
    }

    /** All profile fields and the verifiable password hash survive reopening. */
    @Test
    void registrationPersistsEveryField() throws IOException {
        User created = member();
        User restored = new FileUserRepository(directory.resolve("users.properties")).findByIdKey("m").orElseThrow();
        assertEquals(UserRole.MEMBER, restored.getRole());
        assertEquals("member", restored.getUsername());
        assertEquals("Member", restored.getFullName());
        assertEquals("member@example.com", restored.getEmail());
        assertEquals("456", restored.getPhone());
        assertEquals(created.getPasswordHash(), restored.getPasswordHash());
        assertTrue(hasher.verify("Password123", restored.getPasswordHash()));
        assertFalse(hasher.verify("wrong", restored.getPasswordHash()));
        assertFalse(Files.readString(directory.resolve("users.properties")).contains("Password123"));
    }

    /** Duplicate IDs and usernames are rejected without replacing the original profile. */
    @Test
    void duplicateIdentifiersAreRejected() {
        User original = member();
        assertThrows(IllegalArgumentException.class, () -> member());
        assertThrows(IllegalArgumentException.class,
                () -> accounts.register(" m ", "other", "Password123", "N", "a@b.co", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> accounts.register("other", " member ", "Password123", "N", "a@b.co", "1"));
        assertEquals(1, users.findAll().size());
        assertEquals(original.getPasswordHash(), users.findByIdKey("m").orElseThrow().getPasswordHash());
        assertEquals(1, new FileUserRepository(directory.resolve("users.properties")).findAll().size());
    }

    /** Required profile fields reject missing, blank, oversized and control-character input. */
    @Test
    void invalidFieldsDoNotCreateAccounts() {
        String[] valid = {"m", "member", "Password123", "Member", "member@example.com", "456"};
        for (int field : new int[]{0, 1, 3, 4, 5}) {
            for (String invalid : new String[]{null, " ", "x".repeat(201), "bad" + (char) 27}) {
                String[] input = valid.clone();
                input[field] = invalid;
                assertThrows(IllegalArgumentException.class,
                        () -> accounts.register(input[0], input[1], input[2], input[3], input[4], input[5]));
            }
        }
        for (String email : new String[]{"invalid", "a@b", "a b@c.co", "a@@b.co"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> accounts.register("m", "member", "Password123", "N", email, "1"));
        }
        assertTrue(users.findAll().isEmpty());
        assertFalse(Files.exists(directory.resolve("users.properties")));
    }

    /** Invalid passwords do not leave partially created accounts. */
    @Test
    void invalidPasswordsAreRejected() {
        for (String password : new String[]{null, "", "short", " ".repeat(8), "x".repeat(129)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> accounts.register("m", "member", password, "N", "a@b.co", "1"));
        }
        assertTrue(users.findAll().isEmpty());
    }

    /** Profile whitespace is normalized while password whitespace is preserved. */
    @Test
    void normalizesProfilesWithoutTrimmingPasswords() {
        String password = " Password123 ";
        User user = accounts.register(" m ", " member ", password, " Member ", " a@b.co ", " 456 ");
        assertEquals("m", user.getIdKey());
        assertEquals("member", user.getUsername());
        assertEquals("Member", user.getFullName());
        assertEquals("a@b.co", user.getEmail());
        assertEquals("456", user.getPhone());
        assertTrue(hasher.verify(password, user.getPasswordHash()));
        assertFalse(hasher.verify(password.strip(), user.getPasswordHash()));
    }

    /** Valid boundary lengths produce member accounts and verifiable hashes. */
    @Test
    void passwordBoundariesAreAccepted() {
        for (int length : new int[]{8, 128}) {
            String password = "x".repeat(length);
            // Sprint 2 · US16（Henry）：邮箱和电话必须唯一，因此每个账户使用不同联系方式。
            User user = accounts.register("m" + length, "member" + length, password, "N",
                    "a" + length + "@b.co", "1" + length);
            assertEquals(UserRole.MEMBER, user.getRole());
            assertTrue(hasher.verify(password, user.getPasswordHash()));
        }
    }

    /** Persistence errors are propagated and leave the repository empty. */
    @Test
    void failedPersistenceDoesNotCreateAccount() throws IOException {
        Path blocker = directory.resolve("blocker");
        FileUserRepository blocked = new FileUserRepository(blocker.resolve("users.properties"));
        AccountService service = new AccountService(blocked, hasher);
        Files.writeString(blocker, "regular file");
        assertThrows(IllegalStateException.class,
                () -> service.register("m", "member", "Password123", "N", "a@b.co", "1"));
        assertTrue(blocked.findAll().isEmpty());
    }

    /** Shared validation strips whitespace and accepts the 200-character boundary. */
    @Test
    void requiredTextValidation() {
        assertEquals("hello", Validation.text(" hello ", "Field"));
        assertEquals("x".repeat(200), Validation.text("x".repeat(200), "Field"));
        assertThrows(IllegalArgumentException.class, () -> Validation.text(null, "Field"));
        assertThrows(IllegalArgumentException.class, () -> Validation.text(" ", "Field"));
        assertThrows(IllegalArgumentException.class, () -> Validation.text("x".repeat(201), "Field"));
        assertThrows(IllegalArgumentException.class, () -> Validation.text("bad" + (char) 27, "Field"));
    }
}

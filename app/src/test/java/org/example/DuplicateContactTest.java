package org.example;

import java.nio.file.Path;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ContactValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Sprint 2 · US16 #51】主负责人：Henry。审核人：Yuhan。
 * 测什么：注册、管理员添加、修改资料三种入口都拒绝重复邮箱／电话；错误消息说明是哪一个字段；
 * 邮箱比较不区分大小写并以小写保存；电话比较忽略空格和分隔符；仓库查找方法本身。
 */
class DuplicateContactTest {

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

    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "Admin@Example.com", "+61 400 000 000");
    }

    @Test
    void registrationRejectsDuplicateEmailIgnoringCase() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accounts.register("m", "member", "password123", "M", "ADMIN@example.COM", "111"));
        assertTrue(error.getMessage().startsWith("Email address is already registered"));
        assertEquals(1, users.findAll().size());
    }

    @Test
    void registrationRejectsDuplicatePhoneIgnoringSeparators() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> accounts.register("m", "member", "password123", "M", "m@example.com", "+61-400-000-000"));
        assertTrue(error.getMessage().startsWith("Phone number is already registered"));
    }

    @Test
    void emailIsStoredInLowercase() {
        assertEquals("admin@example.com", users.findByIdKey("a").orElseThrow().getEmail());
        assertEquals("m@example.com",
                accounts.register("m", "member", "password123", "M", "M@Example.COM", "111").getEmail());
    }

    @Test
    void adminCreatedUsersFollowTheSameRules() {
        accounts.login("a", "secret123");
        accounts.addUser("m", "member", "password123", "M", "m@example.com", "111", UserRole.MEMBER);
        assertThrows(IllegalArgumentException.class, () -> accounts.addUser(
                "b", "boss", "password123", "B", "M@EXAMPLE.com", "222", UserRole.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> accounts.addUser(
                "b", "boss", "password123", "B", "b@example.com", "1 1 1", UserRole.ADMIN));
        assertEquals(2, users.findAll().size());
    }

    @Test
    void profileUpdateCannotTakeAnotherUsersContacts() {
        accounts.register("m", "member", "password123", "M", "m@example.com", "111");
        accounts.login("m", "password123");
        assertThrows(IllegalArgumentException.class,
                () -> accounts.updateProfile("M", "admin@example.com", "111"));
        assertThrows(IllegalArgumentException.class,
                () -> accounts.updateProfile("M", "m@example.com", "+61400000000"));
        assertEquals("m@example.com", users.findByIdKey("m").orElseThrow().getEmail());
    }

    @Test
    void repositoryLookupsHandleMissingValues() {
        assertTrue(users.findByEmail(null).isEmpty());
        assertTrue(users.findByPhone(null).isEmpty());
        assertTrue(users.findByEmail("nobody@example.com").isEmpty());
        assertEquals("a", users.findByPhone("(+61) 400.000.000").orElseThrow().getIdKey());
    }

    @Test
    void phoneFormatIsValidated() {
        assertEquals("+61 (2) 9000-0000", ContactValidator.phone(" +61 (2) 9000-0000 "));
        assertThrows(IllegalArgumentException.class, () -> ContactValidator.phone("abc"));
        assertThrows(IllegalArgumentException.class, () -> ContactValidator.phone("+-()"));
    }
}

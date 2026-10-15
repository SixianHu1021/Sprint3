package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Scanner;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.ui.AccountScreens;
import org.example.ui.ConsoleIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
 *
 * 【Sprint 2 · US03 #4 / US12 #13 / US16 #51】主负责人：Henry。
 * 测什么：AccountScreens 页面本身（不依赖 ConsoleUi 合并）：查看资料、回车保留原值、修改并显示新值、
 * 重复邮箱提示、两次新密码不一致、修改密码成功；管理员表单用编号选择角色，错误角色重问。
 */
class ProfileConsoleTest {

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
    private StringWriter out;

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        out = new StringWriter();
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * Creates the screen with scripted input.
     */
    private AccountScreens screens(String... lines) {
        return new AccountScreens(accounts,
                new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true)));
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void profileShowsDetailsAndKeepsBlankFields() {
        accounts.login("m", "password123");
        screens("1", "", "new@example.com", "", "0").profile();
        String text = out.toString();
        assertTrue(text.contains("ID key:    m (cannot be changed)"));
        assertTrue(text.contains("Profile updated."));
        assertTrue(text.contains("Email:     new@example.com"));
        User saved = users.findByIdKey("m").orElseThrow();
        assertEquals("Member", saved.getFullName());
        assertEquals("456", saved.getPhone());
        assertFalse(text.contains("test-only:"));
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void duplicateEmailIsReportedAndProfileKept() {
        accounts.login("m", "password123");
        // Sprint 2 · US14（逐项检查）：重复邮箱立即提示并重问邮箱；回车保留原值。
        screens("1", "", "ADMIN@example.com", "", "", "0").profile();
        assertTrue(out.toString().contains("Error: Email address is already registered to another account"));
        assertTrue(out.toString().contains("Profile updated."));
        assertEquals("member@example.com", users.findByIdKey("m").orElseThrow().getEmail());
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void passwordChangeNeedsMatchingConfirmation() {
        accounts.login("m", "password123");
        // Sprint 2 · US14（逐项检查）：当前密码错误、新密码不合规、确认不一致都只重问该项。
        screens("2", "wrong-password", "password123", "short", "NewPass456", "Different789", "NewPass456",
                "0").profile();
        String text = out.toString();
        assertTrue(text.contains("Error: Current password is incorrect"));
        assertTrue(text.contains("Error: Password must contain 8-128 characters"));
        assertTrue(text.contains("Error: Passwords do not match; type the new password again"));
        assertTrue(text.contains("Password changed."));
        assertTrue(HASHER.verify("NewPass456", users.findByIdKey("m").orElseThrow().getPasswordHash()));
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void guestCannotOpenProfile() {
        assertThrows(IllegalStateException.class, () -> screens("0").profile());
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void adminFormSelectsRoleByNumberAndRetriesInvalidRole() {
        accounts.login("a", "secret123");
        screens("b", "boss", "password123", "Boss", "b@example.com", "789", "3", "2").createAccount("admin");
        assertTrue(out.toString().contains("Error: Role must be 1 (MEMBER) or 2 (ADMIN)."));
        assertEquals(UserRole.ADMIN, users.findByIdKey("b").orElseThrow().getRole());
        screens("c", "carol", "password123", "Carol", "c@example.com", "790", "1").createAccount("admin");
        assertEquals(UserRole.MEMBER, users.findByIdKey("c").orElseThrow().getRole());
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void blankRegistrationFieldIsRepromptedImmediately() {
        screens("", "n", "newbie", "password123", "Newbie", "n@example.com", "999").createAccount("register");
        assertTrue(out.toString().contains("Error: ID key cannot be empty"));
        assertTrue(users.existsByIdKey("n"));
    }
}

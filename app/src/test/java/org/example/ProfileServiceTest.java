package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.Pbkdf2PasswordHasher;
import org.example.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
 *
 * 【Sprint 2 · US03 #4】主负责人：Henry。
 * 测什么：修改姓名／邮箱／电话并在重启后保留；ID、用户名、角色不变；修改密码需要正确的当前密码，
 * 新密码以哈希保存，旧密码失效；访客不能修改；无效输入不改动原资料。
 */
class ProfileServiceTest {

    @TempDir
    Path directory;

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
    private FileUserRepository users;
    private AccountService accounts;

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, hasher);
        accounts.register("m", "member", "Password123", "Member", "member@example.com", "0400 000 001");
        accounts.login("m", "Password123");
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void updatedProfilePersistsAcrossSessions() {
        accounts.updateProfile(" New Name ", "New@Example.com", "0400 999 999");
        User restored = new FileUserRepository(directory.resolve("users.properties")).findByIdKey("m").orElseThrow();
        assertEquals("New Name", restored.getFullName());
        assertEquals("new@example.com", restored.getEmail());
        assertEquals("0400 999 999", restored.getPhone());
        assertEquals("m", restored.getIdKey());
        assertEquals("member", restored.getUsername());
        assertEquals(UserRole.MEMBER, restored.getRole());
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void keepingOwnEmailAndPhoneIsAllowed() {
        User updated = accounts.updateProfile("Renamed", "MEMBER@example.com", "0400000001");
        assertEquals("Renamed", updated.getFullName());
        assertEquals("member@example.com", updated.getEmail());
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void invalidUpdatesLeaveProfileUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile(" ", "a@b.co", "1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile("N", "invalid", "1"));
        assertThrows(IllegalArgumentException.class, () -> accounts.updateProfile("N", "a@b.co", "call me"));
        assertEquals("Member", users.findByIdKey("m").orElseThrow().getFullName());
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void passwordChangeStoresNewHashAndInvalidatesOldPassword() throws Exception {
        String oldHash = users.findByIdKey("m").orElseThrow().getPasswordHash();
        accounts.changePassword("Password123", "BrandNew456");
        String newHash = users.findByIdKey("m").orElseThrow().getPasswordHash();
        assertFalse(oldHash.equals(newHash));
        assertTrue(hasher.verify("BrandNew456", newHash));
        assertFalse(Files.readString(directory.resolve("users.properties")).contains("BrandNew456"));
        accounts.logout();
        assertThrows(IllegalArgumentException.class, () -> accounts.login("m", "Password123"));
        assertEquals("m", accounts.login("m", "BrandNew456").getIdKey());
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void passwordChangeRequiresCorrectCurrentPasswordAndValidNewPassword() {
        assertEquals("Current password is incorrect", assertThrows(IllegalArgumentException.class,
                () -> accounts.changePassword("wrong-password", "BrandNew456")).getMessage());
        assertEquals("New password must contain 8-128 characters", assertThrows(IllegalArgumentException.class,
                () -> accounts.changePassword("Password123", "short")).getMessage());
        assertTrue(hasher.verify("Password123", users.findByIdKey("m").orElseThrow().getPasswordHash()));
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void guestsCannotChangeProfileOrPassword() {
        accounts.logout();
        assertThrows(IllegalStateException.class, () -> accounts.updateProfile("N", "n@b.co", "1"));
        assertThrows(IllegalStateException.class, () -> accounts.changePassword("Password123", "BrandNew456"));
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     */
    @Test
    void fieldChecksNormalizeAndReject() {
        assertEquals("x", accounts.checkNewIdKey(" x "));
        assertThrows(IllegalArgumentException.class, () -> accounts.checkNewIdKey(" "));
        assertEquals("ID key 'm' is already taken",
                assertThrows(IllegalArgumentException.class, () -> accounts.checkNewIdKey("m")).getMessage());
        assertEquals("Username 'member' is already taken",
                assertThrows(IllegalArgumentException.class, () -> accounts.checkNewUsername("member")).getMessage());
        assertEquals("goodpass1", AccountService.checkPassword("goodpass1"));
        assertThrows(IllegalArgumentException.class, () -> AccountService.checkPassword("short"));
        assertEquals("member@example.com", accounts.checkEmail("MEMBER@example.com", "m"));
        assertThrows(IllegalArgumentException.class, () -> accounts.checkEmail("member@example.com", null));
        assertEquals("0400 000 001", accounts.checkPhone("0400 000 001", "m"));
        assertThrows(IllegalArgumentException.class, () -> accounts.checkPhone("0400000001", null));
        assertEquals("Password123", accounts.checkCurrentPassword("Password123"));
        assertThrows(IllegalArgumentException.class, () -> accounts.checkCurrentPassword("wrong-pass"));
        accounts.logout();
        assertThrows(IllegalStateException.class, () -> accounts.checkCurrentPassword("Password123"));
    }
}

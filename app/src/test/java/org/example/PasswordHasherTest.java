package org.example;

import java.util.Base64;
import org.example.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
 *
 * Unit tests for the PBKDF2 password hashing implementation.
 *
 * <p>Verifies salted hashing, password verification, input validation,
 * and rejection of malformed password hashes.
 */
class PasswordHasherTest {

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that password hashes use random salts and can be
     * validated using the original password.
     */
    @Test
    void hashesAreSaltedAndVerifiable() {
        var hasher = new Pbkdf2PasswordHasher();

        String first = hasher.hash("密码-long-password");
        String second = hasher.hash("密码-long-password");

        assertNotEquals(first, second);
        assertFalse(first.contains("long-password"));

        assertTrue(hasher.verify("密码-long-password", first));
        assertTrue(new Pbkdf2PasswordHasher().verify("密码-long-password", second));

        assertFalse(hasher.verify("wrong", first));
        assertFalse(hasher.verify(null, first));
        assertFalse(hasher.verify("password", null));

        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));

        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Verifies that malformed hashes and unsupported parameters
     * are rejected without causing authentication failures.
     */
    @Test
    void malformedHashesFailClosed() {
        var hasher = new Pbkdf2PasswordHasher();

        String salt = Base64.getEncoder().encodeToString(new byte[16]);

        String hash = Base64.getEncoder().encodeToString(new byte[32]);

        for (String encoded : new String[]{
                "",
                "garbage",
                "other$600000$" + salt + "$" + hash,
                "pbkdf2-sha256$no$" + salt + "$" + hash,
                "pbkdf2-sha256$1$" + salt + "$" + hash,
                "pbkdf2-sha256$999999999$" + salt + "$" + hash,
                "pbkdf2-sha256$600000$!$!",
                "pbkdf2-sha256$600000$AA==$" + hash,
                "pbkdf2-sha256$600000$" + salt + "$AA=="
        }) {
            assertFalse(hasher.verify("password", encoded), encoded);
        }
    }
}
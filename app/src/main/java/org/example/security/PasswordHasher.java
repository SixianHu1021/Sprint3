package org.example.security;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
 *
 * Defines operations for hashing and verifying passwords.
 */
public interface PasswordHasher {

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    String hash(String password);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    boolean verify(String password, String hashedPassword);
}
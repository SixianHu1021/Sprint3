package org.example.model;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
 *
 */
public class User {

    private final String idKey;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public User(
            String idKey,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UserRole role) {

        this.idKey = idKey;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getIdKey() {
        return idKey;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getUsername() {
        return username;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getEmail() {
        return email;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    public UserRole getRole() {
        return role;
    }
}
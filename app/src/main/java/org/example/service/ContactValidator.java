package org.example.service;

import java.util.Locale;
import org.example.model.User;
import org.example.repository.UserRepository;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
 *
 * 【Sprint 2 · US16 #51】主负责人：Henry。
 * 做什么：集中处理邮箱、电话的格式和唯一性：邮箱转小写后保存，重复邮箱或电话会被拒绝，并说明是哪一个字段重复。
 * 对接／验证：AccountService.create（注册、首次管理员、管理员添加）和 updateProfile（US03 修改资料）都调用这里，
 * 所以三种入口使用同一套规则；不要在界面层另写一遍。
 */
public final class ContactValidator {

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * Prevents utility class instantiation.
     */
    private ContactValidator() {
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry。
     * 做什么：检查必填、格式含域名，然后转为小写，作为保存和比较用的邮箱。
     * 对接／验证：Sprint 1 的格式规则不变，只增加小写化。
     *
     * @param email raw email input
     * @return normalized lowercase email
     * @throws IllegalArgumentException if the email is missing or malformed
     */
    public static String email(String email) {
        String value = Validation.text(email, "Email").toLowerCase(Locale.ROOT);
        if (!value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email must include a valid domain, for example name@example.com");
        }
        return value;
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry。
     * 做什么：检查电话必填、只包含数字和常见分隔符（空格 + - ( ) .），并至少有一个数字。
     * 对接／验证：保留国际号码格式，如 “+61 400 000 000”。
     *
     * @param phone raw phone input
     * @return trimmed phone number
     * @throws IllegalArgumentException if the phone number is missing or contains other characters
     */
    public static String phone(String phone) {
        String value = Validation.text(phone, "Phone");
        if (!value.matches("[0-9+()\\s.-]+") || value.chars().noneMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Phone may contain only digits, spaces and + - ( ) .");
        }
        return value;
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry。
     * 做什么：确认邮箱和电话没有被其他账户使用；ownerId 为当前用户自己的 ID 时（修改资料），自己的旧值不算重复。
     * 对接／验证：错误消息分别写明 “Email address is already registered” 或 “Phone number is already registered”。
     *
     * @param users user repository
     * @param email normalized email
     * @param phone normalized phone
     * @param ownerId ID key allowed to keep these values, or {@code null} for a new account
     * @throws IllegalArgumentException if another account already uses the email or phone
     */
    public static void requireUnique(UserRepository users, String email, String phone, String ownerId) {
        requireUniqueEmail(users, email, ownerId);
        requireUniquePhone(users, phone, ownerId);
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【逐项检查】主负责人：Henry。
     * 做什么：只检查邮箱是否被其他账户使用；表单输入邮箱后立即调用，不必等电话填完。
     * 对接／验证：AccountService.checkEmail 调用；requireUnique 复用。
     *
     * @param users user repository
     * @param email normalized email
     * @param ownerId ID key allowed to keep this email, or {@code null}
     * @throws IllegalArgumentException if another account uses the email
     */
    public static void requireUniqueEmail(UserRepository users, String email, String ownerId) {
        if (users.findByEmail(email).filter(u -> !isOwner(u, ownerId)).isPresent()) {
            throw new IllegalArgumentException("Email address is already registered to another account");
        }
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【逐项检查】主负责人：Henry。
     * 做什么：只检查电话是否被其他账户使用；表单输入电话后立即调用。
     * 对接／验证：AccountService.checkPhone 调用；requireUnique 复用。
     *
     * @param users user repository
     * @param phone normalized phone
     * @param ownerId ID key allowed to keep this phone, or {@code null}
     * @throws IllegalArgumentException if another account uses the phone
     */
    public static void requireUniquePhone(UserRepository users, String phone, String ownerId) {
        if (users.findByPhone(phone).filter(u -> !isOwner(u, ownerId)).isPresent()) {
            throw new IllegalArgumentException("Phone number is already registered to another account");
        }
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry。
     * 做什么：判断找到的账户是否就是正在修改资料的本人。
     * 对接／验证：仅供 requireUnique 使用。
     */
    private static boolean isOwner(User user, String ownerId) {
        return ownerId != null && user.getIdKey().equals(ownerId);
    }
}

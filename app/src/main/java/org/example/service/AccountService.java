package org.example.service;

import java.util.List;
import java.util.Optional;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.UserRepository;
import org.example.security.PasswordHasher;

/**
 * Registers member profiles using injected storage and password hashing.
 *
 * <p>Sprint 2 变更一览（其余方法为 Sprint 1 原有，本轮不改）：
 * <ul>
 *   <li>【US16 #51】create：邮箱小写化、邮箱／电话唯一。主负责人：Henry。审核人：Yuhan。</li>
 *   <li>【US03 #4】updateProfile、changePassword、requirePasswordPolicy。主负责人：Henry。审核人：Yuhan。</li>
 *   <li>【US12 #13】requireAdmin 改为 public，供 Stella 的 StatisticsService 复用。
 *       改动人：Henry（同一 PR 顺带修改）。审核人：Yuhan（Sprint 1 原作者）、Stella（调用者）。</li>
 * </ul>
 */
public final class AccountService {
    private final UserRepository users;
    private final PasswordHasher hasher;
    private String currentId;

    /**
     * Creates the registration service.
     * @param users repository for lookup and persistence
     * @param hasher password hashing implementation
     */
    public AccountService(UserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    /**
     * Registers a MEMBER after validating the profile and checking uniqueness.
     * Passwords are not trimmed. Email format, username uniqueness and password
     * length limits are implementation policies.
     * @param id unique custom ID key
     * @param username unique username
     * @param password nonblank password of 8-128 characters
     * @param name full name
     * @param email email with a domain
     * @param phone required phone text
     * @return persisted member profile
     * @throws IllegalArgumentException if input is invalid or identifiers are taken
     * @throws IllegalStateException if hashing or persistence fails
     */
    public User register(String id, String username, String password, String name, String email, String phone) {
        return create(id, username, password, name, email, phone, UserRole.MEMBER);
    }

    /**
     * Validates and saves a profile with a supplied role.
     *
     * <p>【Sprint 2 · US16 #51】主负责人：Henry。审核人：Yuhan。
     * 做什么：顺序为 校验资料 → 邮箱／电话格式 → 密码规则 → ID／用户名查重 → 邮箱／电话查重 → 哈希 → 保存。
     * 对接／验证：注册、首次管理员和管理员添加用户都经过这里，因此管理员创建的账户也受同样的唯一性检查。
     */
    private User create(String id, String username, String password, String name,
                        String email, String phone, UserRole role) {
        id = Validation.text(id, "ID key");
        username = Validation.text(username, "Username");
        name = Validation.text(name, "Full name");
        email = ContactValidator.email(email);
        phone = ContactValidator.phone(phone);
        requirePasswordPolicy(password, "Password");
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (users.existsByIdKey(id) || users.existsByUsername(username)) {
            throw new IllegalArgumentException("ID key or username already exists");
        }
        ContactValidator.requireUnique(users, email, phone, null);
        User user = new User(id, username, hasher.hash(password), name, email, phone, role);
        users.save(user);
        return user;
    }

    /**
     * 【Sprint 2 · US03 #4】主负责人：Henry。审核人：Yuhan。
     * 做什么：统一密码长度规则（8–128，不能全是空白）；注册和修改密码共用。
     * 对接／验证：错误消息以字段名开头，例如 “New password must contain 8-128 characters”。
     *
     * @param password password to check; not trimmed
     * @param label field label for the error message
     * @throws IllegalArgumentException if the password breaks the policy
     */
    private static void requirePasswordPolicy(String password, String label) {
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 128) {
            throw new IllegalArgumentException(label + " must contain 8-128 characters");
        }
    }

    /**
     * 【Sprint 2 · US03 #4】主负责人：Henry。审核人：Yuhan。
     * 做什么：已登录用户修改自己的姓名、邮箱、电话。ID key、用户名、角色和密码哈希保持不变。
     * 对接／验证：邮箱／电话经过 ContactValidator，不能与其他用户重复（自己的旧值可以保留）；
     * 保存后重启仍有效（ProfileServiceTest 验证）。界面由 Henry 的 AccountScreens.profile 调用。
     *
     * @param name new full name
     * @param email new email address
     * @param phone new phone number
     * @return the saved profile
     * @throws IllegalStateException if nobody is logged in or saving fails
     * @throws IllegalArgumentException if a field is invalid or already used by another account
     */
    public User updateProfile(String name, String email, String phone) {
        User current = requireUser();
        name = Validation.text(name, "Full name");
        email = ContactValidator.email(email);
        phone = ContactValidator.phone(phone);
        ContactValidator.requireUnique(users, email, phone, current.getIdKey());
        User updated = new User(current.getIdKey(), current.getUsername(), current.getPasswordHash(),
                name, email, phone, current.getRole());
        users.save(updated);
        return updated;
    }

    /**
     * 【Sprint 2 · US03 #4】主负责人：Henry。审核人：Yuhan（密码哈希 Sprint 1 负责人）。
     * 做什么：先验证当前密码，再检查新密码规则，最后保存新哈希；原始新密码不会被保存。
     * 对接／验证：调用 Yuhan 的 PasswordHasher.verify/hash；修改后旧密码登录失败、新密码登录成功。
     *
     * @param currentPassword the user's existing password
     * @param newPassword the replacement password
     * @throws IllegalStateException if nobody is logged in or saving fails
     * @throws IllegalArgumentException if the current password is wrong or the new password is invalid
     */
    public void changePassword(String currentPassword, String newPassword) {
        User current = requireUser();
        if (!hasher.verify(currentPassword, current.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        requirePasswordPolicy(newPassword, "New password");
        users.save(new User(current.getIdKey(), current.getUsername(), hasher.hash(newPassword),
                current.getFullName(), current.getEmail(), current.getPhone(), current.getRole()));
    }

    /**
     * Checks whether the system requires initial administrator setup.
     *
     * @return true if no user accounts exist; false otherwise
     */
    public boolean needsBootstrap() {
        return users.findAll().isEmpty();
    }

    /**
     * Creates the initial administrator account when the user repository is empty.
     *
     * @param id the unique user ID
     * @param username the username
     * @param password the plaintext password
     * @param name the user's full name
     * @param email the user's email address
     * @param phone the user's phone number
     * @return the newly created administrator account
     * @throws IllegalStateException if an account already exists
     */
    public User bootstrapAdmin(String id, String username, String password,
                               String name, String email, String phone) {
        if (!needsBootstrap()) {
            throw new IllegalStateException("Initial administrator already configured");
        }

        return create(id, username, password, name, email, phone, UserRole.ADMIN);
    }

    /**
     * Authenticates a user using their ID key and password.
     *
     * <p>Any existing session is cleared before authentication.
     *
     * @param idKey the user's unique ID key
     * @param password the plaintext password
     * @return the authenticated user
     * @throws IllegalArgumentException if the credentials are invalid
     */
    public User login(String idKey, String password) {
        logout();

        User user = users.findByIdKey(idKey == null ? "" : idKey.strip()).orElse(null);

        if (user == null || !hasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid ID key or password");
        }

        currentId = user.getIdKey();
        return user;
    }

    /**
     * Logs out the current user by clearing the active session.
     */
    public void logout() {
        currentId = null;
    }

    /**
     * Retrieves the currently authenticated user from the repository.
     *
     * @return an Optional containing the current user, or an empty Optional
     *         if no user is authenticated or the account no longer exists
     */
    public Optional<User> currentUser() {
        return currentId == null ? Optional.empty() : users.findByIdKey(currentId);
    }

    /**
     * Ensures that a user is currently authenticated.
     *
     * @return the authenticated user
     * @throws IllegalStateException if no user is logged in
     */
    public User requireUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("Please log in first"));
    }

    /**
     * Ensures that the current user has administrator privileges.
     *
     * @return the authenticated administrator
     * @throws IllegalStateException if no user is logged in or the current
     *         user does not have administrator privileges
     *
     * <p>【Sprint 2 · US12 #13】Henry 在 US03/US16 的 PR 中把它改为 public（只改可见性，逻辑不变）。
     * 审核人：Yuhan。对接：Stella 的 StatisticsService 调用它检查管理员权限。
     */
    public User requireAdmin() {
        User user = requireUser();

        if (user.getRole() != UserRole.ADMIN) {
            throw new IllegalStateException("Administrator access required");
        }

        return user;
    }

    /**
     * Retrieves all registered user accounts.
     *
     * @return a list of all registered users
     * @throws IllegalStateException if the current user is not an administrator
     */
    public List<User> listUsers() {
        requireAdmin();
        return users.findAll();
    }

    /**
     * Creates a new user account with the specified role.
     *
     * <p>Only administrators can create accounts through this method.
     * Input validation and password hashing are handled by the shared
     * account creation logic.
     *
     * @param id the unique user ID
     * @param username the username
     * @param password the plaintext password
     * @param name the user's full name
     * @param email the user's email address
     * @param phone the user's phone number
     * @param role the role assigned to the new user
     * @return the newly created user account
     * @throws IllegalStateException if the current user is not an administrator
     */
    public User addUser(String id, String username, String password,
                        String name, String email, String phone, UserRole role) {
        requireAdmin();
        return create(id, username, password, name, email, phone, role);
    }

    /**
     * Deletes a user account by its unique ID.
     *
     * <p>Administrators cannot delete their own accounts.
     *
     * @param id the unique ID of the account to delete
     * @throws IllegalStateException if the current user is not an administrator
     * @throws IllegalArgumentException if the account does not exist or
     *         the administrator attempts to delete their own account
     */
    public void deleteUser(String id) {
        User actor = requireAdmin();

        if (actor.getIdKey().equals(id)) {
            throw new IllegalArgumentException("Cannot delete your own administrator account");
        }

        if (!users.existsByIdKey(id)) {
            throw new IllegalArgumentException("User not found");
        }

        users.deleteByIdKey(id);
    }
}

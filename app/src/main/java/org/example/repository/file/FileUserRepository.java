package org.example.repository.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.UserRepository;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella（头像的 Base64 持久化与向后兼容）
 *   - 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
 *   - Sprint 1 原作者：Henry（未标 PR 的方法本轮不改）
 *
 * Persists user profiles and supplied password hashes in a properties snapshot.
 * Updates memory only after saving succeeds. Hash generation and authorization
 * belong to the service layer. Intended for a single process without concurrent writers.
 */
public final class FileUserRepository implements UserRepository {
    private final AtomicPropertiesStore store;
    private List<User> users = new ArrayList<>();

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Loads users from a snapshot; an absent file starts an empty repository.
     * @param file user snapshot path
     * @throws IllegalStateException if reading fails or user records are invalid
     */
    public FileUserRepository(Path file) {
        store = new AtomicPropertiesStore(file);
        Properties data = store.read();
        try {
            int count = Integer.parseInt(data.getProperty("count", data.isEmpty() ? "0" : "-1"));
            if (count < 0) {
                throw new IllegalArgumentException("Invalid user count");
            }
            for (int i = 0; i < count; i++) {
                String p = i + ".";
                // 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
                // 【Sprint 3 · US19 #67】旧快照没有 picture/picformat 字段，缺省为“没有头像”，因此老账户照常读取。
                byte[] picture = decodePicture(data.getProperty(p + "picture"));
                String picFormat = data.getProperty(p + "picformat", User.NO_PICTURE);
                User user = new User(data.getProperty(p + "id"), data.getProperty(p + "username"),
                        data.getProperty(p + "hash"), data.getProperty(p + "name"),
                        data.getProperty(p + "email"), data.getProperty(p + "phone"),
                        UserRole.valueOf(data.getProperty(p + "role")),
                        picture, picFormat);
                if (existsByIdKey(user.getIdKey()) || existsByUsername(user.getUsername())) {
                    throw new IllegalArgumentException("Duplicate user in snapshot");
                }
                users.add(user);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid user snapshot: " + file, e);
        }
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Inserts a user or replaces the user with the same ID key, saving before updating memory.
     * @param user profile containing an already generated password hash
     * @throws IllegalArgumentException if another user has the same username
     * @throws IllegalStateException if saving fails
     */
    @Override
    public void save(User user) {
        if (users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername())
                && !u.getIdKey().equals(user.getIdKey()))) {
            throw new IllegalArgumentException("Username already exists");
        }
        List<User> next = new ArrayList<>(users);
        next.removeIf(u -> u.getIdKey().equals(user.getIdKey()));
        next.add(user);
        persist(next);
        users = next;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Serializes every profile field and supplied password hash into a complete snapshot.
     * The caller updates memory only after this method succeeds.
     * @param snapshot users to persist
     * @throws IllegalStateException if saving fails
     */
    private void persist(List<User> snapshot) {
        Properties data = new Properties();
        data.setProperty("count", Integer.toString(snapshot.size()));
        for (int i = 0; i < snapshot.size(); i++) {
            User user = snapshot.get(i);
            String p = i + ".";
            data.setProperty(p + "id", user.getIdKey());
            data.setProperty(p + "username", user.getUsername());
            data.setProperty(p + "hash", user.getPasswordHash());
            data.setProperty(p + "name", user.getFullName());
            data.setProperty(p + "email", user.getEmail());
            data.setProperty(p + "phone", user.getPhone());
            data.setProperty(p + "role", user.getRole().name());
            // 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
            // 【Sprint 3 · US19 #67】头像是二进制，用 Base64 存进 properties；没有头像就不写这两项，保持快照干净。
            if (user.hasProfilePicture()) {
                data.setProperty(p + "picture", Base64.getEncoder().encodeToString(user.getProfilePicture()));
                data.setProperty(p + "picformat", user.getProfilePictureFormat());
            }
        }
        store.write(data);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US19 #67】主负责人：Henry。
     * 做什么：把快照里的 Base64 头像解码成字节数组；缺失时返回空数组（没有头像），损坏时抛出异常由上层包装成
     * “Invalid user snapshot”，与其他损坏字段的处理方式一致。
     * 对接／验证：只在本类构造方法中使用；FileUserRepositoryTest 覆盖旧快照、正常头像和损坏 Base64 三种情况。
     *
     * @param encoded Base64 text, or {@code null}
     * @return decoded picture bytes, or an empty array
     */
    private static byte[] decodePicture(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(encoded.strip());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Finds a user by exact, case-sensitive username.
     * @param username username to look up
     * @return matching user, or an empty optional
     */
    @Override
    public Optional<User> findByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Finds a user by exact, case-sensitive custom ID key.
     * @param idKey ID key to look up
     * @return matching user, or an empty optional
     */
    @Override
    public Optional<User> findByIdKey(String idKey) {
        return users.stream().filter(u -> u.getIdKey().equals(idKey)).findFirst();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Returns an unmodifiable copy of the user list without checking administrator privileges.
     * @return snapshot of all users
     */
    @Override
    public List<User> findAll() {
        return List.copyOf(users);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Checks whether an exact username exists.
     * @param username username to check
     * @return true if present
     */
    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Checks whether an exact ID key exists.
     * @param idKey ID key to check
     * @return true if present
     */
    @Override
    public boolean existsByIdKey(String idKey) {
        return findByIdKey(idKey).isPresent();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Henry（需要修改时先联系原作者）
     * Removes the specified user and saves before updating memory.
     * An absent ID causes the unchanged list to be saved.
     * @param idKey ID key to remove
     * @throws IllegalStateException if saving fails
     */
    @Override
    public void deleteByIdKey(String idKey) {
        List<User> next = new ArrayList<>(users);
        next.removeIf(u -> u.getIdKey().equals(idKey));
        persist(next);
        users = next;
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按邮箱查找，忽略大小写和首尾空格。
     * 对接／验证：旧数据里大小写不同的邮箱也能被识别为重复。
     *
     * @param email email address to look up
     * @return matching user, or an empty optional
     */
    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String key = email.strip().toLowerCase(Locale.ROOT);
        return users.stream()
                .filter(u -> u.getEmail() != null && u.getEmail().strip().toLowerCase(Locale.ROOT).equals(key))
                .findFirst();
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按电话查找，比较前去掉空格、横线、括号和点。
     * 对接／验证：保存的电话仍保留用户输入的格式，只有比较时使用规范化结果。
     *
     * @param phone phone number to look up
     * @return matching user, or an empty optional
     */
    @Override
    public Optional<User> findByPhone(String phone) {
        if (phone == null) {
            return Optional.empty();
        }
        String key = phoneKey(phone);
        return users.stream()
                .filter(u -> u.getPhone() != null && phoneKey(u.getPhone()).equals(key))
                .findFirst();
    }

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：生成电话比较键。
     * 对接／验证：只在本类使用。
     */
    private static String phoneKey(String phone) {
        return phone.replaceAll("[\\s().-]", "");
    }
}

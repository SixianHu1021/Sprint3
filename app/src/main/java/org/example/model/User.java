package org.example.model;

import java.util.Arrays;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
 *   - 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（Sprint 1 的字段和 getter 本轮不改）
 *
 * 【Sprint 3 · US19 #67】主负责人：Henry（账户模型与存储负责人）。
 * 做什么：在 Sprint 1 的不可变 User 上，新增两个“可选”的头像字段：头像的二进制内容和它的格式（jpg/png）。
 * 注册和首次管理员创建仍然不收集头像，所以新账户一开始没有头像（content 为空数组、format 为 NO_PICTURE）。
 * 对接／验证：Stella 的 ProfilePictureService 调用 withProfilePicture 设置头像；Henry 的 FileUserRepository
 * 以 Base64 持久化头像并能读取没有头像的旧快照；Sixian 的 ProfilePictureRenderer / ProfilePictureViewer 读取头像预览。
 */
public class User {

    /** 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 没有头像时记录的格式占位符（与 DigitalScroll.UNKNOWN_FORMAT 区分：这里表示“还没上传头像”）。 */
    public static final String NO_PICTURE = "none";

    private final String idKey;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String email;
    private final String phone;
    private final UserRole role;
    private final byte[] profilePicture;
    private final String profilePictureFormat;

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     * Sprint 1 的 7 参数构造方法：创建一个没有头像的账户。
     * 【Sprint 3 · US19 #67】委托给 9 参数构造方法，头像内容为空、格式为 NO_PICTURE；
     * 这样注册、登录、管理员添加用户等 Sprint 1/2 代码完全不用改。
     */
    public User(
            String idKey,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UserRole role) {
        this(idKey, username, passwordHash, fullName, email, phone, role, new byte[0], NO_PICTURE);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US19 #67】主负责人：Henry。
     * 做什么：完整构造方法，额外保存头像内容（防御性拷贝）和头像格式。
     * 内容为空或格式为空时，统一规整为“没有头像”（空数组 + NO_PICTURE），保证状态一致。
     * 对接／验证：FileUserRepository 读取旧快照（没有 picture 字段）时用这个构造方法补默认值。
     *
     * @param idKey unique custom ID key
     * @param username unique username
     * @param passwordHash stored password hash
     * @param fullName full name
     * @param email email address
     * @param phone phone number
     * @param role account role
     * @param profilePicture profile picture bytes, copied defensively; empty means no picture
     * @param profilePictureFormat lowercase picture format such as {@code png} or {@code jpg};
     *         blank or {@code none} means no picture
     */
    public User(
            String idKey,
            String username,
            String passwordHash,
            String fullName,
            String email,
            String phone,
            UserRole role,
            byte[] profilePicture,
            String profilePictureFormat) {

        this.idKey = idKey;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;

        byte[] picture = profilePicture == null ? new byte[0] : profilePicture;
        String format = profilePictureFormat == null ? "" : profilePictureFormat.strip().toLowerCase(java.util.Locale.ROOT);
        boolean hasPicture = picture.length > 0 && !format.isEmpty() && !format.equals(NO_PICTURE);
        this.profilePicture = hasPicture ? Arrays.copyOf(picture, picture.length) : new byte[0];
        this.profilePictureFormat = hasPicture ? format : NO_PICTURE;
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

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US19 #67 / US20 #68】主负责人：Henry。
     * 做什么：是否已经上传过头像。
     * 对接／验证：AccountScreens 的“我的资料”显示 added / not added；ProfilePictureViewer 预览前先检查。
     *
     * @return true if this account has a profile picture
     */
    public boolean hasProfilePicture() {
        return profilePicture.length > 0 && !profilePictureFormat.equals(NO_PICTURE);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US20 #68】主负责人：Henry。
     * 做什么：返回头像内容的拷贝；没有头像时返回空数组。
     * 对接／验证：Sixian 的 ProfilePictureRenderer / ProfilePictureViewer 读取它；修改拷贝不会影响本对象。
     *
     * @return a copy of the profile picture bytes, or an empty array when there is none
     */
    public byte[] getProfilePicture() {
        return Arrays.copyOf(profilePicture, profilePicture.length);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US19 #67 / US20 #68】主负责人：Henry。
     * 做什么：返回头像格式（png / jpg），没有头像时返回 NO_PICTURE。
     * 对接／验证：预览时用来描述格式并选择打开时的扩展名。
     *
     * @return lowercase picture format, or {@code none}
     */
    public String getProfilePictureFormat() {
        return profilePictureFormat;
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 【Sprint 3 · US19 #67】主负责人：Henry。
     * 做什么：返回一个替换了头像的新 User（其余字段不变）。上传新头像会覆盖旧头像。
     * 对接／验证：Stella 的 ProfilePictureService.setProfilePicture 调用它，再交给 FileUserRepository.save 保存。
     *
     * @param newPicture replacement picture bytes
     * @param newFormat replacement picture format (png / jpg)
     * @return a copy of this user carrying the new profile picture
     */
    public User withProfilePicture(byte[] newPicture, String newFormat) {
        return new User(idKey, username, passwordHash, fullName, email, phone, role, newPicture, newFormat);
    }
}

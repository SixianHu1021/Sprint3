package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.example.model.User;
import org.example.repository.UserRepository;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
 *
 * 【Sprint 3 · US19 #67】主负责人：Stella（Product Owner，本轮的业务校验）。
 * 做什么：已登录用户给自己的账户上传头像。只接受 JPG/JPEG 和 PNG，而且要求扩展名和真实内容（魔数）都对，
 * 所以把一个 .txt 改名成 .png 会被拒绝。文件不存在、不是普通文件或超过大小上限会立即报错，用户可以重试或 cancel。
 * 上传新头像会覆盖旧头像。访客不能上传（requireUser 先检查登录）。
 * 对接/验证：登录检查调用 Yuhan 的 AccountService.requireUser；格式判断调用 Sixian 的 FileFormat.detect；
 * 保存调用 Henry 的 UserRepository.save（User.withProfilePicture）。界面在 Yuhan 的 ProfilePictureScreen。
 */
public final class ProfilePictureService {

    /** 头像大小上限（5 MiB）。头像只需要是一张普通图片，所以比卷轴的 10 MiB 更小。 */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    /** 允许的头像扩展名（US19：只接受 JPG/JPEG 和 PNG）。 */
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png");

    private final UserRepository users;
    private final AccountService accounts;

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 主负责人：Stella。
     * 做什么：保存用户仓库和账户服务引用。
     * 对接/验证：App 传入与其他服务相同的 FileUserRepository 和 AccountService，修改后我的资料立即反映。
     *
     * @param users user repository used to persist the picture
     * @param accounts account service identifying the logged-in user
     */
    public ProfilePictureService(UserRepository users, AccountService accounts) {
        this.users = Objects.requireNonNull(users, "users");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 【Sprint 3 · US19 #67 / US14 #49 逐项检查】主负责人：Stella。
     * 做什么：输入文件路径后立即检查：路径非空、文件存在且是普通文件、在大小上限内、扩展名是 jpg/jpeg/png。
     * 对接/验证：ProfilePictureScreen 用 ConsoleIO.askValid 调用；真正的内容校验（魔数）在读取文件时由 setProfilePicture 完成，
     * 因为这里只拿到路径还没读内容。返回规范化后的 Path 供随后上传。
     *
     * @param rawPath raw file path typed by the user
     * @return the validated path
     * @throws IllegalArgumentException if the path is blank, missing, too large or not a jpg/jpeg/png
     */
    public static Path checkSource(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        Path source = Path.of(rawPath.strip());
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("No readable file was found at that path");
        }
        String extension = extensionOf(source);
        if (!ALLOWED.contains(extension)) {
            throw new IllegalArgumentException("Profile picture must be a JPG, JPEG or PNG file");
        }
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new IllegalArgumentException("Profile picture exceeds the 5 MiB limit");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("That file could not be read; try another one");
        }
        return source;
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 【Sprint 3 · US19 #67】主负责人：Stella。
     * 做什么：读取文件、再次确认大小、按内容（魔数）确认它真的是 JPG 或 PNG，然后保存为当前用户的头像（覆盖旧的）。
     * 即使扩展名是 .png，只要内容不是真正的 PNG/JPG 也会被拒绝。
     * 对接/验证：读取规则与卷轴上传一致（多读 1 字节判断超限，不把超大文件读入内存）；
     * 内容判断调用 Sixian 的 FileFormat.detect（忽略文件名，只看字节），结果必须是 png/jpg/jpeg。
     *
     * @param source local picture file
     * @return the saved user carrying the new picture
     * @throws IllegalStateException if nobody is logged in or saving fails
     * @throws IllegalArgumentException if the file is missing, too large or not a real JPG/PNG image
     * @throws IOException if the file cannot be read
     */
    public User setProfilePicture(Path source) throws IOException {
        User current = accounts.requireUser();
        byte[] content = readSource(source);
        // 只看内容（传 null 文件名），把改名伪装的假图片挡在外面。
        String detected = FileFormat.detect(null, content);
        if (!ALLOWED.contains(detected)) {
            throw new IllegalArgumentException(
                    "That file is not a real JPG or PNG image, even if its name ends in .jpg or .png");
        }
        String format = detected.equals("jpeg") ? "jpg" : detected;
        User updated = current.withProfilePicture(content, format);
        users.save(updated);
        return updated;
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 主负责人：Stella。
     * 做什么：读取头像文件，规则与卷轴上传相同：必须是普通文件，最多 5 MiB。
     * 对接/验证：多读 1 字节判断超限，避免把超大文件完整读入内存。
     */
    private static byte[] readSource(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("No readable file was found at that path");
        }
        byte[] content;
        try (InputStream input = Files.newInputStream(source)) {
            content = input.readNBytes(MAX_BYTES + 1);
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("Profile picture exceeds the 5 MiB limit");
        }
        return content;
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 主负责人：Stella。
     * 做什么：取出路径的小写扩展名，没有扩展名时返回空字符串。
     * 对接/验证：仅用于 checkSource 的扩展名白名单检查。
     */
    private static String extensionOf(Path source) {
        Path fileName = source.getFileName();
        if (fileName == null) {
            return "";
        }
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

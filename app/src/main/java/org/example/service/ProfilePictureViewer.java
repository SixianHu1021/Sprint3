package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.example.model.User;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 3 · US20 #68】主负责人：Sixian（打开预览负责人）。
 * 做什么：像用默认应用打开图片卷轴一样，把当前用户的头像写到临时文件（带正确扩展名），再用电脑默认应用打开。
 * 规则与 Sprint 2 的 ScrollViewer 一致：
 * <ul>
 *   <li>访客不能使用（requireUser 先检查登录）。</li>
 *   <li>没有头像时抛出清楚的说明，而不是崩溃。</li>
 *   <li>打开不算下载也不算上传：从不保存任何卷轴或账户，因此不改变任何计数。</li>
 *   <li>这台机器没有桌面或没有对应应用时，由 FileOpener 给出原因，界面显示后程序继续。</li>
 * </ul>
 * 对接/验证：复用 Sixian 在 Sprint 2 写的 FileOpener（真实 desktop / 测试用假实现），测试不会真的弹窗；
 * 登录检查调用 Yuhan 的 AccountService.requireUser；界面由 Yuhan 的 ProfilePictureScreen 调用。
 */
public final class ProfilePictureViewer {

    private final AccountService accounts;
    private final FileOpener opener;
    private final Path tempRoot;

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：使用系统临时目录的构造方法（真实程序）。
     *
     * @param accounts account service enforcing login
     * @param opener how files are opened on this machine
     */
    public ProfilePictureViewer(AccountService accounts, FileOpener opener) {
        this(accounts, opener, null);
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：可指定临时目录的构造方法；测试传入 @TempDir，避免在系统临时目录留下文件。
     *
     * @param accounts account service enforcing login
     * @param opener how files are opened on this machine
     * @param tempRoot parent folder for viewing copies, or {@code null} for the system temp folder
     */
    public ProfilePictureViewer(AccountService accounts, FileOpener opener, Path tempRoot) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.opener = Objects.requireNonNull(opener, "opener");
        this.tempRoot = tempRoot;
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 【Sprint 3 · US20 #68】主负责人：Sixian。
     * 做什么：检查登录 -> 确认有头像 -> 写临时副本（带 .png/.jpg 扩展名）-> 交给 FileOpener 打开。
     * 不保存任何对象，所以不改变下载/上传计数。
     * 对接/验证：没有头像抛出 IllegalStateException，界面显示说明并停留；无法打开时 FileOpener 抛 IOException。
     *
     * @return path of the temporary viewing copy
     * @throws IllegalStateException if nobody is logged in or the user has no profile picture
     * @throws IOException if the copy cannot be written or this machine cannot open it
     */
    public Path open() throws IOException {
        User user = accounts.requireUser();
        if (!user.hasProfilePicture()) {
            throw new IllegalStateException("You have not added a profile picture yet.");
        }
        Path folder = tempRoot == null
                ? Files.createTempDirectory("vsas-avatar-")
                : Files.createTempDirectory(tempRoot, "vsas-avatar-");
        folder.toFile().deleteOnExit();
        Path copy = folder.resolve(ScrollViewer.safeName(user.getIdKey()) + "." + user.getProfilePictureFormat());
        Files.write(copy, user.getProfilePicture());
        copy.toFile().deleteOnExit();
        opener.open(copy);
        return copy;
    }
}

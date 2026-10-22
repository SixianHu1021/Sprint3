package org.example.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.example.model.User;
import org.example.service.AccountService;
import org.example.service.ProfilePictureRenderer;
import org.example.service.ProfilePictureService;
import org.example.service.ProfilePictureViewer;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
 *
 * 【Sprint 3 · US19 #67 / US20 #68】主负责人：Yuhan（界面集成负责人）。
 * 做什么：我的资料里关于头像的控制台页面：显示头像状态（added / not added）、上传头像（US19）、
 * 预览头像信息、用默认应用打开头像（US20）。所有业务规则都在 service 层，本类只收集输入、显示结果。
 * 对接/验证：输入通过 Yuhan 的 ConsoleIO（必填立即重问、cancel 放弃）；
 * 上传调用 Stella 的 ProfilePictureService；预览调用 Sixian 的 ProfilePictureRenderer / ProfilePictureViewer；
 * 由 Henry 的 AccountScreens.profile() 菜单接入（新增一个 Profile picture 菜单项）。
 */
public final class ProfilePictureScreen {

    private final AccountService accounts;
    private final ProfilePictureService pictures;
    private final ProfilePictureViewer viewer;
    private final ConsoleIO io;

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 主负责人：Yuhan。
     * 做什么：保存账户服务、头像服务、头像查看器和共用输入输出。
     * 对接/验证：ConsoleUi 构造时创建一次，和其他页面共用同一个 ConsoleIO。
     *
     * @param accounts account service identifying the logged-in user
     * @param pictures profile picture service (upload / validation)
     * @param viewer profile picture viewer (open in default app)
     * @param io shared console helper
     */
    public ProfilePictureScreen(AccountService accounts, ProfilePictureService pictures,
                                ProfilePictureViewer viewer, ConsoleIO io) {
        this.accounts = accounts;
        this.pictures = pictures;
        this.viewer = viewer;
        this.io = io;
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 【Sprint 3 · US19 #67 / US20 #68】主负责人：Yuhan。
     * 做什么：头像页面：每轮先显示状态，再显示编号菜单（上传头像 / 预览头像 / 返回）。
     * 对接/验证：访客无法进入（requireUser）；预览在没有头像时显示清楚说明而不是错误。
     */
    public void show() {
        accounts.requireUser();
        Menu menu = new Menu("Profile picture menu:", "Back", "back")
                .option("Upload a new profile picture", "upload", a -> upload())
                .option("Preview profile picture", "preview", a -> preview());
        menu.loop(io, "Picture> ", this::showStatus);
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 【Sprint 3 · US20 #68】主负责人：Yuhan。
     * 做什么：显示当前账户是否已经上传头像（added / not added）及其格式。
     * 对接/验证：数据每次从 AccountService.currentUser 重新读取，上传后立即看到状态变化。
     */
    private void showStatus() {
        User user = accounts.requireUser();
        if (user.hasProfilePicture()) {
            io.info("Profile picture: added (" + user.getProfilePictureFormat() + ")");
        } else {
            io.info("Profile picture: not added");
        }
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 【Sprint 3 · US19 #67 / US14 #49 逐项检查】主负责人：Yuhan。
     * 做什么：输入本地图片路径并立即检查（Stella 的 checkSource），再读取并按内容确认是真图片后保存；成功后显示新格式。
     * 对接/验证：扩展名非 jpg/jpeg/png、文件不存在、过大、假图片都在当时提示并重问；输入 cancel 放弃。
     *
     * @throws IOException if the picture file cannot be read
     */
    private void upload() throws IOException {
        accounts.requireUser();
        io.info("Only JPG, JPEG and PNG images up to 5 MiB are accepted. " + ConsoleIO.CANCEL_HINT);
        Path source = io.askValid("Picture file path: ", ProfilePictureService::checkSource);
        User updated = pictures.setProfilePicture(source);
        io.info("Profile picture uploaded (" + updated.getProfilePictureFormat() + ").");
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 【Sprint 3 · US20 #68】主负责人：Yuhan。
     * 做什么：预览头像信息（格式、大小、像素尺寸），再提供用默认应用打开的子菜单。
     * 对接/验证：没有头像时只显示说明，不进入打开流程；打开失败时显示原因并停留。
     */
    private void preview() {
        User user = accounts.requireUser();
        List<String> lines = ProfilePictureRenderer.render(user);
        lines.forEach(io::info);
        if (!user.hasProfilePicture()) {
            return;
        }
        Menu menu = new Menu("Profile picture preview:", "Back", "back")
                .option("Open in default app", "open", a -> openInApp());
        menu.runOnce(io, "Preview> ");
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 【Sprint 3 · US20 #68】主负责人：Yuhan。
     * 做什么：用电脑默认应用打开头像（和打开图片卷轴一样）；打开不改变任何计数。
     * 对接/验证：无桌面或无应用时 FileOpener 抛 IOException，这里转成界面错误并停留在预览页。
     */
    private void openInApp() {
        try {
            viewer.open();
            io.info("Opened your profile picture in the default application.");
        } catch (IOException e) {
            io.error(e.getMessage());
        }
    }
}

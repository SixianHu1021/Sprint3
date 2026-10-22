package org.example;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.FileOpener;
import org.example.service.ProfilePictureRenderer;
import org.example.service.ProfilePictureService;
import org.example.service.ProfilePictureViewer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 3 · US20 #68】主负责人：Sixian。
 * 测什么：有头像时预览显示格式、大小、像素尺寸（和图片卷轴一致）；没有头像时显示清楚说明；
 * 登录用户可用默认应用打开头像（副本带正确扩展名、内容一致）；没有头像时打开抛出说明；访客不能打开。
 * 全部使用假的 FileOpener，不会弹出窗口（Jenkins 没有屏幕也能运行）。
 */
class ProfilePicturePreviewTest {

    private static final PasswordHasher HASHER = new PasswordHasher() {
        @Override
        public String hash(String password) {
            return "test-only:" + password;
        }

        @Override
        public boolean verify(String password, String encoded) {
            return password != null && hash(password).equals(encoded);
        }
    };

    @TempDir
    Path directory;
    private AccountService accounts;
    private ProfilePictureService pictures;
    private final List<Path> opened = new ArrayList<>();
    private final FileOpener recorder = opened::add;

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     */
    @BeforeEach
    void setUp() {
        FileUserRepository users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        pictures = new ProfilePictureService(users, accounts);
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("m", "password123");
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * Returns a real PNG of the given pixel size.
     */
    private byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 有头像时预览显示格式、大小和像素尺寸（复用图片卷轴的渲染）。
     */
    @Test
    void previewShowsFormatSizeAndDimensions() throws IOException {
        byte[] bytes = png(5, 2);
        User user = new User("m", "member", "h", "Member", "m@example.com", "456", UserRole.MEMBER)
                .withProfilePicture(bytes, "png");
        List<String> lines = ProfilePictureRenderer.render(user);
        assertEquals("Format: Image (.png)", lines.get(0));
        assertTrue(lines.get(1).startsWith("Size: " + bytes.length + " bytes"));
        assertTrue(lines.contains("Image size: 5 x 2 pixels"));
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 没有头像时预览显示清楚说明，而不是错误。
     */
    @Test
    void previewWithoutPictureShowsClearMessage() {
        User user = new User("m", "member", "h", "Member", "m@example.com", "456", UserRole.MEMBER);
        assertEquals(List.of("No profile picture has been added yet."), ProfilePictureRenderer.render(user));
        assertEquals(List.of("No profile picture has been added yet."), ProfilePictureRenderer.render(null));
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 登录用户用默认应用打开头像：副本扩展名正确、内容一致；不改变任何计数（从不保存）。
     */
    @Test
    void memberOpensProfilePictureWithCorrectExtension() throws IOException {
        byte[] bytes = png(4, 4);
        pictures.setProfilePicture(write("avatar.png", bytes));
        Path copy = viewer(recorder).open();
        assertEquals(List.of(copy), opened);
        assertTrue(copy.getFileName().toString().endsWith(".png"));
        assertTrue(copy.startsWith(directory));
        assertArrayEquals(bytes, java.nio.file.Files.readAllBytes(copy));
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 没有头像时打开抛出清楚的说明，不弹窗。
     */
    @Test
    void openingWithoutPictureReportsReason() {
        assertEquals("You have not added a profile picture yet.",
                assertThrows(IllegalStateException.class, () -> viewer(recorder).open()).getMessage());
        assertTrue(opened.isEmpty());
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 没有桌面时打开显示原因，不崩溃。
     */
    @Test
    void machineWithoutDesktopReportsReason() throws IOException {
        pictures.setProfilePicture(write("avatar.png", png(2, 2)));
        assertEquals("Opening files is not available in this session",
                assertThrows(IOException.class, () -> viewer(FileOpener.unavailable()).open()).getMessage());
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 访客不能打开头像。
     */
    @Test
    void guestsCannotOpen() throws IOException {
        pictures.setProfilePicture(write("avatar.png", png(2, 2)));
        accounts.logout();
        assertEquals("Please log in first",
                assertThrows(IllegalStateException.class, () -> viewer(recorder).open()).getMessage());
        assertTrue(opened.isEmpty());
    }

    private ProfilePictureViewer viewer(FileOpener opener) {
        return new ProfilePictureViewer(accounts, opener, directory);
    }

    private Path write(String name, byte[] bytes) throws IOException {
        return java.nio.file.Files.write(directory.resolve(name), bytes);
    }
}

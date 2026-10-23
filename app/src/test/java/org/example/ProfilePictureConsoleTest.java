package org.example;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import org.example.model.User;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.FileOpener;
import org.example.service.ProfilePictureService;
import org.example.service.ProfilePictureViewer;
import org.example.ui.AccountScreens;
import org.example.ui.ConsoleIO;
import org.example.ui.ProfilePictureScreen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
 *
 * 【Sprint 3 · US19 #67 / US20 #68】主负责人：Yuhan。
 * 测什么：头像页面显示状态、编号菜单；上传合法图片并显示成功；非图片扩展名立即重问（US14）；
 * 预览显示尺寸并可选择打开；没有头像时预览显示说明不进入打开；资料页显示 Picture: 状态，菜单含 Profile picture 项。
 * 用假的 FileOpener，不弹窗。
 */
class ProfilePictureConsoleTest {

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
    private FileUserRepository users;
    private AccountService accounts;
    private ProfilePictureService pictures;
    private final List<Path> opened = new ArrayList<>();
    private final FileOpener recorder = opened::add;

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        pictures = new ProfilePictureService(users, accounts);
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("m", "password123");
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Writes a real image to the test folder.
     */
    private Path image(String name, String format) throws IOException {
        BufferedImage image = new BufferedImage(6, 4, BufferedImage.TYPE_INT_RGB);
        Path file = directory.resolve(name);
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Runs the picture screen with scripted input.
     */
    private String screen(String... lines) {
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
        ProfilePictureViewer viewer = new ProfilePictureViewer(accounts, recorder, directory);
        new ProfilePictureScreen(accounts, pictures, viewer, io).show();
        return out.toString();
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 头像页面显示状态和编号菜单，上传合法图片成功后状态变为 added。
     */
    @Test
    void uploadsThroughNumberedMenuAndShowsStatus() throws IOException {
        Path png = image("avatar.png", "png");
        String text = screen("1", png.toString(), "0");
        assertTrue(text.contains("Profile picture: not added"));
        assertTrue(text.contains("  1. Upload a new profile picture"));
        assertTrue(text.contains("  2. Preview profile picture"));
        assertTrue(text.contains("Profile picture uploaded (png)."));
        assertTrue(users.findByIdKey("m").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 上传非图片扩展名立即提示并重问，然后用合法图片成功（US14 逐项检查）。
     */
    @Test
    void invalidPictureIsRepromptedImmediately() throws IOException {
        Path text = Files.writeString(directory.resolve("notes.txt"), "hello");
        Path png = image("avatar.png", "png");
        String output = screen("1", text.toString(), png.toString(), "0");
        assertTrue(output.contains("Error: Profile picture must be a JPG, JPEG or PNG file"));
        assertTrue(output.contains("Profile picture uploaded (png)."));
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Content errors must retry the path without showing the menu in between. */
    @Test
    void invalidContentRetriesImmediatelyUntilAValidImageIsUploaded() throws IOException {
        Path fake = Files.writeString(directory.resolve("fake.png"), "not an image");
        Path broken = Files.write(directory.resolve("broken.jpg"),
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        Path valid = image("valid.png", "png");
        String output = screen("1", fake.toString(), broken.toString(), valid.toString(), "0");
        int error = output.indexOf("Error: That file is not a real JPG or PNG image");
        int success = output.indexOf("Profile picture uploaded (png).");
        assertTrue(error >= 0 && success > error, output);
        String retry = output.substring(error, success);
        assertTrue(retry.contains("Picture file path: "), retry);
        assertFalse(retry.contains("Profile picture menu:"), retry);
        assertArrayEquals(Files.readAllBytes(valid), users.findByIdKey("m").orElseThrow().getProfilePicture());
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Cancelling after a content error preserves the saved picture on disk. */
    @Test
    void cancellingDuringContentRetryPreservesExistingPicture() throws IOException {
        User original = pictures.setProfilePicture(image("original.jpg", "jpg"));
        Path fake = Files.writeString(directory.resolve("fake.png"), "not an image");
        String output = screen("1", fake.toString(), "cancel", "0");
        assertTrue(output.contains("Cancelled."), output);
        assertFalse(output.contains("Profile picture uploaded"), output);
        User restored = new FileUserRepository(directory.resolve("users.properties")).findByIdKey("m").orElseThrow();
        assertArrayEquals(original.getProfilePicture(), restored.getProfilePicture());
        assertEquals(original.getProfilePictureFormat(), restored.getProfilePictureFormat());
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * End of input during retry still reaches the application's normal EOF handler. */
    @Test
    void closedInputDuringContentRetryDoesNotLoop() throws IOException {
        Path fake = Files.writeString(directory.resolve("fake.png"), "not an image");
        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class,
                () -> screen("1", fake.toString()));
        assertFalse(users.findByIdKey("m").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 预览显示尺寸，并可选择用默认应用打开（打开被记录，不改变计数）。
     */
    @Test
    void previewShowsDimensionsAndOpens() throws IOException {
        pictures.setProfilePicture(image("avatar.png", "png"));
        String text = screen("2", "1", "0");
        assertTrue(text.contains("Image size: 6 x 4 pixels"));
        assertTrue(text.contains("  1. Open in default app"));
        assertTrue(text.contains("Opened your profile picture in the default application."));
        assertEquals(1, opened.size());
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 没有头像时预览显示说明，不进入打开流程。
     */
    @Test
    void previewWithoutPictureShowsMessageAndNoOpenOption() {
        String text = screen("2", "0");
        assertTrue(text.contains("No profile picture has been added yet."));
        assertFalse(text.contains("Open in default app"));
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * 资料页显示 Picture 状态，并在菜单里有 Profile picture 项（接入 Henry 的 AccountScreens）。
     */
    @Test
    void profileScreenShowsPictureStatusAndMenuOption() throws IOException {
        pictures.setProfilePicture(image("avatar.png", "png"));
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner("0\n"), new PrintWriter(out, true));
        ProfilePictureViewer viewer = new ProfilePictureViewer(accounts, recorder, directory);
        ProfilePictureScreen pictureScreen = new ProfilePictureScreen(accounts, pictures, viewer, io);
        new AccountScreens(accounts, io, pictureScreen).profile();
        String text = out.toString();
        assertTrue(text.contains("Picture:   added (png)"));
        assertTrue(text.contains("Profile picture (upload / preview)"));
    }
}

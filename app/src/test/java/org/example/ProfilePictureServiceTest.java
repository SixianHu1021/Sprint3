package org.example;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.example.model.User;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ProfilePictureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
 *
 * 【Sprint 3 · US19 #67】主负责人：Stella。
 * 测什么：合法 PNG/JPG 上传并持久化；替换旧头像；非图片类型（扩展名）被拒；改名伪装的假图片（内容）被拒；
 * 文件不存在、超过大小上限被拒；访客不能上传。真实图片用 ImageIO 生成。
 */
class ProfilePictureServiceTest {

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

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
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
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Writes a real image of the requested format to the test folder.
     */
    private Path image(String fileName, String format) throws IOException {
        BufferedImage image = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        Path file = directory.resolve(fileName);
        assertTrue(ImageIO.write(image, format, file.toFile()), "could not encode " + format);
        return file;
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 上传合法 PNG 保存为当前用户头像；重启后仍然存在。
     */
    @Test
    void uploadsValidPngAndPersists() throws IOException {
        User saved = pictures.setProfilePicture(image("avatar.png", "png"));
        assertEquals("png", saved.getProfilePictureFormat());
        assertTrue(saved.hasProfilePicture());
        FileUserRepository reopened = new FileUserRepository(directory.resolve("users.properties"));
        assertTrue(reopened.findByIdKey("m").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * JPEG 被接受，格式规整为 jpg。
     */
    @Test
    void uploadsValidJpgNormalisedToJpg() throws IOException {
        User saved = pictures.setProfilePicture(image("avatar.jpg", "jpg"));
        assertEquals("jpg", saved.getProfilePictureFormat());
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 上传新头像覆盖旧头像。
     */
    @Test
    void uploadingReplacesThePreviousPicture() throws IOException {
        pictures.setProfilePicture(image("first.png", "png"));
        User saved = pictures.setProfilePicture(image("second.jpg", "jpg"));
        assertEquals("jpg", saved.getProfilePictureFormat());
        assertEquals("jpg", users.findByIdKey("m").orElseThrow().getProfilePictureFormat());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression added after the initial Sprint 3 validation. */
    @Test
    void profileEditsPreservePictureAcrossRestart() throws IOException {
        User original = pictures.setProfilePicture(image("avatar.png", "png"));
        User updated = accounts.updateProfile("Renamed", "new@example.com", "789");
        assertEquals("Renamed", updated.getFullName());
        assertPictureEquals(original, updated);
        assertPictureEquals(original, reopenedUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression added after the initial Sprint 3 validation. */
    @Test
    void savingUnchangedProfilePreservesPictureAcrossRestart() throws IOException {
        User original = pictures.setProfilePicture(image("avatar.jpg", "jpg"));
        User updated = accounts.updateProfile(original.getFullName(), original.getEmail(), original.getPhone());
        assertPictureEquals(original, updated);
        assertPictureEquals(original, reopenedUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression added after the initial Sprint 3 validation. */
    @Test
    void passwordChangePreservesPictureAcrossRestartAndLogin() throws IOException {
        User original = pictures.setProfilePicture(image("avatar.png", "png"));
        accounts.changePassword("password123", "replacement456");
        assertPictureEquals(original, accounts.requireUser());
        AccountService reopened = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")), HASHER);
        assertThrows(IllegalArgumentException.class, () -> reopened.login("m", "password123"));
        assertPictureEquals(original, reopened.login("m", "replacement456"));
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression added after the initial Sprint 3 validation. */
    @Test
    void signatureOnlyImagesAreRejectedWithoutReplacingExistingPicture() throws IOException {
        User original = pictures.setProfilePicture(image("avatar.png", "png"));
        byte[][] signatures = {
            {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10},
            {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0}
        };
        String[] names = {"broken.png", "broken.jpg"};
        for (int i = 0; i < signatures.length; i++) {
            Path broken = Files.write(directory.resolve(names[i]), signatures[i]);
            assertEquals(broken, ProfilePictureService.checkSource(broken.toString()));
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> pictures.setProfilePicture(broken));
            assertTrue(error.getMessage().contains("not a real JPG or PNG image"));
            assertPictureEquals(original, accounts.requireUser());
            assertPictureEquals(original, reopenedUser());
        }
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression added after the initial Sprint 3 validation. */
    @Test
    void pngWithValidHeaderButMissingPixelDataIsRejected() throws IOException {
        User original = pictures.setProfilePicture(image("avatar.jpg", "jpg"));
        byte[] png = Files.readAllBytes(image("complete.png", "png"));
        // Keep the signature and complete IHDR chunk, but no image data.
        Path broken = Files.write(directory.resolve("truncated.png"), java.util.Arrays.copyOf(png, 33));
        assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(broken));
        assertPictureEquals(original, reopenedUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Corrective regression: direct callers must obey the extension whitelist. */
    @Test
    void directUploadsRejectUnsupportedExtensionsEvenWithValidImageBytes() throws IOException {
        User original = pictures.setProfilePicture(image("original.jpg", "jpg"));
        byte[] png = Files.readAllBytes(image("valid.png", "png"));
        for (String name : new String[]{"disguised.txt", "noextension", "disguised.gif"}) {
            Path invalid = Files.write(directory.resolve(name), png);
            assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(invalid));
            assertPictureEquals(original, reopenedUser());
        }
        assertEquals("jpg", pictures.setProfilePicture(image("valid.JPEG", "jpg")).getProfilePictureFormat());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Decoders may recover pixels from truncated files; uploads must reject them. */
    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg"})
    void missingPngAndJpegEndingsAreRejected(String format) throws IOException {
        User original = pictures.setProfilePicture(image("original.png", "png"));
        byte[] complete = Files.readAllBytes(image("complete." + format, format));
        int removed = format.equals("png") ? 12 : 2;
        Path truncated = Files.write(directory.resolve("missing-end." + format),
                java.util.Arrays.copyOf(complete, complete.length - removed));
        assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(truncated), format);
        assertPictureEquals(original, reopenedUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * PNG chunk checksums are part of file integrity, even when pixels decode. */
    @Test
    void pngWithCorruptChecksumIsRejected() throws IOException {
        User original = pictures.setProfilePicture(image("original.jpg", "jpg"));
        byte[] content = Files.readAllBytes(image("complete.png", "png"));
        content[29] ^= 1; // IHDR checksum, leaving all pixel data intact.
        Path corrupt = Files.write(directory.resolve("bad-crc.png"), content);
        assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(corrupt));
        assertPictureEquals(original, reopenedUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Valid signatures with damaged payloads must not replace a saved avatar. */
    @Test
    void damagedImagePayloadsAreRejected() throws IOException {
        User original = pictures.setProfilePicture(image("original.png", "png"));
        for (String format : new String[]{"png", "jpg"}) {
            byte[] content = Files.readAllBytes(image("complete." + format, format));
            byte[] damaged = java.util.Arrays.copyOf(content, content.length / 2);
            Path corrupt = Files.write(directory.resolve("bad-payload." + format), damaged);
            assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(corrupt), format);
            assertPictureEquals(original, reopenedUser());
        }
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * Consecutive account changes persist the avatar under the correct owner. */
    @Test
    void consecutiveAccountChangesPreservePictureContentAndOwnershipAfterRestart() throws IOException {
        User member = pictures.setProfilePicture(image("member.png", "png"));
        accounts.register("n", "other", "otherpass123", "Other", "other@example.com", "789");
        accounts.login("n", "otherpass123");
        User other = pictures.setProfilePicture(image("other.jpg", "jpg"));
        accounts.login("m", "password123");
        accounts.updateProfile("Renamed", "new@example.com", "123");
        accounts.changePassword("password123", "replacement456");
        accounts.updateProfile("Renamed Again", "new@example.com", "123");
        FileUserRepository restored = new FileUserRepository(directory.resolve("users.properties"));
        AccountService restarted = new AccountService(restored, HASHER);
        assertThrows(IllegalArgumentException.class, () -> restarted.login("m", "password123"));
        User loggedIn = restarted.login("m", "replacement456");
        assertEquals("m", loggedIn.getIdKey());
        assertEquals("Renamed Again", loggedIn.getFullName());
        assertPictureEquals(member, loggedIn);
        assertEquals("n", restarted.login("n", "otherpass123").getIdKey());
        assertPictureEquals(other, restarted.requireUser());
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry */
    private User reopenedUser() {
        return new FileUserRepository(directory.resolve("users.properties")).findByIdKey("m").orElseThrow();
    }

    /** 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry */
    private static void assertPictureEquals(User expected, User actual) {
        assertTrue(actual.hasProfilePicture());
        assertEquals(expected.getProfilePictureFormat(), actual.getProfilePictureFormat());
        assertArrayEquals(expected.getProfilePicture(), actual.getProfilePicture());
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 非 jpg/jpeg/png 的扩展名在 checkSource 阶段被拒绝。
     */
    @Test
    void nonImageExtensionsAreRejectedByCheckSource() throws IOException {
        Path text = Files.writeString(directory.resolve("notes.txt"), "hello");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProfilePictureService.checkSource(text.toString()));
        assertTrue(error.getMessage().contains("JPG, JPEG or PNG"));
        assertThrows(IllegalArgumentException.class, () -> ProfilePictureService.checkSource(""));
        assertThrows(IllegalArgumentException.class,
                () -> ProfilePictureService.checkSource(directory.resolve("missing.png").toString()));
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 内容不是真图片的 .png（把 txt 改名成 .png）被内容检查拒绝，账户头像不变。
     */
    @Test
    void fakeImageWithImageExtensionIsRejected() throws IOException {
        Path fake = Files.writeString(directory.resolve("fake.png"), "this is plain text, not a PNG");
        // checkSource 只看扩展名，会通过；setProfilePicture 看内容，拒绝。
        assertEquals(fake, ProfilePictureService.checkSource(fake.toString()));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> pictures.setProfilePicture(fake));
        assertTrue(error.getMessage().contains("not a real JPG or PNG image"));
        assertFalse(users.findByIdKey("m").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 超过大小上限的图片被拒绝。
     */
    @Test
    void oversizedPictureIsRejected() throws IOException {
        Path big = directory.resolve("big.png");
        byte[] bytes = new byte[ProfilePictureService.MAX_BYTES + 1];
        bytes[0] = (byte) 0x89;
        bytes[1] = 'P';
        bytes[2] = 'N';
        bytes[3] = 'G';
        Files.write(big, bytes);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProfilePictureService.checkSource(big.toString()));
        assertTrue(error.getMessage().contains("5 MiB"));
        assertThrows(IllegalArgumentException.class, () -> pictures.setProfilePicture(big));
        assertFalse(reopenedUser().hasProfilePicture());
    }

    /**
     * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
     * 访客不能上传头像。
     */
    @Test
    void guestsCannotUpload() throws IOException {
        Path png = image("guest.png", "png");
        accounts.logout();
        assertEquals("Please log in first",
                assertThrows(IllegalStateException.class, () -> pictures.setProfilePicture(png)).getMessage());
    }
}

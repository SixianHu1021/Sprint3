package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
 *
 * 【Sprint 3 · US19 #67】主负责人：Henry。
 * 测什么：User 的头像字段（默认没有头像、withProfilePicture 覆盖、防御性拷贝）；
 * FileUserRepository 以 Base64 持久化头像并能重启后读回；没有 picture 字段的旧快照读成“没有头像”；
 * 损坏的 Base64 头像被拒绝。不验证图片内容合法性（那是 ProfilePictureService 的职责）。
 */
class UserProfilePictureTest {

    @TempDir
    Path directory;
    private Path snapshot;
    private FileUserRepository users;

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     */
    @BeforeEach
    void setUp() {
        snapshot = directory.resolve("users.properties");
        users = new FileUserRepository(snapshot);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     */
    private User member() {
        return new User("m", "member", "fixture-hash", "Member", "member@example.com", "456", UserRole.MEMBER);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 新账户（Sprint 1 的 7 参数构造方法）一开始没有头像，格式为 none。
     */
    @Test
    void newAccountsStartWithoutAProfilePicture() {
        User user = member();
        assertFalse(user.hasProfilePicture());
        assertEquals(User.NO_PICTURE, user.getProfilePictureFormat());
        assertEquals(0, user.getProfilePicture().length);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * withProfilePicture 返回带头像的新对象，其他字段不变；再次设置会覆盖旧头像。
     */
    @Test
    void withProfilePictureReplacesPictureAndKeepsOtherFields() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        User withPicture = member().withProfilePicture(png, "png");
        assertTrue(withPicture.hasProfilePicture());
        assertEquals("png", withPicture.getProfilePictureFormat());
        assertArrayEquals(png, withPicture.getProfilePicture());
        assertEquals("member", withPicture.getUsername());
        assertEquals(UserRole.MEMBER, withPicture.getRole());
        User replaced = withPicture.withProfilePicture(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "jpg");
        assertEquals("jpg", replaced.getProfilePictureFormat());
        assertEquals(3, replaced.getProfilePicture().length);
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 返回的字节数组是拷贝，修改它不会影响 User 内部状态。
     */
    @Test
    void profilePictureBytesAreCopiedDefensively() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 9};
        User user = member().withProfilePicture(png, "png");
        user.getProfilePicture()[0] = 0;
        png[1] = 0;
        assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G', 9}, user.getProfilePicture());
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 头像以 Base64 保存，重启后内容和格式都保留；没有头像的账户重启后仍然没有头像。
     */
    @Test
    void profilePictureSurvivesRestart() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 'r', 'e', 's', 't', 'a', 'r', 't'};
        users.save(member().withProfilePicture(png, "png"));
        users.save(new User("n", "nopic", "h", "No Pic", "n@example.com", "789", UserRole.MEMBER));
        FileUserRepository reopened = new FileUserRepository(snapshot);
        User restored = reopened.findByIdKey("m").orElseThrow();
        assertTrue(restored.hasProfilePicture());
        assertEquals("png", restored.getProfilePictureFormat());
        assertArrayEquals(png, restored.getProfilePicture());
        assertFalse(reopened.findByIdKey("n").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 没有 picture/picformat 字段的旧快照（Sprint 1/2 写的）被读成“没有头像”，向后兼容。
     */
    @Test
    void oldSnapshotWithoutPictureLoadsAsNoPicture() throws Exception {
        Path legacy = directory.resolve("legacy.properties");
        Files.writeString(legacy, "count=1\n0.id=m\n0.username=member\n0.hash=h\n0.name=Member\n"
                + "0.email=member@example.com\n0.phone=456\n0.role=MEMBER\n");
        FileUserRepository legacyRepo = new FileUserRepository(legacy);
        assertFalse(legacyRepo.findByIdKey("m").orElseThrow().hasProfilePicture());
    }

    /**
     * 【PR s3-r1-profile-picture-model】实现：Henry ｜ 审核：Stella
     * 损坏的 Base64 头像在读取快照时被拒绝（与其他损坏字段处理一致）。
     */
    @Test
    void corruptedBase64PictureIsRejected() throws Exception {
        Path bad = directory.resolve("bad.properties");
        Files.writeString(bad, "count=1\n0.id=m\n0.username=member\n0.hash=h\n0.name=Member\n"
                + "0.email=member@example.com\n0.phone=456\n0.role=MEMBER\n0.picformat=png\n0.picture=not*base64!\n");
        assertThrows(IllegalStateException.class, () -> new FileUserRepository(bad));
    }
}

package org.example;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.file.FileUserRepository;
import org.example.security.Pbkdf2PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
 * Corrective integration regression: runs the real App in separate JVMs.
 * Assignment markers identify maintenance ownership, not completed peer review.
 */
class ProfilePictureLifecycleTest {
    @TempDir
    Path directory;

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Upload -> edit -> password change -> unchanged save -> process exit/restart.
     * A second account has a different picture, proving ownership isolation on disk.
     */
    @Test
    void consecutiveAccountChangesSurviveCompleteApplicationRestart() throws Exception {
        Path memberImage = directory.resolve("member.png");
        Path otherImage = directory.resolve("other.jpg");
        assertTrue(ImageIO.write(new BufferedImage(6, 4, BufferedImage.TYPE_INT_RGB), "png", memberImage.toFile()));
        assertTrue(ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "jpg", otherImage.toFile()));
        String first = runApp("first",
                "root", "admin", "adminpass123", "Administrator", "admin@example.com", "111",
                "register", "m", "member", "password123", "Member", "member@example.com", "456",
                "register", "n", "other", "otherpass123", "Other", "other@example.com", "789",
                "login", "n", "otherpass123", "profile", "picture", "upload", otherImage.toString(),
                "back", "back", "logout",
                "login", "m", "password123", "profile", "picture", "upload", memberImage.toString(), "back",
                "edit", "Renamed", "new@example.com", "123",
                "password", "password123", "replacement456", "replacement456",
                "edit", "", "", "", "back", "quit");
        assertFalse(first.contains("Error:"), first);
        assertFalse(first.contains("Input closed."), first);
        assertTrue(first.contains("Password changed."), first);
        assertTrue(first.contains("Profile updated."), first);
        assertTrue(first.contains("Profile picture uploaded (png)."), first);
        assertTrue(first.contains("Profile picture uploaded (jpg)."), first);
        assertStoredPictures(memberImage, otherImage);

        String restarted = runApp("restarted",
                "login", "m", "password123", // Old password must fail in the fresh JVM.
                "login", "m", "replacement456", "profile", "picture", "preview", "back", "back", "back",
                "logout", "login", "n", "otherpass123", "profile", "picture", "preview", "back", "back", "back",
                "quit");
        assertTrue(restarted.contains("Error: Invalid ID key or password"), restarted);
        assertEquals(1, restarted.split("Error:", -1).length - 1, restarted);
        assertFalse(restarted.contains("Input closed."), restarted);
        assertFalse(restarted.contains("First run:"), restarted);
        assertTrue(restarted.contains("Image size: 6 x 4 pixels"), restarted);
        assertTrue(restarted.contains("Image size: 3 x 2 pixels"), restarted);
        assertStoredPictures(memberImage, otherImage);
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * A pre-Sprint-3 snapshot can gain a picture and retain it through account changes. */
    @Test
    void legacySnapshotSupportsFullLifecycleWithoutMigration() throws Exception {
        String hash = new Pbkdf2PasswordHasher().hash("password123");
        Files.writeString(directory.resolve("users.properties"),
                "count=1\n0.id=m\n0.username=member\n0.hash=" + hash
                        + "\n0.name=Member\n0.email=member@example.com\n0.phone=456\n0.role=MEMBER\n");
        assertFalse(new FileUserRepository(directory.resolve("users.properties"))
                .findByIdKey("m").orElseThrow().hasProfilePicture());
        Path picture = directory.resolve("legacy.png");
        assertTrue(ImageIO.write(new BufferedImage(5, 3, BufferedImage.TYPE_INT_RGB), "png", picture.toFile()));
        String first = runApp("legacy-first", "login", "m", "password123", "profile", "picture", "upload",
                picture.toString(), "back", "edit", "Renamed", "", "",
                "password", "password123", "replacement456", "replacement456", "back", "quit");
        assertFalse(first.contains("Error:"), first);
        assertFalse(first.contains("Input closed."), first);
        String restarted = runApp("legacy-restarted", "login", "m", "replacement456", "profile", "picture",
                "preview", "back", "back", "back", "quit");
        assertFalse(restarted.contains("Error:"), restarted);
        assertFalse(restarted.contains("Input closed."), restarted);
        assertFalse(restarted.contains("First run:"), restarted);
        assertTrue(restarted.contains("Image size: 5 x 3 pixels"), restarted);
        User restored = new FileUserRepository(directory.resolve("users.properties")).findByIdKey("m").orElseThrow();
        assertEquals("Renamed", restored.getFullName());
        assertEquals("member@example.com", restored.getEmail());
        assertEquals("456", restored.getPhone());
        assertEquals("png", restored.getProfilePictureFormat());
        assertArrayEquals(Files.readAllBytes(picture), restored.getProfilePicture());
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * Verify bytes, metadata, identity, unrelated fields and credentials from disk. */
    private void assertStoredPictures(Path memberImage, Path otherImage) throws Exception {
        FileUserRepository restored = new FileUserRepository(directory.resolve("users.properties"));
        User member = restored.findByIdKey("m").orElseThrow();
        User other = restored.findByIdKey("n").orElseThrow();
        assertEquals(3, restored.findAll().size());
        assertEquals("m", member.getIdKey());
        assertEquals("member", member.getUsername());
        assertEquals(UserRole.MEMBER, member.getRole());
        assertEquals("Renamed", member.getFullName());
        assertEquals("new@example.com", member.getEmail());
        assertEquals("123", member.getPhone());
        assertEquals("png", member.getProfilePictureFormat());
        assertArrayEquals(Files.readAllBytes(memberImage), member.getProfilePicture());
        assertEquals("other", other.getUsername());
        assertEquals("jpg", other.getProfilePictureFormat());
        assertArrayEquals(Files.readAllBytes(otherImage), other.getProfilePicture());
        assertFalse(restored.findByIdKey("root").orElseThrow().hasProfilePicture());
        Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();
        assertTrue(hasher.verify("replacement456", member.getPasswordHash()));
        assertFalse(hasher.verify("password123", member.getPasswordHash()));
        assertTrue(hasher.verify("otherpass123", other.getPasswordHash()));
    }

    /** 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Sixian
     * File redirection avoids pipe deadlocks and retains transcripts until test cleanup. */
    private String runApp(String name, String... lines) throws Exception {
        Path input = directory.resolve(name + ".input");
        Path output = directory.resolve(name + ".output");
        Files.writeString(input, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classes = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        Process process = new ProcessBuilder(javaExecutable, "-cp", classes, App.class.getName(), directory.toString())
                .redirectInput(input.toFile()).redirectOutput(output.toFile()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Application did not exit");
            String transcript = Files.readString(output, StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), transcript);
            assertTrue(transcript.contains("Goodbye."), transcript);
            return transcript;
        } finally {
            process.destroyForcibly();
        }
    }
}

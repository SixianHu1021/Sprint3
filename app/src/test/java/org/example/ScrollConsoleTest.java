
package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollService;
import org.example.ui.ConsoleUi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
 *   - 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry
 *   - Sprint 1 原作者：Sixian（未标 PR 的方法本轮不改）
 *
 * Tests scroll operations through the console interface.
 */
class ScrollConsoleTest {

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
    private FileScrollRepository repository;
    private ScrollService scrolls;

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @BeforeEach
    void setUp() {
        accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")),
                HASHER);

        repository = new FileScrollRepository(
                directory.resolve("scrolls.properties"));

        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());

        accounts.bootstrapAdmin(
                "a", "admin", "secret123",
                "Admin", "admin@example.com", "123");

        accounts.register(
                "m", "member", "password123",
                "Member", "member@example.com", "456");
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Runs the console using scripted input and captures its output.
     */
    private String cli(String... lines) {
        StringWriter result = new StringWriter();

        ConsoleUi ui = new ConsoleUi(
                accounts,
                scrolls,
                new Scanner(String.join("\n", lines) + "\n"),
                new PrintWriter(result, true));

        ui.run();
        return result.toString();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void memberCanUploadThroughConsole() throws IOException {
        byte[] content = {0, 1, -1, 127};
        Path source = Files.write(directory.resolve("source.bin"), content);

        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Java Notes", source.toString(),
                "quit");

        assertTrue(transcript.contains("Scroll uploaded successfully."));

        var stored = repository.findById("s1").orElseThrow();
        assertEquals("Java Notes", stored.getName());
        assertEquals("m", stored.getUploaderId());
        assertArrayEquals(content, stored.getContent());
        assertEquals(1, stored.getUploadCount());
        assertEquals(0, stored.getDownloadCount());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void administratorCanUploadThroughConsole() throws IOException {
        Path source = Files.write(
                directory.resolve("admin.bin"), new byte[]{10, 20});

        String transcript = cli(
                "login", "a", "secret123",
                "upload", "admin-scroll", "Admin Notes", source.toString(),
                "quit");

        assertTrue(transcript.contains("Scroll uploaded successfully."));
        assertEquals("a", repository.findById("admin-scroll")
                .orElseThrow().getUploaderId());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void guestCannotUploadThroughConsole() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1});

        String transcript = cli(
                "upload",
                "login", "m", "password123",
                "quit");

        assertTrue(transcript.contains("Please log in first"));
        assertFalse(transcript.contains("Scroll uploaded successfully."));
        assertTrue(repository.findAll().isEmpty());
        assertTrue(transcript.contains("Logged in."));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     */
    @Test
    void invalidUploadShowsErrorAndDoesNotSave() {
        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Missing File",
                directory.resolve("missing.bin").toString(),
                "quit");

        assertTrue(transcript.contains("Error:"));
        assertFalse(transcript.contains("Scroll uploaded successfully."));
        assertTrue(repository.findAll().isEmpty());
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【PR s2-r2-numbered-console】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void uploadAppearsOnlyInAuthenticatedMenus() {
        // Sprint 2 · US13（Yuhan）：上传在会员／管理员菜单都是第 2 项，访客菜单没有上传。
        String guest = cli("quit");
        assertTrue(guest.contains("  1. Browse scrolls"));
        assertFalse(guest.contains("Upload a scroll"));

        accounts.login("m", "password123");
        String member = cli("quit");
        assertTrue(member.contains("Member menu:"));
        assertTrue(member.contains("  2. Upload a scroll"));

        accounts.logout();
        accounts.login("a", "secret123");
        String admin = cli("quit");
        assertTrue(admin.contains("Admin menu:"));
        assertTrue(admin.contains("  2. Upload a scroll"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Guests can browse and preview a scroll without logging in.
     */
    @Test
    void guestCanBrowseAndPreview() throws IOException {
        byte[] content = {1, 2, 3};
        Path source = Files.write(directory.resolve("source.bin"), content);

        cli("login", "m", "password123",
                "upload", "s1", "Java Notes", source.toString(),
                "logout", "quit");

        String transcript = cli(
                "browse",
                "preview s1",
                "back",
                "back",
                "quit");

        assertTrue(transcript.contains("Available scrolls:"));
        assertTrue(transcript.contains("s1 | Java Notes | m |"));
        assertTrue(transcript.contains("Scroll preview:"));
        assertTrue(transcript.contains("ID: s1"));
        assertTrue(transcript.contains("Name: Java Notes"));
        assertTrue(transcript.contains("Uploader: m"));
        assertTrue(transcript.contains("Upload date:"));
        assertTrue(transcript.contains("Guest [GUEST]"));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Returning from preview reopens the browse list,
     * and returning from browse reopens the main menu.
     */
    @Test
    void previewBackReturnsToBrowseAndMainMenu() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1});

        cli("login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "logout", "quit");

        String transcript = cli(
                "browse",
                "preview s1",
                "back",
                "back",
                "quit");

        assertEquals(2, transcript.split("Available scrolls:", -1).length - 1);
        assertTrue(transcript.contains("Preview> "));
        assertTrue(transcript.contains("Browse> "));
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Invalid IDs produce an error without ending the browse session.
     */
    @Test
    void invalidPreviewIdDoesNotExitBrowse() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1});

        cli("login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "logout", "quit");

        String transcript = cli(
                "browse",
                "preview missing",
                "preview s1",
                "back",
                "back",
                "quit");

        assertTrue(transcript.contains("Error: Scroll not found: missing"));
        assertTrue(transcript.contains("ID: s1"));
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Previewing does not modify the persisted download counter.
     */
    @Test
    void previewDoesNotIncrementDownloadCount() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1, 2, 3});

        cli("login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "logout", "quit");

        cli("browse", "preview s1", "back", "back", "quit");

        assertEquals(0, repository.findById("s1")
                .orElseThrow().getDownloadCount());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * A logged-in member can download directly from the browse list.
     */
    @Test
    void memberCanDownloadFromBrowse() throws IOException {
        byte[] content = {0, 1, -1, 127};
        Path source = Files.write(directory.resolve("source.bin"), content);
        Path destination = directory.resolve("browse-download.bin");

        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "browse",
                "download s1", destination.toString(),
                "back",
                "quit");

        assertTrue(transcript.contains("Scroll downloaded successfully."));
        assertArrayEquals(content, Files.readAllBytes(destination));
        assertEquals(1, repository.findById("s1")
                .orElseThrow().getDownloadCount());
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * A logged-in member can download from the preview screen.
     */
    @Test
    void memberCanDownloadFromPreview() throws IOException {
        byte[] content = {10, 20, 30};
        Path source = Files.write(directory.resolve("source.bin"), content);
        Path destination = directory.resolve("preview-download.bin");

        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "browse",
                "preview s1",
                "download", destination.toString(),
                "back",
                "back",
                "quit");

        assertTrue(transcript.contains("Scroll preview:"));
        assertTrue(transcript.contains("Scroll downloaded successfully."));
        assertArrayEquals(content, Files.readAllBytes(destination));
        assertEquals(1, repository.findById("s1")
                .orElseThrow().getDownloadCount());
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Guests cannot download from browsing or preview.
     */
    @Test
    void guestCannotDownloadFromBrowseOrPreview() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1, 2, 3});

        cli("login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "logout", "quit");

        String transcript = cli(
                "browse",
                "download s1",
                "preview s1",
                "download",
                "back",
                "back",
                "quit");

        assertEquals(2,
                transcript.split("Please log in first", -1).length - 1);
        assertFalse(transcript.contains("Destination file path:"));
        assertFalse(transcript.contains("Scroll downloaded successfully."));
        assertEquals(0, repository.findById("s1")
                .orElseThrow().getDownloadCount());
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【PR s2-r4-integration】实现：Yuhan ｜ 审核：Henry（R4 起浏览页由 Sixian 的 BrowseScreen 提供，Sixian 确认）
     * A failed download does not display success or change counters.
     */
    @Test
    void failedDownloadDoesNotReportSuccess() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1, 2, 3});

        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "browse",
                "download s1", directory.toString(),
                "cancel",
                "back",
                "quit");

        // Sprint 2 · US14（逐项检查）：文件夹路径立即提示并重问；cancel 返回浏览页。
        assertTrue(transcript.contains("Error: Destination must be a file path, not a folder"));
        assertTrue(transcript.contains("Cancelled."));
        assertFalse(transcript.contains("Scroll downloaded successfully."));
        assertEquals(0, repository.findById("s1")
                .orElseThrow().getDownloadCount());
        assertTrue(transcript.contains("Goodbye."));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Users can continue browsing after a successful download.
     */
    @Test
    void downloadReturnsToBrowse() throws IOException {
        Path source = Files.write(
                directory.resolve("source.bin"), new byte[]{1, 2, 3});

        String transcript = cli(
                "login", "m", "password123",
                "upload", "s1", "Notes", source.toString(),
                "browse",
                "download s1", directory.resolve("download.bin").toString(),
                "preview s1",
                "back",
                "back",
                "quit");

        assertTrue(transcript.contains("Scroll downloaded successfully."));
        assertTrue(transcript.contains("Scroll preview:"));
        assertTrue(transcript.contains("Goodbye."));
        assertEquals(1, repository.findById("s1")
                .orElseThrow().getDownloadCount());
    }

}

package org.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.FileFormat;
import org.example.service.FileOpener;
import org.example.service.ScrollPreviewRenderer;
import org.example.service.ScrollQueryService;
import org.example.service.ScrollService;
import org.example.service.ScrollViewer;
import org.example.ui.BrowseScreen;
import org.example.ui.ConsoleIO;
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
 *   - 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
 *   - 【PR s2-r3-browse-screen】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US17 #52 打开预览】主负责人：Sixian。
 * 测什么：登录用户可用默认应用打开白名单类型（副本带正确扩展名、内容一致）；打开不增加下载次数；
 * 访客不能打开；不在白名单的类型（mp3、未知）被拒绝并说明；没有桌面时显示原因；
 * 预览页编号选项；未知扩展名的文本文件按内容显示前几行。
 * 全部使用假的 FileOpener，不会弹出窗口（Jenkins 没有屏幕也能运行）。
 */
class ScrollViewerTest {

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
    private final List<Path> opened = new ArrayList<>();
    private final FileOpener recorder = opened::add;

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @BeforeEach
    void setUp() throws IOException {
        accounts = new AccountService(new FileUserRepository(directory.resolve("users.properties")), HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("m", "password123");
        scrolls.upload("doc", "Report", Files.writeString(directory.resolve("report.pdf"), "%PDF-1.7 body"));
        scrolls.upload("song", "Song", Files.write(directory.resolve("song.mp3"), new byte[]{'I', 'D', '3', 0}));
        scrolls.upload("notes", "Notes", Files.writeString(directory.resolve("paper.aux"), "\\relax\n\\citation{x}\n"));
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * Creates a viewer that writes its copies inside the test folder.
     */
    private ScrollViewer viewer(FileOpener opener) {
        return new ScrollViewer(scrolls, accounts, opener, directory);
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void memberOpensWhitelistedTypeWithCorrectExtension() throws IOException {
        Path copy = viewer(recorder).open("doc");
        assertEquals(List.of(copy), opened);
        assertTrue(copy.getFileName().toString().endsWith(".pdf"));
        assertTrue(copy.startsWith(directory));
        assertArrayEquals("%PDF-1.7 body".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(copy));
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void openingDoesNotCountAsDownload() throws IOException {
        viewer(recorder).open("doc");
        viewer(recorder).open("doc");
        assertEquals(0, repository.findById("doc").orElseThrow().getDownloadCount());
        assertEquals(1, repository.findById("doc").orElseThrow().getUploadCount());
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void guestsCannotOpen() {
        accounts.logout();
        assertEquals("Please log in first",
                assertThrows(IllegalStateException.class, () -> viewer(recorder).open("doc")).getMessage());
        assertTrue(opened.isEmpty());
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void typesOutsideTheWhitelistAreNotOpened() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> viewer(recorder).open("song"));
        assertTrue(error.getMessage().startsWith("Files of type 'mp3' can't be opened here"));
        assertThrows(IllegalArgumentException.class, () -> viewer(recorder).open("notes"));
        assertThrows(IllegalArgumentException.class, () -> viewer(recorder).open("missing"));
        assertTrue(opened.isEmpty());
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void machineWithoutDesktopReportsReason() {
        assertEquals("Opening files is not available in this session",
                assertThrows(IOException.class, () -> viewer(FileOpener.unavailable()).open("doc")).getMessage());
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void whitelistIsDefaultDeny() {
        for (String format : new String[]{"pdf", "PNG", "jpg", "jpeg", "gif", "bmp", "txt", "md", "csv", "docx"}) {
            assertTrue(FileFormat.isOpenable(format), format);
        }
        for (String format : new String[]{"mp3", "mp4", "zip", "exe", "docm", "aux", "sh", "unknown", "xyz"}) {
            assertFalse(FileFormat.isOpenable(format), format);
        }
        assertFalse(FileFormat.isOpenable(null));
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void unknownTextExtensionsGetTextPreview() {
        List<String> aux = ScrollPreviewRenderer.render(repository.findById("notes").orElseThrow());
        assertEquals("Format: Binary file (.aux)", aux.get(0));
        assertTrue(aux.contains("  | \\relax"));
        List<String> mp3 = ScrollPreviewRenderer.render(repository.findById("song").orElseThrow());
        assertTrue(mp3.get(2).startsWith("First 4 bytes (hex):"));
        DigitalScroll unknownText = new DigitalScroll("u", "U", "hello".getBytes(StandardCharsets.UTF_8), "m",
                LocalDateTime.now(), 1, 0, "unknown");
        assertTrue(ScrollPreviewRenderer.render(unknownText).contains("  | hello"));
    }

    /**
     * 【PR s2-r3-browse-screen】实现：Sixian ｜ 审核：Stella
     * Runs the browse screen with scripted input and the recording opener.
     */
    private String browse(String... lines) {
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
        new BrowseScreen(scrolls, new ScrollQueryService(scrolls), accounts, io, viewer(recorder)).show();
        return out.toString();
    }

    /**
     * 【PR s2-r3-browse-screen】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void previewMenuOffersOpenToMembers() {
        String text = browse("1", "doc", "2", "0", "1", "song", "2", "0", "0");
        assertTrue(text.contains("  2. Open in default app"));
        assertTrue(text.contains("Opened in your default application. (This does not count as a download.)"));
        assertTrue(text.contains("Error: Files of type 'mp3' can't be opened here"));
        assertEquals(1, opened.size());
        assertEquals(0, repository.findById("doc").orElseThrow().getDownloadCount());
    }

    /**
     * 【PR s2-r3-browse-screen】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void guestsSeeInfoPreviewButCannotOpen() {
        accounts.logout();
        String text = browse("1", "doc", "2", "0", "0");
        assertTrue(text.contains("Format: PDF document (.pdf)"));
        assertTrue(text.contains("  2. Open in default app (log in required)"));
        assertTrue(text.contains("Error: Please log in first"));
        assertTrue(opened.isEmpty());
    }

    /**
     * 【PR s2-r3-browse-screen】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void browseScreenWithoutViewerExplainsUnavailable() {
        StringWriter out = new StringWriter();
        ConsoleIO io = new ConsoleIO(new Scanner("1\ndoc\n2\n0\n0\n"), new PrintWriter(out, true));
        new BrowseScreen(scrolls, new ScrollQueryService(scrolls), accounts, io).show();
        assertTrue(out.toString().contains("Error: Opening files is not available in this session"));
    }
}

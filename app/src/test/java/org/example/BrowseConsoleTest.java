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
import org.example.service.ScrollQueryService;
import org.example.service.ScrollService;
import org.example.ui.BrowseScreen;
import org.example.ui.ConsoleIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Sprint 2 · US06 #7 / US17 #52 / US18 #53】主负责人：Sixian。审核人：Stella。
 * 测什么：BrowseScreen 页面本身：编号菜单、筛选后列表与标题、无结果空状态、清除筛选、
 * 按下载次数排序并在标题显示、预览显示格式内容、下载补扩展名并显示保存路径、访客看到“需要登录”。
 */
class BrowseConsoleTest {

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
    private ScrollService scrolls;
    private StringWriter out;

    @BeforeEach
    void setUp() throws IOException {
        accounts = new AccountService(new FileUserRepository(directory.resolve("users.properties")), HASHER);
        scrolls = new ScrollService(new FileScrollRepository(directory.resolve("scrolls.properties")),
                accounts, Clock.systemUTC());
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.register("h", "henry", "password123", "Henry", "henry@example.com", "789");
        accounts.login("m", "password123");
        scrolls.upload("s1", "Java Notes", Files.writeString(directory.resolve("java.txt"), "line one\nline two"));
        scrolls.upload("s2", "Report", Files.writeString(directory.resolve("report.pdf"), "%PDF-1.4"));
        scrolls.download("s2", directory.resolve("warm-up.pdf"));
        accounts.login("h", "password123");
        scrolls.upload("s3", "Python Notes", Files.writeString(directory.resolve("py.txt"), "print()"));
        out = new StringWriter();
    }

    /** Runs the browse screen with scripted input. */
    private String run(String... lines) {
        ConsoleIO io = new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
        new BrowseScreen(scrolls, new ScrollQueryService(scrolls), accounts, io).show();
        return out.toString();
    }

    @Test
    void showsNumberedMenuAndHeader() {
        String text = run("0");
        assertTrue(text.contains("Sorted by: upload order | Filters: none"));
        assertTrue(text.contains("s1 | Java Notes | m |"));
        assertTrue(text.contains("| txt | downloads: 0"));
        assertTrue(text.contains("  1. Preview a scroll"));
        assertTrue(text.contains("  2. Download a scroll"));
        assertTrue(text.contains("  3. Filter scrolls"));
        assertTrue(text.contains("  4. Sort scrolls"));
        assertTrue(text.contains("  5. Clear filters"));
        assertTrue(text.contains("  0. Back"));
    }

    @Test
    void filterByUploaderThenClear() {
        String text = run("3", "1", "H", "5", "0");
        assertTrue(text.contains("Filters: uploader ID contains 'H'"));
        String filtered = text.substring(text.indexOf("Filters: uploader ID contains 'H'"));
        filtered = filtered.substring(0, filtered.indexOf("Browse menu:"));
        assertTrue(filtered.contains("s3 | Python Notes"));
        assertFalse(filtered.contains("s1 | Java Notes"));
        assertTrue(text.contains("Filters cleared."));
        assertEquals(3, text.split("Available scrolls:", -1).length - 1);
    }

    @Test
    void filtersByIdNameAndDateAndShowsEmptyState() {
        String today = java.time.LocalDate.now(Clock.systemUTC()).toString();
        String text = run("3", "2", "S1", "3", "3", "java", "3", "4", today,
                "3", "4", "1999-01-01", "3", "4", "bad-date", "0");
        assertTrue(text.contains("scroll ID contains 'S1', name contains 'java', upload date = " + today));
        assertTrue(text.contains("No scrolls match the current filters. Choose 'Clear filters' to see all scrolls."));
        assertTrue(text.contains("Error: Upload date must use the format yyyy-MM-dd"));
    }

    @Test
    void sortByDownloadCountShowsOrderInHeader() {
        String text = run("4", "2", "0");
        assertTrue(text.contains("Sorted by: download count (most downloaded first)"));
        String sorted = text.substring(text.indexOf("Sorted by: download count"));
        assertTrue(sorted.indexOf("s2 | Report") < sorted.indexOf("s3 | Python Notes"));
        assertTrue(sorted.indexOf("s3 | Python Notes") < sorted.indexOf("s1 | Java Notes"));
    }

    @Test
    void previewShowsFormatAwareContentAndDownloadAddsExtension() {
        Path destination = directory.resolve("from-preview");
        String text = run("1", "s1", "1", destination.toString(), "0", "0");
        assertTrue(text.contains("Format: Text file (.txt)"));
        assertTrue(text.contains("  | line one"));
        assertTrue(text.contains("Scroll downloaded successfully."));
        assertTrue(text.contains("Saved to: " + directory.resolve("from-preview.txt")));
        assertTrue(text.contains("Uploads: 1 | Downloads: 1"));
        assertTrue(Files.exists(directory.resolve("from-preview.txt")));
    }

    @Test
    void guestSeesLoginRequiredAndCannotDownload() {
        accounts.logout();
        String text = run("2", "preview s2", "download", "0", "0");
        assertTrue(text.contains("  2. Download a scroll (log in required)"));
        assertTrue(text.contains("  1. Download this scroll (log in required)"));
        assertEquals(2, text.split("Error: Please log in first", -1).length - 1);
        assertTrue(text.contains("PDF version: 1.4"));
        assertFalse(text.contains("Destination file path:"));
    }

    @Test
    void emptyLibraryShowsMessage() throws IOException {
        ScrollService empty = new ScrollService(new FileScrollRepository(directory.resolve("empty.properties")),
                accounts, Clock.systemUTC());
        ConsoleIO io = new ConsoleIO(new Scanner(""), new PrintWriter(out, true));
        new BrowseScreen(empty, new ScrollQueryService(empty), accounts, io).show();
        assertTrue(out.toString().contains("No scrolls available."));
    }
}

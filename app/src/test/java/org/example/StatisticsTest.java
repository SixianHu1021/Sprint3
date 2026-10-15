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
import org.example.service.ScrollEditService;
import org.example.service.ScrollService;
import org.example.service.StatisticsService;
import org.example.ui.ConsoleIO;
import org.example.ui.StatisticsScreen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Yuhan
 *   - 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
 *
 * 【Sprint 2 · US12 #13】主负责人：Stella。
 * 测什么：管理员看到每个卷轴的上传／下载次数和合计；浏览不改变数字；下载与替换文件会改变；
 * 访客和会员被拒绝；统计页面输出和空状态。
 * （通过主菜单第 7、9 项操作的端到端测试在 Yuhan R4 的 NumberedMenuConsoleTest。）
 */
class StatisticsTest {

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
    private FileScrollRepository repository;
    private ScrollService scrolls;
    private StatisticsService statistics;

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Yuhan
     */
    @BeforeEach
    void setUp() {
        users = new FileUserRepository(directory.resolve("users.properties"));
        accounts = new AccountService(users, HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        statistics = new StatisticsService(scrolls, accounts);
        accounts.bootstrapAdmin("a", "admin", "secret123", "Admin", "admin@example.com", "123");
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Yuhan
     * Uploads two scrolls as the member and downloads one twice.
     */
    private void sampleActivity() throws IOException {
        accounts.login("m", "password123");
        scrolls.upload("s1", "One", Files.writeString(directory.resolve("one.txt"), "1"));
        scrolls.upload("s2", "Two", Files.writeString(directory.resolve("two.txt"), "2"));
        scrolls.download("s1", directory.resolve("d1.txt"));
        scrolls.download("s1", directory.resolve("d2.txt"));
        scrolls.browse();
        scrolls.preview("s2");
        new ScrollEditService(repository, accounts)
                .replaceContent("s2", Files.writeString(directory.resolve("two-v2.txt"), "2b"));
        accounts.login("a", "secret123");
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Yuhan
     */
    @Test
    void adminSeesPerScrollCountsAndTotals() throws IOException {
        sampleActivity();
        StatisticsService.Report report = statistics.report();
        assertEquals(2, report.rows().size());
        StatisticsService.Row one = report.rows().get(0);
        assertEquals("s1", one.scrollId());
        assertEquals(1, one.uploads());
        assertEquals(2, one.downloads());
        StatisticsService.Row two = report.rows().get(1);
        assertEquals(2, two.uploads());
        assertEquals(0, two.downloads());
        assertEquals(3, report.totalUploads());
        assertEquals(2, report.totalDownloads());
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Yuhan
     */
    @Test
    void nonAdminsCannotViewStatistics() {
        assertEquals("Please log in first",
                assertThrows(IllegalStateException.class, () -> statistics.report()).getMessage());
        accounts.login("m", "password123");
        assertEquals("Administrator access required",
                assertThrows(IllegalStateException.class, () -> statistics.report()).getMessage());
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
     */
    @Test
    void screenPrintsTableAndTotals() throws IOException {
        sampleActivity();
        StringWriter out = new StringWriter();
        new StatisticsScreen(statistics, new ConsoleIO(new Scanner(""), new PrintWriter(out, true))).show();
        String text = out.toString();
        assertTrue(text.contains("ID | Name | Uploader | Format | Uploads | Downloads"));
        assertTrue(text.contains("s1 | One | m | txt | 1 | 2"));
        assertTrue(text.contains("s2 | Two | m | txt | 2 | 0"));
        assertTrue(text.contains("Total: 2 scroll(s), 3 upload(s), 2 download(s)"));
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
     */
    @Test
    void screenShowsEmptyState() {
        accounts.login("a", "secret123");
        StringWriter out = new StringWriter();
        new StatisticsScreen(statistics, new ConsoleIO(new Scanner(""), new PrintWriter(out, true))).show();
        assertTrue(out.toString().contains("No scrolls have been uploaded yet."));
    }
}

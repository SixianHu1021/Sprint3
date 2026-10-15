package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.model.DigitalScroll;
import org.example.repository.ScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollFilter;
import org.example.service.ScrollQueryService;
import org.example.service.ScrollService;
import org.example.service.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US06 #7 / US18 #53】主负责人：Sixian。
 * 测什么：按上传者、卷轴 ID、名称、上传日期分别筛选（不区分大小写）、组合筛选、清除筛选回到完整列表、
 * 无结果为空列表；按下载次数从多到少排序，平局按上传日期从新到旧；空列表排序；日期解析错误提示。
 * 使用测试目录内的内存仓库，避免依赖上传时间。
 */
class ScrollQueryServiceTest {

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

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * Test-only in-memory repository with fixed dates and counters.
     */
    private static final class MemoryRepository implements ScrollRepository {
        private final List<DigitalScroll> items;

        MemoryRepository(List<DigitalScroll> items) {
            this.items = items;
        }

        @Override
        public void save(DigitalScroll scroll) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DigitalScroll> findAll() {
            return List.copyOf(items);
        }

        @Override
        public Optional<DigitalScroll> findById(String scrollId) {
            return items.stream().filter(s -> s.getScrollId().equals(scrollId)).findFirst();
        }

        @Override
        public boolean existsById(String scrollId) {
            return findById(scrollId).isPresent();
        }

        @Override
        public boolean existsByName(String name) {
            return items.stream().anyMatch(s -> s.getName().equals(name));
        }

        @Override
        public void deleteById(String scrollId) {
            throw new UnsupportedOperationException();
        }
    }

    @TempDir
    java.nio.file.Path directory;

    private ScrollQueryService query;

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * Creates a scroll with a fixed date and download count.
     */
    private static DigitalScroll scroll(String id, String name, String uploader, String date, long downloads) {
        return new DigitalScroll(id, name, new byte[]{1}, uploader, LocalDateTime.parse(date), 1, downloads, "txt");
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @BeforeEach
    void setUp() {
        List<DigitalScroll> items = List.of(
                scroll("alpha-1", "Java Basics", "henry", "2026-10-16T09:00", 3),
                scroll("beta-2", "Advanced JAVA", "stella", "2026-10-17T10:00", 5),
                scroll("gamma-3", "Python Notes", "henry", "2026-10-18T11:00", 3),
                scroll("delta-4", "Scrum Guide", "sixian", "2026-10-17T23:59", 0));
        AccountService accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")), HASHER);
        query = new ScrollQueryService(new ScrollService(new MemoryRepository(items), accounts,
                java.time.Clock.systemUTC()));
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * Extracts IDs in result order.
     */
    private static List<String> ids(List<DigitalScroll> scrolls) {
        return scrolls.stream().map(DigitalScroll::getScrollId).toList();
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void eachFilterWorksIndividually() {
        ScrollFilter none = ScrollFilter.none();
        assertEquals(List.of("alpha-1", "gamma-3"),
                ids(query.search(none.withUploaderId("HENRY"), SortOrder.UPLOAD_ORDER)));
        assertEquals(List.of("beta-2"), ids(query.search(none.withScrollId("BETA"), SortOrder.UPLOAD_ORDER)));
        assertEquals(List.of("alpha-1", "beta-2"), ids(query.search(none.withName("java"), null)));
        assertEquals(List.of("beta-2", "delta-4"),
                ids(query.search(none.withUploadDate(LocalDate.parse("2026-10-17")), SortOrder.UPLOAD_ORDER)));
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void filtersCombineAndClearingReturnsFullList() {
        ScrollFilter both = ScrollFilter.none().withUploaderId("henry").withName("python");
        assertEquals(List.of("gamma-3"), ids(query.search(both, SortOrder.UPLOAD_ORDER)));
        ScrollFilter cleared = both.withUploaderId(" ").withName(null);
        assertTrue(cleared.isEmpty());
        assertEquals(4, query.search(cleared, SortOrder.UPLOAD_ORDER).size());
        assertEquals(4, query.search(null, SortOrder.UPLOAD_ORDER).size());
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void noMatchesGivesEmptyList() {
        assertTrue(query.search(ScrollFilter.none().withName("rust"), SortOrder.UPLOAD_ORDER).isEmpty());
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void sortByDownloadsIsDescendingWithNewestFirstOnTies() {
        assertEquals(List.of("beta-2", "gamma-3", "alpha-1", "delta-4"),
                ids(query.search(ScrollFilter.none(), SortOrder.MOST_DOWNLOADED)));
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void otherSortOrders() {
        assertEquals(List.of("alpha-1", "beta-2", "gamma-3", "delta-4"),
                ids(query.search(ScrollFilter.none(), SortOrder.UPLOAD_ORDER)));
        assertEquals(List.of("gamma-3", "delta-4", "beta-2", "alpha-1"),
                ids(query.search(ScrollFilter.none(), SortOrder.NEWEST_FIRST)));
        assertTrue(query.search(ScrollFilter.none().withName("none"), SortOrder.MOST_DOWNLOADED).isEmpty());
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void describeShowsActiveConditions() {
        assertEquals("none", ScrollFilter.none().describe());
        String text = ScrollFilter.none().withUploaderId("h").withScrollId("a").withName("j")
                .withUploadDate(LocalDate.parse("2026-10-17")).describe();
        assertEquals("uploader ID contains 'h', scroll ID contains 'a', name contains 'j', "
                + "upload date = 2026-10-17", text);
        assertEquals("download count (most downloaded first)", SortOrder.MOST_DOWNLOADED.label());
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     */
    @Test
    void dateParsing() {
        assertEquals(LocalDate.parse("2026-10-20"), ScrollQueryService.parseDate(" 2026-10-20 "));
        assertNull(ScrollQueryService.parseDate(" "));
        assertNull(ScrollQueryService.parseDate(null));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ScrollQueryService.parseDate("20/10/2026"))
                .getMessage().contains("yyyy-MM-dd"));
    }
}

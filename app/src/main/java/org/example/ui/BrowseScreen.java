package org.example.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.example.model.DigitalScroll;
import org.example.service.AccountService;
import org.example.service.FileFormat;
import org.example.service.ScrollFilter;
import org.example.service.ScrollPreviewRenderer;
import org.example.service.ScrollQueryService;
import org.example.service.ScrollService;
import org.example.service.SortOrder;

/**
 * 【Sprint 2 · US06 #7 / US17 #52 / US18 #53】主负责人：Sixian。审核人：Stella（卷轴字段）、Yuhan（菜单集成）。
 * 做什么：浏览页面（从 Sprint 1 ConsoleUi.browse/preview/download 移来并扩展）：
 * 列表标题显示当前排序和筛选；编号菜单提供预览、下载、筛选、排序、清除筛选；预览页按文件格式显示内容。
 * 对接／验证：数据来自 ScrollQueryService（筛选／排序）和 ScrollService（预览、下载）；访客能看到所有选项，
 * 但选择下载会得到 “Please log in first”（编号对所有角色一致）。不需要离开浏览页就能下载（Sprint 1 要求保持）。
 * BrowseConsoleTest 直接创建本页面测试；Sprint 1 的 ScrollConsoleTest 通过 ConsoleUi 继续验证旧命令。
 */
public final class BrowseScreen {

    private final ScrollService scrolls;
    private final ScrollQueryService query;
    private final AccountService accounts;
    private final ConsoleIO io;
    private ScrollFilter filter = ScrollFilter.none();
    private SortOrder order = SortOrder.UPLOAD_ORDER;

    /**
     * 主负责人：Sixian。
     * 做什么：保存浏览所需服务；每次进入浏览页都从“无筛选、上传顺序”开始。
     *
     * @param scrolls scroll service for preview and download
     * @param query filter and sort service
     * @param accounts account service for login checks
     * @param io shared console helper
     */
    public BrowseScreen(ScrollService scrolls, ScrollQueryService query, AccountService accounts, ConsoleIO io) {
        this.scrolls = scrolls;
        this.query = query;
        this.accounts = accounts;
        this.io = io;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：图书馆为空时显示 “No scrolls available.” 并返回；否则循环显示列表和浏览菜单，直到选择 0。
     * 对接／验证：浏览和预览不改变上传／下载次数。
     */
    public void show() {
        filter = ScrollFilter.none();
        order = SortOrder.UPLOAD_ORDER;
        if (scrolls.browse().isEmpty()) {
            io.info("No scrolls available.");
            return;
        }
        String loginNote = accounts.currentUser().isPresent() ? "" : " (log in required)";
        Menu menu = new Menu("Browse menu:", "Back", "back")
                .option("Preview a scroll", "preview", this::preview)
                .option("Download a scroll" + loginNote, "download", this::download)
                .option("Filter scrolls", "filter", a -> chooseFilter())
                .option("Sort scrolls", "sort", a -> chooseSort())
                .option("Clear filters", "clear", a -> clearFilters());
        menu.loop(io, "Browse> ", this::list);
    }

    /**
     * 【US06 / US18】主负责人：Sixian。
     * 做什么：打印 “Available scrolls:”、当前排序与筛选，以及每行 ID | 名称 | 上传者 | 日期 | 格式 | 下载次数。
     * 对接／验证：筛选后没有结果时显示空状态提示，并提醒可以清除筛选。
     */
    private void list() {
        List<DigitalScroll> shown = query.search(filter, order);
        io.info("Available scrolls:");
        io.info("Sorted by: " + order.label() + " | Filters: " + filter.describe());
        if (shown.isEmpty()) {
            io.info("No scrolls match the current filters. Choose 'Clear filters' to see all scrolls.");
            return;
        }
        for (DigitalScroll scroll : shown) {
            io.info(scroll.getScrollId() + " | " + scroll.getName() + " | " + scroll.getUploaderId() + " | "
                    + scroll.getUploadDate() + " | " + scroll.getFileFormat()
                    + " | downloads: " + scroll.getDownloadCount());
        }
    }

    /**
     * 【US17】主负责人：Sixian。
     * 做什么：预览页：元数据 + 格式说明 + 按格式生成的内容；编号菜单可直接下载或返回列表。
     * 对接／验证：ID 不存在时显示 “Error: Scroll not found: ID” 并留在列表。
     *
     * @param argument scroll ID typed after the alias, or empty
     */
    private void preview(String argument) {
        String id = scrollId(argument);
        DigitalScroll scroll = scrolls.preview(id);
        String loginNote = accounts.currentUser().isPresent() ? "" : " (log in required)";
        Menu menu = new Menu("Preview menu:", "Back to list", "back")
                .option("Download this scroll" + loginNote, "download", a -> download(scroll.getScrollId()));
        menu.loop(io, "Preview> ", () -> showPreview(scroll.getScrollId()));
    }

    /**
     * 【US17】主负责人：Sixian。
     * 做什么：打印预览内容；每轮重新读取，下载后能看到新的下载次数。
     */
    private void showPreview(String id) {
        DigitalScroll scroll = scrolls.preview(id);
        io.info("Scroll preview:");
        io.info("ID: " + scroll.getScrollId());
        io.info("Name: " + scroll.getName());
        io.info("Uploader: " + scroll.getUploaderId());
        io.info("Upload date: " + scroll.getUploadDate());
        io.info("Uploads: " + scroll.getUploadCount() + " | Downloads: " + scroll.getDownloadCount());
        ScrollPreviewRenderer.render(scroll).forEach(io::info);
    }

    /**
     * 【US08 Sprint 1 / US17】主负责人：Sixian。
     * 做什么：先检查登录（访客不会看到路径提示），确认卷轴存在，再询问保存路径；
     * 自动补全格式扩展名，并显示实际保存位置。
     * 对接／验证：下载后留在当前页面，可以继续浏览。
     *
     * @param argument scroll ID typed after the alias, or empty
     * @throws IOException if writing fails
     */
    private void download(String argument) throws IOException {
        accounts.requireUser();
        String id = scrollId(argument);
        DigitalScroll scroll = scrolls.preview(id);
        if (!scroll.getFileFormat().equals(DigitalScroll.UNKNOWN_FORMAT)) {
            io.info("File format: " + FileFormat.describe(scroll.getFileFormat())
                    + " - the ." + scroll.getFileFormat() + " extension is added if missing.");
        }
        String destination = io.askRequired("Destination file path: ", "Destination file path");
        Path saved = scrolls.download(id, Path.of(destination));
        io.info("Scroll downloaded successfully.");
        io.info("Saved to: " + saved);
    }

    /**
     * 【US06】主负责人：Sixian。
     * 做什么：选择一个筛选条件并输入值；直接回车清除该条件；其他条件保持不变（可单独使用）。
     * 对接／验证：日期格式错误显示示例，筛选保持原样。
     */
    private void chooseFilter() {
        Menu menu = new Menu("Filter by:", "Back", "back")
                .option("Uploader ID", "uploader", a -> filter = filter.withUploaderId(
                        io.askOptional("Uploader ID contains (blank to clear): ")))
                .option("Scroll ID", "id", a -> filter = filter.withScrollId(
                        io.askOptional("Scroll ID contains (blank to clear): ")))
                .option("Name", "name", a -> filter = filter.withName(
                        io.askOptional("Name contains (blank to clear): ")))
                .option("Upload date (yyyy-MM-dd)", "date", a -> filter = filter.withUploadDate(
                        ScrollQueryService.parseDate(io.askOptional("Upload date yyyy-MM-dd (blank to clear): "))));
        menu.runOnce(io, "Filter> ");
    }

    /**
     * 【US18】主负责人：Sixian。
     * 做什么：选择排序方式；按下载次数时从多到少，平局按上传日期从新到旧。
     * 对接／验证：选择后列表标题显示新的排序方式。
     */
    private void chooseSort() {
        Menu menu = new Menu("Sort by:", "Back", "back");
        for (SortOrder candidate : SortOrder.values()) {
            menu.option(capitalize(candidate.label()), null, a -> order = candidate);
        }
        menu.runOnce(io, "Sort> ");
    }

    /**
     * 【US06】主负责人：Sixian。
     * 做什么：清除全部筛选条件，返回完整列表（排序方式保留）。
     */
    private void clearFilters() {
        filter = ScrollFilter.none();
        io.info("Filters cleared.");
    }

    /**
     * 主负责人：Sixian。
     * 做什么：别名后带了 ID（如 “preview s1”）就直接使用，否则询问。
     */
    private String scrollId(String argument) {
        return argument.isEmpty() ? io.askRequired("Scroll ID: ", "Scroll ID") : argument;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：首字母大写，用于菜单显示。
     */
    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

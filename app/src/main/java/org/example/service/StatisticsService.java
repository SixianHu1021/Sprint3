package org.example.service;

import java.util.List;
import org.example.model.DigitalScroll;

/**
 * 【Sprint 2 · US12 #13】主负责人：Stella。审核人：Yuhan（管理员权限负责人）。
 * 做什么：管理员查看每个卷轴的上传次数、下载次数，以及全站合计。非管理员调用会被拒绝。
 * 对接／验证：权限调用 AccountService.requireAdmin（Henry 在 R1 改为 public）；数据来自 ScrollService.browse，
 * 不直接读文件。界面在 Stella 的 StatisticsScreen。
 */
public final class StatisticsService {

    /**
     * 主负责人：Stella。
     * 做什么：一行统计数据。
     *
     * @param scrollId scroll ID
     * @param name scroll name
     * @param uploaderId uploader ID key
     * @param format file format
     * @param uploads times the scroll's content has been uploaded
     * @param downloads successful downloads
     */
    public record Row(String scrollId, String name, String uploaderId, String format, long uploads, long downloads) {
    }

    /**
     * 主负责人：Stella。
     * 做什么：完整报告：每个卷轴一行，加上上传和下载合计。
     *
     * @param rows per-scroll statistics
     * @param totalUploads sum of uploads
     * @param totalDownloads sum of downloads
     */
    public record Report(List<Row> rows, long totalUploads, long totalDownloads) {
    }

    private final ScrollService scrolls;
    private final AccountService accounts;

    /**
     * 主负责人：Stella。
     * 做什么：保存卷轴服务和账户服务引用。
     * 对接／验证：ConsoleUi 使用同一组服务创建本类。
     *
     * @param scrolls scroll service providing the list
     * @param accounts account service enforcing admin access
     */
    public StatisticsService(ScrollService scrolls, AccountService accounts) {
        this.scrolls = scrolls;
        this.accounts = accounts;
    }

    /**
     * 主负责人：Stella。
     * 做什么：先检查管理员，再生成每个卷轴的统计和合计。
     * 对接／验证：浏览和预览不改变数字；下载、替换文件后数字变化（StatisticsServiceTest 验证）。
     *
     * @return statistics report
     * @throws IllegalStateException if the current user is not an administrator
     */
    public Report report() {
        accounts.requireAdmin();
        List<DigitalScroll> all = scrolls.browse();
        List<Row> rows = all.stream()
                .map(s -> new Row(s.getScrollId(), s.getName(), s.getUploaderId(), s.getFileFormat(),
                        s.getUploadCount(), s.getDownloadCount()))
                .toList();
        long uploads = all.stream().mapToLong(DigitalScroll::getUploadCount).sum();
        long downloads = all.stream().mapToLong(DigitalScroll::getDownloadCount).sum();
        return new Report(rows, uploads, downloads);
    }
}

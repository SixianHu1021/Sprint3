package org.example.service;

import java.util.Comparator;
import org.example.model.DigitalScroll;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US18 #53】主负责人：Sixian。
 * 做什么：卷轴列表的排序方式。按下载次数排序时默认从多到少；次数相同按上传日期从新到旧。
 * 对接／验证：ScrollQueryService.search 使用 comparator；BrowseScreen 在列表标题显示 label。
 */
public enum SortOrder {
    /** Order in which scrolls were uploaded (Sprint 1 behaviour). */
    UPLOAD_ORDER("upload order"),
    /** Most downloaded first; ties fall back to the newest upload. */
    MOST_DOWNLOADED("download count (most downloaded first)"),
    /** Newest upload first. */
    NEWEST_FIRST("upload date (newest first)");

    private final String label;

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：保存显示文字。
     *
     * @param label text shown in the list header
     */
    SortOrder(String label) {
        this.label = label;
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：返回列表标题中显示的排序说明。
     *
     * @return readable label
     */
    public String label() {
        return label;
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：返回排序比较器；UPLOAD_ORDER 返回 null，表示保持仓库顺序。
     * 对接／验证：ScrollQueryServiceTest 测试相同下载次数的平局规则和空列表。
     *
     * @return comparator, or {@code null} to keep stored order
     */
    public Comparator<DigitalScroll> comparator() {
        Comparator<DigitalScroll> newest = Comparator.comparing(DigitalScroll::getUploadDate).reversed();
        return switch (this) {
            case UPLOAD_ORDER -> null;
            case MOST_DOWNLOADED -> Comparator.comparingLong(DigitalScroll::getDownloadCount).reversed()
                    .thenComparing(newest);
            case NEWEST_FIRST -> newest;
        };
    }
}

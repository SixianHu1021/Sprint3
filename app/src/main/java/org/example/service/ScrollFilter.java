package org.example.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.model.DigitalScroll;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US06 #7】主负责人：Sixian。
 * 做什么：保存当前的筛选条件（上传者 ID、卷轴 ID、名称、上传日期），每个条件都可以单独设置或清除。
 * 文字条件不区分大小写、按“包含”匹配；日期按 yyyy-MM-dd 当天匹配；多个条件同时生效（AND）。
 * 对接／验证：ScrollQueryService.search 使用；BrowseScreen 显示 describe() 的结果。不可变对象，修改时返回新对象。
 *
 * @param uploaderId uploader ID text, or {@code null} when unused
 * @param scrollId scroll ID text, or {@code null} when unused
 * @param name name text, or {@code null} when unused
 * @param uploadDate upload day, or {@code null} when unused
 */
public record ScrollFilter(String uploaderId, String scrollId, String name, LocalDate uploadDate) {

    /**
     * 主负责人：Sixian。
     * 做什么：把空白文字条件统一为 null（表示不筛选）。
     * 对接／验证：用户在筛选提示直接回车即清除该条件。
     */
    public ScrollFilter {
        uploaderId = blankToNull(uploaderId);
        scrollId = blankToNull(scrollId);
        name = blankToNull(name);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：没有任何条件的筛选；“清除筛选”回到完整列表。
     * 对接／验证：BrowseScreen 初始值和 Clear filters 使用。
     *
     * @return empty filter
     */
    public static ScrollFilter none() {
        return new ScrollFilter(null, null, null, null);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：替换上传者条件。
     *
     * @param value uploader ID text; blank clears the condition
     * @return updated filter
     */
    public ScrollFilter withUploaderId(String value) {
        return new ScrollFilter(value, scrollId, name, uploadDate);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：替换卷轴 ID 条件。
     *
     * @param value scroll ID text; blank clears the condition
     * @return updated filter
     */
    public ScrollFilter withScrollId(String value) {
        return new ScrollFilter(uploaderId, value, name, uploadDate);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：替换名称条件。
     *
     * @param value name text; blank clears the condition
     * @return updated filter
     */
    public ScrollFilter withName(String value) {
        return new ScrollFilter(uploaderId, scrollId, value, uploadDate);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：替换上传日期条件。
     *
     * @param value upload day, or {@code null} to clear
     * @return updated filter
     */
    public ScrollFilter withUploadDate(LocalDate value) {
        return new ScrollFilter(uploaderId, scrollId, name, value);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：判断是否没有任何条件。
     *
     * @return true if no condition is set
     */
    public boolean isEmpty() {
        return uploaderId == null && scrollId == null && name == null && uploadDate == null;
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：判断一个卷轴是否满足所有已设置的条件。
     * 对接／验证：ScrollQueryServiceTest 分别测试每个条件、组合条件和大小写。
     *
     * @param scroll scroll to test
     * @return true if every set condition matches
     */
    public boolean matches(DigitalScroll scroll) {
        return contains(scroll.getUploaderId(), uploaderId)
                && contains(scroll.getScrollId(), scrollId)
                && contains(scroll.getName(), name)
                && (uploadDate == null || scroll.getUploadDate().toLocalDate().equals(uploadDate));
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：生成列表标题中的条件说明，例如 “uploader ID contains 'm', upload date = 2026-10-20”。
     * 对接／验证：没有条件时返回 “none”。
     *
     * @return readable description
     */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (uploaderId != null) {
            parts.add("uploader ID contains '" + uploaderId + "'");
        }
        if (scrollId != null) {
            parts.add("scroll ID contains '" + scrollId + "'");
        }
        if (name != null) {
            parts.add("name contains '" + name + "'");
        }
        if (uploadDate != null) {
            parts.add("upload date = " + uploadDate);
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：不区分大小写的“包含”比较；条件为 null 时总是匹配。
     */
    private static boolean contains(String value, String condition) {
        return condition == null
                || value.toLowerCase(Locale.ROOT).contains(condition.toLowerCase(Locale.ROOT));
    }

    /**
     * 【PR s2-r1-query-format】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：去掉首尾空格，空白视为 null。
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

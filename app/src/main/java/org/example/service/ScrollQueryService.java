package org.example.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.example.model.DigitalScroll;

/**
 * 【Sprint 2 · US06 #7 / US18 #53】主负责人：Sixian。审核人：Stella。
 * 做什么：在 ScrollService.browse() 的结果上做筛选和排序，访客与登录用户都可以使用。
 * 对接／验证：只读取 Sprint 1 的 browse()，不改 ScrollService，也不改变上传／下载次数。
 * BrowseScreen（Sixian，R3）调用本类；ScrollQueryServiceTest 覆盖全部验收条件。
 */
public final class ScrollQueryService {

    private final ScrollService scrolls;

    /**
     * 主负责人：Sixian。
     * 做什么：保存 Sprint 1 的卷轴服务引用。
     * 对接／验证：ConsoleUi 用同一个 ScrollService 创建本类，看到的数据与上传／下载一致。
     *
     * @param scrolls scroll service providing the full list
     */
    public ScrollQueryService(ScrollService scrolls) {
        this.scrolls = scrolls;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：先按条件筛选，再按指定方式排序；没有匹配时返回空列表（界面显示空状态提示）。
     * 对接／验证：filter 为 none() 时返回完整列表（“清除筛选”）；order 为 null 等同 UPLOAD_ORDER。
     *
     * @param filter conditions to apply
     * @param order sort order
     * @return matching scrolls in the requested order
     */
    public List<DigitalScroll> search(ScrollFilter filter, SortOrder order) {
        ScrollFilter conditions = filter == null ? ScrollFilter.none() : filter;
        List<DigitalScroll> result = new ArrayList<>(
                scrolls.browse().stream().filter(conditions::matches).toList());
        Comparator<DigitalScroll> comparator = order == null ? null : order.comparator();
        if (comparator != null) {
            result.sort(comparator);
        }
        return List.copyOf(result);
    }

    /**
     * 主负责人：Sixian。
     * 做什么：把用户输入的日期解析为 LocalDate；空白表示清除日期条件。
     * 对接／验证：格式错误给出示例，符合 US14 的统一提示要求。
     *
     * @param text date typed by the user
     * @return parsed date, or {@code null} for blank input
     * @throws IllegalArgumentException if the text is not yyyy-MM-dd
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Upload date must use the format yyyy-MM-dd, for example 2026-10-20");
        }
    }
}

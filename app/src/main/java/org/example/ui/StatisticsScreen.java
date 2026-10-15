package org.example.ui;

import org.example.service.StatisticsService;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
 *
 * 【Sprint 2 · US12 #13】主负责人：Stella。
 * 做什么：管理员统计页面：每个卷轴一行显示上传／下载次数，最后显示合计。
 * 对接／验证：权限检查在 StatisticsService.report（非管理员得到 “Administrator access required”）；
 * Yuhan 在 R4 把管理员菜单第 9 项接到 show()。
 */
public final class StatisticsScreen {

    private final StatisticsService statistics;
    private final ConsoleIO io;

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
     * 主负责人：Stella。
     * 做什么：保存统计服务和共用输入输出。
     *
     * @param statistics statistics service
     * @param io shared console helper
     */
    public StatisticsScreen(StatisticsService statistics, ConsoleIO io) {
        this.statistics = statistics;
        this.io = io;
    }

    /**
     * 【PR s2-r3-my-scrolls-stats-screens】实现：Stella ｜ 审核：Yuhan
     * 主负责人：Stella。
     * 做什么：打印统计表；没有卷轴时显示提示。
     * 对接／验证：StatisticsConsoleTest 检查表头、每行数字和合计。
     */
    public void show() {
        StatisticsService.Report report = statistics.report();
        io.info("Scroll statistics:");
        if (report.rows().isEmpty()) {
            io.info("No scrolls have been uploaded yet.");
            return;
        }
        io.info("ID | Name | Uploader | Format | Uploads | Downloads");
        for (StatisticsService.Row row : report.rows()) {
            io.info(row.scrollId() + " | " + row.name() + " | " + row.uploaderId() + " | " + row.format()
                    + " | " + row.uploads() + " | " + row.downloads());
        }
        io.info("Total: " + report.rows().size() + " scroll(s), " + report.totalUploads() + " upload(s), "
                + report.totalDownloads() + " download(s)");
    }
}

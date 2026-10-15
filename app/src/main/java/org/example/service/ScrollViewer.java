package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.example.model.DigitalScroll;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US17 #52 打开预览】主负责人：Sixian。
 * 做什么：登录用户在预览页选择 “Open in default app” 时，把卷轴写到临时文件（带正确扩展名），
 * 再用电脑的默认应用打开。访客不能使用（访客只看信息预览，见 BrowseScreen）。
 * 规则（团队 10-16 决定）：
 * <ul>
 *   <li>只打开 FileFormat 白名单里的类型（图片、PDF、文本、docx）；其他类型一律不打开，只显示信息预览。</li>
 *   <li>打开<b>不算下载</b>：不增加下载次数；只有预览页 / 列表中的 “Download” 才计数。</li>
 *   <li>临时文件放在系统临时目录的独立子目录中，程序退出时删除。</li>
 * </ul>
 * 对接／验证：调用 Yuhan 的 requireUser、Sixian 的 ScrollService.preview 与 FileFormat.isOpenable；
 * ScrollViewerTest 用假的 FileOpener 验证，不会真的打开窗口。
 */
public final class ScrollViewer {

    private final ScrollService scrolls;
    private final AccountService accounts;
    private final FileOpener opener;
    private final Path tempRoot;

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：使用系统临时目录的构造方法（真实程序）。
     *
     * @param scrolls scroll service used to read the scroll
     * @param accounts account service enforcing login
     * @param opener how files are opened on this machine
     */
    public ScrollViewer(ScrollService scrolls, AccountService accounts, FileOpener opener) {
        this(scrolls, accounts, opener, null);
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：可指定临时目录的构造方法；测试传入 @TempDir，避免在系统临时目录留下文件。
     *
     * @param scrolls scroll service used to read the scroll
     * @param accounts account service enforcing login
     * @param opener how files are opened on this machine
     * @param tempRoot parent folder for viewing copies, or {@code null} for the system temp folder
     */
    public ScrollViewer(ScrollService scrolls, AccountService accounts, FileOpener opener, Path tempRoot) {
        this.scrolls = scrolls;
        this.accounts = accounts;
        this.opener = Objects.requireNonNull(opener, "opener");
        this.tempRoot = tempRoot;
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：检查登录 → 找到卷轴 → 检查类型可打开 → 写临时副本 → 交给 FileOpener 打开。
     * 不修改卷轴，也不增加下载次数。
     * 对接／验证：不可打开的类型抛出 IllegalArgumentException，界面显示原因并留在预览页。
     *
     * @param id scroll ID
     * @return path of the temporary viewing copy
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the scroll is missing or its type cannot be opened
     * @throws IOException if the copy cannot be written or this machine cannot open it
     */
    public Path open(String id) throws IOException {
        accounts.requireUser();
        DigitalScroll scroll = scrolls.preview(id);
        String format = scroll.getFileFormat();
        if (!FileFormat.isOpenable(format)) {
            throw new IllegalArgumentException("Files of type '" + format
                    + "' can't be opened here; the preview above shows its details. Download it to use it.");
        }
        Path folder = tempRoot == null
                ? Files.createTempDirectory("vsas-view-")
                : Files.createTempDirectory(tempRoot, "vsas-view-");
        folder.toFile().deleteOnExit();
        Path copy = folder.resolve(safeName(scroll.getScrollId()) + "." + format);
        Files.write(copy, scroll.getContent());
        // deleteOnExit 按注册的相反顺序删除：先删文件，再删目录。
        copy.toFile().deleteOnExit();
        opener.open(copy);
        return copy;
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：把卷轴 ID 变成安全的文件名（只保留字母、数字、点、横线和下划线）。
     * 对接／验证：防止 ID 中的 “../” 等字符把临时文件写到别的目录。
     */
    static String safeName(String id) {
        String name = id.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() || name.chars().allMatch(c -> c == '.') ? "scroll" : name;
    }
}

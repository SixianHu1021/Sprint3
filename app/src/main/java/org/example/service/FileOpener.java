package org.example.service;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US17 #52 打开预览】主负责人：Sixian。
 * 做什么：“用电脑自带的应用打开文件” 的插口（与 PasswordReader 同样的设计）。
 * 真实程序用 {@link #desktop()}（Java 的 Desktop.open，Mac 用预览／Pages，Windows 用照片／Word 等默认应用）；
 * 测试用假的实现，只记录 “要求打开了哪个文件”，因此 Jenkins（没有屏幕）和本地测试都不会弹出窗口。
 * 对接／验证：只由 ScrollViewer 调用。
 */
@FunctionalInterface
public interface FileOpener {

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * Opens a file with the operating system's default application.
     *
     * @param file file to open
     * @throws IOException if this machine cannot open the file
     */
    void open(Path file) throws IOException;

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：真实的打开方式。先检查这台电脑有没有桌面（服务器、Docker、SSH 没有），
     * 没有就给出清楚的说明；有桌面但没有能打开该类型的应用时，也给出说明而不是崩溃。
     * 对接／验证：App 传给 ConsoleUi；测试不使用（避免真的打开窗口）。
     *
     * @return opener using {@link Desktop#open}
     */
    static FileOpener desktop() {
        return file -> {
            if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("Cannot open files on this machine (no desktop available)");
            }
            try {
                Desktop.getDesktop().open(file.toFile());
            } catch (IOException | IllegalArgumentException e) {
                throw new IOException("No application on this computer can open " + file.getFileName(), e);
            }
        };
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：“不支持打开” 的实现，用于没有配置真实打开方式的情况（例如 Sprint 1 风格的测试构造方法）。
     * 对接／验证：ConsoleUi 默认使用它，只有 App 显式传入 desktop()。
     *
     * @return opener that always reports opening as unavailable
     */
    static FileOpener unavailable() {
        return file -> {
            throw new IOException("Opening files is not available in this session");
        };
    }
}

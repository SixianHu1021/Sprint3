package org.example.ui;

import java.nio.file.Path;
import java.util.List;
import org.example.model.DigitalScroll;
import org.example.service.ScrollEditService;

/**
 * 【Sprint 2 · US10 #11】主负责人：Stella。审核人：Sixian（浏览页面风格一致）、Yuhan（菜单集成）。
 * 做什么：“我的卷轴”页面：列出自己上传的卷轴，编号菜单提供改名、替换文件、删除（删除前确认）。
 * 对接／验证：业务和所有者检查在 Stella 的 ScrollEditService；输入使用 Yuhan 的 ConsoleIO/Menu。
 * MyScrollsConsoleTest 直接创建本页面测试，不依赖 ConsoleUi 合并顺序。
 */
public final class MyScrollsScreen {

    private final ScrollEditService edits;
    private final ConsoleIO io;

    /**
     * 主负责人：Stella。
     * 做什么：保存编辑服务和共用输入输出。
     *
     * @param edits scroll editing service
     * @param io shared console helper
     */
    public MyScrollsScreen(ScrollEditService edits, ConsoleIO io) {
        this.edits = edits;
        this.io = io;
    }

    /**
     * 主负责人：Stella。
     * 做什么：运行页面直到用户选择 0；每轮先刷新列表。
     * 对接／验证：访客进入时 myScrolls() 抛出 “Please log in first”，由主菜单显示错误。
     */
    public void show() {
        edits.myScrolls();
        Menu menu = new Menu("My scrolls menu:", "Back", "back")
                .option("Rename a scroll", "rename", this::rename)
                .option("Replace a scroll's file", "replace", this::replace)
                .option("Delete a scroll", "delete", this::delete);
        menu.loop(io, "MyScrolls> ", this::list);
    }

    /**
     * 主负责人：Stella。
     * 做什么：列出 ID、名称、格式、上传次数和下载次数；没有卷轴时显示提示。
     */
    private void list() {
        List<DigitalScroll> mine = edits.myScrolls();
        if (mine.isEmpty()) {
            io.info("You have not uploaded any scrolls yet.");
            return;
        }
        io.info("Your scrolls:");
        for (DigitalScroll scroll : mine) {
            io.info(scroll.getScrollId() + " | " + scroll.getName() + " | " + scroll.getFileFormat()
                    + " | uploads: " + scroll.getUploadCount() + " | downloads: " + scroll.getDownloadCount());
        }
    }

    /**
     * 主负责人：Stella。
     * 做什么：询问卷轴 ID 和新名称后改名。
     *
     * @param argument scroll ID typed after the alias, or empty
     */
    private void rename(String argument) {
        String id = scrollId(argument);
        String name = io.askRequired("New name: ", "Scroll name");
        edits.rename(id, name);
        io.info("Scroll renamed.");
    }

    /**
     * 主负责人：Stella。
     * 做什么：询问卷轴 ID 和新文件路径后替换内容，并显示新格式。
     *
     * @param argument scroll ID typed after the alias, or empty
     * @throws java.io.IOException if the file cannot be read
     */
    private void replace(String argument) throws java.io.IOException {
        String id = scrollId(argument);
        String source = io.askRequired("New source file path: ", "Source file path");
        DigitalScroll updated = edits.replaceContent(id, Path.of(source));
        io.info("Scroll file replaced (format: " + updated.getFileFormat() + ").");
    }

    /**
     * 主负责人：Stella。
     * 做什么：只有自己的卷轴才询问确认；不存在或属于别人的 ID 不询问，直接交给 edits.delete 给出准确错误。
     * 对接／验证：选择 n 时显示 “Deletion cancelled.”，卷轴保留。
     *
     * @param argument scroll ID typed after the alias, or empty
     */
    private void delete(String argument) {
        String id = scrollId(argument);
        boolean owned = edits.myScrolls().stream().anyMatch(s -> s.getScrollId().equals(id));
        if (owned && !io.confirm("Delete scroll '" + id + "'? This cannot be undone.")) {
            io.info("Deletion cancelled.");
            return;
        }
        edits.delete(id);
        io.info("Scroll deleted.");
    }

    /**
     * 主负责人：Stella。
     * 做什么：别名后带了 ID（如 “delete s1”）就直接使用，否则询问。
     */
    private String scrollId(String argument) {
        return argument.isEmpty() ? io.askRequired("Scroll ID: ", "Scroll ID") : argument;
    }
}

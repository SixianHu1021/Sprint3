package org.example.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US13 #48】主负责人：Yuhan。
 * 做什么：通用编号菜单。显示 “1. Browse scrolls … 0. Quit”，读取编号并调用对应动作；
 * 也接受 Sprint 1 的命令词（如 browse、preview s1）作为别名，旧脚本与习惯仍可用。
 * 对接／验证：主菜单、浏览页、预览页、我的卷轴、个人资料等全部用本类，保证编号格式一致；
 * 越界数字和非数字输入只显示错误，不退出菜单。
 */
public final class Menu {

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：菜单项要执行的动作；argument 是别名后面的参数（例如 “preview s1” 中的 s1），没有就是空字符串。
     * 对接／验证：各页面负责人写自己的动作，不在这里写业务规则。
     */
    @FunctionalInterface
    public interface Action {
        /**
         * Runs the selected option.
         *
         * @param argument text after a typed alias, or an empty string
         * @throws IOException if the action fails while reading or writing files
         */
        void run(String argument) throws IOException;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：一个菜单项：显示文字、可选别名、动作。
     * 对接／验证：hidden 项不编号也不显示，但可以用别名输入；权限仍由服务层检查（例如会员手动输入 users 会被拒绝）。
     *
     * @param label text shown after the number
     * @param alias command word accepted instead of the number, or {@code null}
     * @param hidden whether the option is omitted from the numbered list
     * @param action what to do when selected
     */
    private record Option(String label, String alias, boolean hidden, Action action) {
    }

    private final String title;
    private final String exitLabel;
    private final String exitAlias;
    private final List<Option> options = new ArrayList<>();

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：创建菜单；选项 0 固定为退出／返回。
     * 对接／验证：主菜单用 “Quit/quit”，子页面用 “Back/back”。
     *
     * @param title heading such as {@code Member menu:}
     * @param exitLabel label for option 0
     * @param exitAlias command word for option 0
     */
    public Menu(String title, String exitLabel, String exitAlias) {
        this.title = title;
        this.exitLabel = exitLabel;
        this.exitAlias = exitAlias;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：增加一个显示并编号的选项。
     * 对接／验证：按加入顺序编号 1、2、3…；需要固定编号的共用选项应先加入。
     *
     * @param label text shown after the number
     * @param alias command word accepted instead of the number, or {@code null}
     * @param action what to do when selected
     * @return this menu
     */
    public Menu option(String label, String alias, Action action) {
        options.add(new Option(label, alias, false, action));
        return this;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：增加一个不显示、只能用别名触发的选项（例如访客菜单里的 upload）。
     * 对接／验证：用于保留 Sprint 1 命令词并给出清楚的权限错误，不用于隐藏正常功能。
     *
     * @param alias command word
     * @param action what to do when selected
     * @return this menu
     */
    public Menu hidden(String alias, Action action) {
        options.add(new Option(alias, alias, true, action));
        return this;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：打印标题和 “  1. xxx” 形式的编号选项，最后打印 “  0. Quit/Back”。
     * 对接／验证：ConsoleUiTest/MenuTest 检查这种格式。
     *
     * @param io shared console helper
     */
    public void render(ConsoleIO io) {
        io.info(title);
        List<Option> visible = visibleOptions();
        for (int i = 0; i < visible.size(); i++) {
            io.info("  " + (i + 1) + ". " + visible.get(i).label());
        }
        io.info("  0. " + exitLabel);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：显示菜单、读取一次选择并执行。返回 false 表示用户选了 0（退出／返回）。
     * 动作抛出的 IllegalArgumentException / IllegalStateException / IOException 统一显示为 “Error: …”，
     * 用户留在同一菜单可立即重试。
     * 对接／验证：输入流结束（NoSuchElementException）不在这里吞掉，交给 ConsoleUi.run 结束程序。
     *
     * @param io shared console helper
     * @param prompt prompt such as {@code > } or {@code Browse> }
     * @return false when the exit option was chosen
     */
    public boolean runOnce(ConsoleIO io, String prompt) {
        render(io);
        String raw = io.ask(prompt);
        Selection selection;
        try {
            selection = select(raw);
        } catch (IllegalArgumentException e) {
            io.error(e.getMessage());
            return true;
        }
        if (selection == null) {
            return false;
        }
        try {
            selection.option().action().run(selection.argument());
        } catch (InputCancelledException e) {
            // 用户在表单中输入 cancel：不是错误，回到当前菜单。
            io.info(e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            io.error(e.getMessage());
        }
        return true;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：把输入解析成选项：数字按编号；否则按别名匹配第一个词，剩余部分作为参数。
     * 对接／验证：空输入、越界数字、未知文字都抛出 IllegalArgumentException，消息说明可选范围。
     *
     * @param raw user input
     * @return the selected option, or {@code null} for the exit option
     * @throws IllegalArgumentException if the input does not select an option
     */
    Selection select(String raw) {
        String text = raw == null ? "" : raw.strip();
        int max = visibleOptions().size();
        String range = "Please enter a number between 0 and " + max + ".";
        if (text.isEmpty()) {
            throw new IllegalArgumentException("No option entered. " + range);
        }
        if (text.chars().allMatch(Character::isDigit)) {
            int number;
            try {
                number = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + text + "' is out of range. " + range);
            }
            if (number == 0) {
                return null;
            }
            if (number > max) {
                throw new IllegalArgumentException("'" + text + "' is out of range. " + range);
            }
            return new Selection(visibleOptions().get(number - 1), "");
        }
        String[] parts = text.split("\\s+", 2);
        String word = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].strip() : "";
        if (word.equals(exitAlias)) {
            return null;
        }
        for (Option option : options) {
            if (option.alias() != null && option.alias().equals(word)) {
                return new Selection(option, argument);
            }
        }
        throw new IllegalArgumentException("'" + text + "' is not a valid option. " + range);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：返回需要编号显示的选项。
     * 对接／验证：render 与 select 使用同一份列表，编号不会错位。
     */
    private List<Option> visibleOptions() {
        return options.stream().filter(o -> !o.hidden()).toList();
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：解析结果：选中的项和别名参数。
     * 对接／验证：仅供本类与 MenuTest 使用。
     *
     * @param option selected option
     * @param argument text after an alias
     */
    record Selection(Option option, String argument) {
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：一直运行菜单直到用户选择 0。
     * 对接／验证：子页面（浏览、我的卷轴、资料）直接调用；主菜单因每轮角色可能变化，由 ConsoleUi 每轮重新建菜单。
     *
     * @param io shared console helper
     * @param prompt prompt text
     * @param beforeEachRound printed before every menu display, e.g. the scroll list; may be {@code null}
     */
    public void loop(ConsoleIO io, String prompt, Runnable beforeEachRound) {
        boolean again = true;
        while (again) {
            if (beforeEachRound != null) {
                beforeEachRound.run();
            }
            again = runOnce(io, prompt);
        }
    }
}

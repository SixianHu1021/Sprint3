package org.example.ui;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US14 #49 / US15 #50】主负责人：Yuhan。
 * 做什么：所有界面共用的输入输出工具：读取一行、必填输入立即重问、是/否确认、隐藏密码，
 * 以及统一格式的错误提示 {@code Error: ...}。
 * 对接／验证：Henry 的 AccountScreens、Stella 的 MyScrollsScreen/StatisticsScreen、Sixian 的 BrowseScreen
 * 都只通过本类读写控制台；不要各自新建 Scanner，也不要关闭标准输入。
 */
public final class ConsoleIO {

    /** Word that abandons the current form at any validated prompt. */
    public static final String CANCEL = "cancel";

    /** Shown at the start of multi-field forms. */
    public static final String CANCEL_HINT = "(Type 'cancel' at any prompt to go back.)";

    /** Shown once per password prompt when the terminal cannot hide input. */
    public static final String UNMASKED_WARNING =
            "Warning: this terminal cannot hide passwords; your input will be visible.";

    private final Scanner input;
    private final PrintWriter output;
    private final PasswordReader masked;
    private final boolean warnWhenUnmasked;

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：脚本／测试模式：密码按普通行读取，不打印终端警告，保证测试输出稳定。
     * 对接／验证：Sprint 1 的 ConsoleUi 测试通过这个构造方法继续使用 Scanner 脚本输入。
     *
     * @param input console input
     * @param output console output
     */
    public ConsoleIO(Scanner input, PrintWriter output) {
        this(input, output, null, false);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：完整构造方法；masked 为 null 表示终端不支持隐藏输入。
     * 对接／验证：ConsoleIOTest 传入假的 PasswordReader，验证掩码、回退和警告三种情况。
     *
     * @param input console input
     * @param output console output
     * @param masked hidden-input reader, or {@code null} when unsupported
     * @param warnWhenUnmasked whether to warn before reading a visible password
     */
    public ConsoleIO(Scanner input, PrintWriter output, PasswordReader masked, boolean warnWhenUnmasked) {
        this.input = input;
        this.output = output;
        this.masked = masked;
        this.warnWhenUnmasked = warnWhenUnmasked;
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：真实运行时使用：有终端就用 System.console() 隐藏密码；
     * 没有终端（IDE、Gradle run、管道输入）就回退为普通输入并显示警告。
     * 对接／验证：只由 App.main 调用。演示隐藏密码请用 installDist 生成的启动脚本，而不是 gradlew run。
     *
     * @param input console input
     * @param output console output
     * @return console helper configured for the current process
     */
    public static ConsoleIO system(Scanner input, PrintWriter output) {
        java.io.Console console = System.console();
        PasswordReader reader = console == null ? null : console::readPassword;
        return new ConsoleIO(input, output, reader, true);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：显示提示并读取一整行原始输入（不去空格）。
     * 对接／验证：输入结束时抛出 NoSuchElementException，由 ConsoleUi.run 统一显示 “Input closed.”。
     *
     * @param prompt text displayed before reading
     * @return the entered line
     */
    public String ask(String prompt) {
        output.print(prompt);
        output.flush();
        return input.nextLine();
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：读取必填项；空白输入立即显示 “Error: 字段 cannot be empty.” 并在同一提示重问。
     * 对接／验证：所有表单的 ID、名称、路径等必填项都用它，满足 US14 “先检查、再处理”。
     *
     * @param prompt text displayed before reading
     * @param label field name used in the error message
     * @return trimmed, non-blank input
     */
    public String askRequired(String prompt, String label) {
        while (true) {
            String value = ask(prompt).strip();
            if (!value.isEmpty()) {
                return value;
            }
            error(label + " cannot be empty.");
        }
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 【Sprint 2 · US14 #49 · 逐项检查】主负责人：Yuhan。
     * 做什么：读取一项输入后立刻交给 check 检查；检查抛出 IllegalArgumentException 时显示 “Error: …”，
     * 只重问这一项，不必等整张表填完才报错。输入 cancel 放弃整个表单（抛出 InputCancelledException）。
     * 对接／验证：check 由各业务负责人提供（例如 Henry 的 checkNewIdKey、Stella 的 checkNewScrollId），
     * 返回规范化后的值；IllegalStateException（如未登录）不重问，直接向上抛出。
     *
     * @param prompt text displayed before reading
     * @param check validates and normalizes the raw input
     * @param <T> type of the accepted value
     * @return the first value accepted by {@code check}
     * @throws InputCancelledException if the user types {@code cancel}
     */
    public <T> T askValid(String prompt, Function<String, T> check) {
        while (true) {
            String raw = ask(prompt);
            if (raw.strip().equalsIgnoreCase(CANCEL)) {
                throw new InputCancelledException();
            }
            try {
                return check.apply(raw);
            } catch (IllegalArgumentException e) {
                error(e.getMessage());
            }
        }
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 【Sprint 2 · US14 #49 / US15 #50】主负责人：Yuhan。
     * 做什么：与 askValid 相同，但用隐藏输入读取密码；密码不合规或当前密码错误时立即提示并重问。
     * 对接／验证：注册密码、修改密码使用；登录不使用（登录失败不能透露是 ID 还是密码错）。
     *
     * @param prompt text displayed before reading
     * @param check validates the password and returns it
     * @return the first password accepted by {@code check}
     * @throws InputCancelledException if the user types {@code cancel}
     */
    public String passwordValid(String prompt, Function<String, String> check) {
        while (true) {
            String raw = password(prompt);
            if (raw.strip().equalsIgnoreCase(CANCEL)) {
                throw new InputCancelledException();
            }
            try {
                return check.apply(raw);
            } catch (IllegalArgumentException e) {
                error(e.getMessage());
            }
        }
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：读取可选项并去掉首尾空格；空字符串表示“保持不变／跳过”。
     * 对接／验证：Henry 的资料修改页面用它实现“直接回车保留原值”。
     *
     * @param prompt text displayed before reading
     * @return trimmed input, possibly empty
     */
    public String askOptional(String prompt) {
        return ask(prompt).strip();
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：读取 y/yes/n/no；其他输入显示错误并重问。
     * 对接／验证：Stella 的删除卷轴确认使用它。
     *
     * @param prompt question displayed before reading
     * @return true for yes, false for no
     */
    public boolean confirm(String prompt) {
        while (true) {
            String value = ask(prompt + " (y/n): ").strip().toLowerCase(Locale.ROOT);
            if (value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            error("Please answer y or n.");
        }
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：读取密码。支持隐藏时不回显（退格由终端处理）；不支持时先显示警告再普通读取。
     * 密码只在完整输入后交给服务层检查，不逐键验证。
     * 对接／验证：注册、登录、修改密码三处都调用本方法；读到的字符数组用后立即清零。
     *
     * @param prompt text displayed before reading
     * @return the entered password
     * @throws NoSuchElementException if input closes before a password is entered
     */
    public String password(String prompt) {
        if (masked == null) {
            if (warnWhenUnmasked) {
                output.println(UNMASKED_WARNING);
            }
            return ask(prompt);
        }
        char[] value = masked.readPassword(prompt);
        if (value == null) {
            throw new NoSuchElementException("Input closed");
        }
        try {
            return new String(value);
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：输出一行普通信息。
     * 对接／验证：所有界面共用，保证输出都经过同一个 PrintWriter。
     *
     * @param message text to print
     */
    public void info(String message) {
        output.println(message);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：统一错误格式：总是以 “Error: ” 开头，消息为空时给出通用说明。
     * 对接／验证：US14 要求所有提示语气、格式一致；各界面捕获异常后只调用这里，不自己拼 “Error”。
     *
     * @param message problem description
     */
    public void error(String message) {
        String text = message == null || message.isBlank() ? "Something went wrong. Please try again." : message;
        output.println("Error: " + text);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：把缓冲的输出写到控制台。
     * 对接／验证：ConsoleUi 退出前调用。
     */
    public void flush() {
        output.flush();
    }
}

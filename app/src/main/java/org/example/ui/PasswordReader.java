package org.example.ui;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US15 #50】主负责人：Yuhan。
 * 做什么：定义“隐藏输入读取密码”的插口。真实终端由 {@code System.console()::readPassword} 提供；
 * 测试中用假的实现代替，因此不需要真实终端也能验证掩码路径。
 * 对接／验证：只由 {@link ConsoleIO#password(String)} 调用；其他成员不要直接调用 System.console()。
 */
@FunctionalInterface
public interface PasswordReader {

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * Reads a password without echoing it to the screen.
     *
     * @param prompt text shown before the hidden input
     * @return the entered characters, or {@code null} if input has closed
     */
    char[] readPassword(String prompt);
}

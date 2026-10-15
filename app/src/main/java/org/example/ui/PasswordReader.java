package org.example.ui;

/**
 * 【Sprint 2 · US15 #50】主负责人：Yuhan。审核人：Henry。
 * 做什么：定义“隐藏输入读取密码”的插口。真实终端由 {@code System.console()::readPassword} 提供；
 * 测试中用假的实现代替，因此不需要真实终端也能验证掩码路径。
 * 对接／验证：只由 {@link ConsoleIO#password(String)} 调用；其他成员不要直接调用 System.console()。
 */
@FunctionalInterface
public interface PasswordReader {

    /**
     * Reads a password without echoing it to the screen.
     *
     * @param prompt text shown before the hidden input
     * @return the entered characters, or {@code null} if input has closed
     */
    char[] readPassword(String prompt);
}

package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.example.ui.ConsoleIO;
import org.example.ui.Menu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US13 #48】主负责人：Yuhan。
 * 测什么：编号显示、按数字分派、越界数字、非数字、空输入、别名和别名参数、隐藏选项、动作错误不退出菜单。
 */
class MenuTest {

    private final StringWriter out = new StringWriter();
    private final List<String> calls = new ArrayList<>();

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * Creates a console helper reading the given lines.
     */
    private ConsoleIO io(String... lines) {
        return new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * Builds a three-option menu that records which action ran.
     */
    private Menu menu() {
        return new Menu("Test menu:", "Quit", "quit")
                .option("First", "first", a -> calls.add("first:" + a))
                .option("Second", "second", a -> calls.add("second:" + a))
                .hidden("secret", a -> calls.add("secret:" + a))
                .option("Fails", "fail", a -> {
                    throw new IllegalStateException("Action failed");
                });
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void rendersNumberedOptionsWithExitAsZero() {
        menu().render(io());
        String text = out.toString();
        assertTrue(text.contains("Test menu:"));
        assertTrue(text.contains("  1. First"));
        assertTrue(text.contains("  2. Second"));
        assertTrue(text.contains("  3. Fails"));
        assertTrue(text.contains("  0. Quit"));
        assertFalse(text.contains("secret"));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void numbersDispatchToTheMatchingAction() {
        Menu menu = menu();
        ConsoleIO io = io("2", "1", "0");
        assertTrue(menu.runOnce(io, "> "));
        assertTrue(menu.runOnce(io, "> "));
        assertFalse(menu.runOnce(io, "> "));
        assertEquals(List.of("second:", "first:"), calls);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void outOfRangeAndNonNumericInputShowErrorsWithoutRunning() {
        Menu menu = menu();
        ConsoleIO io = io("7", "abc", "", "   ", "99999999999999999999");
        for (int i = 0; i < 5; i++) {
            assertTrue(menu.runOnce(io, "> "));
        }
        String text = out.toString();
        assertTrue(text.contains("Error: '7' is out of range. Please enter a number between 0 and 3."));
        assertTrue(text.contains("Error: 'abc' is not a valid option. Please enter a number between 0 and 3."));
        assertTrue(text.contains("Error: No option entered. Please enter a number between 0 and 3."));
        assertTrue(text.contains("Error: '99999999999999999999' is out of range."));
        assertTrue(calls.isEmpty());
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void aliasesAndArgumentsAreAcceptedCaseInsensitively() {
        Menu menu = menu();
        ConsoleIO io = io("FIRST s1", "secret", "quit");
        assertTrue(menu.runOnce(io, "> "));
        assertTrue(menu.runOnce(io, "> "));
        assertFalse(menu.runOnce(io, "> "));
        assertEquals(List.of("first:s1", "secret:"), calls);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void failingActionShowsErrorAndKeepsMenuOpen() {
        Menu menu = menu();
        assertTrue(menu.runOnce(io("3"), "> "));
        assertTrue(out.toString().contains("Error: Action failed"));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void loopRedisplaysAfterEachActionUntilExit() {
        int[] rounds = {0};
        menu().loop(io("1", "2", "0"), "> ", () -> rounds[0]++);
        assertEquals(3, rounds[0]);
        assertEquals(3, out.toString().split("Test menu:", -1).length - 1);
    }
}

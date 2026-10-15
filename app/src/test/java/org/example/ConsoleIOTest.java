package org.example;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.example.service.Validation;
import org.example.ui.ConsoleIO;
import org.example.ui.PasswordReader;
import org.example.ui.InputCancelledException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US14 #49 / US15 #50】主负责人：Yuhan。
 * 测什么：必填输入立即重问、统一错误格式、y/n 确认、Validation 的具体消息；
 * 密码走隐藏读取且不回显、字符数组被清零、无终端时显示警告、输入关闭时正确结束。
 */
class ConsoleIOTest {

    private final StringWriter out = new StringWriter();

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * Creates a script-mode helper.
     */
    private ConsoleIO io(String... lines) {
        return new ConsoleIO(new Scanner(String.join("\n", lines) + "\n"), new PrintWriter(out, true));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void requiredInputRepromptsOnBlankAndTrims() {
        String value = io("", "   ", "  s1  ").askRequired("Scroll ID: ", "Scroll ID");
        assertEquals("s1", value);
        assertEquals(2, out.toString().split("Error: Scroll ID cannot be empty.", -1).length - 1);
        assertEquals(3, out.toString().split("Scroll ID: ", -1).length - 1);
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void optionalInputMayBeEmpty() {
        assertEquals("", io("   ").askOptional("Name: "));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void confirmationAcceptsYesNoAndRetriesOtherInput() {
        ConsoleIO io = io("maybe", "YES", "n");
        assertTrue(io.confirm("Delete?"));
        assertFalse(io.confirm("Delete?"));
        assertTrue(out.toString().contains("Error: Please answer y or n."));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void errorsUseOneSharedFormat() {
        ConsoleIO io = io();
        io.error("ID key cannot be empty");
        io.error(null);
        String text = out.toString();
        assertTrue(text.contains("Error: ID key cannot be empty"));
        assertTrue(text.contains("Error: Something went wrong. Please try again."));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void validationMessagesExplainTheProblem() {
        assertEquals("ID key cannot be empty",
                assertThrows(IllegalArgumentException.class, () -> Validation.text(" ", "ID key")).getMessage());
        assertEquals("Name must be at most 200 characters",
                assertThrows(IllegalArgumentException.class,
                        () -> Validation.text("x".repeat(201), "Name")).getMessage());
        assertEquals("Name cannot contain control characters",
                assertThrows(IllegalArgumentException.class,
                        () -> Validation.text("a" + (char) 7, "Name")).getMessage());
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void maskedPasswordIsReadWithoutEchoAndCleared() {
        char[][] handed = new char[1][];
        PasswordReader hidden = prompt -> {
            handed[0] = "s3cret-pass".toCharArray();
            return handed[0];
        };
        ConsoleIO io = new ConsoleIO(new Scanner(""), new PrintWriter(out, true), hidden, true);
        assertEquals("s3cret-pass", io.password("Password: "));
        assertFalse(out.toString().contains("s3cret-pass"));
        assertFalse(out.toString().contains(ConsoleIO.UNMASKED_WARNING));
        assertNotNull(handed[0]);
        assertTrue(new String(handed[0]).chars().allMatch(c -> c == 0));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void closedHiddenInputEndsTheSession() {
        ConsoleIO io = new ConsoleIO(new Scanner(""), new PrintWriter(out, true), prompt -> null, true);
        assertThrows(NoSuchElementException.class, () -> io.password("Password: "));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void terminalWithoutMaskingShowsWarningThenReadsLine() {
        ConsoleIO io = new ConsoleIO(new Scanner("visible123\n"), new PrintWriter(out, true), null, true);
        assertEquals("visible123", io.password("Password: "));
        assertTrue(out.toString().contains(ConsoleIO.UNMASKED_WARNING));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void scriptModeReadsPasswordWithoutWarning() {
        assertEquals("abc12345", io("abc12345").password("Password: "));
        assertFalse(out.toString().contains(ConsoleIO.UNMASKED_WARNING));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void systemFactoryCreatesUsableHelper() {
        ConsoleIO io = ConsoleIO.system(new Scanner("line\n"), new PrintWriter(out, true));
        assertEquals("line", io.ask("> "));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void askValidRetriesUntilAcceptedAndPasswordValidCancels() {
        ConsoleIO io = io("1", "2", "CANCEL");
        int value = io.askValid("Number: ", v -> {
            if (!v.equals("2")) {
                throw new IllegalArgumentException("Not two");
            }
            return 2;
        });
        assertEquals(2, value);
        assertTrue(out.toString().contains("Error: Not two"));
        assertThrows(InputCancelledException.class, () -> io.passwordValid("Password: ", v -> v));
    }

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     */
    @Test
    void askValidCancelsAndPasswordValidRetries() {
        ConsoleIO io = io("cancel", "short", "longenough");
        assertThrows(InputCancelledException.class, () -> io.askValid("Name: ", v -> v));
        String password = io.passwordValid("Password: ", v -> {
            if (v.length() < 8) {
                throw new IllegalArgumentException("Too short");
            }
            return v;
        });
        assertEquals("longenough", password);
        assertTrue(out.toString().contains("Error: Too short"));
    }
}

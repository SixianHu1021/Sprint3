package org.example.service;

/**
 * Validates required profile and scroll text. Passwords require separate validation.
 *
 * <p>【Sprint 2 · US14 #49】主负责人：Yuhan（本轮修改错误消息）。审核人：Henry（Sprint 1 原作者）。
 * 做什么：把一条笼统的错误拆成三种具体说明：为空、过长、含控制字符，让用户知道哪里错了。
 * 对接／验证：校验规则本身（1–200 字符、去首尾空格）与 Sprint 1 完全相同；RegistrationTest 仍然通过，
 * 新增的 InputFeedbackTest 检查每种消息文字。
 */
public final class Validation {
    /** Longest accepted text field. */
    public static final int MAX_LENGTH = 200;

    /**
     * Prevents utility class instantiation.
     */
    private Validation() {
    }

    /**
     * Rejects null, blank, overlong or control-character input, then strips whitespace.
     *
     * <p>主负责人：Yuhan（Sprint 2 消息）／Henry（Sprint 1 规则）。
     * 做什么：按顺序检查空白 → 长度 → 控制字符，抛出具体消息，例如 “ID key cannot be empty”。
     * 对接／验证：AccountService、ScrollService、ScrollEditService 都调用本方法，消息格式因此一致。
     *
     * @param value original input, limited to 200 characters
     * @param label field label for error messages
     * @return normalized text
     * @throws IllegalArgumentException if input is invalid
     */
    public static String text(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(label + " must be at most " + MAX_LENGTH + " characters");
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(label + " cannot contain control characters");
        }
        return value.strip();
    }
}

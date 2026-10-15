package org.example.ui;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
 *
 * 【Sprint 2 · US14 #49】主负责人：Yuhan。
 * 做什么：用户在任何逐项检查的提示中输入 cancel 时抛出，表示放弃当前表单（注册、上传、修改资料等）。
 * 对接／验证：Menu.runOnce 捕获后显示 “Cancelled.” 并回到原菜单；首次管理员设置会提示必须完成并重新开始。
 */
public final class InputCancelledException extends RuntimeException {

    /**
     * 【PR s2-r1-console-io】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：创建取消信号。
     */
    public InputCancelledException() {
        super("Cancelled.");
    }
}

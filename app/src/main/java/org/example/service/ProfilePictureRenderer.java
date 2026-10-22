package org.example.service;

import java.util.ArrayList;
import java.util.List;
import org.example.model.User;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 3 · US20 #68】主负责人：Sixian（预览渲染负责人）。
 * 做什么：像预览图片卷轴一样生成头像的信息预览：格式、字节大小、图片像素尺寸。
 * 没有头像时返回一条清楚的说明，而不是报错。
 * 对接/验证：复用 Sprint 2 的 ScrollPreviewRenderer 的图片渲染（同样用 ImageIO 读尺寸），
 * 保证头像预览和图片卷轴预览看起来一致；界面由 Yuhan 的 ProfilePictureScreen 调用。
 */
public final class ProfilePictureRenderer {

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * Prevents utility class instantiation.
     */
    private ProfilePictureRenderer() {
    }

    /**
     * 【PR s3-r3-profile-picture-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：返回头像预览的多行文本：格式说明、大小，以及（可解析时）像素尺寸。
     * 对接/验证：和图片卷轴预览使用同样的格式说明（FileFormat.describe）和尺寸渲染（ScrollPreviewRenderer.appendImageDetails）。
     *
     * @param user the user whose profile picture is previewed
     * @return lines to print; a single clear line when there is no picture
     */
    public static List<String> render(User user) {
        List<String> lines = new ArrayList<>();
        if (user == null || !user.hasProfilePicture()) {
            lines.add("No profile picture has been added yet.");
            return lines;
        }
        byte[] content = user.getProfilePicture();
        lines.add("Format: " + FileFormat.describe(user.getProfilePictureFormat()));
        lines.add("Size: " + content.length + " bytes");
        // 复用图片卷轴的尺寸渲染，保证头像预览和图片卷轴预览一致（同样用 ImageIO 读像素尺寸）。
        ScrollPreviewRenderer.appendImageDetails(content, lines);
        return lines;
    }
}

package org.example.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.example.model.DigitalScroll;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
 *
 * 【Sprint 2 · US17 #52】主负责人：Sixian。
 * 做什么：按文件格式生成预览内容（只读取，不执行文件，不改变下载次数）：
 * 文本显示前 10 行；图片显示像素尺寸；PDF 显示版本和大约页数；ZIP 列出前 10 个条目；
 * 其他或 unknown：内容是文本就显示前几行（不必列出所有文本扩展名），否则显示前 32 字节十六进制。
 * 对接／验证：BrowseScreen 的预览页调用 render；ScrollFormatTest 为每种格式准备样例文件。
 */
public final class ScrollPreviewRenderer {

    /** Number of text lines or archive entries shown. */
    public static final int MAX_LINES = 10;
    private static final int MAX_LINE_LENGTH = 120;
    private static final int HEX_BYTES = 32;
    private static final Pattern PDF_PAGE = Pattern.compile("/Type\\s*/Page(?![a-zA-Z])");

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * Prevents utility class instantiation.
     */
    private ScrollPreviewRenderer() {
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：返回格式说明、大小和按格式生成的内容预览，每个元素是一行输出。
     * 对接／验证：访客和登录用户都可调用；内容为空时显示 “(empty file)”。
     *
     * @param scroll scroll to preview
     * @return lines to print
     */
    public static List<String> render(DigitalScroll scroll) {
        byte[] content = scroll.getContent();
        List<String> lines = new ArrayList<>();
        lines.add("Format: " + FileFormat.describe(scroll.getFileFormat()));
        lines.add("Size: " + content.length + " bytes");
        if (content.length == 0) {
            lines.add("Content preview: (empty file)");
            return lines;
        }
        switch (FileFormat.category(scroll.getFileFormat())) {
            case TEXT -> text(content, lines);
            case IMAGE -> image(content, lines);
            case PDF -> pdf(content, lines);
            case ARCHIVE -> archive(content, lines);
            // 不认识的扩展名（如 .aux、.log、.tex）：按内容判断，是文本就显示前几行，否则十六进制。
            default -> {
                if (FileFormat.isText(content)) {
                    text(content, lines);
                } else {
                    hex(content, lines);
                }
            }
        }
        return lines;
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：显示前 10 行文本，每行最多 120 字符；超过时说明还有多少行。
     * 对接／验证：扩展名是 txt 但内容不是文本时改为十六进制预览。
     */
    private static void text(byte[] content, List<String> lines) {
        if (!FileFormat.isText(content)) {
            hex(content, lines);
            return;
        }
        // 文件末尾的换行不算作额外的一行。
        String[] all = new String(content, StandardCharsets.UTF_8).split("\\R");
        int shown = Math.min(MAX_LINES, all.length);
        lines.add("Content preview (first " + shown + " lines):");
        for (int i = 0; i < shown; i++) {
            String line = all[i];
            lines.add("  | " + (line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) + "..." : line));
        }
        if (all.length > shown) {
            lines.add("  ... (" + (all.length - shown) + " more lines)");
        }
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：用 ImageIO 读取图片尺寸；无法解析时说明详情不可用，再显示十六进制开头。
     * 对接／验证：测试用 ImageIO 生成 3x2 的 PNG 验证。
     */
    private static void image(byte[] content, List<String> lines) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image != null) {
                lines.add("Image size: " + image.getWidth() + " x " + image.getHeight() + " pixels");
                return;
            }
        } catch (IOException | RuntimeException e) {
            // Fall through to the generic preview below.
        }
        lines.add("Image details unavailable.");
        hex(content, lines);
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：读取 “%PDF-x.y” 版本号，并统计 /Type /Page 出现次数作为大约页数。
     * 对接／验证：只做文本扫描，不解析或执行 PDF 内容。
     */
    private static void pdf(byte[] content, List<String> lines) {
        String raw = new String(content, StandardCharsets.ISO_8859_1);
        if (raw.startsWith("%PDF-") && raw.length() >= 8) {
            lines.add("PDF version: " + raw.substring(5, 8));
        } else {
            lines.add("PDF header not found.");
        }
        Matcher matcher = PDF_PAGE.matcher(raw);
        int pages = 0;
        while (matcher.find()) {
            pages++;
        }
        lines.add("Pages (approx.): " + pages);
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian。
     * 做什么：ZIP／JAR 列出前 10 个条目名称；其他压缩格式或损坏文件显示十六进制。
     * 对接／验证：只读取条目名称，不解压文件到磁盘。
     */
    private static void archive(byte[] content, List<String> lines) {
        List<String> names = new ArrayList<>();
        int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                total++;
                if (names.size() < MAX_LINES) {
                    names.add(entry.getName());
                }
            }
        } catch (IOException e) {
            total = 0;
        }
        if (total == 0) {
            hex(content, lines);
            return;
        }
        lines.add("Archive contents (" + total + " entries):");
        names.forEach(name -> lines.add("  - " + name));
        if (total > names.size()) {
            lines.add("  ... (" + (total - names.size()) + " more entries)");
        }
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 主负责人：Sixian（沿用 Sprint 1 预览思路）。
     * 做什么：显示前 32 字节的十六进制。
     * 对接／验证：用于 unknown、普通二进制和无法解析的文件。
     */
    private static void hex(byte[] content, List<String> lines) {
        lines.add("First " + Math.min(HEX_BYTES, content.length) + " bytes (hex): "
                + HexFormat.ofDelimiter(" ").formatHex(content, 0, Math.min(HEX_BYTES, content.length)));
    }
}

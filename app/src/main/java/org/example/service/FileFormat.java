package org.example.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import org.example.model.DigitalScroll;

/**
 * 【Sprint 2 · US17 #52】主负责人：Sixian。审核人：Stella。
 * 做什么：判断卷轴的文件格式并分类（文本、图片、PDF、压缩包、其他二进制）。
 * 优先使用上传文件的扩展名；没有扩展名时检查文件开头的“魔数”；仍无法判断就是 unknown。
 * 对接／验证：ScrollService.upload（Sixian 在 R2 接入）和 Stella 的 ScrollEditService.replaceContent 都调用 detect；
 * ScrollPreviewRenderer 用 category 选择预览方式。
 */
public final class FileFormat {

    /** How a format should be previewed. */
    public enum Category { TEXT, IMAGE, PDF, ARCHIVE, BINARY, UNKNOWN }

    private static final Set<String> TEXT = Set.of("txt", "md", "csv", "json", "xml", "html", "htm",
            "java", "py", "c", "cpp", "h", "js", "ts", "css", "log", "yml", "yaml", "properties",
            "sh", "sql", "tex", "gradle");
    private static final Set<String> IMAGE = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");
    private static final Set<String> ARCHIVE = Set.of("zip", "jar", "gz", "tar", "7z", "rar");

    /**
     * Prevents utility class instantiation.
     */
    private FileFormat() {
    }

    /**
     * 主负责人：Sixian。
     * 做什么：先看文件名扩展名（1–10 位字母数字，转小写），否则看内容开头，最后判断是否为 UTF-8 文本。
     * 对接／验证：FileFormatTest 覆盖扩展名、大小写、无扩展名的 PDF/PNG/JPG/GIF/ZIP、文本和 unknown。
     *
     * @param fileName original file name, may be {@code null}
     * @param content file bytes, may be {@code null}
     * @return lowercase format, or {@code unknown}
     */
    public static String detect(String fileName, byte[] content) {
        String extension = extension(fileName);
        if (extension != null) {
            return extension;
        }
        byte[] bytes = content == null ? new byte[0] : content;
        if (startsWith(bytes, '%', 'P', 'D', 'F')) {
            return "pdf";
        }
        if (startsWith(bytes, 0x89, 'P', 'N', 'G')) {
            return "png";
        }
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (startsWith(bytes, 'G', 'I', 'F', '8')) {
            return "gif";
        }
        if (startsWith(bytes, 'P', 'K', 3, 4)) {
            return "zip";
        }
        if (bytes.length > 0 && isText(bytes)) {
            return "txt";
        }
        return DigitalScroll.UNKNOWN_FORMAT;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：把格式归类，决定预览方式。
     * 对接／验证：ScrollPreviewRenderer 使用。
     *
     * @param format lowercase format
     * @return preview category
     */
    public static Category category(String format) {
        String value = format == null ? "" : format.toLowerCase(Locale.ROOT);
        if (value.isEmpty() || value.equals(DigitalScroll.UNKNOWN_FORMAT)) {
            return Category.UNKNOWN;
        }
        if (TEXT.contains(value)) {
            return Category.TEXT;
        }
        if (IMAGE.contains(value)) {
            return Category.IMAGE;
        }
        if (value.equals("pdf")) {
            return Category.PDF;
        }
        if (ARCHIVE.contains(value)) {
            return Category.ARCHIVE;
        }
        return Category.BINARY;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：生成给用户看的格式说明，例如 “PDF document (.pdf)”；无法判断时显示 “unknown”。
     * 对接／验证：浏览列表和预览页使用。
     *
     * @param format lowercase format
     * @return readable description
     */
    public static String describe(String format) {
        return switch (category(format)) {
            case TEXT -> "Text file (." + format + ")";
            case IMAGE -> "Image (." + format + ")";
            case PDF -> "PDF document (.pdf)";
            case ARCHIVE -> "Archive (." + format + ")";
            case BINARY -> "Binary file (." + format + ")";
            case UNKNOWN -> DigitalScroll.UNKNOWN_FORMAT;
        };
    }

    /**
     * 主负责人：Sixian。
     * 做什么：判断内容能否作为 UTF-8 文本显示（合法编码，且除换行／制表符外没有控制字符）。
     * 对接／验证：detect 与 ScrollPreviewRenderer 共用。
     *
     * @param bytes content to check
     * @return true if the bytes are printable UTF-8 text
     */
    public static boolean isText(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return text.chars().noneMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t');
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    /**
     * 主负责人：Sixian。
     * 做什么：取出文件名最后一个点后的扩展名；路径分隔符之后才算文件名。
     * 对接／验证：“.gitignore”、“archive.”、超过 10 位或含符号的扩展名都视为没有扩展名。
     */
    private static String extension(String fileName) {
        if (fileName == null) {
            return null;
        }
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return null;
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return extension.matches("[a-z0-9]{1,10}") ? extension : null;
    }

    /**
     * 主负责人：Sixian。
     * 做什么：比较文件开头的字节（魔数）。
     * 对接／验证：仅供 detect 使用。
     */
    private static boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

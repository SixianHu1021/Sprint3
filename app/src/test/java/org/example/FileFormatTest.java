package org.example;

import org.example.service.FileFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Sprint 2 · US17 #52】主负责人：Sixian。审核人：Stella。
 * 测什么：扩展名优先且转小写；路径和隐藏文件；无扩展名时按魔数识别 PDF/PNG/JPG/GIF/ZIP；
 * 纯文本识别为 txt；无法判断为 unknown；分类与描述文字。
 */
class FileFormatTest {

    @Test
    void extensionWinsAndIsLowercased() {
        assertEquals("pdf", FileFormat.detect("Report.PDF", new byte[]{1, 2}));
        assertEquals("jpeg", FileFormat.detect("/tmp/dir.v2/photo.jpeg", null));
        assertEquals("txt", FileFormat.detect("C:\\notes\\a.TXT", null));
    }

    @Test
    void invalidExtensionsFallBackToContent() {
        assertEquals("txt", FileFormat.detect(".gitignore", "build/".getBytes()));
        assertEquals("unknown", FileFormat.detect("archive.", new byte[]{0, 1, 2}));
        assertEquals("unknown", FileFormat.detect("weird.ext-with-dash", new byte[]{0}));
        assertEquals("unknown", FileFormat.detect("x.abcdefghijk", null));
    }

    @Test
    void magicNumbersIdentifyCommonFormats() {
        assertEquals("pdf", FileFormat.detect("noext", "%PDF-1.7".getBytes()));
        assertEquals("png", FileFormat.detect("noext", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0}));
        assertEquals("jpg", FileFormat.detect("noext", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals("gif", FileFormat.detect("noext", "GIF89a".getBytes()));
        assertEquals("zip", FileFormat.detect("noext", new byte[]{'P', 'K', 3, 4}));
        assertEquals("txt", FileFormat.detect(null, "plain text\nline two\t!".getBytes()));
        assertEquals("unknown", FileFormat.detect(null, new byte[]{0, 1, 2, 3}));
        assertEquals("unknown", FileFormat.detect(null, null));
        assertEquals("unknown", FileFormat.detect(null, new byte[]{(byte) 0xC3}));
    }

    @Test
    void categoriesAndDescriptions() {
        assertEquals(FileFormat.Category.TEXT, FileFormat.category("md"));
        assertEquals(FileFormat.Category.IMAGE, FileFormat.category("PNG"));
        assertEquals(FileFormat.Category.PDF, FileFormat.category("pdf"));
        assertEquals(FileFormat.Category.ARCHIVE, FileFormat.category("zip"));
        assertEquals(FileFormat.Category.BINARY, FileFormat.category("bin"));
        assertEquals(FileFormat.Category.UNKNOWN, FileFormat.category(null));
        assertEquals(FileFormat.Category.UNKNOWN, FileFormat.category("unknown"));
        assertEquals("PDF document (.pdf)", FileFormat.describe("pdf"));
        assertEquals("Text file (.txt)", FileFormat.describe("txt"));
        assertEquals("Image (.png)", FileFormat.describe("png"));
        assertEquals("Archive (.zip)", FileFormat.describe("zip"));
        assertEquals("Binary file (.bin)", FileFormat.describe("bin"));
        assertEquals("unknown", FileFormat.describe("unknown"));
    }

    @Test
    void textDetection() {
        assertTrue(FileFormat.isText("héllo\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertFalse(FileFormat.isText(new byte[]{'a', 0}));
    }
}

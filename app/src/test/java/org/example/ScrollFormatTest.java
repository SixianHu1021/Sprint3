package org.example;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.example.model.DigitalScroll;
import org.example.repository.file.FileScrollRepository;
import org.example.repository.file.FileUserRepository;
import org.example.security.PasswordHasher;
import org.example.service.AccountService;
import org.example.service.ScrollPreviewRenderer;
import org.example.service.ScrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 【Sprint 2 · US17 #52】主负责人：Sixian。审核人：Stella。
 * 测什么：上传时记录格式并在重启后保留；下载自动补正确扩展名、已有扩展名不重复、unknown 不改名、
 * 下载后格式仍在；预览按格式显示文本行、图片尺寸、PDF 信息、ZIP 条目、十六进制和空文件。
 */
class ScrollFormatTest {

    private static final PasswordHasher HASHER = new PasswordHasher() {
        @Override
        public String hash(String password) {
            return "test-only:" + password;
        }

        @Override
        public boolean verify(String password, String encoded) {
            return password != null && hash(password).equals(encoded);
        }
    };

    @TempDir
    Path directory;

    private FileScrollRepository repository;
    private ScrollService scrolls;

    @BeforeEach
    void setUp() {
        AccountService accounts = new AccountService(
                new FileUserRepository(directory.resolve("users.properties")), HASHER);
        repository = new FileScrollRepository(directory.resolve("scrolls.properties"));
        scrolls = new ScrollService(repository, accounts, Clock.systemUTC());
        accounts.register("m", "member", "password123", "Member", "member@example.com", "456");
        accounts.login("m", "password123");
    }

    /** Creates an in-memory scroll with the given format. */
    private static DigitalScroll scroll(byte[] content, String format) {
        return new DigitalScroll("x", "X", content, "m", LocalDateTime.now(), 1, 0, format);
    }

    @Test
    void uploadRecordsFormatAndItSurvivesRestart() throws IOException {
        scrolls.upload("s1", "Report", Files.writeString(directory.resolve("report.PDF"), "%PDF-1.7"));
        scrolls.upload("s2", "Blob", Files.write(directory.resolve("blob"), new byte[]{0, 1}));
        FileScrollRepository restored = new FileScrollRepository(directory.resolve("scrolls.properties"));
        assertEquals("pdf", restored.findById("s1").orElseThrow().getFileFormat());
        assertEquals("unknown", restored.findById("s2").orElseThrow().getFileFormat());
    }

    @Test
    void downloadAddsMissingExtensionAndKeepsFormat() throws IOException {
        byte[] content = "%PDF-1.7 body".getBytes(StandardCharsets.ISO_8859_1);
        scrolls.upload("s1", "Report", Files.write(directory.resolve("report.pdf"), content));
        Path saved = scrolls.download("s1", directory.resolve("my-copy"));
        assertEquals(directory.resolve("my-copy.pdf"), saved);
        assertArrayEquals(content, Files.readAllBytes(saved));
        assertEquals(directory.resolve("Already.PDF"), scrolls.download("s1", directory.resolve("Already.PDF")));
        assertEquals(directory.resolve("wrong.txt.pdf"), scrolls.download("s1", directory.resolve("wrong.txt")));
        DigitalScroll stored = repository.findById("s1").orElseThrow();
        assertEquals("pdf", stored.getFileFormat());
        assertEquals(3, stored.getDownloadCount());
    }

    @Test
    void jpegAliasesAndUnknownFormatsKeepTheChosenName() throws IOException {
        scrolls.upload("j", "Photo", Files.write(directory.resolve("photo.jpg"), new byte[]{1}));
        assertEquals(directory.resolve("out.jpeg"), scrolls.download("j", directory.resolve("out.jpeg")));
        scrolls.upload("u", "Blob", Files.write(directory.resolve("blob"), new byte[]{0}));
        assertEquals(directory.resolve("plain"), scrolls.download("u", directory.resolve("plain")));
    }

    @Test
    void folderDestinationIsRejected() throws IOException {
        scrolls.upload("s1", "Notes", Files.writeString(directory.resolve("n.txt"), "x"));
        assertTrue(org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                () -> scrolls.download("s1", directory)).getMessage().contains("not a folder"));
        assertEquals(0, repository.findById("s1").orElseThrow().getDownloadCount());
    }

    @Test
    void textPreviewShowsFirstLines() {
        String text = String.join("\n", "l1", "l2", "l3", "l4", "l5", "l6", "l7", "l8", "l9", "l10", "l11",
                "x".repeat(150));
        List<String> lines = ScrollPreviewRenderer.render(scroll(text.getBytes(StandardCharsets.UTF_8), "txt"));
        assertEquals("Format: Text file (.txt)", lines.get(0));
        assertTrue(lines.contains("Content preview (first 10 lines):"));
        assertTrue(lines.contains("  | l1"));
        assertTrue(lines.contains("  ... (2 more lines)"));
        List<String> longLine = ScrollPreviewRenderer.render(scroll(("y".repeat(130)).getBytes(), "md"));
        assertTrue(longLine.contains("  | " + "y".repeat(120) + "..."));
    }

    @Test
    void nonTextContentWithTextExtensionFallsBackToHex() {
        List<String> lines = ScrollPreviewRenderer.render(scroll(new byte[]{0, 1}, "txt"));
        assertTrue(lines.contains("First 2 bytes (hex): 00 01"));
    }

    @Test
    void imagePreviewShowsDimensions() throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB), "png", png);
        assertTrue(ScrollPreviewRenderer.render(scroll(png.toByteArray(), "png"))
                .contains("Image size: 3 x 2 pixels"));
        List<String> broken = ScrollPreviewRenderer.render(scroll(new byte[]{1, 2, 3}, "png"));
        assertTrue(broken.contains("Image details unavailable."));
    }

    @Test
    void pdfPreviewShowsVersionAndPages() {
        String pdf = "%PDF-1.7\n1 0 obj << /Type /Pages >>\n2 0 obj << /Type /Page >>\n3 0 obj << /Type/Page >>";
        List<String> lines = ScrollPreviewRenderer.render(scroll(pdf.getBytes(StandardCharsets.ISO_8859_1), "pdf"));
        assertTrue(lines.contains("PDF version: 1.7"));
        assertTrue(lines.contains("Pages (approx.): 2"));
        assertTrue(ScrollPreviewRenderer.render(scroll("junk".getBytes(), "pdf")).contains("PDF header not found."));
    }

    @Test
    void zipPreviewListsEntries() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 1; i <= 12; i++) {
                zip.putNextEntry(new ZipEntry("file" + i + ".txt"));
                zip.closeEntry();
            }
        }
        List<String> lines = ScrollPreviewRenderer.render(scroll(bytes.toByteArray(), "zip"));
        assertTrue(lines.contains("Archive contents (12 entries):"));
        assertTrue(lines.contains("  - file1.txt"));
        assertTrue(lines.contains("  ... (2 more entries)"));
        assertTrue(ScrollPreviewRenderer.render(scroll(new byte[]{9, 9}, "zip")).contains("First 2 bytes (hex): 09 09"));
    }

    @Test
    void unknownAndEmptyPreviews() {
        List<String> unknown = ScrollPreviewRenderer.render(scroll(new byte[40], "unknown"));
        assertEquals("Format: unknown", unknown.get(0));
        assertTrue(unknown.get(2).startsWith("First 32 bytes (hex): 00 00"));
        assertTrue(ScrollPreviewRenderer.render(scroll(new byte[0], "txt")).contains("Content preview: (empty file)"));
    }
}

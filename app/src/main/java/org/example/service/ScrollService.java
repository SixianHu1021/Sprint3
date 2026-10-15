package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.example.model.DigitalScroll;
import org.example.model.User;
import org.example.repository.ScrollRepository;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
 *   - 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
 *   - Sprint 1 原作者：Stella（upload）、Sixian（browse/preview/download）（未标 PR 的方法本轮不改）
 *
 * Provides scroll operations on top of a scroll repository.
 * Browsing and previewing are available without login; uploading requires login.
 *
 * <p>Sprint 2 变更（browse/find/preview 为 Sprint 1 原有，本轮不改）：
 * <ul>
 *   <li>R1 · Stella：download 改用 DigitalScroll.withRecordedDownload()，避免下载后丢失格式。</li>
 *   <li>R1 · Stella【US14 #49 逐项检查】：checkNewScrollId、checkNewScrollName、checkSource，
 *       上传表单每输入一项就检查。</li>
 *   <li>R2 · Sixian【US17 #52】：upload 记录文件格式；download 自动补正确扩展名并返回实际保存路径。</li>
 * </ul>
 * 编辑、删除、统计、筛选、排序都放在新的服务类中，避免多人反复修改本文件。
 */
public final class ScrollService {

    /** Largest accepted scroll size in bytes (10 MiB). */
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    private final ScrollRepository scrolls;
    private final AccountService accounts;
    private final Clock clock;

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Creates the service with the shared scroll repository, account service and clock.
     * @param scrolls repository used by scroll operations
     * @param accounts account service used to identify the logged-in user
     * @param clock clock used to record upload dates
     */
    public ScrollService(ScrollRepository scrolls, AccountService accounts, Clock clock) {
        this.scrolls = Objects.requireNonNull(scrolls, "scrolls");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US14 #49 逐项检查】主负责人：Stella。
     * 做什么：上传表单输入卷轴 ID 后立即检查格式和是否已被使用。
     * 对接／验证：ConsoleUi.upload 用 ConsoleIO.askValid 调用；upload 仍做最终检查。
     *
     * @param id raw scroll ID
     * @return normalized scroll ID
     * @throws IllegalArgumentException if the ID is invalid or already used
     */
    public String checkNewScrollId(String id) {
        String value = Validation.text(id, "Scroll ID");
        if (scrolls.existsById(value)) {
            throw new IllegalArgumentException("Scroll ID '" + value + "' is already taken");
        }
        return value;
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US14 #49 逐项检查】主负责人：Stella。
     * 做什么：输入卷轴名称后立即检查格式和是否已被使用；Stella 的改名也复用。
     *
     * @param name raw scroll name
     * @return normalized scroll name
     * @throws IllegalArgumentException if the name is invalid or already used
     */
    public String checkNewScrollName(String name) {
        String value = Validation.text(name, "Scroll name");
        if (scrolls.existsByName(value)) {
            throw new IllegalArgumentException("Scroll name '" + value + "' is already taken");
        }
        return value;
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US14 #49 逐项检查】主负责人：Stella。
     * 做什么：输入文件路径后立即检查：不能为空、必须是存在的普通文件、不能超过 10 MiB。
     * 对接／验证：上传和 “替换文件” 共用；读取内容仍在 upload / replaceContent 中进行。
     *
     * @param path raw file path typed by the user
     * @return the checked path
     * @throws IllegalArgumentException if the path is blank, missing, not a regular file or too large
     */
    public static Path checkSource(String path) {
        String text = Validation.text(path, "Source file path");
        Path source;
        try {
            source = Path.of(text);
        } catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("Source file path is not valid: " + text);
        }
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("No readable file found at: " + text);
        }
        try {
            if (Files.size(source) > MAX_BYTES) {
                throw new IllegalArgumentException("Scroll exceeds the 10 MiB limit");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read file: " + text);
        }
        return source;
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella；Sprint 1 原作者：Stella
     * Uploads a local file as a new scroll owned by the logged-in user.
     * The new scroll records the uploader's ID key, the current date,
     * an upload count of 1 and a download count of 0.
     *
     * <p>主负责人：Stella（Sprint 1）；格式记录：Sixian（Sprint 2 · US17）。
     *
     * @param id unique scroll ID; surrounding whitespace is removed
     * @param name unique scroll name; surrounding whitespace is removed
     * @param source path of the local file to upload
     * @return the saved scroll
     * @throws IllegalStateException if no user is logged in, or saving fails
     * @throws IllegalArgumentException if the ID or name is invalid or already used,
     *         the source is not a regular file, or the file exceeds {@link #MAX_BYTES}
     * @throws IOException if the file cannot be read
     */
    public DigitalScroll upload(String id, String name, Path source) throws IOException {
        User owner = accounts.requireUser();
        id = Validation.text(id, "Scroll ID");
        name = Validation.text(name, "Scroll name");
        if (scrolls.existsById(id) || scrolls.existsByName(name)) {
            throw new IllegalArgumentException("Scroll ID or name already exists");
        }
        if (source == null || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Choose a readable regular file");
        }
        byte[] content;
        try (InputStream input = Files.newInputStream(source)) {
            // Read one byte past the limit so oversized files are detected without loading them fully.
            content = input.readNBytes(MAX_BYTES + 1);
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("Scroll exceeds the 10 MiB limit");
        }
        // 【Sprint 2 · US17 #52】Sixian：上传时按原文件名／内容记录格式。
        String format = FileFormat.detect(source.getFileName() == null ? null : source.getFileName().toString(),
                content);
        DigitalScroll scroll = new DigitalScroll(id, name, content, owner.getIdKey(), LocalDateTime.now(clock),
                1, 0, format);
        scrolls.save(scroll);
        return scroll;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Returns all available scrolls.
     * Guests and registered users can browse without login.
     *
     * @return a list of available scrolls, or an empty list
     */
    public List<DigitalScroll> browse() {
        return scrolls.findAll();
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Finds a scroll by its unique ID.
     *
     * @param id scroll ID
     * @return the matching scroll
     * @throws IllegalArgumentException if no scroll exists
     */
    private DigitalScroll find(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Scroll ID is required");
        }

        return scrolls.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Scroll not found: " + id));
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Sixian（需要修改时先联系原作者）
     * Retrieves a scroll for preview.
     * Previewing does not download the file or change counters.
     *
     * @param id ID of the scroll to preview
     * @return the selected scroll
     */
    public DigitalScroll preview(String id) {
        return find(id);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian；【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella；Sprint 1 原作者：Sixian
     * Downloads a scroll to a local destination.
     * Only authenticated users may download scrolls.
     * A successful download increments the download count.
     *
     * <p>【Sprint 2 · US17 #52】主负责人：Sixian。
     * 做什么：格式已知且目标文件名没有对应扩展名时，自动在末尾补上（例如 report → report.pdf），
     * 写出原始字节后保存下载次数加 1 的卷轴（保留格式）。
     * 对接／验证：返回实际写入的路径，界面显示给用户；格式为 unknown 时不改文件名。
     *
     * @param id ID of the scroll to download
     * @param destination target file path
     * @return the path actually written
     * @throws IllegalStateException if no user is logged in
     * @throws IllegalArgumentException if the scroll ID or destination is invalid
     * @throws IOException if writing the file fails
     */
    public Path download(String id, Path destination) throws IOException {
        accounts.requireUser();

        DigitalScroll scroll = find(id);

        if (destination == null) {
            throw new IllegalArgumentException("Download destination is required");
        }
        if (Files.isDirectory(destination)) {
            // 与 Sprint 1 一致：写入失败类错误使用 IOException（ScrollDownloadTest 依赖）。
            throw new IOException("Destination must be a file path, not a folder");
        }

        Path target = withExtension(destination, scroll.getFileFormat());
        Files.write(target, scroll.getContent());

        // Stella（R1）：用 withRecordedDownload 代替 7 参数构造方法，保留文件格式。
        scrolls.save(scroll.withRecordedDownload());
        return target;
    }

    /**
     * 【PR s2-r2-format-preview】实现：Sixian ｜ 审核：Stella
     * 【Sprint 2 · US17 #52】主负责人：Sixian。
     * 做什么：目标文件名已以该格式结尾（不区分大小写，jpg 与 jpeg 视为相同）就保持不变，否则补上扩展名。
     * 对接／验证：ScrollFormatTest 覆盖已带扩展名、缺少扩展名、错误扩展名和 unknown。
     *
     * @param destination requested path
     * @param format scroll format
     * @return path to write
     */
    static Path withExtension(Path destination, String format) {
        if (destination.getFileName() == null || format == null
                || format.equals(DigitalScroll.UNKNOWN_FORMAT)) {
            return destination;
        }
        String name = destination.getFileName().toString();
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        boolean jpegAlias = (format.equals("jpg") && lower.endsWith(".jpeg"))
                || (format.equals("jpeg") && lower.endsWith(".jpg"));
        if (lower.endsWith("." + format) || jpegAlias) {
            return destination;
        }
        return destination.resolveSibling(name + "." + format);
    }
}

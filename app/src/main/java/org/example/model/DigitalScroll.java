package org.example.model;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
 *   - Sprint 1 原作者：Stella（未标 PR 的方法本轮不改）
 *
 * Stores a scroll's binary content, metadata and upload/download counters.
 * Instances are immutable; a changed counter is represented by a new object.
 *
 * <p>Sprint 2 变更（其余为 Sprint 1 原有）：
 * 【US17 #52】新增 fileFormat 字段和 8 参数构造方法；【US10 #11】新增 withName、withContent；
 * 【US08/US17】新增 withRecordedDownload，下载计数加 1 时保留文件格式。
 * 主负责人：Stella（卷轴模型与存储负责人）。
 */
public class DigitalScroll {

    private final String scrollId;
    private final String name;
    private final byte[] content;
    private final String uploaderId;
    private final LocalDateTime uploadDate;
    private final long uploadCount;
    private final long downloadCount;
    private final String fileFormat;

    /** Format recorded when it cannot be determined. */
    public static final String UNKNOWN_FORMAT = "unknown";

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Creates a newly uploaded scroll with an upload count of 1 and a download count of 0.
     * @param scrollId unique scroll ID
     * @param name unique scroll name
     * @param content binary file content, copied defensively
     * @param uploaderId ID key of the uploading user
     * @param uploadDate time of upload
     * @throws IllegalArgumentException if a required text field is missing
     */
    public DigitalScroll(
            String scrollId,
            String name,
            byte[] content,
            String uploaderId,
            LocalDateTime uploadDate) {
        this(scrollId, name, content, uploaderId, uploadDate, 1, 0);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * Creates or restores a scroll with previously saved counters.
     * @param scrollId unique scroll ID
     * @param name unique scroll name
     * @param content binary file content, copied defensively
     * @param uploaderId ID key of the uploading user
     * @param uploadDate time of upload
     * @param uploadCount recorded uploads, at least 1
     * @param downloadCount recorded successful downloads, at least 0
     * @throws IllegalArgumentException if a required field is missing or a counter is invalid
     */
    public DigitalScroll(String scrollId, String name, byte[] content, String uploaderId,
                         LocalDateTime uploadDate, long uploadCount, long downloadCount) {
        this(scrollId, name, content, uploaderId, uploadDate, uploadCount, downloadCount, UNKNOWN_FORMAT);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US17 #52】主负责人：Stella。
     * 做什么：完整构造方法，额外保存文件格式（小写扩展名，如 pdf、png；无法判断时为 unknown）。
     * 对接／验证：Sixian 的 FileFormat.detect 决定格式；Stella 的 FileScrollRepository 保存并恢复它。
     *
     * @param scrollId unique scroll ID
     * @param name unique scroll name
     * @param content binary file content, copied defensively
     * @param uploaderId ID key of the uploading user
     * @param uploadDate time of upload
     * @param uploadCount recorded uploads, at least 1
     * @param downloadCount recorded successful downloads, at least 0
     * @param fileFormat lowercase format such as {@code pdf}; blank or null becomes {@code unknown}
     * @throws IllegalArgumentException if a required field is missing or a counter is invalid
     */
    public DigitalScroll(String scrollId, String name, byte[] content, String uploaderId,
                         LocalDateTime uploadDate, long uploadCount, long downloadCount, String fileFormat) {
        if (uploadCount < 1 || downloadCount < 0) {
            throw new IllegalArgumentException("Invalid scroll counters");
        }
        this.scrollId = requireText(scrollId);
        this.name = requireText(name);
        this.content = Arrays.copyOf(content, content.length);
        this.uploaderId = requireText(uploaderId);
        this.uploadDate = Objects.requireNonNull(uploadDate, "uploadDate");
        this.uploadCount = uploadCount;
        this.downloadCount = downloadCount;
        this.fileFormat = fileFormat == null || fileFormat.isBlank()
                ? UNKNOWN_FORMAT : fileFormat.strip().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the unique scroll ID used for lookup and selection
     */
    public String getScrollId() {
        return scrollId;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the scroll name shown in lists and previews
     */
    public String getName() {
        return name;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return a copy of the content; modifying it does not change this scroll
     */
    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the ID key of the user who uploaded this scroll
     */
    public String getUploaderId() {
        return uploaderId;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the time this scroll was uploaded
     */
    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the recorded number of uploads
     */
    public long getUploadCount() {
        return uploadCount;
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * @return the recorded number of successful downloads; browsing and previewing do not change it
     */
    public long getDownloadCount() {
        return downloadCount;
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US17 #52】主负责人：Stella。
     * 做什么：返回保存的文件格式。
     * 对接／验证：Sixian 的预览和下载扩展名使用它。
     *
     * @return lowercase format such as {@code pdf}, or {@code unknown}
     */
    public String getFileFormat() {
        return fileFormat;
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US10 #11】主负责人：Stella。
     * 做什么：返回改名后的新对象；ID、内容、上传者、日期、次数和格式都不变。
     * 对接／验证：ScrollEditService.rename 调用；名称唯一性由服务和仓库检查。
     *
     * @param newName replacement name
     * @return renamed copy
     */
    public DigitalScroll withName(String newName) {
        return new DigitalScroll(scrollId, newName, content, uploaderId, uploadDate,
                uploadCount, downloadCount, fileFormat);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US10 #11 / US12 #13】主负责人：Stella。
     * 做什么：返回替换文件后的新对象：内容和格式更新，上传次数加 1（统计“曾经上传过几次”），下载次数保留。
     * 对接／验证：ScrollEditService.replaceContent 调用；管理员统计页会看到上传次数变化。
     *
     * @param newContent replacement bytes
     * @param newFormat format of the replacement file
     * @return updated copy
     */
    public DigitalScroll withContent(byte[] newContent, String newFormat) {
        return new DigitalScroll(scrollId, name, newContent, uploaderId, uploadDate,
                Math.addExact(uploadCount, 1), downloadCount, newFormat);
    }

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US17 #52】主负责人：Stella（模型）。
     * 做什么：返回下载次数加 1 的新对象，并保留文件格式（Sprint 1 的下载代码用 7 参数构造方法，会丢失格式）。
     * 对接／验证：ScrollService.download 改为调用本方法；ScrollFormatTest 验证下载后格式仍在。
     *
     * @return copy with one more recorded download
     */
    public DigitalScroll withRecordedDownload() {
        return new DigitalScroll(scrollId, name, content, uploaderId, uploadDate,
                uploadCount, Math.addExact(downloadCount, 1), fileFormat);
    }

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Stella（需要修改时先联系原作者）
     * Rejects a missing or blank scroll ID, name or uploader ID.
     * @param value text to check
     * @return the unchanged value
     */
    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required domain field is missing");
        }
        return value;
    }
}

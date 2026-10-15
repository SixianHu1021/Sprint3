package org.example.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.example.model.DigitalScroll;
import org.example.model.User;
import org.example.repository.ScrollRepository;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
 *
 * 【Sprint 2 · US10 #11】主负责人：Stella。
 * 做什么：上传者管理自己的卷轴：查看自己上传的列表、改名、替换文件、删除。
 * 只能操作 uploaderId 等于当前登录 ID 的卷轴；管理员也不能修改别人的卷轴（需求原文：不能编辑其他人的卷轴）。
 * 对接／验证：登录检查调用 Yuhan 的 AccountService.requireUser；格式判断调用 Sixian 的 FileFormat.detect；
 * 保存／删除调用 Stella 自己的 ScrollRepository。界面在 Stella 的 MyScrollsScreen。
 */
public final class ScrollEditService {

    private final ScrollRepository scrolls;
    private final AccountService accounts;

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：保存仓库和账户服务引用。
     * 对接／验证：App 传入与 ScrollService 相同的 FileScrollRepository，修改后浏览列表立即反映。
     *
     * @param scrolls scroll repository
     * @param accounts account service for the logged-in user
     */
    public ScrollEditService(ScrollRepository scrolls, AccountService accounts) {
        this.scrolls = Objects.requireNonNull(scrolls, "scrolls");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：返回当前用户上传的全部卷轴（按上传顺序）。
     * 对接／验证：访客调用会得到 “Please log in first”。
     *
     * @return scrolls uploaded by the logged-in user
     */
    public List<DigitalScroll> myScrolls() {
        String me = accounts.requireUser().getIdKey();
        return scrolls.findAll().stream().filter(s -> s.getUploaderId().equals(me)).toList();
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US14 #49 逐项检查】主负责人：Stella。
     * 做什么：“我的卷轴” 输入卷轴 ID 后立即确认它存在且属于当前用户，再询问新名称或新文件。
     * 对接／验证：MyScrollsScreen 用 askValid 调用；别人的卷轴显示 “You can only edit scrolls you uploaded” 并重问。
     *
     * @param id raw scroll ID
     * @return the owned scroll
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the scroll is missing or owned by someone else
     */
    public DigitalScroll checkEditable(String id) {
        try {
            return requireOwned(id, "edit");
        } catch (IllegalStateException e) {
            accounts.requireUser();
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US14 #49 逐项检查】主负责人：Stella。
     * 做什么：改名时输入新名称后立即检查格式和重复；与原名称相同视为允许。
     *
     * @param scroll scroll being renamed
     * @param newName raw new name
     * @return normalized new name
     * @throws IllegalArgumentException if the name is invalid or used by another scroll
     */
    public String checkNewName(DigitalScroll scroll, String newName) {
        String name = Validation.text(newName, "Scroll name");
        if (!name.equals(scroll.getName()) && scrolls.existsByName(name)) {
            throw new IllegalArgumentException("Scroll name '" + name + "' is already taken");
        }
        return name;
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：修改自己卷轴的名称；新名称需非空且不能与其他卷轴重复。ID、内容、次数不变。
     * 对接／验证：修改别人的卷轴抛出 “You can only edit scrolls you uploaded”。
     *
     * @param id scroll ID
     * @param newName replacement name
     * @return the saved scroll
     */
    public DigitalScroll rename(String id, String newName) {
        DigitalScroll scroll = requireOwned(id, "edit");
        String name = Validation.text(newName, "Scroll name");
        if (!name.equals(scroll.getName()) && scrolls.existsByName(name)) {
            throw new IllegalArgumentException("Scroll name already exists");
        }
        DigitalScroll updated = scroll.withName(name);
        scrolls.save(updated);
        return updated;
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：用新的本地文件替换自己卷轴的内容；重新判断格式，上传次数加 1，下载次数保留。
     * 对接／验证：大小限制与上传相同（ScrollService.MAX_BYTES）；文件不存在或过大时不修改原卷轴。
     *
     * @param id scroll ID
     * @param source replacement file
     * @return the saved scroll
     * @throws IOException if the file cannot be read
     */
    public DigitalScroll replaceContent(String id, Path source) throws IOException {
        DigitalScroll scroll = requireOwned(id, "edit");
        byte[] content = readSource(source);
        String format = FileFormat.detect(source.getFileName() == null ? null : source.getFileName().toString(),
                content);
        DigitalScroll updated = scroll.withContent(content, format);
        scrolls.save(updated);
        return updated;
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：删除自己的卷轴；删除后不再出现在浏览列表。
     * 对接／验证：删除别人的卷轴抛出 “You can only delete scrolls you uploaded”；确认提示在界面层。
     *
     * @param id scroll ID
     */
    public void delete(String id) {
        DigitalScroll scroll = requireOwned(id, "delete");
        scrolls.deleteById(scroll.getScrollId());
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：先要求登录，再找到卷轴并确认当前用户是上传者。
     * 对接／验证：找不到 ID 时消息与浏览一致：“Scroll not found: ID”。
     */
    private DigitalScroll requireOwned(String id, String action) {
        User me = accounts.requireUser();
        String scrollId = Validation.text(id, "Scroll ID");
        DigitalScroll scroll = scrolls.findById(scrollId)
                .orElseThrow(() -> new IllegalArgumentException("Scroll not found: " + scrollId));
        if (!scroll.getUploaderId().equals(me.getIdKey())) {
            throw new IllegalStateException("You can only " + action + " scrolls you uploaded");
        }
        return scroll;
    }

    /**
     * 【PR s2-r2-scroll-edit-stats】实现：Stella ｜ 审核：Sixian
     * 主负责人：Stella。
     * 做什么：读取替换文件，规则与 Sprint 1 上传相同：必须是普通文件，最多 10 MiB。
     * 对接／验证：多读 1 字节判断超限，不会把超大文件完整读入内存。
     */
    private static byte[] readSource(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Choose a readable regular file");
        }
        byte[] content;
        try (InputStream input = Files.newInputStream(source)) {
            content = input.readNBytes(ScrollService.MAX_BYTES + 1);
        }
        if (content.length > ScrollService.MAX_BYTES) {
            throw new IllegalArgumentException("Scroll exceeds the 10 MiB limit");
        }
        return content;
    }
}

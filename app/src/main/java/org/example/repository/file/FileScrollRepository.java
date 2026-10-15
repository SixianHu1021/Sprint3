package org.example.repository.file;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.example.model.DigitalScroll;
import org.example.repository.ScrollRepository;

/**
 * Persists scroll content, metadata and counters in a properties snapshot.
 * Binary content is stored as Base64. Updates memory only after saving succeeds.
 * Login and ownership checks belong to the service layer. Intended for a single
 * process without concurrent writers.
 *
 * <p>Sprint 2 变更：【US17 #52】保存／读取 format；【US10 #11】deleteById；
 * save 改为原位替换（更新后不再移到列表末尾，“按上传顺序”排序因此稳定）。
 * 主负责人：Stella。审核人：Sixian。
 */
public final class FileScrollRepository implements ScrollRepository {
    private final AtomicPropertiesStore store;
    private List<DigitalScroll> scrolls = new ArrayList<>();

    /**
     * Loads scrolls from a snapshot; an absent file starts an empty repository.
     * Snapshots without counters load with 1 upload and 0 downloads.
     * @param file scroll snapshot path
     * @throws IllegalStateException if reading fails or scroll records are invalid
     */
    public FileScrollRepository(Path file) {
        store = new AtomicPropertiesStore(file);
        Properties data = store.read();
        try {
            int count = Integer.parseInt(data.getProperty("count", data.isEmpty() ? "0" : "-1"));
            if (count < 0) {
                throw new IllegalArgumentException("Invalid scroll count");
            }
            for (int i = 0; i < count; i++) {
                String p = i + ".";
                DigitalScroll scroll = new DigitalScroll(data.getProperty(p + "id"),
                        data.getProperty(p + "name"), Base64.getDecoder().decode(data.getProperty(p + "content")),
                        data.getProperty(p + "uploader"), LocalDateTime.parse(data.getProperty(p + "date")),
                        Long.parseLong(data.getProperty(p + "uploads", "1")),
                        Long.parseLong(data.getProperty(p + "downloads", "0")),
                        data.getProperty(p + "format", DigitalScroll.UNKNOWN_FORMAT));
                if (existsById(scroll.getScrollId()) || existsByName(scroll.getName())) {
                    throw new IllegalArgumentException("Duplicate scroll in snapshot");
                }
                scrolls.add(scroll);
            }
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid scroll snapshot: " + file, e);
        }
    }

    /**
     * Inserts a scroll or replaces the scroll with the same ID, saving before updating memory.
     * Content and both counters are written together.
     * @param scroll complete scroll to store
     * @throws IllegalArgumentException if another scroll has the same name
     * @throws IllegalStateException if saving fails
     */
    @Override
    public void save(DigitalScroll scroll) {
        if (scrolls.stream().anyMatch(s -> s.getName().equals(scroll.getName())
                && !s.getScrollId().equals(scroll.getScrollId()))) {
            throw new IllegalArgumentException("Scroll name already exists");
        }
        List<DigitalScroll> next = new ArrayList<>(scrolls);
        int index = indexOf(scroll.getScrollId());
        if (index >= 0) {
            next.set(index, scroll);
        } else {
            next.add(scroll);
        }
        persist(next);
        scrolls = next;
    }

    /**
     * 【Sprint 2 · US10 #11】主负责人：Stella。审核人：Sixian。
     * 做什么：删除指定卷轴，先写文件成功再更新内存；不存在的 ID 保存原列表，不报错。
     * 对接／验证：ScrollEditService.delete 先检查所有者再调用；删除后浏览列表不再出现（MyScrollsTest 验证）。
     *
     * @param scrollId ID of the scroll to remove
     * @throws IllegalStateException if saving fails
     */
    @Override
    public void deleteById(String scrollId) {
        List<DigitalScroll> next = new ArrayList<>(scrolls);
        next.removeIf(s -> s.getScrollId().equals(scrollId));
        persist(next);
        scrolls = next;
    }

    /**
     * 【Sprint 2】主负责人：Stella。
     * 做什么：从 Sprint 1 的 save 中抽出的“整份快照写入”，save 与 deleteById 共用；新增 format 字段。
     * 对接／验证：旧数据没有 format 时读取为 unknown（构造方法中处理）。
     *
     * @param snapshot scrolls to persist
     * @throws IllegalStateException if saving fails
     */
    private void persist(List<DigitalScroll> snapshot) {
        Properties data = new Properties();
        data.setProperty("count", Integer.toString(snapshot.size()));
        for (int i = 0; i < snapshot.size(); i++) {
            DigitalScroll item = snapshot.get(i);
            String p = i + ".";
            data.setProperty(p + "id", item.getScrollId());
            data.setProperty(p + "name", item.getName());
            data.setProperty(p + "content", Base64.getEncoder().encodeToString(item.getContent()));
            data.setProperty(p + "uploader", item.getUploaderId());
            data.setProperty(p + "date", item.getUploadDate().toString());
            data.setProperty(p + "uploads", Long.toString(item.getUploadCount()));
            data.setProperty(p + "downloads", Long.toString(item.getDownloadCount()));
            data.setProperty(p + "format", item.getFileFormat());
        }
        store.write(data);
    }

    /**
     * 【Sprint 2】主负责人：Stella。
     * 做什么：返回卷轴在列表中的位置，用于原位替换。
     * 对接／验证：仅供 save 使用。
     */
    private int indexOf(String scrollId) {
        for (int i = 0; i < scrolls.size(); i++) {
            if (scrolls.get(i).getScrollId().equals(scrollId)) {
                return i;
            }
        }
        return -1;
    }

    /** @return an unmodifiable list of all stored scrolls */
    @Override
    public List<DigitalScroll> findAll() {
        return List.copyOf(scrolls);
    }

    /**
     * Finds a scroll by its ID.
     * @param scrollId scroll ID to look up
     * @return the matching scroll, or empty if none exists
     */
    @Override
    public Optional<DigitalScroll> findById(String scrollId) {
        return scrolls.stream().filter(s -> s.getScrollId().equals(scrollId)).findFirst();
    }

    /**
     * Checks whether a scroll ID is already used.
     * @param scrollId scroll ID to check
     * @return true if a scroll has this ID
     */
    @Override
    public boolean existsById(String scrollId) {
        return findById(scrollId).isPresent();
    }

    /**
     * Checks whether a scroll name is already used; comparison is case-sensitive.
     * @param name scroll name to check
     * @return true if a scroll has this name
     */
    @Override
    public boolean existsByName(String name) {
        return scrolls.stream().anyMatch(s -> s.getName().equals(name));
    }
}

package org.example.repository;

import org.example.model.DigitalScroll;

import java.util.List;
import java.util.Optional;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
 *   - Sprint 1 原作者：Yuhan（未标 PR 的方法本轮不改）
 *
 * Defines operations for storing and retrieving digital scrolls.
 */
public interface ScrollRepository {

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    void save(DigitalScroll scroll);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    List<DigitalScroll> findAll();

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    Optional<DigitalScroll> findById(String scrollId);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    boolean existsById(String scrollId);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    boolean existsByName(String name);

    /**
     * 【PR s2-r1-scroll-storage】实现：Stella ｜ 审核：Sixian
     * 【Sprint 2 · US10 #11】主负责人：Stella。
     * 做什么：删除指定 ID 的卷轴并保存；不存在的 ID 不报错。
     * 对接／验证：所有者检查在 ScrollEditService，仓库只负责数据。
     *
     * @param scrollId ID of the scroll to remove
     */
    void deleteById(String scrollId);
}
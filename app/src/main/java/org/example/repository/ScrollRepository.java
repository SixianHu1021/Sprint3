package org.example.repository;

import org.example.model.DigitalScroll;

import java.util.List;
import java.util.Optional;

/**
 * Defines operations for storing and retrieving digital scrolls.
 */
public interface ScrollRepository {

    void save(DigitalScroll scroll);

    List<DigitalScroll> findAll();

    Optional<DigitalScroll> findById(String scrollId);

    boolean existsById(String scrollId);

    boolean existsByName(String name);

    /**
     * 【Sprint 2 · US10 #11】主负责人：Stella。审核人：Sixian。
     * 做什么：删除指定 ID 的卷轴并保存；不存在的 ID 不报错。
     * 对接／验证：所有者检查在 ScrollEditService，仓库只负责数据。
     *
     * @param scrollId ID of the scroll to remove
     */
    void deleteById(String scrollId);
}
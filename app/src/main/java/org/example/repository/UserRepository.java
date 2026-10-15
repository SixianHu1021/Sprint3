package org.example.repository;

import org.example.model.User;

import java.util.List;
import java.util.Optional;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
 *   - Sprint 1 原作者：Yuhan（未标 PR 的方法本轮不改）
 *
 * Defines operations for storing and retrieving users.
 *
 * <p>【Sprint 2 · US16 #51】新增 findByEmail / findByPhone。主负责人：Henry。
 * 对接／验证：AccountService 通过 ContactValidator 调用这两个方法，注册、管理员添加、修改资料共用同一检查。
 */
public interface UserRepository {

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    void save(User user);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    Optional<User> findByUsername(String username);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    Optional<User> findByIdKey(String idKey);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    List<User> findAll();

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    boolean existsByUsername(String username);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    boolean existsByIdKey(String idKey);

    /**
     * 【Sprint 1 已完成 · 本轮不改】原作者：Yuhan（需要修改时先联系原作者）
     */
    void deleteByIdKey(String idKey);

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按邮箱查找用户，比较时不区分大小写。
     * 对接／验证：ContactValidator 用它拒绝重复邮箱；FileUserRepositoryTest 覆盖大小写。
     *
     * @param email email address to look up
     * @return matching user, or an empty optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按电话查找用户；比较时忽略空格、横线、括号和点，“0411 111 111” 与 “0411111111” 视为同一号码。
     * 对接／验证：ContactValidator 用它拒绝重复电话。
     *
     * @param phone phone number to look up
     * @return matching user, or an empty optional
     */
    Optional<User> findByPhone(String phone);
}
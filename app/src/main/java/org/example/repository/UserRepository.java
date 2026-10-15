package org.example.repository;

import org.example.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Defines operations for storing and retrieving users.
 *
 * <p>【Sprint 2 · US16 #51】新增 findByEmail / findByPhone。主负责人：Henry。审核人：Yuhan。
 * 对接／验证：AccountService 通过 ContactValidator 调用这两个方法，注册、管理员添加、修改资料共用同一检查。
 */
public interface UserRepository {

    void save(User user);

    Optional<User> findByUsername(String username);

    Optional<User> findByIdKey(String idKey);

    List<User> findAll();

    boolean existsByUsername(String username);

    boolean existsByIdKey(String idKey);

    void deleteByIdKey(String idKey);

    /**
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按邮箱查找用户，比较时不区分大小写。
     * 对接／验证：ContactValidator 用它拒绝重复邮箱；FileUserRepositoryTest 覆盖大小写。
     *
     * @param email email address to look up
     * @return matching user, or an empty optional
     */
    Optional<User> findByEmail(String email);

    /**
     * 【Sprint 2 · US16 #51】主负责人：Henry。
     * 做什么：按电话查找用户；比较时忽略空格、横线、括号和点，“0411 111 111” 与 “0411111111” 视为同一号码。
     * 对接／验证：ContactValidator 用它拒绝重复电话。
     *
     * @param phone phone number to look up
     * @return matching user, or an empty optional
     */
    Optional<User> findByPhone(String phone);
}
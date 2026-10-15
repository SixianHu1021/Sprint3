package org.example.ui;

import java.util.Locale;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.service.AccountService;

/**
 * 【Sprint 2 · US03 #4 / US16 #51 / US12 #13】主负责人：Henry。审核人：Yuhan（菜单集成）。
 * 做什么：账户相关的控制台页面：注册／首次管理员／管理员添加用户的资料表单（从 Sprint 1 ConsoleUi.createAccount 移来），
 * 以及新的“我的资料”页面（查看资料、修改姓名邮箱电话、修改密码）。
 * 对接／验证：所有输入通过 Yuhan 的 ConsoleIO（必填立即重问、密码隐藏）；业务规则全部在 AccountService，
 * 本类不重复写校验。Yuhan 在 R4 把 ConsoleUi 的 register/add-user/profile 菜单项接到这里。
 */
public final class AccountScreens {

    private final AccountService accounts;
    private final ConsoleIO io;

    /**
     * 主负责人：Henry。
     * 做什么：保存账户服务和共用输入输出。
     * 对接／验证：ConsoleUi 构造时创建一次。
     *
     * @param accounts account service
     * @param io shared console helper
     */
    public AccountScreens(AccountService accounts, ConsoleIO io) {
        this.accounts = accounts;
        this.io = io;
    }

    /**
     * 主负责人：Henry（Sprint 1 写法，Sprint 2 改用 ConsoleIO）。
     * 做什么：收集 ID、用户名、密码、姓名、邮箱、电话；按模式调用 bootstrapAdmin、addUser 或 register。
     * 管理员模式先检查权限，再询问资料（未授权时不会出现 “Full name:” 提示）。
     * 对接／验证：空白字段在输入时立即重问（US14）；重复邮箱／电话由 AccountService 拒绝并说明字段（US16）；
     * 管理员可选择 MEMBER 或 ADMIN（US12）。AccountConsoleTest 覆盖三种模式。
     *
     * @param mode {@code bootstrap}, {@code admin} or {@code register}
     */
    public void createAccount(String mode) {
        if (mode.equals("admin")) {
            accounts.listUsers();
        }
        String id = io.askRequired("ID key: ", "ID key");
        String username = io.askRequired("Username: ", "Username");
        String password = io.password("Password: ");
        String name = io.askRequired("Full name: ", "Full name");
        String email = io.askRequired("Email: ", "Email");
        String phone = io.askRequired("Phone: ", "Phone");
        switch (mode) {
            case "bootstrap" -> accounts.bootstrapAdmin(id, username, password, name, email, phone);
            case "admin" -> accounts.addUser(id, username, password, name, email, phone, askRole());
            default -> accounts.register(id, username, password, name, email, phone);
        }
        io.info("Account created. Use login to sign in.");
    }

    /**
     * 主负责人：Henry。
     * 做什么：管理员添加用户时选择角色：1 或 MEMBER、2 或 ADMIN；其他输入显示错误并重问。
     * 对接／验证：US12 “管理员可以创建管理员和普通用户”；US14 输入错误不退出表单。
     */
    private UserRole askRole() {
        while (true) {
            String value = io.ask("Role (1 = MEMBER, 2 = ADMIN): ").strip().toUpperCase(Locale.ROOT);
            if (value.equals("1") || value.equals("MEMBER")) {
                return UserRole.MEMBER;
            }
            if (value.equals("2") || value.equals("ADMIN")) {
                return UserRole.ADMIN;
            }
            io.error("Role must be 1 (MEMBER) or 2 (ADMIN).");
        }
    }

    /**
     * 【Sprint 2 · US03 #4】主负责人：Henry。
     * 做什么：“我的资料”页面：每轮先显示当前资料，再显示编号菜单（修改资料 / 修改密码 / 返回）。
     * 对接／验证：访客无法进入（requireUser）；ID key 只显示、不可修改。ProfileConsoleTest 覆盖。
     */
    public void profile() {
        accounts.requireUser();
        Menu menu = new Menu("Profile menu:", "Back", "back")
                .option("Edit full name, email and phone", "edit", a -> editDetails())
                .option("Change password", "password", a -> changePassword());
        menu.loop(io, "Profile> ", this::showProfile);
    }

    /**
     * 【Sprint 2 · US03 #4】主负责人：Henry。
     * 做什么：显示 ID、用户名、姓名、邮箱、电话和角色；不显示密码或哈希。
     * 对接／验证：数据每次从 AccountService.currentUser 重新读取，修改后立即看到新值。
     */
    private void showProfile() {
        User user = accounts.requireUser();
        io.info("Your profile:");
        io.info("  ID key:    " + user.getIdKey() + " (cannot be changed)");
        io.info("  Username:  " + user.getUsername());
        io.info("  Full name: " + user.getFullName());
        io.info("  Email:     " + user.getEmail());
        io.info("  Phone:     " + user.getPhone());
        io.info("  Role:      " + user.getRole());
    }

    /**
     * 【Sprint 2 · US03 #4 / US16 #51】主负责人：Henry。
     * 做什么：逐项询问新值，直接回车保留原值，然后一次性保存。
     * 对接／验证：邮箱／电话与他人重复时显示 “Error: Email address is already registered…” 并保持原资料。
     */
    private void editDetails() {
        User user = accounts.requireUser();
        String name = keep(io.askOptional("Full name [" + user.getFullName() + "]: "), user.getFullName());
        String email = keep(io.askOptional("Email [" + user.getEmail() + "]: "), user.getEmail());
        String phone = keep(io.askOptional("Phone [" + user.getPhone() + "]: "), user.getPhone());
        accounts.updateProfile(name, email, phone);
        io.info("Profile updated.");
    }

    /**
     * 【Sprint 2 · US03 #4 / US15 #50】主负责人：Henry。
     * 做什么：输入当前密码、新密码和确认密码（全部隐藏输入）；两次新密码不一致时不调用服务。
     * 对接／验证：当前密码错误、长度不符合时显示错误；成功后新密码以哈希保存。
     */
    private void changePassword() {
        String current = io.password("Current password: ");
        String next = io.password("New password: ");
        String confirm = io.password("Confirm new password: ");
        if (!next.equals(confirm)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        accounts.changePassword(current, next);
        io.info("Password changed.");
    }

    /**
     * 主负责人：Henry。
     * 做什么：空输入表示保留原值。
     */
    private static String keep(String value, String current) {
        return value.isEmpty() ? current : value;
    }
}

package org.example.ui;

import java.util.Locale;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.service.AccountService;

/**
 * 【分工】本文件涉及以下工作（每个方法注释的第一行写明它属于哪个 PR、谁实现、谁审核）：
 *   - 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
 *   - 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Henry（仅在资料菜单接入头像页面，不改其它方法）
 *
 * 【Sprint 2 · US03 #4 / US16 #51 / US12 #13】主负责人：Henry。
 * 做什么：账户相关的控制台页面：注册／首次管理员／管理员添加用户的资料表单（从 Sprint 1 ConsoleUi.createAccount 移来），
 * 以及新的“我的资料”页面（查看资料、修改姓名邮箱电话、修改密码）。
 * 对接／验证：所有输入通过 Yuhan 的 ConsoleIO（必填立即重问、密码隐藏）；业务规则全部在 AccountService，
 * 本类不重复写校验。Yuhan 在 R4 把 ConsoleUi 的 register/add-user/profile 菜单项接到这里。
 *
 * <p>【Sprint 3 · US19 #67 / US20 #68】主负责人：Yuhan（接线）。
 * 做什么：“我的资料”菜单新增一个“Profile picture”项，进入 Yuhan 的 ProfilePictureScreen（上传／预览头像）。
 * 为什么放在这里：资料入口是 Henry 的页面，但头像功能全部写在新文件 ProfilePictureScreen 里，
 * 本文件只多一个菜单项、不改原有方法，避免多人反复改同一文件。pictureScreen 为 null 时不显示该项（Sprint 2 测试不受影响）。
 */
public final class AccountScreens {

    private final AccountService accounts;
    private final ConsoleIO io;
    private final ProfilePictureScreen pictureScreen;

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry。
     * 做什么：保存账户服务和共用输入输出（Sprint 2 构造方法，不接入头像页面）。
     * 对接／验证：ProfileConsoleTest 等 Sprint 2 测试继续使用这个构造方法，资料菜单不含头像项。
     *
     * @param accounts account service
     * @param io shared console helper
     */
    public AccountScreens(AccountService accounts, ConsoleIO io) {
        this(accounts, io, null);
    }

    /**
     * 【PR s3-r4-profile-picture-screen】实现：Yuhan ｜ 审核：Henry
     * 主负责人：Yuhan。
     * 做什么：完整构造方法，额外接入头像页面；pictureScreen 非空时资料菜单出现“Profile picture”项。
     * 对接／验证：ConsoleUi 在 R4 用这个构造方法创建 AccountScreens，并把 ProfilePictureScreen 传进来。
     *
     * @param accounts account service
     * @param io shared console helper
     * @param pictureScreen profile picture screen, or {@code null} to hide the option
     */
    public AccountScreens(AccountService accounts, ConsoleIO io, ProfilePictureScreen pictureScreen) {
        this.accounts = accounts;
        this.io = io;
        this.pictureScreen = pictureScreen;
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * 主负责人：Henry（Sprint 1 写法，Sprint 2 改用 ConsoleIO）。
     * 做什么：收集 ID、用户名、密码、姓名、邮箱、电话；按模式调用 bootstrapAdmin、addUser 或 register。
     * 管理员模式先检查权限，再询问资料（未授权时不会出现 “Full name:” 提示）。
     * 对接／验证：每一项输入后立即检查并只重问该项（US14）：重复 ID／用户名、密码不合规、邮箱／电话格式或重复（US16）；
     * 管理员可选择 MEMBER 或 ADMIN（US12）。AccountConsoleTest 覆盖三种模式。
     *
     * @param mode {@code bootstrap}, {@code admin} or {@code register}
     */
    public void createAccount(String mode) {
        if (mode.equals("admin")) {
            accounts.listUsers();
        }
        // 【US14 逐项检查】每输入一项就检查，错误只重问这一项；输入 cancel 放弃表单。
        io.info(ConsoleIO.CANCEL_HINT);
        String id = io.askValid("ID key: ", accounts::checkNewIdKey);
        String username = io.askValid("Username: ", accounts::checkNewUsername);
        String password = io.passwordValid("Password: ", AccountService::checkPassword);
        String name = io.askValid("Full name: ", v -> org.example.service.Validation.text(v, "Full name"));
        String email = io.askValid("Email: ", v -> accounts.checkEmail(v, null));
        String phone = io.askValid("Phone: ", v -> accounts.checkPhone(v, null));
        switch (mode) {
            case "bootstrap" -> accounts.bootstrapAdmin(id, username, password, name, email, phone);
            case "admin" -> accounts.addUser(id, username, password, name, email, phone, askRole());
            default -> accounts.register(id, username, password, name, email, phone);
        }
        io.info("Account created. Use login to sign in.");
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
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
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US03 #4】主负责人：Henry。
     * 做什么：“我的资料”页面：每轮先显示当前资料，再显示编号菜单（修改资料 / 修改密码 / 返回）。
     * 对接／验证：访客无法进入（requireUser）；ID key 只显示、不可修改。ProfileConsoleTest 覆盖。
     */
    public void profile() {
        accounts.requireUser();
        Menu menu = new Menu("Profile menu:", "Back", "back")
                .option("Edit full name, email and phone", "edit", a -> editDetails())
                .option("Change password", "password", a -> changePassword());
        // 【Sprint 3 · US19 #67 / US20 #68】Yuhan 接线：有头像页面时，资料菜单多一个“Profile picture”项。
        if (pictureScreen != null) {
            menu.option("Profile picture (upload / preview)", "picture", a -> pictureScreen.show());
        }
        menu.loop(io, "Profile> ", this::showProfile);
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
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
        // 【Sprint 3 · US20 #68】Yuhan 接线：在资料页显示是否已添加头像（added / not added）。
        io.info("  Picture:   " + (user.hasProfilePicture()
                ? "added (" + user.getProfilePictureFormat() + ")" : "not added"));
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US03 #4 / US16 #51】主负责人：Henry。
     * 做什么：逐项询问新值并立即检查，直接回车保留原值，然后一次性保存。
     * 对接／验证：邮箱／电话与他人重复时显示 “Error: Email address is already registered…” 并保持原资料。
     */
    private void editDetails() {
        User user = accounts.requireUser();
        String me = user.getIdKey();
        io.info("Press Enter to keep a value. " + ConsoleIO.CANCEL_HINT);
        // 【US14 逐项检查】每一项输入后立即检查；空输入保留原值。
        String name = io.askValid("Full name [" + user.getFullName() + "]: ",
                v -> v.isBlank() ? user.getFullName() : org.example.service.Validation.text(v, "Full name"));
        String email = io.askValid("Email [" + user.getEmail() + "]: ",
                v -> v.isBlank() ? user.getEmail() : accounts.checkEmail(v, me));
        String phone = io.askValid("Phone [" + user.getPhone() + "]: ",
                v -> v.isBlank() ? user.getPhone() : accounts.checkPhone(v, me));
        accounts.updateProfile(name, email, phone);
        io.info("Profile updated.");
    }

    /**
     * 【PR s2-r2-account-screens】实现：Henry ｜ 审核：Yuhan
     * 【Sprint 2 · US03 #4 / US15 #50】主负责人：Henry。
     * 做什么：输入当前密码、新密码和确认密码（全部隐藏输入），每项输入后立即检查。
     * 对接／验证：当前密码错误、长度不符合时显示错误；成功后新密码以哈希保存。
     */
    private void changePassword() {
        // 【US14 逐项检查】当前密码错误、新密码不合规、两次不一致都立即提示并只重问该项。
        io.info(ConsoleIO.CANCEL_HINT);
        String current = io.passwordValid("Current password: ", accounts::checkCurrentPassword);
        String next = io.passwordValid("New password: ", AccountService::checkPassword);
        io.passwordValid("Confirm new password: ", v -> {
            if (!v.equals(next)) {
                throw new IllegalArgumentException("Passwords do not match; type the new password again");
            }
            return v;
        });
        accounts.changePassword(current, next);
        io.info("Password changed.");
    }

}

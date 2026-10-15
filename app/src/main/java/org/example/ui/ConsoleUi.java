package org.example.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.example.model.DigitalScroll;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.service.AccountService;
import org.example.service.ScrollEditService;
import org.example.service.ScrollQueryService;
import org.example.service.ScrollService;
import org.example.service.StatisticsService;

/**
 * Provides a role-aware console interface.
 *
 * <p>【Sprint 2 · US13 #48 / US14 #49 / US15 #50】主负责人：Yuhan（菜单集成，本文件只由 Yuhan 修改）。
 * 审核人：Henry（R2 主审）、全组（R4 集成验收）。
 * 做什么：显示身份、按角色生成编号主菜单，并把菜单项接到各成员的页面类：
 * Henry 的 AccountScreens（注册／添加用户／我的资料）、Sixian 的 BrowseScreen（浏览／筛选／排序／预览／下载）、
 * Stella 的 MyScrollsScreen（我的卷轴）和 StatisticsScreen（统计）。
 * 对接／验证：编号在三种角色间一致：1 永远是 Browse scrolls，0 永远是 Quit；会员和管理员共用 1–5，管理员额外 6–9。
 * Sprint 1 的命令词（browse、login、upload、users…）作为隐藏别名继续可用，权限仍由服务层检查。
 */
public final class ConsoleUi {

    private final AccountService accounts;
    private final ScrollService scrolls;
    private final ConsoleIO io;
    private final AccountScreens accountScreens;
    private final BrowseScreen browseScreen;
    private final MyScrollsScreen myScrollsScreen;
    private final StatisticsScreen statisticsScreen;

    /**
     * 主负责人：Yuhan。
     * 做什么：保留 Sprint 1 的 4 参数构造方法，供已有测试使用；密码按普通行读取，不提供卷轴编辑服务。
     * 对接／验证：ConsoleUiTest、AccountConsoleTest、ScrollConsoleTest 无需改构造方式。
     *
     * @param accounts account management service
     * @param scrolls scroll management service
     * @param input console input
     * @param output console output
     */
    public ConsoleUi(AccountService accounts, ScrollService scrolls,
                     Scanner input, PrintWriter output) {
        this(accounts, scrolls, null, new ConsoleIO(input, output));
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：完整构造方法：创建各成员的页面对象，全部共用同一个 ConsoleIO。
     * 对接／验证：App 传入 ConsoleIO.system（真实终端隐藏密码）和 ScrollEditService；
     * edits 为 null 时 “My scrolls” 显示错误而不是崩溃。
     *
     * @param accounts account management service
     * @param scrolls scroll management service
     * @param edits scroll editing service, or {@code null} if unavailable
     * @param io shared console helper
     */
    public ConsoleUi(AccountService accounts, ScrollService scrolls, ScrollEditService edits, ConsoleIO io) {
        this.accounts = accounts;
        this.scrolls = scrolls;
        this.io = io;
        this.accountScreens = new AccountScreens(accounts, io);
        this.browseScreen = new BrowseScreen(scrolls, new ScrollQueryService(scrolls), accounts, io);
        this.myScrollsScreen = edits == null ? null : new MyScrollsScreen(edits, io);
        this.statisticsScreen = new StatisticsScreen(new StatisticsService(scrolls, accounts), io);
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：先完成首次管理员设置，然后每轮显示身份和对应角色的编号菜单，直到选择 0 或输入结束。
     * 对接／验证：任何无效输入只显示 “Error: …” 并重新显示当前菜单，不退出程序（US13/US14）。
     */
    public void run() {
        io.info("Virtual Scroll Access System — Sprint 2");

        try {
            while (accounts.needsBootstrap()) {
                io.info("First run: create the initial administrator (no default password).");
                try {
                    accountScreens.createAccount("bootstrap");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    io.error(e.getMessage());
                }
            }
            boolean running = true;
            while (running) {
                showIdentity();
                running = mainMenu().runOnce(io, "> ");
            }
        } catch (NoSuchElementException e) {
            io.info("Input closed.");
        }

        io.info("Goodbye.");
        io.flush();
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：按当前角色生成主菜单。共用选项先加入，所以编号在会员和管理员间一致；
     * 其余命令作为隐藏别名加入，输入后由服务层给出 “Please log in first” 或 “Administrator access required”。
     * 对接／验证：访客 1–3；会员 1–5；管理员 1–9；0 = Quit。ConsoleUiTest 检查每种角色的编号文字。
     */
    private Menu mainMenu() {
        var current = accounts.currentUser();
        Menu menu;
        if (current.isEmpty()) {
            menu = new Menu("Guest menu:", "Quit", "quit")
                    .option("Browse scrolls", "browse", a -> browseScreen.show())
                    .option("Register", "register", a -> accountScreens.createAccount("register"))
                    .option("Log in", "login", a -> login());
            addHidden(menu, "upload", "my-scrolls", "profile", "logout", "users", "add-user", "delete-user", "stats");
            return menu;
        }
        boolean admin = current.get().getRole() == UserRole.ADMIN;
        menu = new Menu(admin ? "Admin menu:" : "Member menu:", "Quit", "quit")
                .option("Browse scrolls", "browse", a -> browseScreen.show())
                .option("Upload a scroll", "upload", a -> upload())
                .option("My scrolls (edit / delete)", "my-scrolls", a -> myScrolls())
                .option("My profile", "profile", a -> accountScreens.profile())
                .option("Log out", "logout", a -> logout());
        if (admin) {
            menu.option("View all users", "users", a -> showUsers())
                    .option("Add a user", "add-user", a -> accountScreens.createAccount("admin"))
                    .option("Delete a user", "delete-user", a -> deleteUser())
                    .option("Scroll statistics", "stats", a -> statisticsScreen.show());
        } else {
            addHidden(menu, "users", "add-user", "delete-user", "stats");
        }
        addHidden(menu, "register", "login");
        return menu;
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：把命令词加入为隐藏别名，指向与可见选项相同的动作。
     * 对接／验证：保证 Sprint 1 的脚本和权限测试（例如会员输入 users）结果不变。
     */
    private void addHidden(Menu menu, String... aliases) {
        for (String alias : aliases) {
            menu.hidden(alias, a -> {
                switch (alias) {
                    case "browse" -> browseScreen.show();
                    case "register" -> accountScreens.createAccount("register");
                    case "login" -> login();
                    case "upload" -> upload();
                    case "my-scrolls" -> myScrolls();
                    case "profile" -> accountScreens.profile();
                    case "logout" -> logout();
                    case "users" -> showUsers();
                    case "add-user" -> accountScreens.createAccount("admin");
                    case "delete-user" -> deleteUser();
                    default -> statisticsScreen.show();
                }
            });
        }
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：显示当前姓名、用户名和角色，未登录时显示访客（Sprint 1 原有）。
     */
    private void showIdentity() {
        String identity = accounts.currentUser()
                .map(user -> user.getFullName()
                        + " (" + user.getUsername() + ") ["
                        + user.getRole() + "]")
                .orElse("Guest [GUEST]");

        io.info(identity);
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：ID key 必填立即检查；密码通过 ConsoleIO 隐藏输入（US15）。
     * 对接／验证：错误凭据统一提示 “Invalid ID key or password”（Sprint 1 原有）。
     */
    private void login() {
        accounts.login(io.askRequired("ID key: ", "ID key"), io.password("Password: "));
        io.info("Logged in.");
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：退出登录，回到访客菜单。
     */
    private void logout() {
        accounts.requireUser();
        accounts.logout();
        io.info("Logged out.");
    }

    /**
     * 主负责人：Yuhan（菜单接线）；上传业务：Stella（Sprint 1）。审核人：Stella。
     * 做什么：先检查登录，再用必填输入收集 ID、名称、路径；成功后显示识别出的格式。
     *
     * @throws IOException if the source file cannot be read
     */
    private void upload() throws IOException {
        accounts.requireUser();
        String id = io.askRequired("Scroll ID: ", "Scroll ID");
        String name = io.askRequired("Scroll name: ", "Scroll name");
        String source = io.askRequired("Source file path: ", "Source file path");
        DigitalScroll scroll = scrolls.upload(id, name, Path.of(source));
        io.info("Scroll uploaded successfully.");
        io.info("Detected format: " + scroll.getFileFormat());
    }

    /**
     * 主负责人：Yuhan（接线）；页面：Stella。
     * 做什么：打开 “我的卷轴” 页面；未配置编辑服务时给出清楚的错误。
     */
    private void myScrolls() {
        accounts.requireUser();
        if (myScrollsScreen == null) {
            throw new IllegalStateException("Scroll editing is not available in this session");
        }
        myScrollsScreen.show();
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：显示管理员可查看的用户资料，不显示密码哈希（Sprint 1 原有）。
     */
    private void showUsers() {
        for (User user : accounts.listUsers()) {
            io.info(
                    user.getIdKey() + " | "
                            + user.getUsername() + " | "
                            + user.getFullName() + " | "
                            + user.getEmail() + " | "
                            + user.getPhone() + " | "
                            + user.getRole());
        }
    }

    /**
     * 主负责人：Yuhan。
     * 做什么：管理员删除用户；先检查权限再询问 ID。
     */
    private void deleteUser() {
        accounts.listUsers();
        accounts.deleteUser(io.askRequired("User ID to delete: ", "User ID"));
        io.info("User deleted.");
    }
}

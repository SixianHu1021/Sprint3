<!-- 主负责人：Henry（Scrum Master，协调执行与合并）；四位成员核对自己的工作。本文是计划，不是已完成记录。 -->
# Sprint 2：每一轮谁开分支、写什么、怎样提 PR 和合并

**起点：团队仓库 `main` 目前停在 `dcaab95`（Sprint 1 回顾会议记录），代码与 tag `sprint-1` 相同。**
以下操作都在**团队仓库**执行；`sprint2_copy` 只用来查参考代码（里面已经是本 Sprint 全部完成后的样子）。
每一轮结束后才进入下一轮。没有开发任务的人负责审核或准备测试，不需要硬开一个空分支。

文件位置：生产代码在 `app/src/main/java/org/example/`，测试在 `app/src/test/java/org/example/`。
下面“写函数”包括构造方法。每个文件/函数的注释已写好 **主负责人 / 审核人 / 做什么 / 对接／验证**，照着做即可。

## Sprint 2 一页概览

| 项目 | 内容 |
| --- | --- |
| Sprint 时间 | 2026-10-15（周四，Week 10 tutorial）→ 2026-10-22（周四，Week 11 tutorial 演示） |
| 报告和源码截止 | 2026-10-25（周日）23:59，Canvas |
| Sprint Goal | 让 VSAS 更好用、更安全：用户能管理自己的资料和卷轴，按条件找到卷轴并按格式预览；管理员能看到统计；所有菜单编号化，输入错误立即提示，密码隐藏输入。 |
| 角色 | Product Owner：Stella；Scrum Master：Henry；开发：Yuhan、Sixian（四人都写代码） |
| 故事 | 10 个，共 38 点（见下表） |
| 版本标签 | `sprint-2`（沿用 Sprint 1 的 `sprint-1` 命名） |

| 成员 | 故事（Issue） | 点数 |
| --- | --- | --- |
| Henry（SM） | US03 修改资料和密码 #4（5）、US16 邮箱/电话不重复 #51（3）；另含 CI 覆盖率门槛 | 8 |
| Stella（PO） | US10 编辑/删除自己的卷轴 #11（5）、US12 管理员创建用户与统计 #13（3）；另含卷轴模型/存储改动、README | 8 |
| Yuhan | US13 编号菜单 #48（5）、US14 无效输入即时提示 #49（3）、US15 隐藏密码 #50（2） | 10 |
| Sixian | US06 筛选 #7（5）、US17 按格式预览 #52（5）、US18 按下载次数排序 #53（2） | 12 |

PO 和 SM 的编码量略少于两位开发者，符合任务要求；两人每一轮都有代码 PR。

## 本 Sprint 的防冲突规则（Sprint 1 回顾的改进行动）

Sprint 1 回顾指出：“共享 ConsoleUi 需要顺序集成，部分入口职责不清”。本 Sprint 这样处理：

1. **`ConsoleUi.java` 只有 Yuhan 修改**（R2、R4 两次，都是 Yuhan 自己的 PR）。其他人把页面写在**自己的新文件**里：
   Henry → `ui/AccountScreens.java`；Sixian → `ui/BrowseScreen.java`；Stella → `ui/MyScrollsScreen.java`、`ui/StatisticsScreen.java`。
2. **新功能放进新服务类**，不再反复改 `ScrollService`：Stella → `ScrollEditService`、`StatisticsService`；
   Sixian → `ScrollQueryService`、`ScrollFilter`、`SortOrder`、`FileFormat`、`ScrollPreviewRenderer`；Henry → `ContactValidator`。
3. 仍需修改的共享文件只有下表几处，每处只指定一个人、一个轮次：

| 共享文件 | 谁改 | 哪一轮 | 改什么 |
| --- | --- | --- | --- |
| `service/AccountService.java` | Henry | R1 | `create` 加唯一性；新增 `updateProfile/changePassword`；`requireAdmin` 改 public |
| `service/Validation.java` | Yuhan | R1 | 只改错误消息文字 |
| `model/DigitalScroll.java`、`repository/ScrollRepository.java`、`repository/file/FileScrollRepository.java` | Stella | R1 | 格式字段、`with*` 方法、`deleteById`、原位替换 |
| `service/ScrollService.java` | Stella（R1 一行）→ Sixian（R2） | R1、R2 | R1：`download` 改用 `withRecordedDownload()`；R2：上传记录格式、下载补扩展名 |
| `repository/UserRepository.java`、`FileUserRepository.java` | Henry | R1 | `findByEmail/findByPhone` |
| `ui/ConsoleUi.java`、`App.java` | Yuhan | R2、R4 | 编号菜单；接入所有页面 |
| `app/build.gradle`、`Jenkinsfile` | Henry | R1 | 覆盖率门槛、Checkstyle 阶段、打包可执行文件 |
| `README.md` | Stella | R5 | Sprint 2 功能和运行说明 |

4. 中间 PR 用 `Relates to #N`；故事全部验收后才在 Issue 中勾选并关闭，不用 `Closes` 提前关闭。

## 先记住：每个分支都按这个流程提交（与 Sprint 1 相同）

### 开始写代码之前

先在团队仓库终端确认 `pwd` 和 `git remote -v` 是 `A2-T16-2`，不是 copy。
`git status --short` 应没有尚未处理的修改（注意 `app/data/` 是本地运行数据，不要提交）。

```sh
git switch main
git pull --ff-only origin main
git switch -c s2-r1-你的任务名     # 每次新任务都从最新 main 开分支
```

若分支已经创建过，用 `git switch 分支名` 返回它，不重复执行 `-c`。

### 写完这一份任务之后

1. 运行 `./gradlew clean build`（现在包含 Checkstyle 和 **≥75% 覆盖率门槛**），解决失败。
2. 在 IDE 的 Git 面板只选本任务的源文件和测试，查看 diff，再暂存。以 Henry 第一轮为例：

```sh
git add app/src/main/java/org/example/service/ContactValidator.java
git add app/src/main/java/org/example/service/AccountService.java
git add app/src/test/java/org/example/ProfileServiceTest.java
git diff --cached --check
git diff --cached
git commit -m "feat(accounts): update profile and reject duplicate contacts (#4, #51)"
git push -u origin HEAD
```

3. GitHub 点 **Compare & pull request**：base `main`，compare 自己的分支；填写下文给出的 PR 标题。
   正文写：实际完成内容、`./gradlew clean build` 结果（测试数、覆盖率）、**手动运行验证步骤**（回顾改进行动 3）、`Relates to #N`。
4. 邀请本轮指定的审核人。审核人检查代码、注释中的“对接／验证”、测试；作者在同一分支继续修改、commit、push。
5. 审核通过 + Jenkins 检查的是**最新提交** → 由 Henry（SM）合并；Henry 自己的 PR 由 Yuhan 合并。
   与 Sprint 1 实际做法一致，使用 **Create a merge commit**。
6. 在看板把卡片移到 Done 前，保存 PR 链接和 Jenkins 构建号（报告要用）。所有人 `git switch main && git pull --ff-only origin main`。

---

## 第 0 步（10-15 周四 tutorial 后 ～ 10-16 周五）：Sprint 2 Planning

- **主持：Henry（SM）。PO Stella 讲解优先级和验收条件**，全员 Planning Poker 估点（结果见上表，可在会上调整并记录理由）。
- Stella 把 10 个故事加上 `sprint2` 标签和负责人（GitHub Assignee），为 #48–#53 补 `sprint2` 标签，
  在每个 Issue 下贴本文对应的任务清单（Issue 里已有的复选框就是任务）。
- 看板（GitHub Projects）新建 Sprint 2 视图：Todo / In Progress / In Review / Done。**会后截图**。
- Henry 把 Sprint 1 tutor 反馈（预览按格式、下载保留扩展名、排序搜索）对应到 US17、US18、US06，写入会议记录。
- 会议记录：`docs/meeting-notes/sprint-2-planning-2026-10-16.md`（日期、时长、出席、决定、Sprint Goal、估点、分工、截图）。

---

## 第一轮（10-16 周五 ～ 10-17 周六）：四人并行打基础，互不改同一文件

### Henry 先开一个小 CI 分支（SM 工作，建议第一份合并）

```sh
git switch -c s2-r1-ci-quality-gate
```

| 文件 | 要做的事 |
| --- | --- |
| `app/build.gradle` | 新增 `jacocoTestCoverageVerification` 规则：LINE 覆盖率 ≥ 0.75；`check` 依赖它 |
| `Jenkinsfile` | 新增 `Code Quality` 阶段（checkstyleMain/checkstyleTest，归档报告）；Package 阶段增加 `distZip` 并归档 zip |

- **PR 标题：** `Enforce coverage gate and add code quality stage to Jenkins`
- **审核人：** Yuhan。**通过标准：** Jenkins 新构建出现 Code Quality 阶段，artifacts 里有 `app.zip`。截图 Stage View。

### Henry：US03 + US16 业务层

```sh
git switch -c s2-r1-profile-contacts
```

| 文件 | 要实现的函数 |
| --- | --- |
| `repository/UserRepository.java` | 新增接口方法 `findByEmail()`、`findByPhone()` |
| `repository/file/FileUserRepository.java` | 实现上面两个方法，私有 `phoneKey()` |
| 新建 `service/ContactValidator.java` | 私有构造方法、`email()`、`phone()`、`requireUnique()`、私有 `isOwner()` |
| `service/AccountService.java` | 修改 `create()`；新增 `requirePasswordPolicy()`、`updateProfile()`、`changePassword()`；`requireAdmin()` 改为 `public` |
| 新建测试 `ProfileServiceTest.java`、`DuplicateContactTest.java` | 见文件头注释 |
| 修改测试 `RegistrationTest.passwordBoundariesAreAccepted` | 两个账户改用不同邮箱/电话（US16 后旧写法会被拒绝） |

- **PR 标题：** `Update profile and password and reject duplicate contacts`
- **关联：** `Relates to #4, #51`；正文说明 `requireAdmin` 改 public 供 Stella 的统计使用（#13）。
- **审核人：** Yuhan（登录、密码哈希的 Sprint 1 负责人）。
- **通过标准：** 资料修改重启后仍在；旧密码失效；大写邮箱与已有邮箱视为重复；管理员添加用户也被检查。

### Yuhan：US13/US14/US15 公共输入层

```sh
git switch -c s2-r1-console-io
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/PasswordReader.java` | 接口 `readPassword(prompt)` |
| 新建 `ui/ConsoleIO.java` | 两个构造方法、`system()`、`ask()`、`askRequired()`、`askOptional()`、`confirm()`、`password()`、`info()`、`error()`、`flush()` |
| 新建 `ui/Menu.java` | 构造方法、`option()`、`hidden()`、`render()`、`runOnce()`、`select()`、`loop()`、`Action`、`Option`、`Selection` |
| `service/Validation.java` | `text()` 拆成三条具体错误消息（规则不变） |
| 新建测试 `MenuTest.java`、`ConsoleIOTest.java` | 见文件头注释 |

- **PR 标题：** `Add numbered menu and shared console input helpers`
- **关联：** `Relates to #48, #49, #50`。**审核人：** Henry；Sixian 看 `Menu` 的别名参数是否满足浏览页。
- **通过标准：** 不改 ConsoleUi，现有 93 个测试全部仍然通过；新测试覆盖越界、非数字、空输入、隐藏密码与警告。

### Stella：卷轴模型与存储（为 US10、US17 准备）

```sh
git switch -c s2-r1-scroll-storage
```

| 文件 | 要实现或修改的函数 |
| --- | --- |
| `model/DigitalScroll.java` | `UNKNOWN_FORMAT`、`fileFormat` 字段、8 参数构造方法（7 参数构造方法委托给它）、`getFileFormat()`、`withName()`、`withContent()`、`withRecordedDownload()` |
| `repository/ScrollRepository.java` | 新增 `deleteById()` |
| `repository/file/FileScrollRepository.java` | 读取 `format`（缺省 unknown）；`save()` 改为原位替换；新增 `deleteById()`、私有 `persist()`、`indexOf()` |
| `service/ScrollService.java` | **只改 `download()` 的一行**：`scrolls.save(scroll.withRecordedDownload())`（否则下载后格式丢失） |
| 新建测试 `ScrollStorageTest.java` | 见文件头注释 |

- **PR 标题：** `Store scroll formats and support scroll deletion`
- **关联：** `Relates to #11, #52`。**审核人：** Sixian（格式与下载的使用者）。
- **通过标准：** 旧的 `scrolls.properties`（没有 format）仍能读取；下载/改名后卷轴在列表中的位置不变。

### Sixian：筛选、排序和格式识别（新文件）

**等 Stella 的存储 PR 合并后再提 PR**（`FileFormat` 用到 `DigitalScroll.UNKNOWN_FORMAT`，测试替身实现了新的 `deleteById`）。
可以先在本地开发，提 PR 前同步：`git fetch origin && git merge --no-edit origin/main`。

```sh
git switch -c s2-r1-query-format
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `service/FileFormat.java` | `detect()`、`category()`、`describe()`、`isText()`、私有 `extension()`、`startsWith()` |
| 新建 `service/ScrollFilter.java` | record + `none()`、四个 `with*()`、`isEmpty()`、`matches()`、`describe()` |
| 新建 `service/SortOrder.java` | 三个枚举值、`label()`、`comparator()` |
| 新建 `service/ScrollQueryService.java` | 构造方法、`search()`、`parseDate()` |
| 新建测试 `FileFormatTest.java`、`ScrollQueryServiceTest.java` | 见文件头注释 |

- **PR 标题：** `Add scroll filtering, sorting and file format detection`
- **关联：** `Relates to #7, #52, #53`。**审核人：** Stella。
- **通过标准：** 四种筛选可单独使用并可组合；清除后回到完整列表；下载次数排序从多到少、平局按日期从新到旧。

**本轮合并顺序：** Henry CI 最先；Henry 业务层、Yuhan 输入层任意先后；**Stella 存储 → Sixian 筛选/格式**。
合并完大家同步 main。

---

## 第二轮（10-18 周日 ～ 10-19 周一）：业务接入 + 第一版编号菜单

**10-18 周日晚 Stand-up 1（Henry 主持，15 分钟）**：每人说完成了什么、接下来做什么、阻碍；Stella 更新看板并截图。

### Sixian：US17 接入上传和下载（先合并）

```sh
git switch -c s2-r2-format-preview
```

| 文件 | 要实现的函数 |
| --- | --- |
| `service/ScrollService.java` | `upload()`：调用 `FileFormat.detect` 并用 8 参数构造方法保存；`download()`：拒绝文件夹目标、补扩展名、返回实际路径；新增 `withExtension()` |
| 新建 `service/ScrollPreviewRenderer.java` | `render()`、私有 `text()`、`image()`、`pdf()`、`archive()`、`hex()` |
| 新建测试 `ScrollFormatTest.java` | 见文件头注释 |

- **PR 标题：** `Record file formats and render format-aware previews`
- **关联：** `Relates to #52`。**审核人：** Stella（`upload` 是她 Sprint 1 的方法）。
- **通过标准：** 上传 `report.PDF` 记录为 `pdf`；下载到 `my-copy` 实际保存为 `my-copy.pdf`；原有下载测试全部仍通过。

### Stella：US10 编辑/删除 + US12 统计（Sixian 之后合并）

```sh
git switch -c s2-r2-scroll-edit-stats
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `service/ScrollEditService.java` | 构造方法、`myScrolls()`、`rename()`、`replaceContent()`、`delete()`、私有 `requireOwned()`、`readSource()` |
| 新建 `service/StatisticsService.java` | `Row`、`Report`、构造方法、`report()` |
| 新建测试 `ScrollEditServiceTest.java`、`StatisticsTest.java`（先写 `adminSeesPerScrollCountsAndTotals`、`nonAdminsCannotViewStatistics` 两个服务测试） | 见文件头注释 |

- **PR 标题：** `Let uploaders edit and delete their scrolls and add admin statistics`
- **关联：** `Relates to #11, #13`。**审核人：** Sixian（编辑）、Yuhan（`requireAdmin` 用法）。
- **为什么在 Sixian 之后合并：** 测试断言上传后的格式为 `txt`，依赖 Sixian 本轮的上传改动。

### Henry：账户页面（US03 界面 + 表单迁移）

```sh
git switch -c s2-r2-account-screens
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/AccountScreens.java` | 构造方法、`createAccount()`（从 ConsoleUi 迁来并改用 ConsoleIO）、私有 `askRole()`、`profile()`、`showProfile()`、`editDetails()`、`changePassword()`、`keep()` |
| 新建测试 `ProfileConsoleTest.java` | 直接创建 `AccountScreens` 测试，不依赖 ConsoleUi |

- **PR 标题：** `Add account screens for profile editing and account forms`
- **关联：** `Relates to #4, #12, #13`。**审核人：** Yuhan。
- 此时 ConsoleUi 里仍有旧的 `createAccount`；**Henry 不改 ConsoleUi**，R4 由 Yuhan 换成调用本类。

### Yuhan：第一版编号菜单（US13/US14/US15 可演示）

```sh
git switch -c s2-r2-numbered-console
```

| 文件 | 要做的事 |
| --- | --- |
| `ui/ConsoleUi.java` | 构造方法改为内部创建 `ConsoleIO`；`run()` 用 `Menu` 生成访客/会员/管理员编号菜单；所有输入改用 `ConsoleIO`（必填重问、统一 `Error:`、`password()`）；Sprint 1 命令词作为隐藏别名保留 |
| `App.java` | 改用 `ConsoleIO.system(...)`，真实终端隐藏密码 |
| 测试 `ConsoleUiTest`、`ScrollConsoleTest.uploadAppearsOnlyInAuthenticatedMenus` | 只修改断言菜单文字的几行（Sixian 审核后者） |

- **PR 标题：** `Switch console to numbered menus with consistent input feedback`
- **关联：** `Relates to #48, #49, #50`。**审核人：** Henry（主审）、Sixian（浏览命令兼容）。
- **说明：** 本轮只编号 Sprint 1 已有功能，编号在 R4 加入新页面后定稿。

**本轮合并顺序：Sixian → Stella；Henry、Yuhan 任意先后（两者文件不重叠）。**

---

## 第三轮（10-19 周一 ～ 10-20 周二）：各自的页面类（全部是新文件）

### Sixian：浏览页面

```sh
git switch -c s2-r3-browse-screen
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/BrowseScreen.java` | 构造方法、`show()`、`list()`、`preview()`、`showPreview()`、`download()`、`chooseFilter()`、`chooseSort()`、`clearFilters()`、`scrollId()`、`capitalize()` |
| 新建测试 `BrowseConsoleTest.java` | 见文件头注释 |

- **PR 标题：** `Add browse screen with filters, sorting and format previews`
- **关联：** `Relates to #7, #52, #53`。**审核人：** Stella（字段）、Yuhan（菜单风格）。

### Stella：我的卷轴 + 统计页面

```sh
git switch -c s2-r3-my-scrolls-stats-screens
```

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/MyScrollsScreen.java` | 构造方法、`show()`、`list()`、`rename()`、`replace()`、`delete()`、`scrollId()` |
| 新建 `ui/StatisticsScreen.java` | 构造方法、`show()` |
| 新建测试 `MyScrollsConsoleTest.java`；`StatisticsTest` 加入 `screenPrintsTableAndTotals`、`screenShowsEmptyState` | 见文件头注释 |

- **PR 标题：** `Add My scrolls and statistics screens`
- **关联：** `Relates to #11, #13`。**审核人：** Sixian、Yuhan。

### Henry / Yuhan 本轮做什么

- **Henry（SM）**：审核上面两份 PR；准备演示数据和演示脚本（`delivery.md` 的场景表）；组织 Stand-up 2；检查看板和 Jenkins。
- **Yuhan**：审核两份 PR 中 `Menu`/`ConsoleIO` 的用法，提前在本地准备 R4 集成分支。

**10-20 周二晚 Stand-up 2（Henry 主持）**：确认 R1–R3 全部合并、R4 开始；记录阻碍和 Jenkins 构建号；截图出席。

**两份 PR 是不同的新文件，可以任意先后合并。合并后大家同步 main。**

---

## 第四轮（10-20 周二 ～ 10-21 周三）：Yuhan 接入全部页面

```sh
git switch -c s2-r4-integration
```

`ui/ConsoleUi.java`：

```text
构造方法(accounts, scrolls, edits, io)    创建 AccountScreens / BrowseScreen / MyScrollsScreen / StatisticsScreen
保留 4 参数构造方法                         供 Sprint 1 测试继续使用
mainMenu()       访客 1–3；会员 1–5；管理员 1–9；0 = Quit（定稿编号）
addHidden()      Sprint 1 命令词别名
login/logout/upload/myScrolls/showUsers/deleteUser
删除旧的 createAccount/browse/preview/download（已分别迁到 Henry、Sixian 的页面）
```

`App.java`：创建 `ScrollEditService`，传入新的 4 参数构造方法。
新建测试 `NumberedMenuConsoleTest.java`（全流程只用数字、三种角色编号一致、无效输入、ID 为空重问、隐藏密码、
管理员第 7/9 项创建用户与查看统计）。

- **PR 标题：** `Integrate Sprint 2 screens into the numbered console`
- **关联：** 十个故事全部 `Relates to`。
- **审核人：Henry 主审；Stella、Sixian 各自确认自己页面的入口和编号。**
- **合并标准：** `./gradlew clean build` 通过（190 个左右测试、覆盖率 >75%）；Jenkins 对**这一提交**构建成功；
  四人按 `delivery.md` 场景表手动走一遍。

如有问题，由问题所属模块负责人从最新 main 开 `s2-fix-具体问题`，同样提 PR、审核和合并。

---

## 第五轮（10-21 周三 ～ 10-22 周四）：验收、README、打标签、演示

1. **Stella（PO）：** 开 `s2-r5-readme` 更新 README（Sprint 2 功能、编号菜单表、`installDist` 运行方式和 JDK 21 要求、测试与覆盖率）；Henry 审核。
2. **每个故事负责人：** 对照 Issue 复选框和 `acceptance-criteria.md` 逐条验收，在 Issue 评论贴 PR 链接和测试名，勾选复选框；
   **PO Stella 最终确认**后关闭 Issue。
3. **Henry：** 确认 Jenkins 最新 main 构建成功，记录构建号、commit SHA、测试数、覆盖率（截图）。
4. **打标签（Henry，团队同意后）：**

```sh
git switch main && git pull --ff-only origin main
git rev-parse HEAD                     # 记下演示 SHA
git tag -a sprint-2 DEMO_COMMIT_SHA -m "Sprint 2 demonstration"
git push origin sprint-2
```

   不覆盖已有标签；提交到 Canvas 的源码 = 这个 tag 的代码。
5. **10-22 周四 tutorial：Sprint Review / 演示**（每人演示自己的故事，顺序见 `delivery.md`），记录 tutor 反馈。
6. **演示后 Sprint 2 Retrospective**（Henry 主持，20 分钟）：做得好 / 需改进 / 行动项，记录到
   `docs/meeting-notes/sprint-2-retrospective-2026-10-22.md`。
7. **10-23 ～ 10-25：** 写报告（Overleaf），附封面签名，提交报告 + `sprint-2` 源码到 Canvas。

---

## 每轮要保存什么，填报告哪里（Overleaf 报告章节）

| 发生的事情 | 当时保存什么 | 报告位置 |
| --- | --- | --- |
| Planning | 日期、时长、出席、Sprint Goal、估点和理由、分工、看板截图 | 1.2、1.3、2.1 |
| 两次 Stand-up | 每人进展、阻碍、下一步；Zoom 出席截图；当时 Jenkins 构建号 | 2.2、附录 |
| 每个 PR | PR 链接/编号、审核意见、修改结果 | 3.1、3.3、4.1 |
| 合并冲突或集成问题 | 哪个文件、原因、如何解决；没有冲突也如实写 | 3.1 |
| Jenkins | Stage View 截图、构建号、SHA、测试数、覆盖率、Code Quality 阶段 | 3.2 |
| 看板变化 | Sprint 开始、中途、结束三张截图 | 1.2、2.4 |
| 功能验收 | 每个故事的代表性终端截图 | 1.1、4.4 |
| Review 与 Retrospective | tutor 反馈、行动项及负责人 | 2.3 |
| 个人工作 | 技术（代码、测试、审核）和非技术（会议、看板、文档、演示） | 4.2、5.4 |

Jenkins 必须检查 PR 最新提交或明确的待合并 SHA，不能拿旧 main 的成功结果证明新 PR 通过。
参考 copy 的情况按实际与课程规则说明，代码注释中的“主负责人”是分工，不是个人作者证明。

## 发生阻碍时，只用这三条规则

1. **分支需要队友已合并代码：** 先提交自己的修改，再 `git fetch origin`、`git merge --no-edit origin/main`。
   冲突时找共享方法负责人一起解决，测试后再提交和推送，不强制覆盖别人的代码。
2. **自己 PR 需要修改：** 在同一分支继续 commit/push，原 PR 自动更新，不重新开重复 PR。
3. **依赖的 PR 还没合并：** 先审核或准备测试，不把不完整代码当完成，也不用凑空分支/空提交。

## 一眼看完整顺序

```text
第 0 步：Planning（估点 38 点，Sprint Goal，看板）
第一轮：Henry CI 门槛 → Henry 资料/联系方式 + Yuhan 输入层 + Stella 存储 → Sixian 筛选/排序/格式
        （Stand-up 1：10-18）
第二轮：Sixian 格式接入 → Stella 编辑/统计服务；Henry 账户页面；Yuhan 第一版编号菜单
第三轮：Sixian 浏览页面；Stella 我的卷轴 + 统计页面；Henry 演示脚本（Stand-up 2：10-20）
第四轮：Yuhan 接入全部页面 → 四人验收 → 合并
第五轮：Stella README → 故事 Done → 标签 sprint-2 → 10-22 演示 → 回顾 → 10-25 报告
```

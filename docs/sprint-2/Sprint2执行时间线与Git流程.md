<!-- 主负责人：Henry（Scrum Master，协调执行与合并）；四位成员核对自己的工作。本文是计划，不是已完成记录。 -->
# Sprint 2：每一轮谁开分支、写什么、谁审核、怎样合并

**起点：团队仓库 `A2-T16-2` 的 `main` 停在 `dcaab95`（tag `sprint-1` 之后只加了会议记录）。**
以下操作都在**团队仓库**执行；`sprint2`（个人仓库）只用来查参考代码，里面已经是本 Sprint 全部完成后的样子。
每一轮结束后才进入下一轮。没有开发任务的人负责审核或准备测试，不需要硬开空分支。

文件位置：生产代码在 `app/src/main/java/org/example/`，测试在 `app/src/test/java/org/example/`。

## 先看懂代码里的分工标记

`sprint2` 里**每个类、每个方法（包括测试方法）**注释的第一行都写明它属于哪个 PR、谁实现、谁审核：

```java
/**
 * 【PR s2-r1-profile-contacts】实现：Henry ｜ 审核：Yuhan
 * 主负责人：Henry。
 * 做什么：……
 * 对接／验证：……
 */
```

| 标记 | 含义 |
| --- | --- |
| `【PR 分支名】实现：A ｜ 审核：B` | 这段代码在该分支的 PR 里由 A 写，B 审核。一个方法被两个 PR 修改时两段都写，用 “；” 分开 |
| `【Sprint 1 已完成 · 本轮不改】原作者：A` | Sprint 1 的代码，本 Sprint 不动；真要改先联系原作者 |
| 类注释开头的 `【分工】` | 列出本文件涉及的所有 PR，方便知道这个文件会被谁改 |

**提 PR 时：只提交注释第一行写着你这个分支名的代码。** 审核人就是那一行的 “审核”。

## Sprint 2 一页概览

| 项目 | 内容 |
| --- | --- |
| Sprint 时间 | 2026-10-15（周四，Week 10 tutorial）→ 2026-10-22（周四，Week 11 tutorial 演示） |
| 报告和源码截止 | 2026-10-25（周日）23:59，Canvas |
| Sprint Goal | 让 VSAS 更好用、更安全：用户能管理自己的资料和卷轴，按条件找到卷轴并按格式预览；管理员能看到统计；所有菜单编号化，输入错误立即提示，密码隐藏输入。 |
| 角色 | Product Owner：Stella；Scrum Master：Henry；开发：Yuhan、Sixian（四人都写代码） |
| 故事 | 10 个，共 38 点（US17 加入 “用默认应用打开” 后建议改为 8 点，合计 41） |
| 测试 | 完成后共 211 个（Sprint 1 的 93 个 + 新增 118 个），行覆盖率约 97% |
| 版本标签 | `sprint-2` |

| 成员 | 故事（Issue） | 点数 | 负责的 PR |
| --- | --- | --- | --- |
| Henry（SM） | US03 修改资料和密码 #4、US16 邮箱/电话不重复 #51；CI 门槛 | 5 + 3 = 8 | P1、P2、P7 |
| Stella（PO） | US10 编辑/删除自己的卷轴 #11、US12 管理员创建用户与统计 #13；卷轴模型/存储；README | 5 + 3 = 8 | P4、P8、P11、P13 |
| Yuhan | US13 编号菜单 #48、US14 即时提示 #49、US15 隐藏密码 #50 | 5 + 3 + 2 = 10 | P3、P9、P12 |
| Sixian | US06 筛选 #7、US17 按格式预览 / 用默认应用打开 #52、US18 排序 #53 | 5 + 5(→8) + 2 = 12(→15) | P5、P6、P10 |

## PR 总表（与代码注释的分支名一一对应）

| # | 轮次 | 分支 | 实现 | 审核 | 内容 | 合并顺序 |
| --- | --- | --- | --- | --- | --- | --- |
| P1 | R1 | `s2-r1-ci-quality-gate` | Henry | Yuhan | 覆盖率门槛、Jenkins Code Quality / 打包 | 本轮第一份 |
| P2 | R1 | `s2-r1-profile-contacts` | Henry | Yuhan | US03/US16 业务层 + 账户逐项检查 | 任意 |
| P3 | R1 | `s2-r1-console-io` | Yuhan | Henry | ConsoleIO、Menu、隐藏密码、cancel、具体错误消息 | 任意 |
| P4 | R1 | `s2-r1-scroll-storage` | Stella | Sixian | 卷轴格式字段、删除、原位替换、上传逐项检查 | 在 P5 之前 |
| P5 | R1 | `s2-r1-query-format` | Sixian | Stella | 筛选、排序、格式识别、可打开白名单 | 在 P4 之后 |
| P6 | R2 | `s2-r2-format-preview` | Sixian | Stella | 上传记录格式、下载补扩展名、按格式预览、用默认应用打开（服务层） | 在 P8 之前 |
| P7 | R2 | `s2-r2-account-screens` | Henry | Yuhan | 注册/添加用户表单（逐项检查）、我的资料页面 | 任意 |
| P8 | R2 | `s2-r2-scroll-edit-stats` | Stella | Sixian（编辑）、Yuhan（统计） | 编辑/删除服务 + 统计服务 | 在 P6 之后 |
| P9 | R2 | `s2-r2-numbered-console` | Yuhan | Henry | 第一版编号主菜单、上传逐项检查、App 隐藏密码 | 任意 |
| P10 | R3 | `s2-r3-browse-screen` | Sixian | Stella | 浏览页：筛选、排序、预览、下载、“用默认应用打开” 选项 | 任意 |
| P11 | R3 | `s2-r3-my-scrolls-stats-screens` | Stella | Sixian（我的卷轴）、Yuhan（统计页） | 我的卷轴页面、统计页面 | 任意 |
| P12 | R4 | `s2-r4-integration` | Yuhan | Henry（主审）；Stella、Sixian 确认自己页面 | 接入全部页面、定稿编号、App 接入打开功能 | 最后 |
| P13 | R5 | `s2-r5-readme` | Stella | Henry | README | 打标签前 |

## 客户 10-16 新增的两项要求（已包含在上面的 PR 中）

**1. 逐项检查（US14 #49）。** 表单每输入一项就检查，出错立即显示 `Error: …` 并**只重问这一项**：
ID / 用户名已被使用、密码不合规、邮箱 / 电话格式或重复、卷轴 ID / 名称重复、文件不存在或过大、
当前密码错误、确认密码不一致、不是自己的卷轴、下载路径是文件夹。任何提示输入 `cancel` 放弃表单。
登录除外（失败仍统一提示，不透露是 ID 还是密码错）。

| 谁 | 提供什么 | PR |
| --- | --- | --- |
| Yuhan | `ConsoleIO.askValid()`、`passwordValid()`、`InputCancelledException`、`Menu` 捕获 cancel | P3 |
| Henry | `AccountService.checkNewIdKey/checkNewUsername/checkPassword/checkEmail/checkPhone/checkCurrentPassword`、`ContactValidator.requireUniqueEmail/requireUniquePhone` | P2 |
| Stella | `ScrollService.checkNewScrollId/checkNewScrollName/checkSource`（P4）；`ScrollEditService.checkEditable/checkNewName`（P8） | P4、P8 |
| 各页面负责人 | 在自己的页面调用：Henry `AccountScreens`（P7）、Yuhan `ConsoleUi.upload`（P9）、Stella `MyScrollsScreen`（P11）、Sixian `BrowseScreen` 下载路径（P10） | — |

**2. 用默认应用打开预览（US17 #52，Sixian）。**
访客保持现在的信息预览；登录用户在预览页多一项 `2. Open in default app`。
只打开白名单类型（png、jpg/jpeg、gif、bmp、pdf、txt、md、csv、docx），其他类型只显示信息预览；
**打开不算下载**；没有桌面（Jenkins/Docker/SSH）或没有对应应用时显示原因，不崩溃；测试用假的 `FileOpener`，不弹窗。
服务层在 P6，浏览页选项在 P10，`App` 传入真实的 `FileOpener.desktop()` 在 P12。

## 本 Sprint 的防冲突规则（Sprint 1 回顾的改进行动）

1. **`ConsoleUi.java`、`App.java` 只有 Yuhan 修改**（P9、P12）。其他人把页面写在**自己的新文件**里。
2. **新功能放进新服务类**，不再反复改 `ScrollService`。
3. 仍需修改的共享文件，每处只指定一个人、一个 PR：

| 共享文件 | 谁改 | PR | 改什么 |
| --- | --- | --- | --- |
| `service/AccountService.java` | Henry | P2 | `create` 加唯一性；`updateProfile/changePassword`；6 个 `check…`；`requireAdmin` 改 public |
| `service/Validation.java` | Yuhan | P3 | 只改错误消息文字 |
| `model/DigitalScroll.java`、`repository/ScrollRepository.java`、`repository/file/FileScrollRepository.java` | Stella | P4 | 格式字段、`with*` 方法、`deleteById`、原位替换 |
| `service/ScrollService.java` | Stella → Sixian | P4 → P6 | P4：`download` 一行 + 3 个上传 `check…`；P6：上传记录格式、下载补扩展名 |
| `repository/UserRepository.java`、`FileUserRepository.java` | Henry | P2 | `findByEmail/findByPhone` |
| `service/FileFormat.java` | Sixian | P5 | 新文件（含 `isOpenable` 白名单） |
| `ui/ConsoleUi.java`、`App.java` | Yuhan | P9 → P12 | 编号菜单；接入所有页面和打开功能 |
| Sprint 1 测试 `ConsoleUiTest`、`ScrollConsoleTest`、`AccountConsoleTest`、`RegistrationTest` | 见下文各 PR | P2、P9、P12 | 只改因新行为而变化的断言，原作者审核 |
| `app/build.gradle`、`Jenkinsfile` | Henry | P1 | 覆盖率门槛、Checkstyle 阶段、打包 |
| `README.md` | Stella | P13 | Sprint 2 功能和运行说明 |

4. 中间 PR 用 `Relates to #N`；故事全部验收后才在 Issue 中勾选并关闭，不用 `Closes` 提前关闭。

## 每个分支都按这个流程提交（与 Sprint 1 相同）

### 开始写代码之前

先在团队仓库终端确认 `pwd` 和 `git remote -v` 是 `A2-T16-2`。`git status --short` 应没有未处理的修改
（`data/`、`app/data/` 是本地运行数据，不要提交）。

```sh
git switch main
git pull --ff-only origin main
git switch -c s2-r1-你的任务名     # 每次新任务都从最新 main 开分支
```

若分支已经创建过，用 `git switch 分支名` 返回它，不重复执行 `-c`。

### 写完这一份任务之后

1. 运行 `./gradlew clean build`（包含 Checkstyle 和 **≥75% 覆盖率门槛**），解决失败。
2. 只选本分支的源文件和测试，查看 diff，再暂存。以 P2（Henry）为例：

```sh
git add app/src/main/java/org/example/repository/UserRepository.java
git add app/src/main/java/org/example/repository/file/FileUserRepository.java
git add app/src/main/java/org/example/service/ContactValidator.java
git add app/src/main/java/org/example/service/AccountService.java
git add app/src/test/java/org/example/ProfileServiceTest.java
git add app/src/test/java/org/example/DuplicateContactTest.java
git add app/src/test/java/org/example/RegistrationTest.java
git diff --cached --check
git diff --cached
git commit -m "feat(accounts): update profile, reject duplicate contacts, check fields immediately (#4, #51)"
git push -u origin HEAD
```

3. GitHub 点 **Compare & pull request**：base `main`；填写下文的 PR 标题。正文写：实际完成内容、
   `./gradlew clean build` 结果（测试数、覆盖率）、**手动运行验证步骤**（回顾改进行动 3）、`Relates to #N`。
4. 邀请 PR 总表中的审核人。审核人检查代码、注释中的 “对接／验证”、测试；作者在同一分支继续修改、commit、push。
5. 审核通过 + Jenkins 检查的是**最新提交** → 由 Henry（SM）合并；Henry 自己的 PR 由 Yuhan 合并。
   与 Sprint 1 一致，使用 **Create a merge commit**。
6. 卡片移到 Done 前保存 PR 链接和 Jenkins 构建号（报告要用）。所有人 `git switch main && git pull --ff-only origin main`。

### 从 `sprint2` 复制代码时注意

- 复制**整份新文件**没问题；对已有文件，只复制注释第一行写着本分支名的方法。
- 某些测试文件跨两个 PR（`ScrollViewerTest`：P6 → P10；`StatisticsTest`：P8 → P11）：第一份 PR 只放标着自己分支名的测试方法，
  并删掉还不存在的 import（例如 P6 时还没有 `BrowseScreen`），第二份 PR 再补上其余方法。
- 每份 PR 提交前都必须 `./gradlew clean build` 通过。

---

## 第 0 步（10-15 周四 tutorial 后 ～ 10-16 周五）：Sprint 2 Planning

- **主持：Henry（SM）。PO Stella 讲解优先级和验收条件**，全员 Planning Poker 估点（结果见上表，可在会上调整并记录理由；
  US03 由 Sprint 0 的 3 点上调为 5 点，US12 由 8 点下调为 3 点，US17 建议 5 → 8 点）。
- Stella 把 10 个故事加上 `sprint2` 标签和负责人（GitHub Assignee），在每个 Issue 下贴本文对应的任务清单。
- 看板（GitHub Projects）新建 Sprint 2 视图：Todo / In Progress / Review / Done。**会后截图**。
- Henry 把 Sprint 1 tutor 反馈（按格式预览、输入校验、易用性）和 10-16 客户新要求（逐项检查、用默认应用打开）
  对应到 US14、US17，写入会议记录。
- 会议记录：`docs/meeting-notes/sprint-2-planning-2026-10-16.md`（日期、时长、出席、决定、Sprint Goal、估点、分工、截图）。

---

## 第一轮（10-16 周五 ～ 10-17 周六）：四人并行打基础

### P1 · Henry：`s2-r1-ci-quality-gate`（建议本轮第一份合并）　审核：Yuhan

| 文件 | 要做的事 |
| --- | --- |
| `app/build.gradle` | `jacocoTestCoverageVerification` 规则：LINE 覆盖率 ≥ 0.75；`check` 依赖它 |
| `Jenkinsfile` | 新增 `Code Quality` 阶段（checkstyleMain/checkstyleTest，归档报告）；Package 阶段增加 `distZip` 并归档 zip |

- **PR 标题：** `Enforce coverage gate and add code quality stage to Jenkins`
- **通过标准：** Jenkins 新构建出现 Code Quality 阶段，artifacts 里有 `app.zip`。截图 Stage View。

### P2 · Henry：`s2-r1-profile-contacts`　审核：Yuhan

| 文件 | 要实现的函数 |
| --- | --- |
| `repository/UserRepository.java` | 接口方法 `findByEmail()`、`findByPhone()` |
| `repository/file/FileUserRepository.java` | `findByEmail()`、`findByPhone()`、私有 `phoneKey()` |
| 新建 `service/ContactValidator.java` | 私有构造方法、`email()`、`phone()`、`requireUnique()`、`requireUniqueEmail()`、`requireUniquePhone()`、私有 `isOwner()` |
| `service/AccountService.java` | 修改 `create()`；新增 `requirePasswordPolicy()`、`updateProfile()`、`changePassword()`；逐项检查 `checkNewIdKey()`、`checkNewUsername()`、`checkPassword()`、`checkEmail()`、`checkPhone()`、`checkCurrentPassword()`；`requireAdmin()` 改为 `public` |
| 测试（15 个） | 新建 `ProfileServiceTest`（7，含 `fieldChecksNormalizeAndReject`）、`DuplicateContactTest`（7）；修改 `RegistrationTest.passwordBoundariesAreAccepted`（两个账户改用不同邮箱/电话） |

- **PR 标题：** `Update profile and password and reject duplicate contacts`
- **关联：** `Relates to #4, #49, #51`；正文说明 `requireAdmin` 改 public 供 Stella 的统计使用（#13）。
- **通过标准：** 资料修改重启后仍在；旧密码失效；大写邮箱视为重复；管理员添加用户也被检查；每个 `check…` 单独可测。

### P3 · Yuhan：`s2-r1-console-io`　审核：Henry（Sixian 看 `Menu` 别名参数是否满足浏览页）

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/PasswordReader.java` | 接口 `readPassword(prompt)` |
| 新建 `ui/ConsoleIO.java` | 两个构造方法、`system()`、`ask()`、`askRequired()`、`askValid()`、`passwordValid()`、`askOptional()`、`confirm()`、`password()`、`info()`、`error()`、`flush()`；常量 `CANCEL`、`CANCEL_HINT`、`UNMASKED_WARNING` |
| 新建 `ui/InputCancelledException.java` | 构造方法 |
| 新建 `ui/Menu.java` | `Action`、`Option`、构造方法、`option()`、`hidden()`、`render()`、`runOnce()`（捕获错误与 cancel）、`select()`、`visibleOptions()`、`Selection`、`loop()` |
| `service/Validation.java` | `text()` 拆成三条具体错误消息（规则不变） |
| 测试（18 个） | 新建 `MenuTest`（6）、`ConsoleIOTest`（12，含 `askValid` 两个） |

- **PR 标题：** `Add numbered menu and shared console input helpers`
- **关联：** `Relates to #48, #49, #50`。
- **通过标准：** 不改 ConsoleUi，现有 93 个测试全部仍然通过；新测试覆盖越界、非数字、空输入、逐项重问、cancel、隐藏密码与警告。

### P4 · Stella：`s2-r1-scroll-storage`　审核：Sixian

| 文件 | 要实现或修改的函数 |
| --- | --- |
| `model/DigitalScroll.java` | `UNKNOWN_FORMAT`、`fileFormat` 字段、8 参数构造方法（7 参数委托给它）、`getFileFormat()`、`withName()`、`withContent()`、`withRecordedDownload()` |
| `repository/ScrollRepository.java` | `deleteById()` |
| `repository/file/FileScrollRepository.java` | 构造方法读取 `format`（缺省 unknown）；`save()` 原位替换；新增 `deleteById()`、私有 `persist()`、`indexOf()` |
| `service/ScrollService.java` | `download()` 一行改为 `scrolls.save(scroll.withRecordedDownload())`；新增上传逐项检查 `checkNewScrollId()`、`checkNewScrollName()`、`checkSource()` |
| 测试（8 个） | 新建 `ScrollStorageTest`（8，含 `uploadFieldChecksRejectTakenIdsNamesAndBadFiles`） |

- **PR 标题：** `Store scroll formats, support deletion and check upload fields`
- **关联：** `Relates to #11, #49, #52`。
- **通过标准：** 旧的 `scrolls.properties`（没有 format）仍能读取；下载/改名后卷轴在列表中的位置不变。

### P5 · Sixian：`s2-r1-query-format`（等 P4 合并后再提 PR）　审核：Stella

`FileFormat` 用到 `DigitalScroll.UNKNOWN_FORMAT`，测试替身实现了新的 `deleteById`，所以要先同步 P4：
`git fetch origin && git merge --no-edit origin/main`。

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `service/FileFormat.java` | `Category`、私有构造方法、`detect()`、`category()`、`isOpenable()`（打开白名单）、`describe()`、`isText()`、私有 `extension()`、`startsWith()` |
| 新建 `service/ScrollFilter.java` | record、`none()`、四个 `with*()`、`isEmpty()`、`matches()`、`describe()`、私有 `contains()`、`blankToNull()` |
| 新建 `service/SortOrder.java` | 三个枚举值、`label()`、`comparator()` |
| 新建 `service/ScrollQueryService.java` | 构造方法、`search()`、`parseDate()` |
| 测试（12 个） | 新建 `FileFormatTest`（5）、`ScrollQueryServiceTest`（7） |

- **PR 标题：** `Add scroll filtering, sorting and file format detection`
- **关联：** `Relates to #7, #52, #53`。
- **通过标准：** 四种筛选可单独使用并可组合；清除后回到完整列表；下载次数排序从多到少、平局按日期从新到旧。

**本轮合并顺序：** P1 最先；P2、P3 任意；**P4 → P5**。合并完大家同步 main。

---

## 第二轮（10-18 周日 ～ 10-19 周一）：业务接入 + 第一版编号菜单

**10-18 周日晚 Stand-up 1（Henry 主持，15 分钟）**：每人说完成了什么、接下来做什么、阻碍；Stella 更新看板并截图。

### P6 · Sixian：`s2-r2-format-preview`（先合并）　审核：Stella

| 文件 | 要实现的函数 |
| --- | --- |
| `service/ScrollService.java` | `upload()`：调用 `FileFormat.detect` 并用 8 参数构造方法保存；`download()`：拒绝文件夹目标、补扩展名、返回实际路径；新增 `withExtension()` |
| 新建 `service/ScrollPreviewRenderer.java` | 私有构造方法、`render()`、私有 `text()`、`image()`、`pdf()`、`archive()`、`hex()`；不认识的扩展名按内容判断是否文本 |
| 新建 `service/FileOpener.java` | 接口 `open()`、`desktop()`（真实打开，检查有没有桌面）、`unavailable()` |
| 新建 `service/ScrollViewer.java` | 两个构造方法、`open()`（登录检查 → 白名单 → 临时副本 → 打开，不计下载）、`safeName()` |
| 测试（17 个） | 新建 `ScrollFormatTest`（10）；新建 `ScrollViewerTest` 的服务层部分（7：`memberOpens…`、`openingDoesNotCountAsDownload`、`guestsCannotOpen`、`typesOutsideTheWhitelist…`、`machineWithoutDesktop…`、`whitelistIsDefaultDeny`、`unknownTextExtensions…`），此时不要 import `BrowseScreen` |

- **PR 标题：** `Record file formats, render format-aware previews and open scrolls in the default app`
- **关联：** `Relates to #52`。
- **通过标准：** 上传 `report.PDF` 记录为 `pdf`；下载到 `my-copy` 保存为 `my-copy.pdf`；打开 PDF 不改变下载次数；mp3 被拒绝；原有下载测试仍通过。

### P7 · Henry：`s2-r2-account-screens`　审核：Yuhan

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/AccountScreens.java` | 构造方法、`createAccount()`（从 ConsoleUi 迁来，每项用 `askValid/passwordValid` 和 P2 的 `check…` 立即检查）、私有 `askRole()`、`profile()`、`showProfile()`、`editDetails()`（逐项检查，回车保留原值）、`changePassword()`（当前密码、新密码、确认密码逐项检查） |
| 测试（6 个） | 新建 `ProfileConsoleTest`（直接创建 `AccountScreens`，不依赖 ConsoleUi） |

- **PR 标题：** `Add account screens with immediate field checks`
- **关联：** `Relates to #4, #13, #49, #51`。
- 此时 ConsoleUi 里仍有旧的 `createAccount`；**Henry 不改 ConsoleUi**，P12 由 Yuhan 换成调用本类。

### P8 · Stella：`s2-r2-scroll-edit-stats`（P6 之后合并）　审核：Sixian（编辑）、Yuhan（统计）

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `service/ScrollEditService.java` | 构造方法、`myScrolls()`、`checkEditable()`、`checkNewName()`、`rename()`、`replaceContent()`、`delete()`、私有 `requireOwned()`、`readSource()` |
| 新建 `service/StatisticsService.java` | `Row`、`Report`、构造方法、`report()` |
| 测试（12 个） | 新建 `ScrollEditServiceTest`（10，含 `fieldChecksForEditing`）；新建 `StatisticsTest` 的服务部分（2：`adminSeesPerScrollCountsAndTotals`、`nonAdminsCannotViewStatistics`），此时不要 import `StatisticsScreen` |

- **PR 标题：** `Let uploaders edit and delete their scrolls and add admin statistics`
- **关联：** `Relates to #11, #13, #49`。
- **为什么在 P6 之后合并：** 测试断言上传后的格式为 `txt`，依赖 P6 的上传改动。

### P9 · Yuhan：`s2-r2-numbered-console`　审核：Henry（Sixian 看浏览命令兼容）

| 文件 | 要做的事 |
| --- | --- |
| `ui/ConsoleUi.java` | 4 参数构造方法内部创建 `ConsoleIO`；`run()` 用 `Menu` 生成访客/会员/管理员编号菜单；`mainMenu()`、`addHidden()`（Sprint 1 命令词作为隐藏别名）；`showIdentity()`、`login()`、`logout()`、`showUsers()`、`deleteUser()` 改用 `ConsoleIO`；`upload()` 用 `askValid` + P4 的 `check…` 逐项检查 |
| `App.java` | 改用 `ConsoleIO.system(...)`，真实终端隐藏密码 |
| 测试（5 个） | 修改 `ConsoleUiTest` 的 4 个菜单文字断言、`ScrollConsoleTest.uploadAppearsOnlyInAuthenticatedMenus`（原作者 Sixian 审核） |

- **PR 标题：** `Switch console to numbered menus with consistent input feedback`
- **关联：** `Relates to #48, #49, #50`。
- **说明：** 本轮只编号 Sprint 1 已有功能（旧的注册表单和浏览页暂时留在 ConsoleUi，改用 ConsoleIO），编号在 P12 定稿。

**本轮合并顺序：P6 → P8；P7、P9 任意先后（文件不重叠）。**

---

## 第三轮（10-19 周一 ～ 10-20 周二）：各自的页面类（全部是新文件）

### P10 · Sixian：`s2-r3-browse-screen`　审核：Stella（Yuhan 看菜单风格）

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/BrowseScreen.java` | 两个构造方法（5 参数版接收 `ScrollViewer`）、`show()`、`list()`、`preview()`（预览菜单：1 下载、2 用默认应用打开）、`showPreview()`、`download()`（下载路径逐项检查）、`openInApp()`、`chooseFilter()`、`chooseSort()`、`clearFilters()`、`scrollId()`、`capitalize()` |
| 测试（10 个） | 新建 `BrowseConsoleTest`（7）；`ScrollViewerTest` 补上浏览页部分（3：`previewMenuOffersOpenToMembers`、`guestsSeeInfoPreviewButCannotOpen`、`browseScreenWithoutViewerExplainsUnavailable`，以及辅助方法 `browse()` 和 `BrowseScreen` import） |

- **PR 标题：** `Add browse screen with filters, sorting, format previews and open-in-app`
- **关联：** `Relates to #7, #49, #52, #53`。

### P11 · Stella：`s2-r3-my-scrolls-stats-screens`　审核：Sixian（我的卷轴）、Yuhan（统计页）

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `ui/MyScrollsScreen.java` | 构造方法、`show()`、`list()`、`rename()`、`replace()`、`delete()`、`ownedScroll()`（输入 ID 立即检查是否属于自己）、`scrollId()` |
| 新建 `ui/StatisticsScreen.java` | 构造方法、`show()` |
| 测试（7 个） | 新建 `MyScrollsConsoleTest`（5）；`StatisticsTest` 补上 `screenPrintsTableAndTotals`、`screenShowsEmptyState` |

- **PR 标题：** `Add My scrolls and statistics screens`
- **关联：** `Relates to #11, #13, #49`。

### Henry / Yuhan 本轮做什么

- **Henry（SM）**：审核；准备演示数据和演示脚本（`delivery.md`）；组织 Stand-up 2；检查看板和 Jenkins。
- **Yuhan**：审核两份 PR 中 `Menu`/`ConsoleIO` 的用法，在本地准备 P12。

**10-20 周二晚 Stand-up 2（Henry 主持）**：确认 R1–R3 全部合并、R4 开始；记录阻碍和 Jenkins 构建号；截图出席。

**P10、P11 是不同的新文件，可以任意先后合并。合并后大家同步 main。**

---

## 第四轮（10-20 周二 ～ 10-21 周三）：P12 · Yuhan 接入全部页面

```sh
git switch -c s2-r4-integration
```

审核：**Henry 主审**；Stella、Sixian 各自确认自己页面的入口和编号。

| 文件 | 要做的事 |
| --- | --- |
| `ui/ConsoleUi.java` | 新增构造方法 `(accounts, scrolls, edits, io)` 和 `(…, opener)`，创建 `AccountScreens`、`BrowseScreen`（带 `ScrollViewer`）、`MyScrollsScreen`、`StatisticsScreen`；保留 4 参数构造方法供 Sprint 1 测试使用；`run()` 首次管理员设置不能被 cancel 跳过；`mainMenu()` 定稿编号（访客 1–3、会员 1–5、管理员 1–9、0 = Quit）；`addHidden()`；新增 `myScrolls()`；删除旧的 `createAccount/browse/preview/download`（已迁到 Henry、Sixian 的页面） |
| `App.java` | 创建 `ScrollEditService`；传入 `FileOpener.desktop()`（真实打开） |
| 测试（17 个） | 新建 `NumberedMenuConsoleTest`（8）、`ImmediateFieldCheckTest`（6，端到端逐项检查与 cancel；Henry、Stella 确认各自字段）；修改 `AccountConsoleTest` 2 个（注册改为逐项检查，Henry 确认）、`ScrollConsoleTest.failedDownloadDoesNotReportSuccess`（下载路径逐项检查，Sixian 确认） |

- **PR 标题：** `Integrate Sprint 2 screens into the numbered console`
- **关联：** 十个故事全部 `Relates to`。
- **合并标准：** `./gradlew clean build` 通过（211 个测试、覆盖率 >75%）；Jenkins 对**这一提交**构建成功；
  四人按 `delivery.md` 场景表手动走一遍（包括在真实终端用 `app/build/install/app/bin/app` 打开一个 PDF）。

如有问题，由问题所属模块负责人（看注释第一行）从最新 main 开 `s2-fix-具体问题`，同样提 PR、审核和合并。

---

## 第五轮（10-21 周三 ～ 10-22 周四）：验收、README、打标签、演示

1. **P13 · Stella（PO）：** `s2-r5-readme` 更新 README（Sprint 2 功能、编号菜单表、逐项检查与 cancel、用默认应用打开、
   `installDist` 运行方式和 JDK 21 要求、测试与覆盖率）；Henry 审核。
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
| Planning | 日期、时长、出席、Sprint Goal、估点和理由、分工、看板截图 | 1.2、1.3、2.1、附录 B |
| 两次 Stand-up | 每人进展、阻碍、下一步；Zoom 出席截图；当时 Jenkins 构建号 | 2.2、附录 A |
| 每个 PR（P1–P13） | PR 链接/编号、审核意见、修改结果 | 3.1、3.4 |
| 合并冲突或集成问题 | 哪个文件、原因、如何解决；没有冲突也如实写 | 3.1 |
| Jenkins | Stage View 截图、构建号、SHA、测试数、覆盖率、Code Quality 阶段 | 1.1、3.2 |
| 看板变化 | Sprint 开始、结束截图 | 2.4 |
| 功能验收 | 每个故事的代表性终端截图（含逐项检查、打开 PDF） | 1.1、3.3 |
| Review 与 Retrospective | tutor 反馈、行动项及负责人 | 2.3 |
| 个人工作 | 技术（代码、测试、审核）和非技术（会议、看板、文档、演示） | 4.4 |

Jenkins 必须检查 PR 最新提交或明确的待合并 SHA，不能拿旧 main 的成功结果证明新 PR 通过。
代码注释中的 “实现 / 审核” 是分工，不是个人作者证明；参考 `sprint2` 的情况按实际与课程规则说明。

## 发生阻碍时，只用这三条规则

1. **分支需要队友已合并代码：** 先提交自己的修改，再 `git fetch origin`、`git merge --no-edit origin/main`。
   冲突时找该方法注释第一行写的实现人一起解决，测试后再提交和推送，不强制覆盖别人的代码。
2. **自己 PR 需要修改：** 在同一分支继续 commit/push，原 PR 自动更新，不重新开重复 PR。
3. **依赖的 PR 还没合并：** 先审核或准备测试，不把不完整代码当完成，也不用凑空分支/空提交。

## 一眼看完整顺序

```text
第 0 步：Planning（估点，Sprint Goal，看板）
第一轮：P1 Henry CI → P2 Henry 资料/联系方式/账户检查 + P3 Yuhan 输入层 + P4 Stella 存储/上传检查 → P5 Sixian 筛选/排序/格式
        （Stand-up 1：10-18）
第二轮：P6 Sixian 格式/预览/打开 → P8 Stella 编辑/统计服务；P7 Henry 账户页面；P9 Yuhan 第一版编号菜单
第三轮：P10 Sixian 浏览页面；P11 Stella 我的卷轴 + 统计页面；Henry 演示脚本（Stand-up 2：10-20）
第四轮：P12 Yuhan 接入全部页面 → 四人验收 → 合并
第五轮：P13 Stella README → 故事 Done → 标签 sprint-2 → 10-22 演示 → 回顾 → 10-25 报告
```

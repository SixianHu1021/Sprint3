<!-- 主负责人：Henry（Scrum Master，协调执行与合并）；四位成员核对自己的工作。本文是计划，不是已完成记录。 -->
# Sprint 3：每一轮谁开分支、写什么、谁审核、怎样合并

**起点：团队仓库 `A2-T16-2` 的 `main` 停在 Sprint 2 之后（tag `sprint-2`，Sprint 2 的 10 个故事已完成，共 211 个测试）。**
以下操作都在**团队仓库**执行；`A3-copy`（个人参考仓库）只用来查参考代码，里面已经是本 Sprint 全部完成后的样子。
**2026-10-24 纠错说明：** 下文仍是原定 P1–P4、R1–R4 的计划，不是团队完成记录。
初版 235 个测试未覆盖本次发现的缺陷；当前本地参考实现为 251 个测试通过，详见 [validation.md](validation.md)。
账户更新保留头像归 P1，严格校验归 P2，内容错误重试及独立进程验证归 P4；不新增任务或轮次。
每一轮结束后才进入下一轮。没有开发任务的人负责审核或准备测试，不需要硬开空分支。

文件位置：生产代码在 `app/src/main/java/org/example/`，测试在 `app/src/test/java/org/example/`。

## 先看懂代码里的分工标记

`A3-copy` 里**每个类、每个方法（包括测试方法）**注释的第一行都写明它属于哪个 PR、谁实现、谁审核：

```java
/**
 * 【PR s3-r2-profile-picture-service】实现：Stella ｜ 审核：Henry
 * 主负责人：Stella。
 * 做什么：……
 * 对接/验证：……
 */
```

| 标记 | 含义 |
| --- | --- |
| `【PR 分支名】实现：A ｜ 审核：B` | 这段代码在该分支的 PR 里由 A 写，B 审核。一个方法被两个 PR 修改时两段都写，用 “；” 分开 |
| `【Sprint 1/2 已完成 · 本轮不改】原作者：A` | 以前 Sprint 的代码，本 Sprint 不动；真要改先联系原作者 |
| 类注释开头的 `【分工】` | 列出本文件涉及的所有 PR，方便知道这个文件会被谁改 |

**提 PR 时：只提交注释第一行写着你这个分支名的代码。** 审核人就是那一行的 “审核”。

## Sprint 3 一页概览

| 项目 | 内容 |
| --- | --- |
| Sprint 时间 | 2026-10-22（周四，Week 11 tutorial 之后）→ 2026-10-29（周四，Week 12 tutorial 演示） |
| 报告和源码截止 | 2026-11-01（周日）23:59，Canvas（以课程公布为准） |
| Sprint Goal | 让用户的账户更个性化：登录用户能从“我的资料”上传头像（只接受真实的 JPG/PNG），并能像预览图片卷轴一样预览头像、用默认应用打开；访客不能使用；旧账户向后兼容。 |
| 角色 | Product Owner：Stella；Scrum Master：Henry；开发：Yuhan、Sixian（四人都写代码） |
| 故事 | 2 个，共 13 点（US19 = 8，US20 = 5） |
| 测试 | 初版参考实现 235 个（211 + 24）；10-24 纠错后 251 个（另加 16 个回归执行用例），行覆盖率 96.24% |
| 版本标签 | `sprint-3` |

| 成员 | 故事（Issue） | 点数 | 负责的 PR |
| --- | --- | --- | --- |
| Henry（SM） | US19 的模型与存储（头像字段 + Base64 持久化 + 向后兼容） | 约 4（US19 的一部分） | P1 |
| Stella（PO） | US19 的业务校验（JPG/PNG 扩展名+内容、大小、覆盖、访客） | 约 4（US19 的一部分） | P2 |
| Yuhan（开发） | US19 + US20 的界面：头像页面（上传/状态/预览/打开）、ConsoleUi/App 接线 | 约 5（偏多） | P4 |
| Sixian（开发） | US20 的预览渲染与打开（复用图片预览 + FileOpener） | 约 5（偏多） | P3 |

> 说明：Sprint 3 只有两条故事，为了让四人都有有意义的编码量并避免反复改同一文件，
> 我们把 US19 沿“模型/存储 → 业务校验 → 界面”切成三段（Henry、Stella、Yuhan），
> US20 沿“预览/打开服务 → 界面”切成两段（Sixian、Yuhan）。PO/SM 的编码量略少于两位开发。

## PR 总表（与代码注释的分支名一一对应）

| # | 轮次 | 分支 | 实现 | 审核 | 内容 | 合并顺序 |
| --- | --- | --- | --- | --- | --- | --- |
| P1 | R1 | `s3-r1-profile-picture-model` | Henry | Stella | User 头像字段（可选）、withProfilePicture；FileUserRepository Base64 持久化、读旧快照；AccountService 账户更新保留头像（纠错） | 本轮第一份 |
| P2 | R2 | `s3-r2-profile-picture-service` | Stella | Henry | ProfilePictureService：checkSource（扩展名/大小）、setProfilePicture（服务自身检查扩展名、严格解码及完整性、覆盖、访客） | 在 P1 之后 |
| P3 | R2 | `s3-r3-profile-picture-preview` | Sixian | Stella | ProfilePictureRenderer（复用图片预览）、ProfilePictureViewer（复用 FileOpener）；ScrollPreviewRenderer 抽出 appendImageDetails | 在 P1 之后（与 P2 并行） |
| P4 | R3 | `s3-r4-profile-picture-screen` | Yuhan | Sixian（界面）、Henry（资料接线确认） | ProfilePictureScreen（上传/状态/预览/打开）；AccountScreens 资料菜单接入；ConsoleUi/App 接线 | 最后（P2、P3 之后） |

## 本 Sprint 的防冲突规则（沿用 Sprint 1/2 回顾的改进行动）

1. **新功能放进新文件**：头像的服务和页面全部是新类（ProfilePictureService / Renderer / Viewer / ProfilePictureScreen），
   不反复改共享业务文件；本次纠错必须修改 AccountService 的两处账户重建，固定归 P1/Henry，ScrollService 无需修改。
2. **共享文件每处只指定一个人、一个 PR**：

| 共享文件 | 谁改 | PR | 改什么 |
| --- | --- | --- | --- |
| `model/User.java` | Henry | P1 | 新增可选头像字段、9 参数构造方法、hasProfilePicture/get../withProfilePicture（7 参数构造方法委托，Sprint 1/2 代码不改） |
| `repository/file/FileUserRepository.java` | Henry | P1 | 读写 Base64 头像、读没有 picture 字段的旧快照、decodePicture |
| `service/AccountService.java` | Henry | P1 | 10-24 纠错：updateProfile/changePassword 重建时保留头像字节和格式；不改变新账户默认值 |
| `service/ScrollPreviewRenderer.java` | Sixian | P3 | 把图片渲染抽成包内可见的 appendImageDetails（image() 委托），供头像预览复用（同一原作者，只扩展不改规则） |
| `ui/AccountScreens.java` | Yuhan（接线） | P4 | 新增可选 pictureScreen 构造参数；资料菜单多一个 “Profile picture” 项；资料页显示 Picture 状态（不改原有方法） |
| `ui/ConsoleUi.java`、`App.java` | Yuhan | P4 | 新增接收 ProfilePictureService/Viewer 的构造方法与接线（保留旧构造方法供 Sprint 1/2 测试） |

3. 中间 PR 用 `Relates to #N`；故事全部验收后才在 Issue 中勾选并关闭，不用 `Closes` 提前关闭。

## 每个分支都按这个流程提交（与 Sprint 1/2 相同）

### 开始写代码之前

先在团队仓库终端确认 `pwd` 和 `git remote -v` 是 `A2-T16-2`。`git status --short` 应没有未处理的修改
（`data/`、`app/data/` 是本地运行数据，不要提交）。

```sh
git switch main
git pull --ff-only origin main
git switch -c s3-r1-你的任务名     # 每次新任务都从最新 main 开分支
```

若分支已经创建过，用 `git switch 分支名` 返回它，不重复执行 `-c`。

### 写完这一份任务之后

1. 运行 `./gradlew clean build`（包含 Checkstyle 和 ≥75% 覆盖率门槛），解决失败。
2. 只选本分支的源文件和测试，查看 diff，再暂存。以 P1（Henry）为例：

```sh
git add app/src/main/java/org/example/model/User.java
git add app/src/main/java/org/example/repository/file/FileUserRepository.java
git add app/src/test/java/org/example/UserProfilePictureTest.java
git diff --cached --check
git diff --cached
git commit -m "feat(accounts): store optional profile picture with backward-compatible snapshot (#67)"
git push -u origin HEAD
```

3. GitHub 点 **Compare & pull request**：base `main`；填写下文的 PR 标题。正文写：实际完成内容、
   `./gradlew clean build` 结果（测试数、覆盖率）、**手动运行验证步骤**、`Relates to #N`。
4. 邀请 PR 总表中的审核人。审核人检查代码、注释中的 “对接/验证”、测试；作者在同一分支继续修改、commit、push。
5. 审核通过 + Jenkins 检查的是**最新提交** → 由 Henry（SM）合并；Henry 自己的 PR 由 Stella 合并。
   与 Sprint 1/2 一致，使用 **Create a merge commit**。
6. 卡片移到 Done 前保存 PR 链接和 Jenkins 构建号（报告要用）。所有人 `git switch main && git pull --ff-only origin main`。

### 从 `A3-copy` 复制代码时注意

- 复制**整份新文件**没问题；对已有文件（User、FileUserRepository、ScrollPreviewRenderer、AccountScreens、ConsoleUi、App），
  只复制注释第一行写着本分支名的方法/片段。
- 每份 PR 提交前都必须 `./gradlew clean build` 通过。

---

## 第 0 步（10-22 周四 tutorial 后 ～ 10-23 周五）：Sprint 3 Planning

- **主持：Henry（SM）。PO Stella 讲解优先级和验收条件**，全员 Planning Poker 估点（结果见上表，可在会上调整并记录理由；
  US19 = 8，US20 = 5）。
- Stella 把 2 个故事（#67、#68）加上 `sprint3` 标签和负责人（GitHub Assignee），在每个 Issue 下贴本文对应的任务清单。
- 看板（GitHub Projects）新建 Sprint 3 视图：Todo / In Progress / Review / Done。**会后截图**。
- Henry 把 Week 11 的 tutor/客户反馈对应到 US19/US20，写入会议记录。
- 会议记录：`docs/meeting-notes/sprint-3-planning-2026-10-23.md`（日期、时长、出席、决定、Sprint Goal、估点、分工、截图）。

---

## 第一轮（10-23 周五 ～ 10-24 周六）：模型与存储打基础

### P1 · Henry：`s3-r1-profile-picture-model`（本轮唯一一份，必须先合并）　审核：Stella

| 文件 | 要做的事 |
| --- | --- |
| `model/User.java` | 新增可选字段 `profilePicture`、`profilePictureFormat` 和常量 `NO_PICTURE`；9 参数构造方法（7 参数委托给它，Sprint 1/2 代码不改）；`hasProfilePicture()`、`getProfilePicture()`（拷贝）、`getProfilePictureFormat()`、`withProfilePicture()` |
| `repository/file/FileUserRepository.java` | 构造方法读取 `picture`（Base64）、`picformat`（缺省 none）；`persist()` 对有头像的用户写这两项；私有 `decodePicture()` |
| `service/AccountService.java`（10-24 纠错，P1 职责内） | `updateProfile` / `changePassword` 保留头像字节及格式；在服务回归及 P4 独立进程测试中验证 |
| 测试（6 个；初版表写 7 是笔误） | 新建 `UserProfilePictureTest`：默认没有头像、覆盖、防御性拷贝、Base64 重启后仍在、旧快照读成没有头像、损坏 Base64 被拒 |

- **PR 标题：** `Store an optional profile picture with a backward-compatible snapshot`
- **关联：** `Relates to #67`。
- **通过标准：** 没有 picture 字段的旧 `users.properties` 仍能读取；上传头像重启后仍在；返回的字节是拷贝。

**本轮只有 P1。合并后大家同步 main，P2、P3 才能从新 main 开分支（它们依赖 User 的新方法）。**

---

## 第二轮（10-24 周六 ～ 10-26 周一）：业务校验 + 预览/打开服务（两份并行）

**10-25 周日晚 Stand-up 1（Henry 主持，15 分钟）**：每人说完成了什么、接下来做什么、阻碍；Stella 更新看板并截图。

### P2 · Stella：`s3-r2-profile-picture-service`　审核：Henry

| 文件 | 要实现的函数 |
| --- | --- |
| 新建 `service/ProfilePictureService.java` | 构造方法、`checkSource()`（路径/扩展名/大小的逐项检查）、`setProfilePicture()`（复用 checkSource → 有界读取 → 魔数/严格解码/PNG 完整性 → 覆盖保存）、私有 `readSource()`、`extensionOf()`、`canDecode()`、`hasValidPngChunks()`（后两者为纠错辅助函数）；常量 `MAX_BYTES`（5 MiB）、白名单 `jpg/jpeg/png` |
| 测试（初版 7，纠错后 18；含与 P1 协作的账户更新回归） | `ProfilePictureServiceTest`：合法 PNG/JPG 上传并持久化、覆盖旧头像、非图片扩展名被拒、改名假图片（内容）被拒、超大被拒、访客不能上传（真实图片用 ImageIO 生成） |

- **PR 标题：** `Validate and store profile pictures (JPG/PNG by extension and content)`
- **关联：** `Relates to #67`。
- **通过标准：** `.txt` 改名成 `.png` 被拒；上传新头像覆盖旧头像；访客得到 “Please log in first”。

### P3 · Sixian：`s3-r3-profile-picture-preview`（与 P2 并行，都依赖 P1）　审核：Stella

| 文件 | 要实现的函数 |
| --- | --- |
| `service/ScrollPreviewRenderer.java` | 把图片尺寸渲染抽成包内可见的 `appendImageDetails()`，原 `image()` 委托给它（同一原作者 Sixian，只扩展不改规则） |
| 新建 `service/ProfilePictureRenderer.java` | `render(user)`：格式说明 + 大小 + 像素尺寸（复用 appendImageDetails）；没有头像时返回说明 |
| 新建 `service/ProfilePictureViewer.java` | 两个构造方法（可指定临时目录供测试）、`open()`：登录检查 → 有头像 → 临时副本（正确扩展名）→ FileOpener 打开，不改计数 |
| 测试（6 个） | 新建 `ProfilePicturePreviewTest`：有头像显示格式/大小/尺寸、没有头像显示说明、打开扩展名正确且内容一致、没有头像打开报错、没有桌面报原因、访客不能打开（用假 FileOpener） |

- **PR 标题：** `Preview the profile picture like an image scroll and open it in the default app`
- **关联：** `Relates to #68`。
- **通过标准：** 预览输出与图片卷轴一致（`Image size: W x H pixels`）；打开不改变任何下载/上传计数；访客被拒。

**本轮合并顺序：P2、P3 文件不重叠，可任意先后。合并后大家同步 main。**

---

## 第三轮（10-26 周一 ～ 10-28 周三）：P4 · Yuhan 接入界面

**10-27 周二晚 Stand-up 2（Henry 主持）**：确认 R1–R2 全部合并、R3 开始；记录阻碍和 Jenkins 构建号；截图出席。

### P4 · Yuhan：`s3-r4-profile-picture-screen`　审核：Sixian（界面）、Henry（资料接线确认）

```sh
git switch -c s3-r4-profile-picture-screen
```

| 文件 | 要做的事 |
| --- | --- |
| 新建 `ui/ProfilePictureScreen.java` | 构造方法、`show()`（编号菜单：上传/预览/返回）、`showStatus()`、`upload()`（askValid + checkSource，再 setProfilePicture；内容/读取错误立即重问，cancel 仍返回菜单）、`preview()`（渲染 + 可打开子菜单）、`openInApp()` |
| `ui/AccountScreens.java` | 新增可选 `pictureScreen` 构造参数；`profile()` 菜单在非空时多一个 “Profile picture” 项；`showProfile()` 显示 `Picture:` 状态（不改原有方法） |
| `ui/ConsoleUi.java` | 新增接收 `ProfilePictureService`、`ProfilePictureViewer` 的构造方法，创建 `ProfilePictureScreen` 并传入 `AccountScreens`；保留旧构造方法供 Sprint 1/2 测试；banner 改为 “Sprint 3” |
| `App.java` | 创建 `ProfilePictureService`（共用同一个 userRepository）和 `ProfilePictureViewer`（真实 `FileOpener.desktop()`），用新构造方法启动 |
| 测试（初版实际 5，原表写 4 是笔误；纠错后 8 + 独立 JVM 2） | `ProfilePictureConsoleTest`、`ProfilePictureLifecycleTest`：编号菜单上传并显示状态、非图片立即重问（US14）、预览显示尺寸并可打开、没有头像预览只显示说明、资料页显示 Picture 状态且菜单含该项 |

- **PR 标题：** `Add the profile picture screen and wire it into My profile`
- **关联：** `Relates to #67, #68`。
- **合并标准：** `./gradlew clean build` 通过（10-24 本地纠错基准为 251 个测试、覆盖率 >75%；团队最新提交必须重跑）；Jenkins 对**这一提交**构建成功；
  四人按 `delivery.md` 场景表手动走一遍（包括在真实终端用 `app/build/install/app/bin/app` 打开一张头像）。

如有问题，由问题所属模块负责人（看注释第一行）从最新 main 开 `s3-fix-具体问题`，同样提 PR、审核和合并。

---

## 第四轮（10-28 周三 ～ 10-29 周四）：验收、README、打标签、演示

1. **Stella（PO）：** 更新 README（Sprint 3 功能：头像上传与预览、JPG/PNG 校验、访客限制、向后兼容）；Henry 审核。
2. **每个故事负责人：** 对照 Issue 复选框和 `acceptance-criteria.md` 逐条验收，在 Issue 评论贴 PR 链接和测试名，勾选复选框；
   **PO Stella 最终确认**后关闭 Issue。
3. **Henry：** 确认 Jenkins 最新 main 构建成功，记录构建号、commit SHA、测试数、覆盖率（截图）。
4. **打标签（Henry，团队同意后）：**

```sh
git switch main && git pull --ff-only origin main
git rev-parse HEAD                     # 记下演示 SHA
git tag -a sprint-3 DEMO_COMMIT_SHA -m "Sprint 3 demonstration"
git push origin sprint-3
```

   不覆盖已有标签；提交到 Canvas 的源码 = 这个 tag 的代码。
5. **10-29 周四 tutorial：Sprint Review / 演示**（每人演示自己的部分，顺序见 `delivery.md`），记录 tutor 反馈。
6. **演示后 Sprint 3 Retrospective**（Henry 主持，20 分钟）：做得好 / 需改进 / 行动项，记录到
   `docs/meeting-notes/sprint-3-retrospective-2026-10-29.md`。
7. **报告：** 写报告（Overleaf），附封面签名，提交报告 + `sprint-3` 源码到 Canvas。

---

## 每轮要保存什么，填报告哪里（Overleaf 报告章节）

| 发生的事情 | 当时保存什么 | 报告位置 |
| --- | --- | --- |
| Planning | 日期、时长、出席、Sprint Goal、估点和理由、分工、看板截图 | 1.2、1.3、2.1、附录 B |
| 两次 Stand-up | 每人进展、阻碍、下一步；Zoom 出席截图；当时 Jenkins 构建号 | 2.2、附录 A |
| 每个 PR（P1–P4） | PR 链接/编号、审核意见、修改结果 | 3.1、3.4 |
| 合并冲突或集成问题 | 哪个文件、原因、如何解决；没有冲突也如实写 | 3.1 |
| Jenkins | Stage View 截图、构建号、SHA、测试数、覆盖率、Code Quality 阶段 | 1.1、3.2 |
| 看板变化 | Sprint 开始、结束截图 | 2.4 |
| 功能验收 | 每个故事的代表性终端截图（含上传头像、预览与打开头像） | 1.1、3.3 |
| Review 与 Retrospective | tutor 反馈、行动项及负责人 | 2.3 |
| 个人工作 | 技术（代码、测试、审核）和非技术（会议、看板、文档、演示） | 4.4 |

Jenkins 必须检查 PR 最新提交或明确的待合并 SHA，不能拿旧 main 的成功结果证明新 PR 通过。
代码注释中的 “实现 / 审核” 是分工，不是个人作者证明；参考 `A3-copy` 的情况按实际与课程规则说明。

## 发生阻碍时，只用这三条规则

1. **分支需要队友已合并代码：** 先提交自己的修改，再 `git fetch origin`、`git merge --no-edit origin/main`。
   冲突时找该方法注释第一行写的实现人一起解决，测试后再提交和推送，不强制覆盖别人的代码。
2. **自己 PR 需要修改：** 在同一分支继续 commit/push，原 PR 自动更新，不重新开重复 PR。
3. **依赖的 PR 还没合并：** 先审核或准备测试，不把不完整代码当完成，也不用凑空分支/空提交。

## 一眼看完整顺序

```text
第 0 步：Planning（估点 US19=8 / US20=5，Sprint Goal，看板）
第一轮：P1 Henry 头像模型/存储（必须先合并）
        （Stand-up 1：10-25）
第二轮：P2 Stella 头像校验服务；P3 Sixian 头像预览/打开服务（两份并行，都依赖 P1）
第三轮：P4 Yuhan 头像页面 + 资料接线 + App 接线（Stand-up 2：10-27）→ 四人验收 → 合并
第四轮：Stella README → 故事 Done → 标签 sprint-3 → 10-29 演示 → 回顾 → 报告
```

## 2026-10-24 验证补记（不新增轮次）

本地 `clean test` / `clean build` 均通过：251 个测试，0 失败，行覆盖率 96.24%。
初版 235 → 前次本地修正 240 → 本次审计 251 的时间顺序及失败复现见 [validation.md](validation.md)。
模型/存储格式不变，账户更新保留头像是现有功能的必要纠错；新增私有校验函数和回归测试不增加开发任务。
原计划仍为 2 个故事、13 点、4 份 PR 任务、4 轮及 Planning；审核与合并安排不变。

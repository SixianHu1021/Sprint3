<!-- 主负责人：Henry（Scrum Master，汇总验证结果）；各功能负责人检查自己的测试结果。 -->
# Local validation record

## Initial reference run — 2026-10-22

Run on 2026-10-22 in `A3-copy` (reference implementation), Java 21 toolchain. **Not a Jenkins run** and
not evidence for the team repository: re-run on the team repository after the P4 merge and record the Jenkins build.

- Command: `./gradlew clean build` (includes Checkstyle and the 75% line-coverage gate).
- Result: BUILD SUCCESSFUL; `checkstyleMain`/`checkstyleTest` passed; `jacocoTestCoverageVerification` passed.
- JUnit: **235 tests, 0 failures, 0 errors, 0 skipped** (211 from Sprint 2 + 24 new).
- JaCoCo, all production classes:
  - Lines: 96.3%
  - Instructions: 96.7%
- No Sprint 1/2 test needed changing: all new behaviour is in new files, and the account model stayed
  backward compatible (the Sprint 1 7-arg `User` constructor and old snapshots still work).
- Jenkins pipeline commands verified locally: `checkstyleMain checkstyleTest jacocoTestCoverageVerification jar distZip`
  → `app/build/distributions/app.zip`.
- Installed-distribution smoke test (`app/build/install/app/bin/app <data-dir>` with piped input):
  bootstrap admin → member login → My profile → Profile picture → upload a real PNG (accepted) →
  upload a renamed text file as `.png` (rejected) → preview (format/size/dimensions) → restart with the same
  data directory (picture still present) → load an old Sprint 2 snapshot (loads as "not added").
- Note: the start script needs Java 21; point `JAVA_HOME` at a JDK 21 if the default `java` is older.
  Opening a picture needs a desktop; on a headless machine the program shows the reason and continues.

The initial run did not cover avatar retention after account updates, damaged image signatures/payloads,
service calls with unsupported extensions, or immediate retry after a content error. Its passing result
must not be used as evidence that these cases worked. The historical class-count table listed 7 model
and 4 console tests; source inspection corrects those clerical figures to 6 and 5. The total remains 235.

| Test class | Initial tests (2026-10-22) | After corrective audit (2026-10-24) | Maintenance owner |
| --- | --- | --- | --- |
| UserProfilePictureTest | 6 | 6 | Henry |
| ProfilePictureServiceTest | 7 | 18 | Stella (account preservation coordinated with Henry) |
| ProfilePicturePreviewTest | 6 | 6 | Sixian |
| ProfilePictureConsoleTest | 5 | 8 | Yuhan |
| ProfilePictureLifecycleTest | 0 | 2 | Yuhan (integration), Henry (persistence confirmation) |
| Sprint 3 total | 24 | 40 | |

## Corrective audit — 2026-10-24 (local A3-copy only)

### Baseline, reproduction and causes

- The checkout already contained the preceding local fixes and five regression tests: the initial
  `./gradlew clean test` run for this audit passed **240 tests**. The original reference baseline is
  the 235-test run above; the independent review's 16 supplementary tests were not present locally.
  Equivalent regressions were added without weakening or removing original tests.
- The preceding five regressions had failed before the first local fixes (profile edit, unchanged
  profile save, password change, signature-only images and PNG without pixel data), then passed.
- This audit reproduced five further failing test methods before its fixes: direct upload with valid
  PNG bytes under an unsupported extension, missing image endings, a PNG checksum error, immediate
  content retry, and cancellation during content retry. Additional passing cases cover damaged
  payloads, EOF, consecutive account changes, ownership and actual process restarts.
- **Shared persistence cause confirmed:** `AccountService.updateProfile` and `changePassword` used the
  seven-argument `User` constructor, which intentionally creates an account without a picture.
  Saving that replacement therefore removed the picture from the persisted snapshot. Both now use
  the full constructor with the current picture bytes and format. Restart exposed the saved loss;
  it was not a separate deserialization bug.
- **Content validation cause:** magic bytes alone accepted invalid PNG/JPEG files. The first local
  fix added decoding, but ImageIO can recover truncated files and ignore PNG CRCs/IEND. Upload now
  decodes pixels, rejects decoder warnings, and checks PNG chunk lengths, CRCs and terminal IEND.
- **Extension cause:** `setProfilePicture` did not call `checkSource`, allowing direct API callers
  to bypass the extension whitelist. The service now reuses that validation before bounded reading.
- **Retry cause:** content validation happened outside `ConsoleIO.askValid`; its exception reached
  `Menu.runOnce`, which returned to the menu. `ProfilePictureScreen.upload` now retries the path after
  content/read errors. Cancel still reaches the existing menu cancellation handler; EOF reaches
  the application's normal input-closed handler; persistence/authentication failures are not retried.

### Complete User construction and persistence trace

| Path inspected | Result |
| --- | --- |
| `User` seven-argument constructor → nine-argument constructor | New accounts intentionally start without a picture; no change needed |
| `User` nine-argument constructor, getters, `withProfilePicture` | Defensive byte copies; format normalization; replacement keeps identity, role, hash and contacts |
| `AccountService.create` (register/bootstrapAdmin/addUser) | New-account path correctly uses seven arguments |
| `AccountService.updateProfile` | Preserves bytes and format while replacing name/email/phone |
| `AccountService.changePassword` | Preserves bytes and format while replacing only the password hash |
| `ProfilePictureService.setProfilePicture` | Uses current authenticated user and `withProfilePicture`; saves only after all validation |
| `FileUserRepository` constructor / `decodePicture` | Restores Base64 bytes and format to the full constructor; missing legacy fields default to no picture |
| `FileUserRepository.save` / `persist` / `deleteByIdKey` | Replaces by ID; writes all account fields and picture keys; other users retained; memory changes only after persistence succeeds |
| `AtomicPropertiesStore.read/write` | UTF-8 properties; temporary file then replacement (atomic move when supported); no schema change |
| Login/session, UI, renderer/viewer, App startup | Login reads stored users; session stores ID, not a stale User; App opens the same data-directory snapshot; preview never saves accounts |

A production-source search of all `new User`, constructors, `withProfilePicture` and repository saves
found no other reconstruction path. Neither `User.java` nor `FileUserRepository.java` required changes.
No new snapshot keys, migration or dependencies were introduced. Existing legacy records remain readable;
already-lost avatar bytes cannot be recovered from a snapshot that no longer contains them.

### Verification results

- `./gradlew clean test`: **251 tests, 0 failures, 0 errors, 0 skipped**.
- `./gradlew clean build`: **BUILD SUCCESSFUL**, including Checkstyle and the 75% line-coverage gate.
- JaCoCo production coverage: **96.24% lines, 96.62% instructions, 88.30% branches**.
- `git diff --check`: passed.
- `Sprint3_Report.tex`: local `pdflatex` compilation passed (two passes; temporary output outside
  the repository). The app compiler did not return a result; the terminal compiler supplied the check.
- Test growth: **235 original + 16 corrective test executions = 251** (five from the preceding local
  fix, eleven from this audit; one test method has separate PNG/JPEG parameterized executions).
- `ProfilePictureLifecycleTest.consecutiveAccountChangesSurviveCompleteApplicationRestart` runs two
  separate JVMs using the real `App`, console, PBKDF2 hasher and file repository in a temporary directory.
  First process: create admin and two members → upload different PNG/JPEG pictures → edit profile →
  change password → save unchanged profile → exit. Second process: old password rejected → new password
  accepted → preview both users' correct dimensions → exit. Fresh repository reads verify exact bytes,
  formats, ownership, contacts, role and credentials after both processes.
- `legacySnapshotSupportsFullLifecycleWithoutMigration` starts with a hand-written pre-Sprint-3
  snapshot (no picture keys) and repeats upload → profile edit → password change → exit → fresh-JVM
  login and preview; exact bytes and preserved contact fields are checked from disk.
- Existing preview/open tests still verify bytes and extensions via a fake opener; no real desktop
  application was launched. No Jenkins run, team review, remote changes, PR, merge or tag is claimed.

### Development history and documentation consistency

This is corrective maintenance within existing US19/US20, **not a new development round**.
The plan remains **2 stories, 13 points, 4 PR tasks (P1–P4), 4 rounds (R1–R4), plus Planning**.
Maintenance stays with P1/Henry for account preservation, P2/Stella for validation, and P4/Yuhan for
retry/integration; reviewer names are ownership assignments, not evidence of completed reviews.
New private helpers `canDecode` and `hasValidPngChunks` belong to P2; the existing account update and
upload methods are corrected, with no new production class or public interface. The new lifecycle test
class belongs to P4 integration. More tests/helpers do not create new stories, PR tasks or rounds.

Inspected `README.md`, all six Sprint 3 Markdown records (architecture, acceptance, delivery, validation,
work allocation and execution timeline), `Sprint3_Report.tex`, the Sprint 2 plans/validation/architecture,
and the implementation/test sources. Updated the README and Sprint 3 records/report for current behavior,
maintenance ownership and dated verification. Sprint 1/2 historical records and earlier milestone totals
remain unchanged. Earlier test results are not retroactively presented as having verified these fixes.

Changed production files: `AccountService.java`, `ProfilePictureService.java`, `ProfilePictureScreen.java`.
Changed existing tests: `ProfilePictureServiceTest.java`, `ProfilePictureConsoleTest.java`.
New test: `ProfilePictureLifecycleTest.java`. At the end of that corrective audit, all changes
were local and uncommitted; the later pre-submission review is recorded separately below.

## Independent pre-submission review — 2026-10-24

- Verdict: **PASS**, with no unresolved blocking findings after a read-only inspection of the full
  tracked diff, the untracked lifecycle test, production construction/persistence paths, regression
  tests, README, all seven Sprint 3 records/report, local history and Git routing configuration.
  This is an agent review of the implementation, not a claim of human team approval or Jenkins results.
- Fresh `./gradlew clean test` and `./gradlew clean build` runs passed. Independent XML inspection
  confirms **251 tests, 0 failures, 0 errors, 0 skipped** and JaCoCo **96.24% lines, 96.62% instructions,
  88.30% branches**. The base commit contains 235 original `@Test` methods; all remain present.
- All five defect categories have regression evidence, including two tests that launch fresh App
  JVMs for persistence/ownership and legacy-snapshot lifecycles. `git diff --check` and local Markdown
  link checks passed. No new dependencies, generated artifacts, active Git hooks or secret-pattern
  matches were found in the reviewed submission files. Desktop opening remains tested with a fake
  opener; no new manual OS-viewer test or Jenkins run is claimed.
- The original **2 stories, 13 points, 4 PR tasks and 4 rounds** remain unchanged. Earlier results
  retain their dates; this review does not retroactively certify the missed cases in the 235-test run.
- Authorized submission destination: **`https://github.com/SixianHu1021/Sprint3.git`, branch `main`**.
  Its advertised default branch and `main` both matched local base commit
  `47a998931690bfe388444e0c0df029c02ec60712` during review. No `url.*.insteadOf` or
  `url.*.pushInsteadOf` rules were configured; `remote.pushDefault` was unset. The push must use
  the explicit authorized URL, preserve existing history and be rechecked immediately before execution.
  University team-repository workflow examples elsewhere in these records are historical plans;
  they do not authorize a push or modification to that repository for this submission.
- This is a **pre-push record**, not proof of publication. The final submission report must separately
  verify the remote `main` SHA and provide the direct personal-repository commit URL after pushing.

## To fill in after the team-repository run

| Item | Value |
| --- | --- |
| Final `main` commit SHA (tag `sprint-3`) | `________` |
| Jenkins build number / URL | `________` |
| Tests passed / failed | `____ / ____` |
| Line / branch coverage | `____% / ____%` |
| Picture opened in a real desktop app verified (who, OS) | `________` |

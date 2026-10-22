<!-- 主负责人：Henry（Scrum Master，汇总验证结果）；各功能负责人检查自己的测试结果。 -->
# Local validation record

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

| New test class | Owner | Tests |
| --- | --- | --- |
| UserProfilePictureTest | Henry | 7 |
| ProfilePictureServiceTest | Stella | 7 |
| ProfilePicturePreviewTest | Sixian | 6 |
| ProfilePictureConsoleTest | Yuhan | 4 |

## To fill in after the team-repository run

| Item | Value |
| --- | --- |
| Final `main` commit SHA (tag `sprint-3`) | `________` |
| Jenkins build number / URL | `________` |
| Tests passed / failed | `____ / ____` |
| Line / branch coverage | `____% / ____%` |
| Picture opened in a real desktop app verified (who, OS) | `________` |

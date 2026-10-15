<!-- 主负责人：Henry（Scrum Master，汇总验证结果）；各功能负责人检查自己的测试结果。 -->
# Local validation record

Run on 2026-10-15 in `sprint2_copy` (reference implementation), Java 21 toolchain. **Not a Jenkins run** and
not evidence for the team repository: re-run on the team repository after the R4 merge and record the Jenkins build.

- Command: `./gradlew clean build` (now includes Checkstyle and the new 75% line-coverage gate).
- Result: BUILD SUCCESSFUL; `checkstyleMain`/`checkstyleTest` passed; `jacocoTestCoverageVerification` passed.
- JUnit: **190 tests, 0 failures, 0 errors, 0 skipped** (93 from Sprint 1 + 97 new).
- JaCoCo, all production classes:
  - Lines: 1011 / 1037 = 97.49%
  - Instructions: 5245 / 5359 = 97.87%
  - Branches: 433 / 472 = 91.74%
  - Methods: 232 / 235 = 98.72%
- Sprint 1 tests changed only where behaviour intentionally changed: menu text assertions
  (`ConsoleUiTest` ×3, `ScrollConsoleTest` ×1, US13) and one registration test that reused an email/phone (US16).
- Jenkins pipeline commands verified locally: `checkstyleMain checkstyleTest jacocoTestCoverageVerification jar distZip`
  → `app/build/distributions/app.zip`.
- Installed-distribution smoke test (`app/build/install/app/bin/app <data-dir>` with piped input):
  bootstrap admin → register two members (second rejected for an upper-case duplicate email) → member login →
  upload `.txt` and `.pdf` (formats detected) → download PDF to a path without extension (saved as `.pdf`) →
  preview text (first lines) → sort by downloads → filter by name → rename in My scrolls → edit phone in profile →
  logout → admin statistics (counts correct). Piped input shows the unmasked-password warning as designed.
- Note: the start script needs Java 21; the default `java` on the test machine was 18, so `JAVA_HOME` was pointed at
  the Gradle-provisioned JDK 21. Check the demo laptop before the tutorial.

| New test class | Owner | Tests |
| --- | --- | --- |
| MenuTest, ConsoleIOTest, NumberedMenuConsoleTest | Yuhan | 6 + 10 + 8 = 24 |
| ProfileServiceTest, DuplicateContactTest, ProfileConsoleTest | Henry | 6 + 7 + 6 = 19 |
| ScrollStorageTest, ScrollEditServiceTest, MyScrollsConsoleTest, StatisticsTest | Stella | 7 + 9 + 5 + 4 = 25 |
| FileFormatTest, ScrollQueryServiceTest, ScrollFormatTest, BrowseConsoleTest | Sixian | 5 + 7 + 10 + 7 = 29 |

## To fill in after the team-repository run

| Item | Value |
| --- | --- |
| Final `main` commit SHA (tag `sprint-2`) | `________` |
| Jenkins build number / URL | `________` |
| Tests passed / failed | `____ / ____` |
| Line / branch coverage | `____% / ____%` |
| Masked password verified in a real terminal (who, OS) | `________` |

<!-- 主负责人：Stella（Product Owner，验收与演示顺序）；Henry（SM）准备演示数据；四人分别演示自己的故事。 -->
# Sprint 2 acceptance and delivery

This is a runnable demo plan and checklist, not a claim that meetings, CI runs or individual work occurred.
See [criterion-to-code/test mapping](acceptance-criteria.md).

## Demo preparation — Henry (day before the tutorial)

1. On the tagged commit: `./gradlew clean build` and `./gradlew :app:installDist`.
2. Check `java -version` is 21 on the demo laptop (or set `JAVA_HOME`).
3. Start with a fresh directory: `app/build/install/app/bin/app demo-data` (real terminal → hidden passwords).
   Before the tutorial, create the admin (first run) and register Alice so the demo starts from the guest menu.
4. Prepare sample files in one folder: `notes.txt` (several lines), `report.pdf`, `picture.png`, `bundle.zip`,
   and a file without extension.
5. Keep the Jenkins Stage View, the coverage report and the GitHub board open in browser tabs.

## Demo data

| Role | ID key | Username | Password | Name | Email | Phone |
| --- | --- | --- | --- | --- | --- | --- |
| Admin (first run) | `admin01` | `admin` | `AdminPass123` | Ada Admin | `ada@example.com` | `0400000000` |
| Member | `u001` | `alice` | `AlicePass123` | Alice Smith | `alice@example.com` | `0411111111` |
| Member | `u002` | `bob` | `BobPass1234` | Bob Lee | `bob@example.com` | `0422222222` |

## Demo order and expected outcomes (about 2 minutes each)

| # | Presenter / story | Steps | Expected outcome |
| --- | --- | --- | --- |
| 1 | Yuhan — US13, US14, US15 | Show guest menu; type `9`, `abc`, empty line; `3` login with blank ID then real ID; password typed | Numbered menus; clear `Error:` messages; menu re-shown; password not echoed |
| 2 | Henry — US16 | Guest `2` register Bob with email `ALICE@example.com`, then with phone `0411 111 111`, then valid data | Both duplicates rejected naming the field; third succeeds |
| 3 | Henry — US03 | Alice `4` My profile → `1` keep name, change phone → `2` change password (mismatch, then valid) → log out and log in with new password | Profile updated and persisted; ID shown as unchangeable; old password fails |
| 4 | Sixian — US17 | Alice `2` upload `notes.txt`, `report.pdf`, `picture.png`; `1` browse → preview each; download PDF to `~/Desktop/copy` | Formats detected; text lines / image size / PDF info shown; file saved as `copy.pdf` |
| 5 | Sixian — US06, US18 | Browse → `3` filter by uploader, name, date; empty result; `5` clear; `4` sort by download count | Header shows filters and sort order; empty-state message; ties newest first |
| 6 | Stella — US10 | Alice `3` My scrolls → rename, replace `notes.txt` with a PDF, delete with `n` then `y`; Bob tries `delete` Alice's scroll | Only own scrolls listed; changes visible in browse; Bob rejected |
| 7 | Stella — US12 | Admin `7` add MEMBER (role 1) and ADMIN (role 2); `9` statistics | Both accounts created; per-scroll uploads/downloads and totals; member cannot open stats |
| 8 | Henry — CI | Jenkins Stage View (Code Quality, Coverage gate, archived `app.zip`), tag `sprint-2` on GitHub | Green build for the tagged SHA |

Restart the program with the same data directory at the end to show that profile changes, formats,
renames, deletions and counters persist.

## Actual submission work — do not mark complete without evidence

- [ ] Sprint 2 Planning held and recorded (Sprint Goal, estimates, owners, board screenshot).
- [ ] Two stand-ups recorded with attendance screenshots.
- [ ] Every story's Issue checkboxes ticked with PR links; PO accepted and closed the Issue.
- [ ] Every PR reviewed by the named reviewer; Jenkins green for the latest commit before merge.
- [ ] Tag `sprint-2` created on the demonstrated commit and pushed.
- [ ] All four members demonstrate in the Week 11 tutorial (22 Oct); tutor feedback recorded.
- [ ] Retrospective held and recorded with action owners.
- [ ] Report (Overleaf) with evidence, contribution tables and signed coversheet; report + `sprint-2` source on Canvas
      by Sunday 25 Oct 23:59.

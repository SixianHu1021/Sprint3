<!-- 主负责人：Stella（Product Owner，验收与演示顺序）；Henry（SM）准备演示数据；四人分别演示自己的部分。 -->
# Sprint 3 acceptance and delivery

This is a runnable demo plan and checklist, not a claim that meetings, CI runs or individual work occurred.
See [criterion-to-code/test mapping](acceptance-criteria.md).

## Demo preparation — Henry (day before the tutorial)

1. On the tagged commit: `./gradlew clean build` and `./gradlew :app:installDist`.
2. Check `java -version` is 21 on the demo laptop (or set `JAVA_HOME`).
3. Start with a fresh directory: `app/build/install/app/bin/app demo-data` (real terminal → hidden passwords).
   Before the tutorial, create the admin (first run) and register Alice so the demo starts from the guest menu.
4. Prepare sample files in one folder: `avatar.png` (a real PNG), `avatar.jpg` (a real JPG),
   `fake.png` (actually a text file renamed to .png), truncated PNG/JPEG files,
   `notes.txt`, a `.txt` containing valid PNG bytes, and a `huge.png` above 5 MiB.
5. To show backward compatibility: keep a copy of a Sprint 2 `users.properties` (no picture fields) to load.
6. Keep the Jenkins Stage View, the coverage report and the GitHub board open in browser tabs.

## Demo data

| Role | ID key | Username | Password | Name | Email | Phone |
| --- | --- | --- | --- | --- | --- | --- |
| Admin (first run) | `admin01` | `admin` | `AdminPass123` | Ada Admin | `ada@example.com` | `0400000000` |
| Member | `u001` | `alice` | `AlicePass123` | Alice Smith | `alice@example.com` | `0411111111` |

## Demo order and expected outcomes (about 2 minutes each)

| # | Presenter / part | Steps | Expected outcome |
| --- | --- | --- | --- |
| 1 | Henry — US19 model/storage | Login as Alice → My profile → Profile picture → upload `avatar.png` → edit profile → change password → save unchanged profile; quit and restart with the same data directory → login with new password → My profile | Status remains "added (png)" through updates and restart; bytes/format belong to Alice (Base64 persisted); old password rejected |
| 2 | Henry — backward compatibility | Point the app at an old Sprint 2 `users.properties` (no picture fields) | Accounts load normally, shown as "Picture: not added" |
| 3 | Stella — US19 validation | My profile → Profile picture → upload `notes.txt` (rejected), `fake.png` and truncated PNG/JPEG (rejected), `huge.png` (too large), then `avatar.jpg` | Each invalid input shows a clear `Error:` and re-asks for a path without a menu; cancel during retry preserves the previous picture; `avatar.jpg` succeeds |
| 4 | Sixian — US20 preview/open | My profile → Profile picture → Preview; then "Open in default app" | Shows Format / Size / "Image size: W x H pixels"; the picture opens in the OS viewer; counts unchanged |
| 5 | Sixian — US20 edge cases | A member with no picture → Preview; (optionally) a headless/SSH session → Open | "No profile picture has been added yet."; headless shows the reason and the program continues |
| 6 | Yuhan — US19/US20 UI | Walk the numbered Profile picture menu end to end; show guest cannot reach it | Numbered menu, status line, cancel works; guest has no access |
| 7 | Henry — CI | Jenkins Stage View (Code Quality, Coverage gate, archived `app.zip`), tag `sprint-3` on GitHub | Green build for the tagged SHA |

Restart the program with the same data directory at the end to show the profile picture persists.

## Actual submission work — do not mark complete without evidence

- [ ] Sprint 3 Planning held and recorded (Sprint Goal, estimates US19=8/US20=5, owners, board screenshot).
- [ ] Two stand-ups recorded with attendance screenshots.
- [ ] Every story's Issue checkboxes ticked with PR links; PO accepted and closed the Issue.
- [ ] Every PR reviewed by the named reviewer; Jenkins green for the latest commit before merge.
- [ ] Tag `sprint-3` created on the demonstrated commit and pushed.
- [ ] All four members demonstrate in the Week 12 tutorial (29 Oct); tutor feedback recorded.
- [ ] Retrospective held and recorded with action owners.
- [ ] Report (Overleaf) with evidence, contribution tables and signed coversheet; report + `sprint-3` source on Canvas by the deadline.

## Corrective verification note — 2026-10-24

These strengthened checks belong to the existing P1/P2/P4 responsibilities and R1–R4 plan; they do
not introduce a new story, task or round. The initial 2026-10-22 run missed the account-update and
content-retry cases. The local corrective suite now has 251 passing tests, including two independent-JVM
lifecycle tests (two users and a legacy snapshot). See [validation](validation.md) for exact evidence.
The demo table remains a checklist; no team acceptance, Jenkins build or real desktop-open result is implied.

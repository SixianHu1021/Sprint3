# Sprint 1 Retrospective Meeting

**Date:** 15 October 2026

**Time and duration:** 12:55-13:15 (20 minutes)

**Attendees:** Qinghan (Henry) Wang, Yuhan Pan, Stella Xu, Sixian Hu

**Sprint:** Sprint 1

## What Went Well

- Shared models and interfaces provided a consistent foundation for parallel development. Changes were integrated incrementally, and no merge conflicts occurred during Sprint 1.
- Peer review identified the Gradle standard-input issue before final integration. The issue was corrected and verified before the pull request was merged.
- Jenkins and automated tests supported verification of the integrated application. All four members completed their assigned functionality, and the final integration passed all 93 automated tests.

## What Could Be Improved

- The expected scope of scroll preview was clarified late. The team identified the need for file-type-specific preview behaviour through tutor feedback at the Sprint Review.
- Changes to the shared ConsoleUi class required sequential integration. Responsibilities for some console entry points were initially unclear, requiring additional coordination.

## Proposed Improvement Actions

| Action | Target |
|---|---|
| Review tutor feedback and clarify the affected acceptance criteria before implementation. | Sprint 2 planning |
| Define service, console, and test responsibilities, including the integration order for shared files. | Before Sprint 2 development |
| Include README-based startup and relevant manual workflow verification in pull request validation notes. | Before each Sprint 2 merge |

Responsibilities will be assigned during Sprint 2 planning.

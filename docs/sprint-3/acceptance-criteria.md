<!-- 主负责人：Stella（Product Owner，最终验收）；每个故事负责人核对自己的条目。 -->
# Sprint 3 acceptance criteria traceability

Source: GitHub Issues #67, #68 (read 2026-10-22). Production paths are relative to
`app/src/main/java/org/example/`; tests are in `app/src/test/java/org/example/`.
The PO marks a story Done only after every criterion below is demonstrated and the Issue checkboxes are ticked.

## US19 — Henry (model/storage), Stella (validation), Yuhan (screen) — Upload a profile picture (#67)

| Acceptance criterion | Implementation | Evidence (tests) |
| --- | --- | --- |
| Registration does not ask for a picture; new accounts start without one | `User` 7-arg constructor delegates with `NO_PICTURE`; `AccountScreens.createAccount` unchanged | `UserProfilePictureTest.newAccountsStartWithoutAProfilePicture`; `ProfilePictureConsoleTest` (status "not added") |
| A logged-in user can add a picture from the edit profile section | `AccountScreens.profile` → `ProfilePictureScreen.show/upload` | `ProfilePictureConsoleTest.uploadsThroughNumberedMenuAndShowsStatus` |
| User provides a local file path | `ProfilePictureScreen.upload` with `askValid` | `uploadsThroughNumberedMenuAndShowsStatus` |
| Only JPG/JPEG and PNG accepted; other types rejected with a clear error | `ProfilePictureService.checkSource` extension whitelist | `ProfilePictureServiceTest.nonImageExtensionsAreRejectedByCheckSource`; `ProfilePictureConsoleTest.invalidPictureIsRepromptedImmediately` |
| A file whose content is not a real JPG/PNG is rejected even with .jpg/.png name | `ProfilePictureService.setProfilePicture` uses `FileFormat.detect(null, bytes)` | `ProfilePictureServiceTest.fakeImageWithImageExtensionIsRejected` |
| Invalid path / missing file / over the size limit reported immediately; retry or cancel | `checkSource` + `ConsoleIO.askValid` (cancel) | `nonImageExtensionsAreRejectedByCheckSource`, `oversizedPictureIsRejected`, `invalidPictureIsRepromptedImmediately` |
| Uploading a new picture replaces the previous one | `User.withProfilePicture`, `setProfilePicture` saves over | `ProfilePictureServiceTest.uploadingReplacesThePreviousPicture` |
| Picture stored with the account and persists across sessions | `FileUserRepository` Base64 persistence | `UserProfilePictureTest.profilePictureSurvivesRestart`; `ProfilePictureServiceTest.uploadsValidPngAndPersists` |
| Guests cannot upload a profile picture | `ProfilePictureService.setProfilePicture` → `requireUser` | `ProfilePictureServiceTest.guestsCannotUpload` |
| (Backward compatibility) older accounts without a picture load correctly | `FileUserRepository` defaults missing fields | `UserProfilePictureTest.oldSnapshotWithoutPictureLoadsAsNoPicture` |

## US20 — Sixian (preview/open), Yuhan (screen) — Preview the profile picture like an image scroll (#68)

| Acceptance criterion | Implementation | Evidence (tests) |
| --- | --- | --- |
| My profile shows whether a picture has been added | `AccountScreens.showProfile` prints `Picture:` status; `ProfilePictureScreen.showStatus` | `ProfilePictureConsoleTest.profileScreenShowsPictureStatusAndMenuOption` |
| User can choose to preview the picture from My profile | `ProfilePictureScreen.preview` (menu item) | `ProfilePictureConsoleTest.previewShowsDimensionsAndOpens` |
| Preview shows the same details as an image scroll (format, size, dimensions) | `ProfilePictureRenderer.render` reuses `ScrollPreviewRenderer.appendImageDetails` | `ProfilePicturePreviewTest.previewShowsFormatSizeAndDimensions` |
| User can open the picture in the default application, like an image scroll | `ProfilePictureViewer.open` reuses `FileOpener` | `ProfilePicturePreviewTest.memberOpensProfilePictureWithCorrectExtension`; `ProfilePictureConsoleTest.previewShowsDimensionsAndOpens` |
| No picture → clear message instead of an error | `ProfilePictureRenderer.render` / `ProfilePictureViewer.open` | `ProfilePicturePreviewTest.previewWithoutPictureShowsClearMessage`, `openingWithoutPictureReportsReason`; `ProfilePictureConsoleTest.previewWithoutPictureShowsMessageAndNoOpenOption` |
| Cannot open on this machine → reason shown, program continues | `FileOpener` throws; `ProfilePictureScreen.openInApp` shows `Error:` | `ProfilePicturePreviewTest.machineWithoutDesktopReportsReason` |
| Previewing does not change any scroll download/upload counts | `ProfilePictureViewer.open` never saves anything | `ProfilePicturePreviewTest.memberOpensProfilePictureWithCorrectExtension` (nothing saved) |
| Guests cannot preview profile pictures | `requireUser` in viewer; screen requires login | `ProfilePicturePreviewTest.guestsCannotOpen` |

## Additional policies confirmed with the tutor (to raise at the Week 11/12 tutorial)

- Profile picture size limit is 5 MiB (smaller than the 10 MiB scroll limit), since avatars are small images.
- JPEG files are normalised to the `jpg` format label for a consistent extension when opened.
- Opening or previewing a profile picture is never counted as a scroll download or upload.
- Profile pictures are stored inside the user snapshot as Base64; snapshots written before Sprint 3 remain readable.

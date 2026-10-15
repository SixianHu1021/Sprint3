<!-- 主负责人：Stella（Product Owner，最终验收）；每个故事负责人核对自己的条目。 -->
# Sprint 2 acceptance criteria traceability

Source: GitHub Issues #4, #7, #11, #13, #48–#53 (read 2026-10-15). Production paths are relative to
`app/src/main/java/org/example/`; tests are in `app/src/test/java/org/example/`.
The PO marks a story Done only after every criterion below is demonstrated and the Issue checkboxes are ticked.

## US03 — Henry — Update profile and password (#4)

| Acceptance criterion | Implementation | Evidence (tests) |
| --- | --- | --- |
| View current profile details | `AccountScreens.profile/showProfile` | `ProfileConsoleTest.profileShowsDetailsAndKeepsBlankFields` |
| Update phone, email, full name | `AccountService.updateProfile`, `AccountScreens.editDetails` | `ProfileServiceTest.updatedProfilePersistsAcrossSessions` |
| Change password | `AccountService.changePassword`, `AccountScreens.changePassword` | `passwordChangeStoresNewHashAndInvalidatesOldPassword`, `passwordChangeNeedsMatchingConfirmation` |
| New password hashed before storing | `hasher.hash` in `changePassword` | `passwordChangeStoresNewHashAndInvalidatesOldPassword` (file has no raw password) |
| ID key cannot change | `updateProfile` copies ID/username/role | `updatedProfilePersistsAcrossSessions`; profile shows “(cannot be changed)” |
| Updates persist across sessions | `FileUserRepository.save` | `updatedProfilePersistsAcrossSessions` (re-opens the file) |

## US06 — Sixian — Filter scrolls (#7)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Filter by uploader ID / scroll ID / name / upload date | `ScrollFilter.matches`, `ScrollQueryService.search`, `BrowseScreen.chooseFilter` | `ScrollQueryServiceTest.eachFilterWorksIndividually`, `BrowseConsoleTest.filtersByIdNameAndDateAndShowsEmptyState` |
| Filters applied individually | Each `with*` changes one condition | `eachFilterWorksIndividually`, `filterByUploaderThenClear` |
| Clearing returns full list | `ScrollFilter.none()`, blank value, “Clear filters” | `filtersCombineAndClearingReturnsFullList`, `filterByUploaderThenClear` |
| No matches shows empty-state message | `BrowseScreen.list` | `filtersByIdNameAndDateAndShowsEmptyState` |

## US10 — Stella — Edit and delete own scrolls (#11)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| View list of own uploads | `ScrollEditService.myScrolls`, `MyScrollsScreen.list` | `ScrollEditServiceTest.listsOnlyOwnScrolls`, `MyScrollsConsoleTest.listsOnlyOwnScrollsWithNumberedActions` |
| Edit name of own scroll | `rename`, `DigitalScroll.withName` | `renameKeepsEverythingElseAndPersists`, `renameAndReplaceThroughNumbers` |
| Replace binary content | `replaceContent`, `withContent` | `replaceContentUpdatesFormatAndUploadCountButKeepsDownloads` |
| Delete own scroll | `delete`, `FileScrollRepository.deleteById` | `deleteRemovesScrollFromBrowseAndAfterRestart`, `deleteAsksForConfirmation` |
| Cannot edit / delete others' scrolls | `requireOwned` | `cannotEditOrDeleteOtherUsersScrolls`, `otherUsersScrollsCannotBeChanged` |
| Deleted scrolls no longer listed | repository snapshot | `deleteRemovesScrollFromBrowseAndAfterRestart` |

## US12 — Stella — Admin creates users and views statistics (#13)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Create general (MEMBER) user | `AccountScreens.createAccount("admin")` role 1 | `ProfileConsoleTest.adminFormSelectsRoleByNumberAndRetriesInvalidRole`, `NumberedMenuConsoleTest.adminCreatesBothRolesAndOpensStatisticsByNumber` |
| Create admin user | role 2 | same tests; Sprint 1 `AccountConsoleTest.administratorCreatesMemberAndAdministrator` |
| View upload / download count per scroll | `StatisticsService.report`, `StatisticsScreen.show` | `StatisticsTest.adminSeesPerScrollCountsAndTotals`, `screenPrintsTableAndTotals` |
| Statistics per scroll | `Report.rows` | `screenPrintsTableAndTotals` |
| Non-admins cannot access | `AccountService.requireAdmin` (now public) | `nonAdminsCannotViewStatistics`, `ConsoleUiTest.memberMenuHidesAdminCommands` |

## US13 — Yuhan — Numbered menus (#48)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Every menu shows numbered options | `Menu.render` used by all screens | `MenuTest.rendersNumberedOptionsWithExitAsZero`, `BrowseConsoleTest.showsNumberedMenuAndHeader` |
| Select by typing the number | `Menu.select/runOnce` | `numbersDispatchToTheMatchingAction`, `NumberedMenuConsoleTest.wholeWorkflowUsesNumbersOnly` |
| Invalid number → clear error, no crash | `Menu.select` range message | `outOfRangeAndNonNumericInputShowErrorsWithoutRunning`, `invalidMenuInputShowsErrorAndRedisplaysSameMenu` |
| Menu re-displays after each action | `Menu.loop`, `ConsoleUi.run` | `loopRedisplaysAfterEachActionUntilExit` |
| Consistent across guest/user/admin | `ConsoleUi.mainMenu` | `sharedOptionsHaveTheSameNumberForEveryRole` |

## US14 — Yuhan — Immediate feedback on invalid input (#49)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Invalid input detected before processing | `ConsoleIO.askRequired`, `Menu.select`, `ScrollQueryService.parseDate` | `ConsoleIOTest.requiredInputRepromptsOnBlankAndTrims`, `blankLoginIdIsRejectedImmediately` |
| Message says what was wrong | `Validation.text` specific messages | `validationMessagesExplainTheProblem` |
| User stays on same menu / prompt | re-prompt loops; `Menu.runOnce` catches errors | `failingActionShowsErrorAndKeepsMenuOpen`, `invalidMenuInputShowsErrorAndRedisplaysSameMenu` |
| No crash or exit | `ConsoleUi.run` | all console tests end with “Goodbye.” |
| Consistent tone and format | `ConsoleIO.error` → `Error: …` | `errorsUseOneSharedFormat` |

## US15 — Yuhan — Hidden password entry (#50)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Password masked at the terminal | `ConsoleIO.system` → `System.console()::readPassword` | `maskedPasswordIsReadWithoutEchoAndCleared`, manual demo with installed script |
| Registration, login, password change | `AccountScreens.createAccount/changePassword`, `ConsoleUi.login` all call `io.password` | `maskedReaderKeepsPasswordOffScreen` |
| Warning without masking support | `ConsoleIO.UNMASKED_WARNING` | `terminalWithoutMaskingShowsWarningThenReadsLine` |
| Backspace works | handled by the OS terminal in `readPassword` | manual demo |
| Validation after full entry | `password()` returns the whole line; services validate | `ProfileServiceTest.passwordChangeRequiresCorrectCurrentPasswordAndValidNewPassword` |

## US16 — Henry — Unique email and phone (#51)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Reject duplicate email / phone at registration | `ContactValidator.requireUnique` in `AccountService.create` | `registrationRejectsDuplicateEmailIgnoringCase`, `registrationRejectsDuplicatePhoneIgnoringSeparators` |
| Message names the duplicated field | “Email address is …” / “Phone number is …” | same tests |
| Email case-insensitive | `findByEmail`, lowercase storage | `emailIsStoredInLowercase` |
| Admin-created users checked | `addUser` → `create` | `adminCreatedUsersFollowTheSameRules` |
| Profile updates cannot clash | `updateProfile` with owner exemption | `profileUpdateCannotTakeAnotherUsersContacts`, `keepingOwnEmailAndPhoneIsAllowed` |

## US17 — Sixian — Preview by file format (#52)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| Preview shows file format | `BrowseScreen.showPreview`, `FileFormat.describe` | `previewShowsFormatAwareContentAndDownloadAddsExtension` |
| Format captured at upload | `ScrollService.upload` + `FileFormat.detect`; persisted by `FileScrollRepository` | `uploadRecordsFormatAndItSurvivesRestart`, `FileFormatTest` |
| Unknown format shows “unknown” | `DigitalScroll.UNKNOWN_FORMAT` | `unknownAndEmptyPreviews`, `oldSnapshotsWithoutFormatLoadAsUnknown` |
| Download saved with correct extension | `ScrollService.withExtension` | `downloadAddsMissingExtensionAndKeepsFormat`, `jpegAliasesAndUnknownFormatsKeepTheChosenName` |
| Available to guests and members | preview needs no login | `guestSeesLoginRequiredAndCannotDownload` |
| (Tutor feedback) preview content by type | `ScrollPreviewRenderer` | text / image / pdf / zip / hex tests in `ScrollFormatTest` |

## US18 — Sixian — Sort by download count (#53)

| Acceptance criterion | Implementation | Evidence |
| --- | --- | --- |
| “Sort by download count” option | `BrowseScreen.chooseSort`, `SortOrder.MOST_DOWNLOADED` | `sortByDownloadCountShowsOrderInHeader` |
| Descending by default | comparator reversed | `sortByDownloadsIsDescendingWithNewestFirstOnTies` |
| Ties → upload date newest first | `thenComparing(newest)` | same test |
| Current sort order visible in header | `BrowseScreen.list` “Sorted by: …” | `sortByDownloadCountShowsOrderInHeader` |
| Available to guests and members | no login check | `guestSeesLoginRequiredAndCannotDownload` (guest browses) |

## Additional policies to confirm with the tutor

- Phone numbers may contain only digits, spaces and `+ - ( ) .`.
- Admins cannot edit or delete other users' scrolls (strict reading of the specification).
- Replacing a scroll's file counts as an upload in the statistics.
- Download destinations that are folders are rejected; existing files are overwritten (Sprint 1 behaviour).

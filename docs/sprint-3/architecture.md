<!-- 主负责人：Yuhan（界面集成，整理架构文档）；Henry、Stella、Sixian 核对各自模块。 -->
# Sprint 3 architecture and ownership

## Goal and boundaries

Add a personal profile picture to each account. A logged-in user uploads a JPG/PNG from "My profile"
(validated by both extension and real content), the picture is stored with the account (Base64),
and the user can preview it exactly like an image scroll and open it in the default application.
Two stories, 13 points. The design keeps the Sprint 1/2 layering and adds **new service and screen
classes per feature** so that four people work in parallel with almost no shared-file edits. The only
shared files touched are `User`/`FileUserRepository` (Henry), `ScrollPreviewRenderer` (Sixian, same
original author), and the wiring files `AccountScreens`/`ConsoleUi`/`App` (Yuhan).

```mermaid
flowchart TD
    App[App: composition root] --> UI[ConsoleUi: role menus + wiring]
    UI --> AS[AccountScreens - Henry page, Yuhan wires picture]
    AS --> PS[ProfilePictureScreen - Yuhan]
    PS --> Pic[ProfilePictureService - Stella]
    PS --> Rend[ProfilePictureRenderer - Sixian]
    PS --> View[ProfilePictureViewer - Sixian]
    Pic --> Accounts[AccountService]
    Pic --> Users[UserRepository]
    Pic --> Format[FileFormat]
    Rend --> Preview[ScrollPreviewRenderer.appendImageDetails]
    View --> Accounts
    View --> Opener[FileOpener]
    Users -. implemented by .-> FileUsers[FileUserRepository: Base64 picture]
    Accounts --> Users
```

- `model`: `User` stays immutable; two optional fields (`profilePicture`, `profilePictureFormat`) default to
  "no picture". The 7-arg Sprint 1 constructor delegates to the 9-arg one, so no Sprint 1/2 code changes.
- `repository`: `FileUserRepository` stores the picture as Base64 inside the user snapshot; snapshots without the
  `picture`/`picformat` keys load as "no picture" (backward compatible).
- `service`: all rules live here. `ProfilePictureService` validates (extension + content magic number + size +
  login) and saves; `ProfilePictureRenderer` reuses the image preview; `ProfilePictureViewer` reuses `FileOpener`.
- `ui`: `ProfilePictureScreen` only collects input and prints results; it is reached from `AccountScreens.profile`.

## Class diagram (Sprint 3 additions)

```mermaid
classDiagram
    class User { +hasProfilePicture() +getProfilePicture() +getProfilePictureFormat() +withProfilePicture() }
    class FileUserRepository { -decodePicture() +save() }
    class ProfilePictureService { +checkSource()$ +setProfilePicture() +MAX_BYTES$ }
    class ProfilePictureRenderer { +render(user)$ }
    class ProfilePictureViewer { +open() }
    class ScrollPreviewRenderer { ~appendImageDetails()$ +render(scroll)$ }
    class FileOpener { <<interface>> +open() +desktop()$ +unavailable()$ }
    class ProfilePictureScreen { +show() -upload() -preview() -openInApp() }
    class AccountScreens { +profile() -showProfile() }
    ProfilePictureScreen --> ProfilePictureService
    ProfilePictureScreen --> ProfilePictureRenderer
    ProfilePictureScreen --> ProfilePictureViewer
    ProfilePictureService --> User
    ProfilePictureService --> FileUserRepository
    ProfilePictureRenderer --> ScrollPreviewRenderer
    ProfilePictureViewer --> FileOpener
    AccountScreens --> ProfilePictureScreen
```

## Responsibility mapping

| Owner | Story (part) | Implementation responsibility |
| --- | --- | --- |
| Henry (SM) | US19 #67 — model/storage (~4 pts) | `User` picture fields + `withProfilePicture`; `FileUserRepository` Base64 persistence and backward compatibility; CI gate |
| Stella (PO) | US19 #67 — validation (~4 pts) | `ProfilePictureService`: `checkSource`, `setProfilePicture` (extension + content + size + login + replace); README |
| Sixian | US20 #68 — preview/open (~5 pts) | `ProfilePictureRenderer`, `ProfilePictureViewer`; extract `ScrollPreviewRenderer.appendImageDetails` |
| Yuhan | US19 + US20 — UI (~5 pts) | `ProfilePictureScreen`; wire into `AccountScreens`, `ConsoleUi`, `App` |

Owner comments are assignments for maintenance and explanation, not authorship claims.

## Representative sequence: upload then preview a profile picture (US19 + US20)

```mermaid
sequenceDiagram
    actor User
    participant UI as ProfilePictureScreen
    participant P as ProfilePictureService
    participant A as AccountService
    participant R as UserRepository
    participant F as FileFormat
    participant RN as ProfilePictureRenderer
    User->>UI: 1 (Upload) + picture path
    UI->>P: checkSource(path) (extension, size)
    P-->>UI: validated Path
    UI->>P: setProfilePicture(path)
    P->>A: requireUser()
    A-->>P: current user or "Please log in first"
    P->>F: detect(null, bytes)  (content magic number)
    F-->>P: "png" / "jpg" or reject fake image
    P->>R: save(user.withProfilePicture(bytes, "png"))
    P-->>UI: updated user
    User->>UI: 2 (Preview)
    UI->>RN: render(user)
    RN-->>UI: Format / Size / Image size: W x H pixels
```

## Decisions and practical limits

1. **Optional, backward-compatible model.** The picture is an optional field; the Sprint 1 constructor still works,
   and snapshots written before Sprint 3 load as "no picture". No Sprint 1/2 behaviour changes.
2. **Validate by extension AND content.** `checkSource` checks the extension (jpg/jpeg/png) and size early;
   `setProfilePicture` then confirms the bytes are a real PNG/JPG via `FileFormat.detect`, so a renamed `.txt` is rejected.
3. **5 MiB limit.** Avatars are small, so the limit is half the 10 MiB scroll limit.
4. **Reuse, don't duplicate.** Preview reuses the image rendering (`appendImageDetails`) and opening reuses `FileOpener`,
   keeping the avatar experience identical to image scrolls and avoiding new preview/open logic.
5. **No counters touched.** Previewing or opening a picture never saves a scroll or account, so no download/upload
   count changes. Guests cannot upload or preview (service-layer `requireUser`).
6. **New files per feature.** Service and screen are new classes; the only shared edits are the account model/storage
   (Henry), the extracted image helper (Sixian, same author) and the wiring files (Yuhan).

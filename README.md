# Virtual Scroll Access System (VSAS)
## Team

SOFT2412/COMP9412 — Tutorial T16, Group 2

Team members:

- Stella Xu（Product Owner）
- Henry Wang（Scrum Master）
- Yuhan Pan
- Sixian Hu

## 1. Project Overview

The Virtual Scroll Access System (VSAS) is a Java-based console application
developed for SOFT2412/COMP9412 Assignment 2/3.

The system allows users to register accounts, authenticate securely,
upload and browse digital scrolls, preview scroll information,
and download binary files.

Administrators can manage user accounts, including viewing, creating,
and deleting users.

The project follows Agile Scrum practices and uses GitHub for
collaborative development and Jenkins for continuous integration.

## 2. Sprint 1 Features

The following user stories are implemented in Sprint 1:

| User Story | Feature | Description |
|------------|---------|-------------|
| US01 | User Registration | Register a new member account with profile information. |
| US02 | User Login | Authenticate using an ID key and password. |
| US05 | Browse Scrolls | Browse the list of available digital scrolls. |
| US07 | Preview Scroll | View scroll metadata before downloading. |
| US08 | Download Scroll | Download binary scroll files and update download counts. |
| US09 | Upload Scroll | Upload binary scroll files as an authenticated user. |
| US11 | Admin Management | Allow administrators to view, create, and delete user accounts. |

### Authentication and Authorization

- Passwords are securely hashed using PBKDF2-HMAC-SHA256.
- Users log in using their ID key and password.
- Invalid credentials are rejected with a consistent error message.
- User sessions remain active until logout.
- Role-based access control distinguishes guests, members, and administrators.
- Only administrators can manage user accounts.
- Guests cannot upload or download scrolls.

### Data Persistence

- User accounts are stored in a file-based repository.
- Digital scroll metadata and binary content are stored persistently.
- Account and scroll data remain available after application restart.
- Download counts are updated after successful downloads.

## 2a. Sprint 2 Features

| User Story | Issue | Feature | Owner |
|------------|-------|---------|-------|
| US03 | #4 | Update profile (name, email, phone) and change password | Henry |
| US06 | #7 | Filter scrolls by uploader ID, scroll ID, name and upload date | Sixian |
| US10 | #11 | Rename, replace and delete your own scrolls (My scrolls) | Stella |
| US12 | #13 | Admin creates MEMBER/ADMIN users and views per-scroll upload/download statistics | Stella |
| US13 | #48 | Numbered menus for guest, member and admin | Yuhan |
| US14 | #49 | Immediate, consistent `Error: ...` feedback; prompts are re-asked | Yuhan |
| US15 | #50 | Passwords hidden while typing (with a warning where the terminal cannot hide input) | Yuhan |
| US16 | #51 | Email (case-insensitive) and phone numbers must be unique | Henry |
| US17 | #52 | Preview by file format (text lines, image size, PDF info, ZIP entries, hex); correct extension on download | Sixian |
| US18 | #53 | Sort scrolls by download count (most downloaded first, ties newest first) | Sixian |

## 3. Requirements

- Java Development Kit (JDK) 21
- Git
- macOS, Linux, or Windows
- Gradle Wrapper (included in the repository)

A separate Gradle installation is not required.

## 4. Build and Run

### Clone the Repository

Clone the project repository and navigate to the project directory.

### Build the Application

On macOS or Linux:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

### Run the Application

Quick start through Gradle (password input is **visible** here, because Gradle
does not give the program a real terminal; a warning is printed):

```bash
./gradlew --no-configuration-cache --console=plain :app:run
```

Recommended for demos — run the installed start script, which uses the real
terminal so passwords are hidden while typing (US15):

```bash
./gradlew :app:installDist
app/build/install/app/bin/app            # macOS / Linux
app\build\install\app\bin\app.bat       # Windows
```

The start script needs a Java 21 runtime on `PATH` or in `JAVA_HOME`.
An optional argument selects the data directory, for example
`app/build/install/app/bin/app demo-data`.

By default, application data is stored in:

```text
data/
```

The directory contains persistent user and scroll data.

## 5. Using the Application

Every menu is numbered. Type the number and press Enter; `0` always quits or
goes back. Shared options keep the same number for every role:

| Guest | Member | Admin |
|-------|--------|-------|
| 1. Browse scrolls | 1. Browse scrolls | 1. Browse scrolls |
| 2. Register | 2. Upload a scroll | 2. Upload a scroll |
| 3. Log in | 3. My scrolls (edit / delete) | 3. My scrolls (edit / delete) |
| | 4. My profile | 4. My profile |
| | 5. Log out | 5. Log out |
| | | 6. View all users |
| | | 7. Add a user |
| | | 8. Delete a user |
| | | 9. Scroll statistics |
| 0. Quit | 0. Quit | 0. Quit |

The Sprint 1 command words (`browse`, `login`, `upload`, `preview s1`, ...)
are still accepted as shortcuts.

### First-Time Setup

When the application starts with an empty user repository,
the user is prompted to create the initial administrator account.

The administrator provides:

- ID key
- Username
- Password
- Full name
- Email
- Phone number

The initial administrator setup is only available when the
user repository is empty.

### User Registration

Guests can register new member accounts by providing
their personal details.

Newly registered users receive the MEMBER role.

### Login and Logout

Users authenticate using their ID key and password.

After successful login, the console displays the user's
name and role.

Users can log out to return to guest mode.

### Scroll Management

Authenticated users can upload binary scroll files. The file format is
detected from the file extension (or the file contents) and stored.

The browse screen (all users) lists every scroll and offers: preview,
download, filter (uploader ID, scroll ID, name, upload date `yyyy-MM-dd`),
sort (upload order, most downloaded first, newest first) and clear filters.
The list header always shows the active sort order and filters.

Previews show metadata plus format-specific content: the first lines of text
files, image dimensions, PDF version and page count, ZIP entries, or a hex
dump for other files.

Logged-in users can also choose **Open in default app** on the preview screen to view
PDFs, images (png, jpg, gif, bmp), text files (txt, md, csv) and Word documents (docx)
in the computer's own application, such as Preview or Word. Other types (for example mp3
or mp4) show the information preview only. Opening does not count as a download, and it
needs a computer with a desktop (it is unavailable over SSH or in Docker).

Logged-in users can download from the list or the preview screen. If the
destination has no matching extension, the correct one is added (for example
`report` becomes `report.pdf`). Successful downloads update the download count.

**My scrolls** lets uploaders rename, replace the file of, or delete (after
confirmation) only the scrolls they uploaded.

### My Profile

Logged-in users can view their profile, change their full name, email and
phone (press Enter to keep a value), and change their password (current
password required, new password entered twice). The ID key cannot be changed.
Emails and phone numbers must be unique across all accounts.

### Administrator Operations

Administrators can:

- View all registered user accounts.
- Create new MEMBER or ADMIN accounts (role `1` = MEMBER, `2` = ADMIN).
- Delete existing user accounts.
- View upload and download counts for every scroll, with totals.

Administrator operations are protected by role-based
access control.

## 6. Testing

The project uses JUnit 5 for automated testing.

Run all tests:

```bash
./gradlew clean test
```

Run a specific test class:

```bash
./gradlew :app:test --tests "org.example.AppTest"
```

### Code Coverage

JaCoCo is used to measure automated test coverage.

Generate the coverage report:

```bash
./gradlew :app:jacocoTestReport
```

The HTML report is generated at:

```text
app/build/reports/jacoco/test/html/index.html
```

Coverage is enforced: `./gradlew build` (and the Jenkins `Coverage` stage)
fails if overall line coverage drops below 75%.

Sprint 2 local results (`./gradlew clean build`, 2026-10-15, before team review —
re-run and update at the end of the sprint):

| Metric | Coverage |
|--------|----------|
| Tests | 211 passed, 0 failed |
| Line Coverage | 97% |
| Branch Coverage | 92% |

### Code Quality

Checkstyle is used to enforce Java coding conventions.

Run:

```bash
./gradlew :app:checkstyleMain :app:checkstyleTest
```

Checkstyle reports are generated under:

```text
app/build/reports/checkstyle/
```

## 7. Project Structure

```text
app/
├── src/
│   ├── main/
│   │   └── java/org/example/
│   │       ├── App.java
│   │       ├── model/
│   │       ├── repository/
│   │       │   └── file/
│   │       ├── security/
│   │       ├── service/
│   │       └── ui/
│   └── test/
│       └── java/org/example/
└── build.gradle
```

### Main Components

| Component | Responsibility |
|-----------|----------------|
| App | Initializes repositories, services, and the console UI. |
| Model | Defines users, roles, and digital scrolls. |
| Repository | Provides data access and persistence. |
| Security | Provides secure password hashing and verification. |
| AccountService | Handles registration, login, sessions, and admin operations. |
| ScrollService | Handles scroll browsing, uploading, and downloading. |
| ScrollEditService | Rename, replace and delete the current user's own scrolls. |
| ScrollQueryService | Filters and sorts the scroll list. |
| StatisticsService | Admin-only per-scroll upload/download statistics. |
| FileFormat / ScrollPreviewRenderer | Detects file formats and renders format-aware previews. |
| ConsoleUi | Builds the numbered, role-aware main menu and connects the screens. |
| ConsoleIO / Menu | Shared input, error formatting, masked passwords and numbered menus. |
| AccountScreens / BrowseScreen / MyScrollsScreen / StatisticsScreen | One console screen class per feature area. |

## 8. Continuous Integration

Jenkins is used for continuous integration.

The CI pipeline is defined in:

```text
Jenkinsfile
```

The pipeline is used to validate project changes through
automated build and testing steps.

GitHub pull requests are used for collaborative code review
before changes are merged into the main branch.

## 9. Development Workflow

The project follows Scrum practices.

Development activities include:

- Sprint planning and backlog management.
- User stories with acceptance criteria.
- GitHub Issues for implementation tasks.
- Feature branches for parallel development.
- Pull requests and peer code reviews.
- Automated unit testing and continuous integration.
- Sprint review and retrospective.

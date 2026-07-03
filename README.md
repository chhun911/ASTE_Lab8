# Private Cloud Storage App — Lab 08

A small **Spring Boot** backend where each user manages files and folders inside
their **own** isolated cloud storage. New users are granted a **50 MB** quota,
can manage their profile, and can delete their own account together with all of
their data.

The project ships with a test suite that demonstrates **all ten testing
methods** from Lesson 08, driven by **JUnit 5 / AssertJ** and **Playwright for
Java**, and reported with **Allure**.

---

## Tech stack

| Concern                    | Choice                                          |
|----------------------------|-------------------------------------------------|
| Language                   | Java 17                                         |
| Framework                  | Spring Boot 3.2 (Web, Data JPA, Validation)     |
| Database                   | H2 (in-memory)                                  |
| UI                         | Thymeleaf (server-rendered)                     |
| Unit / integration tests   | JUnit 5 + AssertJ + Hamcrest                    |
| HTTP + browser tests       | Playwright for Java                             |
| Reporting                  | Allure                                          |
| Build                      | Maven (with `./mvnw` wrapper — no local install)|

---

## Functional requirements covered

| #  | Capability   | Where                                                        |
|----|--------------|--------------------------------------------------------------|
| R1 | Register     | `POST /api/auth/register` — new user gets 50 MB              |
| R2 | Authenticate | `POST /api/auth/login` — bearer token per request           |
| R3 | Manage profile | `GET/PUT /api/me` — view / update display name & password |
| R4 | Delete account | `DELETE /api/me` — removes the user and all their data    |
| R5 | Folders      | create / list / rename / **move** / delete                  |
| R6 | Files        | upload / download / list / rename / **move** / delete       |
| R7 | Quota        | over-quota uploads are rejected (`413`)                      |
| R8 | Isolation    | a user can never see or touch another user's files          |

---

## Project layout

```
private-cloud-storage/
├── src/main/java/edu/itc/cloud/
│   ├── PrivateCloudStorageApplication.java
│   ├── model/        # User, Folder, FileEntity
│   ├── repository/   # Spring Data JPA repositories
│   ├── service/      # UserService, StorageService, domain exceptions
│   └── web/          # REST controllers, AuthService, DTOs, error handler
│       └── ui/       # WebUiController (Thymeleaf, session-based)
├── src/main/resources/
│   ├── application.properties
│   └── templates/    # login.html, dashboard.html
├── src/test/java/edu/itc/cloud/
│   ├── TestingMethodsServiceTest.java   # methods 1–8 + move + isolation + deletion
│   ├── CloudApiPlaywrightTest.java      # schema / regex / contains / exception over HTTP
│   ├── CloudUiPlaywrightTest.java       # browser UI test + screenshot snapshot
│   └── support/TestFiles.java
├── .github/workflows/ci.yml             # build + test + Allure report
├── .mvn/ · mvnw · mvnw.cmd              # Maven wrapper
├── pom.xml
└── README.md
```

---

## Run the app

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# Web UI     http://localhost:8080/            (register, then manage files/folders)
# REST API   http://localhost:8080/api/...
# H2 console http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:cloud)
```

Open <http://localhost:8080/>, click **Register** (you get 50 MB), then create
folders and upload files from the dashboard.

### Try the API with curl

```bash
# Register (returns a token) — new user gets 50 MB
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"sok@itc.edu","password":"Secret123!"}'

TOKEN=...   # paste the token from the response

# Profile + quota usage
curl -s http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"

# Create a folder
curl -s -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Documents"}'

# Upload a file (multipart)
curl -s -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN" -F "file=@./photo.png"
```

---

## Run the tests

```bash
./mvnw test
```

> The browser-driven `CloudUiPlaywrightTest` downloads a Chromium browser on the
> first run, so the first build needs internet access. Later runs are offline.
> To install the browser explicitly:
>
> ```bash
> ./mvnw exec:java -Dexec.classpathScope=test \
>   -Dexec.mainClass=com.microsoft.playwright.CLI \
>   -Dexec.args="install --with-deps chromium"
> ```

---

## View the Allure report

```bash
# Option A — Maven plugin (no separate CLI needed)
./mvnw allure:serve
# or build static HTML into target/site/allure-maven-plugin
./mvnw allure:report

# Option B — Allure CLI (https://allurereport.org)
allure serve target/allure-results
```

---

## Testing methods → where they are demonstrated

All ten methods from Lesson 08 are exercised. Tests are grouped by `@Feature`
in the Allure report so each method is easy to find.

| #  | Testing method     | Demonstrated in                                                                                     |
|----|--------------------|-----------------------------------------------------------------------------------------------------|
| 1  | Content equals     | `TestingMethodsServiceTest.newUserQuotaEqualsFiftyMb` · `CloudApiPlaywrightTest.profileContract`    |
| 2  | Contains           | `TestingMethodsServiceTest.listingContainsUploadedName` · `CloudApiPlaywrightTest.folderListingContainsNewFolder` |
| 3  | Regex matched      | `TestingMethodsServiceTest.shareLinkAndEmailMatchPatterns` · `CloudApiPlaywrightTest.profileContract` · `CloudUiPlaywrightTest` (URL) |
| 4  | Formula matched    | `TestingMethodsServiceTest.freeSpaceFollowsFormula` · `moveFileBetweenFoldersKeepsUsage`            |
| 5  | Predicate          | `TestingMethodsServiceTest.usageNeverExceedsQuota`                                                  |
| 6  | Collection         | `TestingMethodsServiceTest.collectionShapeIsExact` · `moveFileBetweenFoldersKeepsUsage`             |
| 7  | Exception          | `TestingMethodsServiceTest.overQuotaThrows` · `CloudApiPlaywrightTest.unauthenticatedIsForbidden`   |
| 8  | Tolerance / range  | `TestingMethodsServiceTest.toleranceAndRange`                                                       |
| 9  | Schema / JSON      | `CloudApiPlaywrightTest.profileContract`                                                            |
| 10 | Visual / snapshot  | `CloudUiPlaywrightTest.registerThroughUiAndSeeQuota` (browser screenshot attached to Allure)        |

**Bonus coverage:** per-user **isolation** (`usersAreIsolated`), **account
deletion** (`deleteAccountWipesData`) and **move** (`moveFileBetweenFoldersKeepsUsage`).

---

## Design notes

- **Isolation** is enforced in `StorageService`: every folder/file operation
  loads the resource and checks `ownerId` against the acting user, throwing
  `AccessDeniedException` (HTTP `403`) otherwise.
- **Quota** is tracked as `usedBytes` on the `User` and kept in sync on every
  upload/delete, so `free = quota − Σ(file sizes)` stays a testable formula.
- The in-memory bearer-token store (`web/AuthService`) and the password hash in
  `UserService` are **deliberately minimal** for the lab — in production swap in
  Spring Security + JWT and BCrypt.
- The H2 database is in-memory and resets on restart; file bytes are stored in
  the DB as a BLOB to keep the project self-contained.

---

## Submission checklist

- [x] Spring Boot app: register (50 MB), files & folders, profile, self-delete
- [x] Per-user isolation enforced everywhere
- [x] JUnit + Playwright tests covering **all ten** testing methods
- [x] Allure report with `@Feature` grouping, steps & a screenshot attachment
- [x] README with run instructions + method-mapping table
- [ ] Push to a **public** GitHub repo and submit the link to Moodle

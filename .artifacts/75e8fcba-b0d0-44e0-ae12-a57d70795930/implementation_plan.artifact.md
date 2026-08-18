# Stability Checks and Storage Enforcement Plan

Perform final storage enforcement, stability improvements using Zip4j, and permission handling for ZPower.

## User Review Required

> [!IMPORTANT]
> This update migrates ZIP operations from standard `java.util.zip` to `net.lingala.zip4j`. This library handles large files and specific ZIP headers more reliably on Android.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/gradle/libs.versions.toml)
- Add `zip4j` version and library definition.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/build.gradle.kts)
- Add `zip4j` dependency.

---

### Data Layer

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- **Storage Enforcement**: Strictly use `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)` for the "Gold Knowledge" folder.
- **Directory Creation**: Ensure `data/` and `images/` subfolders are created in `ensureStorageDirectories`.
- **Zip4j Migration**:
    - Replace `ZipOutputStream` and `ZipInputStream` with `net.lingala.zip4j.ZipFile`.
    - Implement stream-based operations for Zip4j to handle `ContentResolver` URIs safely.
    - Wrap all file/zip operations in `withContext(Dispatchers.IO)`.

---

### UI Layer

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/MainActivity.kt)
- **Permissions**: Refine `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` logic to ensure robust handling on Android 11+.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify dependencies.
- Build the app: `./gradlew :app:assembleDebug`.

### Manual Verification
- Verify the "Gold Knowledge" folder is created in Documents.
- Test export and import of backups to ensure Zip4j works correctly.
- Verify that the app requests "All Files Access" if not granted.

# Robust Data Sync and Image Loading Plan

Update ZPower to handle manual data swaps in `Documents/Gold Knowledge/` more robustly, ensure consistent image loading from filenames, and implement background sync.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/SettingsRepository.kt)
- Add `LAST_DB_UPDATE_TIME` to Preferences DataStore to track when the internal Room database was last modified.
- Add `updateLastDbUpdateTime(time: Long)` function.

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- In the `init` block's invalidation tracker, call `settingsRepository.updateLastDbUpdateTime(System.currentTimeMillis())`.
- Update `syncDatabaseToJson` to compare `LAST_DB_UPDATE_TIME` with `dataFile.lastModified()`. Only export if DB is newer.
- Ensure `isSyncing` flag prevents `syncDataWithJson` and `syncDatabaseToJson` from triggering each other in an infinite loop.
- Add `forceRefreshData(context: Context)` that ignores timestamps and performs a full import from `data.json`.

### [Logic Layer]

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Add `refreshData()` function that calls `repository.forceRefreshData()`.
- Add `syncExternalChanges()` function for use in `onResume`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/MainActivity.kt)
- Add `onResume` override.
- Access the `NavigationViewModel` (it's scoped to the activity/content) and call `syncExternalChanges()`.

### [UI Layer]

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Add a "Refresh Data" icon/button in the `TopAppBar` (specifically in the actions section, maybe near the Search or Edit icons).

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Refine image loading logic to ensure that if `imagePath` is a simple filename, it's correctly resolved using `repository.getLocalImageFile`. (The current code already does this, but I will double-check the fallback behavior).

## Verification Plan

### Automated Tests
- Build the project: `./gradlew assembleDebug`

### Manual Verification
1. **Manual JSON Swap**:
   - Close the app.
   - Manually edit `Documents/Gold Knowledge/data/data.json`.
   - Open the app. Verify changes are reflected immediately (via `syncDataWithJson` in `init` or `onResume`).
2. **Manual Image Swap**:
   - Drop a new image into `Documents/Gold Knowledge/images/`.
   - Update `data.json` to reference this filename.
   - Verify the app displays the new image.
3. **Safety Check**:
   - Make a change in the app (e.g., edit a Room name).
   - Verify `data.json` is updated (it should be because DB is newer).
   - Manually replace `data.json` with an older version.
   - Verify the app imports the older version (because file time > last sync time).
4. **Refresh Button**:
   - Use the "Refresh Data" button and verify it forces an import.

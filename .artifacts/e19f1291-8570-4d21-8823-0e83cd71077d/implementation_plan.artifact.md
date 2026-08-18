# Implementation Plan - ZPower Refinement

Refine ZPower with recursive hierarchy, UI enhancements, and search upgrades.

## Proposed Changes

### Data Layer

#### [MODIFY] [Entities.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/entity/Entities.kt)
- Update `SubProcess` entity to support recursion:
  - Replace `childProcessId` with `parentId: Long` and `parentType: String`.
  - Add `@Ignore var subProcesses: List<SubProcess>` to `SubProcess` for JSON mirroring.

#### [MODIFY] [ZPowerDao.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/dao/ZPowerDao.kt)
- Update `SubProcessDao` with `getSubProcessesByParent(parentId: Long, parentType: String)`.
- Update `SearchDao` to include image path and search path in results.

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/SettingsRepository.kt)
- Add `SEARCH_PATH_COLOR` preference.

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Update `exportDatabaseToJson` and `importDatabaseFromJson` to handle recursive `SubProcess` structure.
- Add recursive helper functions for JSON sync.

### UI Layer

#### [MODIFY] [ZPowerTopBar.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ZPowerTopBar.kt)
- Make the search bar more rounded (`RoundedCornerShape(32.dp)`).

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Add "Search Path Color" setting with a grid of colors.

#### [MODIFY] [SearchResults.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/SearchResults.kt)
- Update `SearchResultItem` to show image thumbnail.
- Display hierarchical path using the custom "Search Path Color".
- Truncate path with ellipsis.

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Increase height of the text bar section in `SquareGlassCard`.

### Navigation

#### [MODIFY] [Routes.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/Routes.kt)
- Update `SubProcessList` route to support recursive navigation (passing `parentId` and `parentType`).

#### [MODIFY] [SubProcessListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SubProcessListScreen.kt)
- Update to fetch sub-processes based on `parentId` and `parentType`.
- Handle navigation into nested sub-processes.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.

### Manual Verification
- Verify search bar is rounded.
- Verify "Search Path Color" setting works and updates search results.
- Verify infinite recursion of sub-processes by adding nested levels and navigating.
- Verify JSON export/import preserves recursive structure.
- Verify `SquareGlassCard` text section height.
